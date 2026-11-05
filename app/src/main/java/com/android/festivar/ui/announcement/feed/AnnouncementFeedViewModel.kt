// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "AnnouncementFeedViewModel"

/**
 * Which announcements the feed shows, selected with the chips at the top of the screen.
 *
 * @property priority The priority of the announcements shown, or `null` to show all of them.
 */
enum class AnnouncementFilter(val priority: AnnouncementPriority?) {
  URGENT(AnnouncementPriority.URGENT),
  WARNING(AnnouncementPriority.WARNING),
  INFO(AnnouncementPriority.INFO),
  ALL(null),
}

/** Whether [announcement] is shown when this filter is selected. */
private fun AnnouncementFilter.matches(announcement: Announcement): Boolean =
    priority == null || announcement.priority == priority

/**
 * The unpinned announcements updated on the same day.
 *
 * @property date The day of the update, in the time zone of the ViewModel's clock.
 * @property announcements The announcements of that day, the most recently updated first.
 */
data class AnnouncementFeedSection(
    val date: LocalDate,
    val announcements: List<Announcement>,
)

/**
 * Represents the UI state of the announcement feed.
 *
 * @property pinnedAnnouncements Every pinned announcement, the most recently updated first,
 *   whatever the filter. The screen reads [shownPinnedAnnouncements].
 * @property sections Every unpinned announcement by day, the most recent day first, whatever the
 *   filter. The screen reads [shownSections].
 * @property selectedFilter Which announcements are shown: [shownPinnedAnnouncements] and
 *   [shownSections]. A refresh keeps it.
 * @property isLoading Whether the announcements are currently being fetched.
 * @property errorMsg The string resource of the failed fetch, or `null` if there is no error. It
 *   stays until a fetch succeeds, so the screen can keep showing the error with a retry.
 */
data class AnnouncementFeedUIState(
    val pinnedAnnouncements: List<Announcement> = emptyList(),
    val sections: List<AnnouncementFeedSection> = emptyList(),
    val selectedFilter: AnnouncementFilter = AnnouncementFilter.ALL,
    val isLoading: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /** The [pinnedAnnouncements] selected by [selectedFilter]. */
  val shownPinnedAnnouncements: List<Announcement>
    get() = pinnedAnnouncements.filter { selectedFilter.matches(it) }

  /**
   * The [sections] with only the announcements selected by [selectedFilter], without empty ones.
   */
  val shownSections: List<AnnouncementFeedSection>
    get() =
        sections
            .map { section ->
              section.copy(announcements = section.announcements.filter(selectedFilter::matches))
            }
            .filter { it.announcements.isNotEmpty() }

  /**
   * Whether there is no announcement at all, neither pinned nor in a section, whatever
   * [selectedFilter]. Also `true` during the first load, so the screen checks [isLoading] first to
   * not flash its empty state.
   */
  val hasNoAnnouncements: Boolean
    get() = pinnedAnnouncements.isEmpty() && sections.isEmpty()

  /**
   * How many announcements each filter selects, pinned ones included. Every filter has an entry,
   * and the counts don't depend on [selectedFilter].
   */
  val counts: Map<AnnouncementFilter, Int>
    get() {
      val announcements = pinnedAnnouncements + sections.flatMap { it.announcements }
      return AnnouncementFilter.entries.associateWith { filter ->
        announcements.count { filter.matches(it) }
      }
    }
}

/**
 * ViewModel of the announcement feed.
 *
 * Manages the UI state by fetching an event's announcements through the [EventsRepository]. Nothing
 * is fetched until [refreshUIState] is called: the screen calls it each time it is shown. Until the
 * first fetch, the UI state is already loading.
 *
 * Pinned announcements appear separately, followed by unpinned announcements grouped by their
 * update date. The announcements in the state have their `updatedAt` in the time zone of [clock],
 * so the times the screen shows always match the date headers.
 *
 * @property eventId The unique identifier of the event whose announcements are shown.
 * @property eventsRepository The repository used to fetch the announcements.
 * @property clock Gives the time zone of the announcements' `updatedAt` and the sections' dates.
 *   The device's clock by default.
 */
class AnnouncementFeedViewModel(
    private val eventId: String,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.repository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

  // Loading from the start: nothing is fetched until the screen calls refreshUIState on resume, and
  // until then the screen must show its progress indicator, not its empty state.
  private val _uiState = MutableStateFlow(AnnouncementFeedUIState(isLoading = true))
  val uiState: StateFlow<AnnouncementFeedUIState> = _uiState.asStateFlow()

  /** The fetch of the announcements in progress, cancelled when a newer one starts. */
  private var getAnnouncementsJob: Job? = null

  /** Selects which announcements are shown. */
  fun selectFilter(filter: AnnouncementFilter) {
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  /**
   * Refreshes the UI state by fetching the announcements from the repository, e.g. when the screen
   * is shown again, or when the user pulls to refresh or retries.
   */
  fun refreshUIState() {
    getAnnouncements()
  }

  /**
   * Fetches the event's announcements from the repository and updates the UI state. Cancels the
   * previous fetch, so that an older result never overwrites a newer one. A failure keeps the
   * announcements already shown, and the error stays until a fetch succeeds.
   */
  private fun getAnnouncements() {
    getAnnouncementsJob?.cancel()
    _uiState.update { it.copy(isLoading = true) }
    getAnnouncementsJob = viewModelScope.launch {
      try {
        val event = eventsRepository.getEvent(eventId)
        // A newer fetch may have cancelled this one while the repository was fetching. A
        // repository that ignores cancellation still returns its announcements, so stop here
        // before these stale announcements reach the UI state.
        ensureActive()
        val announcements =
            event.announcements.map {
              it.copy(updatedAt = it.updatedAt.withZoneSameInstant(clock.zone))
            }
        val pinned =
            announcements.filter { it.pinned }.sortedByDescending { it.updatedAt.toInstant() }
        val sections =
            announcements
                .filterNot { it.pinned }
                .groupBy { it.updatedAt.toLocalDate() }
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
      } catch (e: Exception) {
        // A cancelled fetch must not touch the state. A CancellationException from
        // the repository while this fetch is still active is a normal failure.
        ensureActive()
        Log.e(LOG_TAG, "Error loading the announcements of event $eventId", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = R.string.announcement_feed_load_failed)
        }
      }
    }
  }
}
