// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.details

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.announcement.Announcement
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
 * UI state of the Announcement Details screen.
 *
 * @property announcement the announcement shown, or null until loaded.
 * @property authorName the author's name, or null if the account no longer exists.
 * @property authorNameRes the string resource for a missing author's name, or null when known.
 * @property eventEndTime the end time of the event that contains the announcement.
 * @property isLoading true while the announcement is being loaded.
 * @property isUpdating true while the pin state is being saved.
 * @property errorMsg the string resource of the latest load or save failure, or null.
 */
data class AnnouncementDetailsUiState(
    val announcement: Announcement? = null,
    val authorName: String? = null,
    @param:StringRes val authorNameRes: Int? = null,
    val eventEndTime: ZonedDateTime? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    @param:StringRes val errorMsg: Int? = null,
) {
  /** Whether the pin button can be used. */
  val canTogglePin: Boolean
    get() = announcement != null && !isLoading && !isUpdating
}

/**
 * Loads an announcement and its author, and saves changes to its pin state.
 *
 * Loading starts when the screen calls [load], such as on each resume.
 *
 * @param eventId identifier of the event containing the announcement.
 * @param announcementId identifier of the announcement to show.
 * @param dataRepository provides the event and user repositories used by this ViewModel.
 */
class AnnouncementDetailsViewModel(
    private val eventId: String,
    private val announcementId: String,
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

  /** Loads the event, announcement, and author. A failed reload keeps the last loaded details. */
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
        _uiState.update {
          it.copy(
              announcement = announcement,
              authorName = authorName,
              authorNameRes =
                  if (authorName == null) R.string.announcement_details_user_fetch_failed else null,
              eventEndTime = event.endDate,
              isLoading = false,
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

  /** Toggles whether this announcement is pinned. Does nothing while loading or saving. */
  fun togglePin() {
    val before = _uiState.getAndUpdate {
      if (it.canTogglePin) it.copy(isUpdating = true, errorMsg = null) else it
    }
    val announcement = before.announcement?.takeIf { before.canTogglePin } ?: return
    val pinned = !announcement.pinned

    viewModelScope.launch {
      try {
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
