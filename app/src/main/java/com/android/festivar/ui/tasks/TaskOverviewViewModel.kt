// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The overview's single-select filter chips. */
enum class OverviewFilter {
  OPEN,
  MINE,
  DONE,
  ALL,
}

/** The part of the day a task falls in; the screen turns it into its section label. */
enum class OverviewGroup {
  NOW,
  MORNING,
  AFTERNOON,
  EVENING,
  ANYTIME,
}

/**
 * One overview row as data; the screen turns it into words. [peopleNeeded] is the task's
 * [Task.maxAssign], and [isFull] means no place is left on it.
 */
data class OverviewRowUi(
    val task: Task,
    val location: String,
    val peopleNeeded: Int,
    val estimatedMinutes: Int?,
    val startTime: LocalTime?,
    val assigned: Int,
    val isMine: Boolean,
    val isFull: Boolean,
)

/**
 * The rows of one part of one day. [day] is null only for [OverviewGroup.ANYTIME], whose tasks have
 * no start; a [OverviewGroup.NOW] section always belongs to today.
 */
data class OverviewSectionUi(
    val group: OverviewGroup,
    val rows: List<OverviewRowUi>,
    val day: LocalDate? = null,
)

/**
 * UI state of the task overview.
 *
 * @property counts how many tasks each chip holds, before the search applies.
 * @property sections the visible rows, day by day and then by part of the day, in start order.
 * @property isLoading true while a load runs, the first one and every
 *   [TaskOverviewViewModel.refresh].
 */
data class TaskOverviewUiState(
    val filter: OverviewFilter = OverviewFilter.OPEN,
    val searchOpen: Boolean = false,
    val query: String = "",
    val counts: Map<OverviewFilter, Int> = emptyMap(),
    val sections: List<OverviewSectionUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
)

/**
 * The task overview of one event: every task, filtered by chip and search, grouped by day and time
 * of day. The NOW group follows the clock on its own, every [REGROUP_PERIOD], so a task that comes
 * within the hour moves up without the user touching anything.
 *
 * @param userId the current user, or null when nobody is signed in (MINE is then empty).
 * @param clock decides what "now" is, so the NOW group can be tested.
 */
class TaskOverviewViewModel(
    private val eventId: String,
    private val userId: String? = null,
    private val repository: TasksRepository = TasksRepositoryProvider.repository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(TaskOverviewUiState())
  val uiState: StateFlow<TaskOverviewUiState> = _uiState.asStateFlow()

  private var tasks: List<Task> = emptyList()
  private var load: Job? = null

  init {
    refresh()
    viewModelScope.launch {
      while (isActive) {
        delay(REGROUP_PERIOD.toMillis())
        _uiState.update { derive(it) }
      }
    }
  }

  /**
   * Reloads this event's tasks; on failure the last sections stay and [errorMsg] is set. A load
   * still running is cancelled first, so the newest call is the only one that can touch the state.
   */
  fun refresh() {
    load?.cancel()
    load = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        tasks =
            repository
                .getAllTasks(eventId)
                .sortedWith(compareBy(nullsLast()) { it: Task -> it.startTime }.thenBy { it.title })
        _uiState.update { derive(it.copy(isLoading = false, errorMsg = null)) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMsg = e.message ?: e.toString()) }
      }
    }
  }

  fun selectFilter(filter: OverviewFilter) {
    _uiState.update { derive(it.copy(filter = filter)) }
  }

  /** Opens or closes the search field; closing it clears the query. */
  fun toggleSearch() {
    _uiState.update { derive(it.copy(searchOpen = !it.searchOpen, query = "")) }
  }

  fun setQuery(query: String) {
    _uiState.update { derive(it.copy(query = query)) }
  }

  private fun isMine(task: Task): Boolean =
      userId != null && task.assignees.any { it.uid == userId }

  /** The one place that says when a task has no place left; OPEN and the full badge both use it. */
  private fun isFull(task: Task): Boolean = task.assignees.size >= task.maxAssign

  private fun matches(task: Task, filter: OverviewFilter): Boolean =
      when (filter) {
        OverviewFilter.OPEN -> !task.completed && !isFull(task)
        OverviewFilter.MINE -> isMine(task)
        OverviewFilter.DONE -> task.completed
        OverviewFilter.ALL -> true
      }

  private fun derive(state: TaskOverviewUiState): TaskOverviewUiState {
    val query = state.query.trim()
    val now = LocalDateTime.now(clock)
    val sections =
        tasks
            .filter { matches(it, state.filter) }
            .filter {
              query.isEmpty() ||
                  it.title.contains(query, ignoreCase = true) ||
                  it.location.contains(query, ignoreCase = true)
            }
            .groupBy { sectionOf(it, now) }
            .toSortedMap(
                compareBy<Section, LocalDate?>(nullsLast()) { it.first }.thenBy { it.second }
            )
            .map { (section, rows) ->
              OverviewSectionUi(section.second, rows.map { row(it) }, section.first)
            }
    val counts =
        OverviewFilter.entries.associateWith { filter -> tasks.count { matches(it, filter) } }
    return state.copy(sections = sections, counts = counts)
  }

  /** Which day a task's section sits under, then which part of that day. */
  private fun sectionOf(task: Task, now: LocalDateTime): Section {
    val start = task.startTime ?: return null to OverviewGroup.ANYTIME
    val startsWithinTheHour = !start.isBefore(now) && !start.isAfter(now.plusHours(1))
    val startedToday = !start.isAfter(now) && start.toLocalDate() == now.toLocalDate()
    return when {
      !task.completed && (startsWithinTheHour || startedToday) ->
          now.toLocalDate() to OverviewGroup.NOW
      start.hour < 12 -> start.toLocalDate() to OverviewGroup.MORNING
      start.hour < 18 -> start.toLocalDate() to OverviewGroup.AFTERNOON
      else -> start.toLocalDate() to OverviewGroup.EVENING
    }
  }

  private fun row(task: Task) =
      OverviewRowUi(
          task = task,
          location = task.location,
          peopleNeeded = task.maxAssign,
          estimatedMinutes = task.estimatedTime?.toMinutes()?.toInt(),
          startTime = task.startTime?.toLocalTime(),
          assigned = task.assignees.size,
          isMine = isMine(task),
          isFull = isFull(task),
      )

  companion object {
    /** How often the NOW group is recomputed against the clock. */
    val REGROUP_PERIOD: Duration = Duration.ofMinutes(1)
  }
}

private typealias Section = Pair<LocalDate?, OverviewGroup>
