// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.model.event.role.Permission
import com.android.festivar.ui.announcement.ANNOUNCEMENT_SAVE_TIMEOUT_MS
import com.android.festivar.ui.announcement.AnnouncementFormEditor
import com.android.festivar.ui.announcement.AnnouncementFormState
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
 * UI state of the Create announcement screen. The form is an [AnnouncementFormState], which holds
 * what the user typed and the field rules; this state adds the saving and publishing flags.
 *
 * The content is required, like the title, because story #218 lists both as required fields. The
 * announcement's [Announcement.updatedAt] is the time of the first "Publish" tap, kept across
 * retries, so that a retry neither duplicates the announcement nor moves it in a feed sorted by
 * time.
 *
 * @property form What the user typed, with the field rules of an announcement form.
 * @property isSaving True while the announcement is being saved. It keeps a second tap on "Publish"
 *   from saving the announcement twice (see [canPublish]).
 * @property isPublished True once the announcement is stored. The form is final from then on.
 * @property canCreateAnnouncement Whether the signed-in user's role in the event has
 *   [Permission.CAN_CREATE_ANNOUNCEMENT]. `true` until the event is loaded, so that the screen does
 *   not leave before the role is known or after a failed load.
 * @property errorMsg String resource of the error to show (see
 *   `R.string.create_announcement_error_save_failed`), or `null`.
 */
data class CreateAnnouncementUiState(
    val form: AnnouncementFormState = AnnouncementFormState(),
    val isSaving: Boolean = false,
    val isPublished: Boolean = false,
    val canCreateAnnouncement: Boolean = true,
    @StringRes val errorMsg: Int? = null,
) {
  /**
   * Whether the user changed anything in the form, so that leaving it would lose something. Keep it
   * in sync with the form fields: a field missing here is lost without asking.
   */
  val hasChanges: Boolean
    get() =
        form.title.isNotEmpty() ||
            form.content.isNotEmpty() ||
            form.priority != AnnouncementPriority.INFO

  /** Whether going back must ask first: a published announcement has nothing left to lose. */
  val needsDiscardConfirmation: Boolean
    get() = hasChanges && !isPublished

  /**
   * Whether the "Publish" button can be pressed. It is false while a save is running and once the
   * announcement is published, so that a tap just after the save cannot publish a second one, and
   * when the user's role does not allow creating an announcement.
   */
  val canPublish: Boolean
    get() = form.isValid && !isSaving && !isPublished && canCreateAnnouncement

  /**
   * Whether the inputs can be changed. They lock during a save and once the announcement is
   * published, when the form is final, so what is shown stays what was published.
   */
  val isEditable: Boolean
    get() = !isSaving && !isPublished
}

private const val TAG = "CreateAnnouncementViewModel"

/** The message shown when the announcement could not be saved, whatever the cause. */
@StringRes private val SAVE_FAILED_MESSAGE = R.string.create_announcement_error_save_failed

/**
 * Tells whether [userId] is a member of [event] whose role has
 * [Permission.CAN_CREATE_ANNOUNCEMENT]. A non-member (e.g. kicked out meanwhile) cannot, instead of
 * making [Event.roleOf] throw.
 */
private fun canCreateAnnouncementIn(event: Event, userId: String): Boolean =
    userId in event.roles && event.canAct(userId, Permission.CAN_CREATE_ANNOUNCEMENT)

/**
 * Holds the state of the Create announcement screen and adds the new announcement to the event in
 * the [EventsRepository] of [dataRepository].
 *
 * Only a member whose role has [Permission.CAN_CREATE_ANNOUNCEMENT] can publish. The role is read
 * when the screen opens, to fill [CreateAnnouncementUiState.canCreateAnnouncement], and again
 * before each save, since it may have changed in between.
 *
 * @param eventId The event the announcement is published in. It cannot be blank.
 * @param currentUserId Gives the uid of the signed-in user, who becomes the author and whose role
 *   is checked. It is asked again when the user taps "Publish", so a sign-out or an account change
 *   since the screen opened is taken into account. A blank uid saves nothing and shows the save
 *   error.
 * @param dataRepository Gives the [EventsRepository] where the announcement is saved.
 * @param clock Gives the publication time. The device's clock by default.
 * @param saveTimeoutMs How long a save, with the check of the role before it, may last before it is
 *   reported as failed, so that the form never stays stuck in the saving state. A failed save is
 *   followed by a read of the event under the same timeout, so the saving state lasts at most twice
 *   this long.
 */
class CreateAnnouncementViewModel(
    private val eventId: String,
    private val currentUserId: () -> String,
    private val dataRepository: DataRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val saveTimeoutMs: Long = ANNOUNCEMENT_SAVE_TIMEOUT_MS,
) : ViewModel(), AnnouncementFormEditor {

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository

  // The id and time of the announcement being published. They are kept across retries: if a save
  // timed out or failed after reaching the server, the retry targets the same announcement.
  private var pendingId: String? = null
  private var pendingUpdatedAt: ZonedDateTime? = null

  private val _uiState = MutableStateFlow(CreateAnnouncementUiState())
  /** The state of the form, read by the screen. */
  val uiState: StateFlow<CreateAnnouncementUiState> = _uiState.asStateFlow()

  init {
    // An Announcement needs an event: a blank one is a mistake of the caller.
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    // The role needs the loaded event, so it is read once the screen opens. After _uiState, which
    // loadPermission may update before the constructor returns.
    viewModelScope.launch { loadPermission() }
  }

  /**
   * Sets [CreateAnnouncementUiState.canCreateAnnouncement] from the role of the signed-in user in
   * the event. A failed load changes nothing, so the form stays open.
   */
  private suspend fun loadPermission() {
    val event =
        try {
          eventsRepository.getEvent(eventId)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "The event could not be loaded to check the user's role", e)
          return
        }
    val canCreate = canCreateAnnouncementIn(event, currentUserId())
    _uiState.update { it.copy(canCreateAnnouncement = canCreate) }
  }

  /** Once the announcement is published the form is final: later edits are ignored. */
  override fun applyEdit(change: (AnnouncementFormState) -> AnnouncementFormState) =
      _uiState.update {
        if (it.isPublished) it else it.copy(form = change(it.form))
      }

  /**
   * Publishes the announcement if the form is valid and the user's role allows it. Does nothing
   * while a save is running. A role that no longer allows it saves nothing, shows the save error
   * and sets [CreateAnnouncementUiState.canCreateAnnouncement] to `false`.
   */
  fun publish() {
    val state = _uiState.value
    if (!state.canPublish) return
    val form = state.form
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
        withTimeout(saveTimeoutMs) {
          // The role is checked again before writing: it may have changed since the screen
          // opened, and canCreateAnnouncement is still true while the first load runs.
          val event = eventsRepository.getEvent(eventId)
          if (canCreateAnnouncementIn(event, authorId)) {
            eventsRepository.addAnnouncement(eventId, announcement)
            _uiState.update { it.copy(isSaving = false, isPublished = true) }
          } else {
            Log.e(TAG, "publish called by a user whose role cannot create announcements")
            _uiState.update {
              it.copy(
                  isSaving = false,
                  canCreateAnnouncement = false,
                  errorMsg = SAVE_FAILED_MESSAGE,
              )
            }
          }
        }
      } catch (e: TimeoutCancellationException) {
        onPublishFailed(form, announcement, e)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isSaving = false) }
        throw e
      } catch (e: Exception) {
        onPublishFailed(form, announcement, e)
      }
    }
  }

  // A failed save may still have reached the server (for example a timeout). If the announcement
  // exists, it is published, and updated if the form changed since that save; otherwise the error
  // is shown and the user can retry with the same id and time.
  private suspend fun onPublishFailed(
      form: AnnouncementFormState,
      announcement: Announcement,
      e: Exception,
  ) {
    // Not an error yet: the announcement may have been stored anyway, which is checked below.
    Log.w(TAG, "Adding the announcement failed, checking whether it was stored", e)
    val alreadySaved =
        try {
          withTimeout(saveTimeoutMs) {
            val stored =
                eventsRepository.getEvent(eventId).announcementOrNull(announcement.announcementId)
            if (stored != null && !form.matches(stored)) {
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
