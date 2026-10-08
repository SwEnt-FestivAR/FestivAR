package com.android.festivar.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

data class OverviewSectionUi(val group: OverviewGroup, val rows: List<OverviewRowUi>)

/**
 * UI state of the task overview.
 *
 * @property counts how many tasks the OPEN, MINE and DONE chips hold, before the search applies.
 * @property sections the visible rows, grouped by part of the day, in start order.
 * @property isLoading true only until the first load ends.
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
 * The task overview of one event: every task, filtered by chip and search, grouped by time of day.
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

  init {
    refresh()
  }

  /** Reloads this event's tasks; on failure the last sections stay and [errorMsg] is set. */
  fun refresh() {
    viewModelScope.launch {
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

  private fun matches(task: Task, filter: OverviewFilter): Boolean =
      when (filter) {
        OverviewFilter.OPEN -> !task.completed && task.assignees.size < task.maxAssign
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
            .groupBy { groupOf(it, now) }
            .toSortedMap()
            .map { (group, rows) -> OverviewSectionUi(group, rows.map { row(it) }) }
    val counts =
        listOf(OverviewFilter.OPEN, OverviewFilter.MINE, OverviewFilter.DONE).associateWith { filter
          ->
          tasks.count { matches(it, filter) }
        }
    return state.copy(sections = sections, counts = counts)
  }

  private fun groupOf(task: Task, now: LocalDateTime): OverviewGroup {
    val start = task.startTime ?: return OverviewGroup.ANYTIME
    val startsWithinTheHour = !start.isBefore(now) && !start.isAfter(now.plusHours(1))
    val startedToday = !start.isAfter(now) && start.toLocalDate() == now.toLocalDate()
    return when {
      !task.completed && (startsWithinTheHour || startedToday) -> OverviewGroup.NOW
      start.hour < 12 -> OverviewGroup.MORNING
      start.hour < 18 -> OverviewGroup.AFTERNOON
      else -> OverviewGroup.EVENING
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
          isFull = task.assignees.size >= task.maxAssign,
      )
}
