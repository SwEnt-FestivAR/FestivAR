// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.announcement.Announcement
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnnouncementFeedSection(
    val date: LocalDate,
    val announcements: List<Announcement>,
)

data class AnnouncementFeedUiState(
    val pinnedAnnouncements: List<Announcement> = emptyList(),
    val sections: List<AnnouncementFeedSection> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
)

/**
 * Loads an event's announcements for the feed.
 *
 * Nothing loads until [refresh] is called. Pinned announcements appear separately, followed by
 * unpinned announcements grouped by their update date in the clock's time zone.
 */
class AnnouncementFeedViewModel(
    private val eventId: String,
    private val repository: EventsRepository = EventsRepositoryProvider.repository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(AnnouncementFeedUiState())
  val uiState: StateFlow<AnnouncementFeedUiState> = _uiState.asStateFlow()

  private var refreshJob: Job? = null

  /** Fetches the latest announcements. A failed refresh keeps the last successful feed. */
  fun refresh() {
    refreshJob?.cancel()
    _uiState.update { it.copy(isLoading = true) }
    refreshJob = viewModelScope.launch {
      try {
        val announcements = repository.getEvent(eventId).announcements
        ensureActive()
        val pinned =
            announcements.filter { it.pinned }.sortedByDescending { it.updatedAt.toInstant() }
        val sections =
            announcements
                .filterNot { it.pinned }
                .groupBy { it.updatedAt.withZoneSameInstant(clock.zone).toLocalDate() }
                .toSortedMap(reverseOrder())
                .map { (date, items) ->
                  AnnouncementFeedSection(
                      date = date,
                      announcements = items.sortedByDescending { it.updatedAt.toInstant() },
                  )
                }
        _uiState.update {
          it.copy(
              pinnedAnnouncements = pinned,
              sections = sections,
              isLoading = false,
              errorMsg = null,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMsg = e.message ?: e.toString()) }
      }
    }
  }
}
