// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.FieldError
import java.time.Clock
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Everything the user typed in the Create announcement form, plus the rules that decide whether it
 * can be turned into an [Announcement]. It is a plain immutable value, so these rules are testable
 * without any UI.
 *
 * The content is required, like the title, because story #218 lists both as required fields. The
 * announcement's [Announcement.updatedAt] is the time of the first "Publish" tap, kept across
 * retries, so that a retry neither duplicates the announcement nor moves it in a feed sorted by
 * time.
 *
 * @property title The title as typed, trimmed when the announcement is built.
 * @property content The body of the announcement as typed, trimmed when it is built.
 * @property priority How urgent the announcement is. [AnnouncementPriority.INFO] by default.
 * @property titleEdited Whether the title was edited, so that an empty one is reported.
 * @property contentEdited Whether the content was edited, so that an empty one is reported.
 * @property isSaving True while the announcement is being saved. It keeps a second tap on "Publish"
 *   from saving the announcement twice (see [canPublish]).
 * @property isPublished True once the announcement is stored. The form is final from then on.
 * @property errorMsg String resource of the error to show (see
 *   `R.string.create_announcement_error_save_failed`), or `null`.
 */
data class CreateAnnouncementUiState(
    val title: String = "",
    val content: String = "",
    val priority: AnnouncementPriority = AnnouncementPriority.INFO,
    val titleEdited: Boolean = false,
    val contentEdited: Boolean = false,
    val isSaving: Boolean = false,
    val isPublished: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /**
   * The title error. It only appears once the title has been edited, so that an untouched form does
   * not start with an error.
   */
  val titleError: FieldError?
    get() = if (titleEdited && title.isBlank()) FieldError.EMPTY_ANNOUNCEMENT_TITLE else null

  /** The content error. Like [titleError], it only appears once the content has been edited. */
  val contentError: FieldError?
    get() = if (contentEdited && content.isBlank()) FieldError.EMPTY_ANNOUNCEMENT_CONTENT else null

  /**
   * Whether the user changed anything in the form, so that leaving it would lose something. Keep it
   * in sync with the form fields: a field missing here is lost without asking.
   */
  val hasChanges: Boolean
    get() = title.isNotEmpty() || content.isNotEmpty() || priority != AnnouncementPriority.INFO

  /** Whether going back must ask first: a published announcement has nothing left to lose. */
  val needsDiscardConfirmation: Boolean
    get() = hasChanges && !isPublished

  /**
   * Whether the "Publish" button can be pressed. It is false while a save is running and once the
   * announcement is published, so that a tap just after the save cannot publish a second one.
   */
  val canPublish: Boolean
    get() = isValid && !isSaving && !isPublished

  private val isValid: Boolean
    get() = title.isNotBlank() && content.isNotBlank()

  /**
   * Builds the [Announcement] described by this form, with its title and content trimmed.
   *
   * @param announcementId The id of the new announcement, kept across retries.
   * @param authorId The uid of the user publishing the announcement, who becomes its author.
   * @param updatedAt The time the announcement is published.
   * @throws IllegalArgumentException if the title or the content is blank, or if [announcementId]
   *   or [authorId] is blank.
   */
  fun toAnnouncement(
      announcementId: String,
      authorId: String,
      updatedAt: ZonedDateTime,
  ): Announcement {
    require(isValid) { "The form is not valid." }
    return Announcement(
        announcementId = announcementId,
        title = title.trim(),
        authorId = authorId,
        content = content.trim(),
        priority = priority,
        updatedAt = updatedAt,
    )
  }
}

private const val TAG = "CreateAnnouncementViewModel"

private const val SAVE_TIMEOUT_MS = 15_000L

/** The message shown when the announcement could not be saved, whatever the cause. */
@StringRes private val SAVE_FAILED_MESSAGE = R.string.create_announcement_error_save_failed

/**
 * Holds the state of the Create announcement screen and adds the new announcement to the event in
 * [eventsRepository].
 *
 * @param eventId The event the announcement is published in. It cannot be blank.
 * @param currentUserId Gives the uid of the signed-in user, who becomes the author. It is asked
 *   when the user taps "Publish", not when the screen opens, so a sign-out or an account change in
 *   between is taken into account. A blank uid saves nothing and shows the save error.
 * @param eventsRepository Where the announcement is saved. It is the app's repository by default;
 *   the tests give an in-memory one.
 * @param clock Gives the publication time. The device's clock by default.
 * @param saveTimeoutMs How long a save may last before it is reported as failed, so that the form
 *   never stays stuck in the saving state. A failed save is followed by a read of the event under
 *   the same timeout, so the saving state lasts at most twice this long.
 */
class CreateAnnouncementViewModel(
    private val eventId: String,
    private val currentUserId: () -> String,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.repository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
) : ViewModel() {
  // The id and time of the announcement being published. They are kept across retries: if a save
  // timed out or failed after reaching the server, the retry targets the same announcement.
  private var pendingId: String? = null
  private var pendingUpdatedAt: ZonedDateTime? = null

  init {
    // An Announcement needs an event: a blank one is a mistake of the caller.
    require(eventId.isNotBlank()) { "eventId must not be blank" }
  }

  private val _uiState = MutableStateFlow(CreateAnnouncementUiState())
  /** The state of the form, read by the screen. */
  val uiState: StateFlow<CreateAnnouncementUiState> = _uiState.asStateFlow()

  // Once the announcement is published the form is final: later edits are ignored.
  private fun edit(change: (CreateAnnouncementUiState) -> CreateAnnouncementUiState) =
      _uiState.update {
        if (it.isPublished) it else change(it)
      }

  /** Sets the title and marks it as edited, so that an empty title is reported. */
  fun updateTitle(title: String) = edit { it.copy(title = title, titleEdited = true) }

  /** Sets the content and marks it as edited, so that an empty content is reported. */
  fun updateContent(content: String) = edit { it.copy(content = content, contentEdited = true) }

  /** Sets the priority. */
  fun updatePriority(priority: AnnouncementPriority) = edit { it.copy(priority = priority) }

  /** Publishes the announcement if the form is valid. Does nothing while a save is running. */
  fun publish() {
    val form = _uiState.value
    if (!form.canPublish) return
    val authorId = currentUserId()
    if (authorId.isBlank()) {
      // Nobody is signed in: an announcement without an author cannot be saved, so save nothing
      // and show the save error.
      Log.e(TAG, "publish called without a signed-in user")
      _uiState.update { it.copy(errorMsg = SAVE_FAILED_MESSAGE) }
      return
    }
    // Built from the copy of the form taken above, outside the try: the form was just checked, so a
    // failure here is a bug and must not be shown to the user as a failed save.
    val id = pendingId ?: eventsRepository.getNewUid()
    val updatedAt = pendingUpdatedAt ?: ZonedDateTime.now(clock)
    pendingId = id
    pendingUpdatedAt = updatedAt
    val announcement = form.toAnnouncement(id, authorId, updatedAt)
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        withTimeout(saveTimeoutMs) { eventsRepository.addAnnouncement(eventId, announcement) }
        _uiState.update { it.copy(isSaving = false, isPublished = true) }
      } catch (e: TimeoutCancellationException) {
        onPublishFailed(announcement, e)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isSaving = false) }
        throw e
      } catch (e: Exception) {
        onPublishFailed(announcement, e)
      }
    }
  }

  // Compares only the fields the form sets. Full equality would also compare the time zone of
  // updatedAt, the links and the pin, so a copy read back from a remote store would always differ.
  private fun Announcement.sameFormAs(other: Announcement): Boolean =
      title == other.title && content == other.content && priority == other.priority

  // A failed save may still have reached the server (for example a timeout). If the announcement
  // exists, it is published, and updated if the form changed since that save; otherwise the error
  // is shown and the user can retry with the same id and time.
  private suspend fun onPublishFailed(announcement: Announcement, e: Exception) {
    // Not an error yet: the announcement may have been stored anyway, which is checked below.
    Log.w(TAG, "Adding the announcement failed, checking whether it was stored", e)
    val alreadySaved =
        try {
          withTimeout(saveTimeoutMs) {
            val stored =
                eventsRepository.getEvent(eventId).announcements.firstOrNull {
                  it.announcementId == announcement.announcementId
                }
            if (stored != null && !stored.sameFormAs(announcement)) {
              // The stored author stays: a retry by another user never changes who published it.
              eventsRepository.editAnnouncement(
                  eventId,
                  announcement.copy(authorId = stored.authorId),
              )
            }
            stored != null
          }
        } catch (_: TimeoutCancellationException) {
          Log.e(TAG, "The announcement could not be checked in time, so it may not be stored")
          false
        } catch (e: CancellationException) {
          throw e
        } catch (checkError: Exception) {
          Log.e(TAG, "The announcement could not be checked, so it may not be stored", checkError)
          false
        }
    if (alreadySaved) Log.i(TAG, "The announcement was stored despite the failure")
    _uiState.update {
      if (alreadySaved) it.copy(isSaving = false, isPublished = true)
      else it.copy(isSaving = false, errorMsg = SAVE_FAILED_MESSAGE)
    }
  }
}
