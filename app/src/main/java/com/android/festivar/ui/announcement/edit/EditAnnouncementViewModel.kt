// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.edit

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.ui.announcement.ANNOUNCEMENT_SAVE_TIMEOUT_MS
import com.android.festivar.ui.announcement.AnnouncementFormEditor
import com.android.festivar.ui.announcement.AnnouncementFormState
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
 * UI state of the Edit announcement screen. The form is an [AnnouncementFormState], filled from
 * [announcement].
 *
 * @property announcement The announcement as stored, or null until it is loaded (or if it could not
 *   be loaded).
 * @property form What the user edits, with the field rules of an announcement form.
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
    val form: AnnouncementFormState = AnnouncementFormState(),
    val isOrganizer: () -> Boolean = { false },
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /**
   * Whether the form differs from [announcement] on a field the user can edit (see
   * [AnnouncementFormState.matches]).
   */
  val hasChanges: Boolean
    get() = announcement != null && !form.matches(announcement)

  /** Whether going back must ask first: a saved edit has nothing left to lose. */
  val needsDiscardConfirmation: Boolean
    get() = hasChanges && !isSaved

  /**
   * Whether the "Save" button can be pressed. [hasChanges] implies that an announcement is loaded.
   */
  val canSave: Boolean
    get() = isOrganizer() && form.isValid && hasChanges && !isSaving && !isSaved
}

// The model allows a blank content but the form requires one, so the reason "Save" is disabled must
// be visible from the start.
private fun Announcement.toForm(): AnnouncementFormState =
    AnnouncementFormState(
        title = title,
        content = content,
        priority = priority,
        contentEdited = content.isBlank(),
    )

private const val TAG = "EditAnnouncementViewModel"

/**
 * Holds the state of the Edit announcement screen: loads the announcement, edits it in an
 * [AnnouncementFormState] and saves it through the [EventsRepository] of [dataRepository]. Only the
 * owner of the event (the organizer) can save.
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
    private val saveTimeoutMs: Long = ANNOUNCEMENT_SAVE_TIMEOUT_MS,
) : ViewModel(), AnnouncementFormEditor {

  init {
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(announcementId.isNotBlank()) { "announcementId must not be blank" }
  }

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository

  private val _uiState = MutableStateFlow(EditAnnouncementUiState())

  /** The state of the form, read by the screen. */
  val uiState: StateFlow<EditAnnouncementUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

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
            event.announcementOrNull(announcementId)
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

  /**
   * Applies a change of a form field, for the setters of [AnnouncementFormEditor]. A form that is
   * not loaded yet, being saved or already saved ignores every change, since the screen leaves on
   * save.
   */
  override fun applyEdit(change: (AnnouncementFormState) -> AnnouncementFormState) =
      _uiState.update {
        if (it.announcement == null || it.isSaving || it.isSaved) it
        else it.copy(form = change(it.form))
      }

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
            editOrStoredCopy(form)
          } catch (e: CancellationException) {
            // Also reached when the read-back is cancelled, so the form never stays in saving.
            _uiState.update { it.copy(isSaving = false) }
            throw e
          }
      _uiState.update {
        if (saved != null) it.copy(announcement = saved, isSaving = false, isSaved = true)
        else it.copy(isSaving = false, errorMsg = R.string.edit_announcement_error_save_failed)
      }
    }
  }

  /**
   * Saves [form], or reads the announcement back when the save failed. A cancellation is rethrown
   * from the save and from the read-back alike, for [save] to handle in one place.
   *
   * @return The stored announcement, or null if the edit is not stored.
   */
  private suspend fun editOrStoredCopy(form: AnnouncementFormState): Announcement? =
      try {
        withTimeout(saveTimeoutMs) { editLatest(form) }
      } catch (e: TimeoutCancellationException) {
        storedCopy(form, e)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        storedCopy(form, e)
      }

  /**
   * Applies the three form fields to the latest stored copy and saves it. It starts from the latest
   * stored copy, not the one loaded when the screen opened, so a pin toggled or a link changed
   * since then is kept. It runs inside the try of the save on purpose, since it depends on a fresh
   * read.
   */
  private suspend fun editLatest(form: AnnouncementFormState): Announcement {
    val latest =
        eventsRepository.getEvent(eventId).announcementOrNull(announcementId)
            ?: throw NoSuchElementException(
                "Announcement $announcementId not found in event $eventId"
            )
    // A device clock behind the stored time keeps the stored time, so the model's edit-time rule
    // never fails.
    val now = ZonedDateTime.now(clock)
    val at = if (now.isBefore(latest.updatedAt)) latest.updatedAt else now
    val edited =
        latest
            .editTitle(form.trimmedTitle, at)
            .editContent(form.trimmedContent, at)
            .editPriority(form.priority, at)
    eventsRepository.editAnnouncement(eventId, edited)
    return edited
  }

  /**
   * A failed save may still have reached the server (for example a timeout): reads the announcement
   * back and returns it if it holds the edit.
   */
  private suspend fun storedCopy(form: AnnouncementFormState, e: Exception): Announcement? {
    Log.w(TAG, "Editing the announcement failed, checking whether it was stored", e)
    return try {
      withTimeout(saveTimeoutMs) {
        eventsRepository.getEvent(eventId).announcementOrNull(announcementId)?.takeIf {
          form.matches(it)
        }
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
