// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.details

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.canDeleteAnnouncement
import com.android.festivar.model.event.canEditAnnouncement
import com.android.festivar.model.event.role.Permission
import com.android.festivar.model.user.UsersRepository
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "AnnouncementDetailsVM"

/**
 * What the signed-in user can do on the announcement shown, as decided by their role in the event.
 *
 * @property canSeeAnnouncement true if the user may see the announcement. It is true until the
 *   event is loaded, and false for a user who is not a member of the event.
 * @property canPin true if the user may pin or unpin the announcement, which counts as editing it.
 * @property canEditAnnouncement true if the user may edit the announcement.
 * @property canDeleteAnnouncement true if the user may delete the announcement.
 */
data class AnnouncementDetailsPermissions(
    val canSeeAnnouncement: Boolean = true,
    val canPin: Boolean = false,
    val canEditAnnouncement: Boolean = false,
    val canDeleteAnnouncement: Boolean = false,
)

/**
 * Computes what the member [uid] can do on [announcement] in this [Event]. A user who is not a
 * member of the event cannot see the announcement, nor do anything on it.
 */
private fun Event.permissionsOf(
    uid: String,
    announcement: Announcement,
): AnnouncementDetailsPermissions =
    if (uid !in roles) AnnouncementDetailsPermissions(canSeeAnnouncement = false)
    else
        AnnouncementDetailsPermissions(
            canSeeAnnouncement = canAct(uid, Permission.CAN_SEE_ANNOUNCEMENT),
            canPin = canEditAnnouncement(uid, announcement),
            canEditAnnouncement = canEditAnnouncement(uid, announcement),
            canDeleteAnnouncement = canDeleteAnnouncement(uid, announcement),
        )

/**
 * UI state of the Announcement Details screen.
 *
 * @property announcement the announcement shown, or null until loaded.
 * @property authorName the author's name, or null if the account no longer exists.
 * @property authorNameRes the string resource for a missing author's name, or null when known.
 * @property eventEndTime the end time of the event that contains the announcement.
 * @property isLoading true while the announcement is being loaded.
 * @property isUpdating true while the pin state is being saved.
 * @property permissions what the signed-in user can do on the announcement, updated at each load.
 * @property errorMsg the string resource of the latest load or save failure, or null.
 */
data class AnnouncementDetailsUiState(
    val announcement: Announcement? = null,
    val authorName: String? = null,
    @param:StringRes val authorNameRes: Int? = null,
    val eventEndTime: ZonedDateTime? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val permissions: AnnouncementDetailsPermissions = AnnouncementDetailsPermissions(),
    @param:StringRes val errorMsg: Int? = null,
) {
  /** Whether the pin button can be used: the user may pin and nothing is loading or saving. */
  val canTogglePin: Boolean
    get() = announcement != null && permissions.canPin && !isLoading && !isUpdating
}

/**
 * Loads an announcement and its author, and saves changes to its pin state.
 *
 * Loading starts when the screen calls [load], such as on each resume.
 *
 * @param eventId identifier of the event containing the announcement.
 * @param announcementId identifier of the announcement to show.
 * @param currentUserId gives the uid of the signed-in user. It is read at each load and again
 *   before saving the pin, so a change of account while the screen is open is taken into account.
 * @param dataRepository provides the event and user repositories used by this ViewModel.
 */
class AnnouncementDetailsViewModel(
    private val eventId: String,
    private val announcementId: String,
    private val currentUserId: () -> String,
    private val dataRepository: DataRepository,
) : ViewModel() {
  private val eventsRepository: EventsRepository = dataRepository.eventsRepository
  private val usersRepository: UsersRepository = dataRepository.usersRepository

  init {
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(announcementId.isNotBlank()) { "announcementId must not be blank" }
  }

  private val _uiState = MutableStateFlow(AnnouncementDetailsUiState())
  val uiState: StateFlow<AnnouncementDetailsUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  /**
   * Loads the event, announcement, author, and what the user can do on the announcement. An
   * announcement the user may not see is not kept. A failed reload keeps the last loaded details.
   */
  fun load() {
    if (_uiState.value.isUpdating || loadJob?.isActive == true) return
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val event = eventsRepository.getEvent(eventId)
        val announcement =
            event.announcements.firstOrNull { it.announcementId == announcementId }
                ?: throw NoSuchElementException(
                    "Announcement $announcementId not found in event $eventId"
                )
        val authorName =
            try {
              val author = usersRepository.getUser(announcement.authorId)
              listOf(author.name, author.surname).filter(String::isNotBlank).joinToString(" ")
            } catch (_: NoSuchElementException) {
              null
            }
        val permissions = event.permissionsOf(currentUserId(), announcement)
        _uiState.update {
          it.copy(
              announcement = announcement.takeIf { permissions.canSeeAnnouncement },
              authorName = authorName,
              authorNameRes =
                  if (authorName == null) R.string.announcement_details_user_fetch_failed else null,
              eventEndTime = event.endDate,
              isLoading = false,
              permissions = permissions,
              errorMsg =
                  it.errorMsg.takeUnless { error ->
                    error == R.string.announcement_details_load_failed
                  },
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading announcement $announcementId from event $eventId", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = R.string.announcement_details_load_failed)
        }
      }
    }
  }

  /**
   * Toggles whether this announcement is pinned. Does nothing while loading or saving, or if the
   * user may not pin. The user's role is checked again before saving: a refused save shows the save
   * error and updates [AnnouncementDetailsUiState.permissions].
   */
  fun togglePin() {
    val before = _uiState.getAndUpdate {
      if (it.canTogglePin) it.copy(isUpdating = true, errorMsg = null) else it
    }
    val announcement = before.announcement?.takeIf { before.canTogglePin } ?: return
    val pinned = !announcement.pinned

    viewModelScope.launch {
      try {
        // The role may have changed since the last load, so it is checked again before saving.
        val event = eventsRepository.getEvent(eventId)
        val current =
            event.announcementOrNull(announcementId)
                ?: throw NoSuchElementException(
                    "Announcement $announcementId not found in event $eventId"
                )
        val permissions = event.permissionsOf(currentUserId(), current)
        if (!permissions.canPin) {
          // Shows the up-to-date permissions, so the pin button goes away after the first refusal.
          _uiState.update {
            it.copy(
                announcement = it.announcement.takeIf { permissions.canSeeAnnouncement },
                isUpdating = false,
                permissions = permissions,
                errorMsg = R.string.announcement_details_save_failed,
            )
          }
          return@launch
        }
        val updated = eventsRepository.setAnnouncementPinned(eventId, announcementId, pinned)
        _uiState.update { it.copy(announcement = updated, isUpdating = false) }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error updating pin for announcement $announcementId", e)
        _uiState.update {
          it.copy(isUpdating = false, errorMsg = R.string.announcement_details_save_failed)
        }
      }
    }
  }
}
