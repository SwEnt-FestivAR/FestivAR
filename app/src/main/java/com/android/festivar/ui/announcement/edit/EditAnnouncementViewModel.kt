// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.edit

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
import com.android.festivar.ui.announcement.create.CreateAnnouncementUiState
import java.time.Clock
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * UI state of the Edit announcement screen. The form is the Create announcement one, filled from
 * [announcement].
 *
 * The form's own `isSaving`, `isPublished` and `errorMsg` are never set by
 * [EditAnnouncementViewModel], and its `hasChanges` and `needsDiscardConfirmation` mean
 * "non-empty", which is wrong for a prefilled form. So this state owns [isSaving], [isSaved],
 * [errorMsg], [hasChanges] and [needsDiscardConfirmation]. [CreateAnnouncementUiState.canPublish]
 * is used as "the form is valid" because the form's saving and published flags stay false here.
 *
 * @property announcement The announcement as stored, or null until it is loaded (or if it could not
 *   be loaded).
 * @property form What the user edits, with the field rules of the Create announcement form.
 * @property isOrganizer Whether the current user is the organizer (owner) of the event, read when
 *   asked so a sign out after the load is seen. Only the organizer can save.
 * @property isLoading True while a load runs; false before [EditAnnouncementViewModel.load] is
 *   called.
 * @property isSaving True while a save is running, so a second tap does nothing.
 * @property isSaved True once the edit is stored; the form is then final.
 * @property errorMsg The string resource of the last failure, or null.
 */
data class EditAnnouncementUiState(
    val announcement: Announcement? = null,
    val form: CreateAnnouncementUiState = CreateAnnouncementUiState(),
    val isOrganizer: () -> Boolean = { false },
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /**
   * Whether the form differs from [announcement] on a field the user can edit. Spaces around the
   * texts are not a change, since the save trims them.
   */
  val hasChanges: Boolean
    get() =
        announcement != null &&
            (form.title.trim() != announcement.title.trim() ||
                form.content.trim() != announcement.content.trim() ||
                form.priority != announcement.priority)

  /** Whether going back must ask first: a saved edit has nothing left to lose. */
  val needsDiscardConfirmation: Boolean
    get() = hasChanges && !isSaved

  /**
   * Whether the "Save" button can be pressed. [hasChanges] implies that an announcement is loaded.
   */
  val canSave: Boolean
    get() = isOrganizer() && form.canPublish && hasChanges && !isSaving && !isSaved
}

// The model allows a blank content but the form requires one, so the reason "Save" is disabled must
// be visible from the start.
private fun Announcement.toForm(): CreateAnnouncementUiState =
    CreateAnnouncementUiState(
        title = title,
        content = content,
        priority = priority,
        contentEdited = content.isBlank(),
    )

private const val TAG = "EditAnnouncementViewModel"

private const val SAVE_TIMEOUT_MS = 15_000L

/**
 * Holds the state of the Edit announcement screen: loads the announcement, edits it in the Create
 * announcement form and saves it through the [EventsRepository] of [dataRepository]. Only the owner
 * of the event (the organizer) can save.
 *
 * @param eventId The event the announcement belongs to. It cannot be blank.
 * @param announcementId The announcement to edit. It cannot be blank.
 * @param currentUserId Gives the uid of the signed-in user; only the owner of the event can save.
 *   It is asked when needed, so a sign-out or an account change after the load is taken into
 *   account.
 * @param dataRepository Gives the [EventsRepository] the announcement is read from and saved to.
 * @param clock Gives the time of the edit. The device's clock by default.
 * @param saveTimeoutMs How long a save may last before it is reported as failed. A failed save is
 *   followed by a read under the same timeout.
 */
class EditAnnouncementViewModel(
    private val eventId: String,
    private val announcementId: String,
    private val currentUserId: () -> String,
    private val dataRepository: DataRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
) : ViewModel() {

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository

  init {
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(announcementId.isNotBlank()) { "announcementId must not be blank" }
  }

  private val _uiState = MutableStateFlow(EditAnnouncementUiState())

  /** The state of the form, read by the screen. */
  val uiState: StateFlow<EditAnnouncementUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  private fun Event.announcementOrNull(): Announcement? = announcements.firstOrNull {
    it.announcementId == announcementId
  }

  /**
   * Loads the announcement once; a running load is not started twice, and a resume never wipes
   * unsaved edits. After a failed load it can be called again.
   */
  fun load() {
    if (loadJob?.isActive == true || _uiState.value.announcement != null) return
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    loadJob = viewModelScope.launch {
      try {
        val event = eventsRepository.getEvent(eventId)
        val announcement =
            event.announcementOrNull()
                ?: throw NoSuchElementException(
                    "Announcement $announcementId not found in event $eventId"
                )
        val ownerId = event.ownerId
        _uiState.update {
          it.copy(
              announcement = announcement,
              form = announcement.toForm(),
              isOrganizer = { ownerId == currentUserId() },
              isLoading = false,
              errorMsg = null,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error loading announcement $announcementId of event $eventId", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = R.string.edit_announcement_error_load_failed)
        }
      }
    }
  }

  // A finished form ignores every change, since the screen leaves on save.
  private fun edit(change: (CreateAnnouncementUiState) -> CreateAnnouncementUiState) =
      _uiState.update {
        if (it.announcement == null || it.isSaved) it else it.copy(form = change(it.form))
      }

  /** Sets the title and marks it as edited, so that an empty title is reported. */
  fun updateTitle(title: String) = edit { it.copy(title = title, titleEdited = true) }

  /** Sets the content and marks it as edited, so that an empty content is reported. */
  fun updateContent(content: String) = edit { it.copy(content = content, contentEdited = true) }

  /** Sets the priority. */
  fun updatePriority(priority: AnnouncementPriority) = edit { it.copy(priority = priority) }

  /**
   * Saves the edited announcement. Does nothing unless [EditAnnouncementUiState.canSave]; a loaded
   * announcement of an event the user no longer owns shows the save error instead.
   */
  fun save() {
    val state = _uiState.value
    // Checked before canSave, which also reads isOrganizer: the screen may still show the button
    // enabled from before a sign out, so the tap must say why nothing is saved.
    if (state.announcement != null && !state.isOrganizer()) {
      Log.e(TAG, "save called by a user who does not own event $eventId")
      _uiState.update { it.copy(errorMsg = R.string.edit_announcement_error_save_failed) }
      return
    }
    if (!state.canSave) return
    // A snapshot: typing during the save changes nothing that is stored.
    val form = state.form
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      val saved: Announcement? =
          try {
            withTimeout(saveTimeoutMs) { editLatest(form) }
          } catch (e: TimeoutCancellationException) {
            storedCopy(form, e)
          } catch (e: CancellationException) {
            _uiState.update { it.copy(isSaving = false) }
            throw e
          } catch (e: Exception) {
            storedCopy(form, e)
          }
      _uiState.update {
        if (saved != null) it.copy(announcement = saved, isSaving = false, isSaved = true)
        else it.copy(isSaving = false, errorMsg = R.string.edit_announcement_error_save_failed)
      }
    }
  }

  // It starts from the latest stored copy, not the one loaded when the screen opened, so a pin
  // toggled or a link changed since then is kept (writing the loaded copy is the lost update of
  // https://github.com/SwEnt-FestivAR/FestivAR/issues/186 for tasks). Only the three form fields
  // are applied. It is inside the try on purpose, since it depends on a fresh read.
  private suspend fun editLatest(form: CreateAnnouncementUiState): Announcement {
    val latest =
        eventsRepository.getEvent(eventId).announcementOrNull()
            ?: throw NoSuchElementException(
                "Announcement $announcementId not found in event $eventId"
            )
    // A device clock behind the stored time keeps the stored time, so the model's edit-time rule
    // never fails.
    val now = ZonedDateTime.now(clock)
    val at = if (now.isBefore(latest.updatedAt)) latest.updatedAt else now
    val edited =
        latest
            .editTitle(form.title.trim(), at)
            .editContent(form.content.trim(), at)
            .editPriority(form.priority, at)
    eventsRepository.editAnnouncement(eventId, edited)
    return edited
  }

  // Compares only the fields the form sets. Full equality would also compare the time zone of
  // updatedAt, so a copy read back from a remote store would differ.
  private fun Announcement.matches(form: CreateAnnouncementUiState): Boolean =
      title == form.title.trim() && content == form.content.trim() && priority == form.priority

  // A failed save may still have reached the server (for example a timeout): read the announcement
  // back and return it if it holds the edit.
  private suspend fun storedCopy(form: CreateAnnouncementUiState, e: Exception): Announcement? {
    Log.w(TAG, "Editing the announcement failed, checking whether it was stored", e)
    return try {
      withTimeout(saveTimeoutMs) {
        eventsRepository.getEvent(eventId).announcementOrNull()?.takeIf { it.matches(form) }
      }
    } catch (_: TimeoutCancellationException) {
      Log.e(TAG, "The announcement could not be checked in time")
      null
    } catch (e: CancellationException) {
      throw e
    } catch (checkError: Exception) {
      Log.e(TAG, "The announcement could not be checked", checkError)
      null
    }
  }
}
