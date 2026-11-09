// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.ui.task.matchesQuery
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Whether a task is still to do or done; the status chips of My tasks. */
enum class MyTaskStatus {
  TODO,
  DONE,
}

/**
 * One row of My tasks as data; the screen turns it into words. A tap on the row opens the task
 * details of [task], through its [Task.taskId].
 *
 * @property eventTitle the title of the task's event, or null when that event could not be found.
 * @property startTime when the task starts, in the time zone of the phone; null when it has none.
 * @property minutesUntilStart how many minutes are left before an unfinished task starts within the
 *   hour, for the "in 28 min" line; null otherwise.
 */
data class MyTaskRowUi(
    val task: Task,
    val eventTitle: String?,
    val startTime: LocalTime?,
    val minutesUntilStart: Int?,
)

/**
 * The rows of one day, sorted by start time then title, done and to do mixed. [day] is null only
 * for the tasks without a start, which come last.
 */
data class MyTasksDaySection(
    val day: LocalDate?,
    val rows: List<MyTaskRowUi>,
)

/**
 * One line of the events filter. [count] is how many tasks of this event pass the other filters.
 */
data class EventFilterOption(
    val eventId: String,
    val title: String?,
    val count: Int,
    val selected: Boolean,
)

/**
 * UI state of My tasks.
 *
 * The filters form three groups: [statuses], [priorities] and [eventIds]. A task must match one
 * selected value of every group, and an empty group lets every task through. The search applies on
 * top of them.
 *
 * @property statusCounts how many tasks each status chip would hold, given the other filters.
 * @property priorityCounts how many tasks each priority chip would hold, given the other filters.
 * @property events the events of the user's tasks, sorted by title, for the events filter.
 * @property sections the visible rows, day by day in the time zone of the phone.
 * @property totalTasks how many tasks the user has taken, before any filter.
 * @property isLoading true until the first load lands, and again while every
 *   [MyTasksViewModel.refresh] runs.
 * @property errorMsg why the last load failed, for the snackbar; null when it did not fail.
 */
data class MyTasksUiState(
    val statuses: Set<MyTaskStatus> = emptySet(),
    val priorities: Set<Priority> = emptySet(),
    val eventIds: Set<String> = emptySet(),
    val searchOpen: Boolean = false,
    val query: String = "",
    val statusCounts: Map<MyTaskStatus, Int> = emptyMap(),
    val priorityCounts: Map<Priority, Int> = emptyMap(),
    val events: List<EventFilterOption> = emptyList(),
    val sections: List<MyTasksDaySection> = emptyList(),
    val totalTasks: Int = 0,
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
) {
  /**
   * The number shown on the "Filters" chip; the events have their own selector and are not in it.
   */
  val activeFilterCount: Int
    get() = statuses.size + priorities.size

  /** The user has taken no task at all: the "No tasks yet" state. */
  val hasNoTasks: Boolean
    get() = !isLoading && errorMsg == null && totalTasks == 0

  /** The user has tasks, but the filters and the search hide all of them. */
  val nothingMatches: Boolean
    get() = totalTasks > 0 && sections.isEmpty()
}

/**
 * My tasks: every task the signed-in user has taken, across all their events, filtered and grouped
 * by day.
 *
 * Nothing loads until [refresh] is called: the screen calls it on every resume, the first one
 * included, so a load at construction would fetch the tasks twice on open.
 *
 * @param userId the signed-in user, whose tasks are shown.
 * @param dataRepository where the user, their tasks and the events are read: the app's one unless a
 *   test passes its own.
 * @param clock decides what "now" is and the time zone of the times and days shown.
 */
class MyTasksViewModel(
    private val userId: String,
    private val dataRepository: DataRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(MyTasksUiState())
  val uiState: StateFlow<MyTasksUiState> = _uiState.asStateFlow()

  private var tasks: List<Task> = emptyList()
  private var eventTitles: Map<String, String?> = emptyMap()
  private var load: Job? = null

    /**********************************************************************************
     *                   Filters helping functions & objects
     **********************************************************************************/

    /** The filter group a value belongs to, so a chip's count can leave its own group out. */
    private enum class Group {
        STATUS,
        PRIORITY,
        EVENT,
    }


    private fun statusOf(task: Task): MyTaskStatus =
        if (task.completed) MyTaskStatus.DONE else MyTaskStatus.TODO

    /**
     * Whether [task] passes the filters and the search of [state], leaving out the group [ignore]: a
     * chip's count must not depend on the other chips of its own group.
     */
    private fun passes(task: Task, state: MyTasksUiState, ignore: Group? = null): Boolean =
        (ignore == Group.STATUS || state.statuses.isEmpty() || statusOf(task) in state.statuses) &&
                (ignore == Group.PRIORITY || state.priorities.isEmpty() || task.priority in state.priorities) &&
                (ignore == Group.EVENT || state.eventIds.isEmpty() || task.eventId in state.eventIds) &&
                task.matchesQuery(state.query)




    /**********************************************************************************
     *                          UIstate modifications
     **********************************************************************************/

  /** Selects or unselects a status chip; several can be selected at once. */
  fun toggleStatus(status: MyTaskStatus) {
    _uiState.update { derive(it.copy(statuses = it.statuses.toggle(status))) }
  }

  /** Selects or unselects a priority chip; several can be selected at once. */
  fun togglePriority(priority: Priority) {
    _uiState.update { derive(it.copy(priorities = it.priorities.toggle(priority))) }
  }

  /** Checks or unchecks an event in the events filter; several can be checked at once. */
  fun toggleEvent(eventId: String) {
    _uiState.update { derive(it.copy(eventIds = it.eventIds.toggle(eventId))) }
  }

  /** Unchecks every event, so the tasks of all events show again. */
  fun clearEvents() {
    _uiState.update { derive(it.copy(eventIds = emptySet())) }
  }

  /** Drops every filter and the search query, so every task shows again. */
  fun clearFilters() {
    _uiState.update {
      derive(
          it.copy(statuses = emptySet(), priorities = emptySet(), eventIds = emptySet(), query = "")
      )
    }
  }

  /** Opens or closes the search field; closing it clears the query. */
  fun toggleSearch() {
    _uiState.update { derive(it.copy(searchOpen = !it.searchOpen, query = "")) }
  }

  /** Replaces the search text; a blank query hides nothing. */
  fun setQuery(query: String) {
    _uiState.update { derive(it.copy(query = query)) }
  }

  /** Hides the error snackbar. */
  fun dismissError() {
    _uiState.update { it.copy(errorMsg = null) }
  }

    /**********************************************************************************
     *                          Loading data
     **********************************************************************************/

  /**
   * The tasks listed in the user's [com.android.festivar.model.user.User.tasksAssigned] in which
   * they are an assignee: that list also holds the tasks they created without taking them. The
   * tasks are read in parallel.
   */
  private suspend fun loadAssignedTasks(): List<Task> = coroutineScope {
    val taskIds = dataRepository.usersRepository.getUser(userId).tasksAssigned
    taskIds
        .map { id -> async { dataRepository.tasksRepository.getTask(id) } }
        .awaitAll()
        .filter { task -> task.assignees.any { it.uid == userId } } // Removes tasks where the user is the owner and not an assignee
  }

  /**
   * The title of each event in [eventIds], read in parallel. An event that no longer exists maps to
   * null, so one missing event does not hide every task.
   */
  private suspend fun loadEventTitles(eventIds: List<String>): Map<String, String?> =
      coroutineScope {
        eventIds
            .map { id ->
              async {
                val title =
                    try {
                      dataRepository.eventsRepository.getEvent(id).title
                    } catch (_: NoSuchElementException) {
                      null
                    }
                id to title
              }
            }
            .awaitAll()
            .toMap()
      }




    /**********************************************************************************
     *                 Refresh & convert elements to UI-exploitable ones
     **********************************************************************************/


    /**
     * Reloads the user's tasks and the titles of their events; on failure the last sections stay and
     * [MyTasksUiState.errorMsg] is set. A load still running is canceled first, so the newest call
     * is the only one that can touch the state. Selected events that no longer hold any task are
     * dropped from the filter.
     */
    fun refresh() {
        load?.cancel()
        load = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val loaded = loadAssignedTasks()
                val titles = loadEventTitles(loaded.map { it.eventId }.distinct())
                tasks = loaded
                eventTitles = titles
                _uiState.update {
                    derive(
                        it.copy(
                            eventIds = it.eventIds.intersect(titles.keys),
                            isLoading = false,
                            errorMsg = null,
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMsg = e.message ?: e.toString()) }
            }
        }
    }

  /**
   * Rebuilds what the screen shows from [tasks] and the filters, search and clock in [state]: the
   * visible rows grouped by day, the counts of every chip and the events filter. Everything else in
   * [state] is kept as is.
   */
  private fun derive(state: MyTasksUiState): MyTasksUiState {
    val now = ZonedDateTime.now(clock)

      // We will give to the UIstate the filtered Tasks organized into sections of rows.
    val sections =
        tasks
            .filter { passes(it, state) }
            .map { row(it, now) }
            .groupBy { row -> row.task.startTime?.withZoneSameInstant(clock.zone)?.toLocalDate() }
            .toSortedMap(nullsLast())
            .map { (day, rows) ->
              MyTasksDaySection(
                  day,
                  rows.sortedWith(
                      compareBy<MyTaskRowUi, LocalTime?>(nullsLast()) { it.startTime }
                          .thenBy { it.task.title }
                  ),
              )
            }

      // We will give the UIstate the counts of each tasks present in the status filter.
    val statusCounts =
        MyTaskStatus.entries.associateWith { status ->
          tasks.count { passes(it, state, Group.STATUS) && statusOf(it) == status }
        }

      // We will give the UIstate the counts of each tasks present in the priority filter.
    val priorityCounts =
        Priority.entries.associateWith { priority ->
          tasks.count { passes(it, state, Group.PRIORITY) && it.priority == priority }
        }

      // We will give the UIstate all the event filters options here and the counts of each tasks present in the event filter.
    val events =
        eventTitles
            .map { (eventId, title) ->
              EventFilterOption(
                  eventId = eventId,
                  title = title,
                  count = tasks.count { passes(it, state, Group.EVENT) && it.eventId == eventId },
                  selected = eventId in state.eventIds,
              )
            }
            .sortedWith(
                compareBy<EventFilterOption, String?>(nullsLast()) { it.title }
                    .thenBy { it.eventId }
            )

    return state.copy(
        sections = sections,
        statusCounts = statusCounts,
        priorityCounts = priorityCounts,
        events = events,
        totalTasks = tasks.size,
    )
  }

  /**
   * Flattens [task] into the data one row shows, with its time in the phone's time zone.
   * Is a convertor of a [Task] into a [MyTaskRowUi] that may be used by the screen.
   */
  private fun row(task: Task, now: ZonedDateTime): MyTaskRowUi {
    val start = task.startTime?.withZoneSameInstant(clock.zone)
    val startsWithinTheHour =
        start != null && !task.completed && !start.isBefore(now) && !start.isAfter(now.plusHours(1))
    return MyTaskRowUi(
        task = task,
        eventTitle = eventTitles[task.eventId],
        startTime = start?.toLocalTime(),
        minutesUntilStart =
            if (startsWithinTheHour) Duration.between(now, start).toMinutes().toInt() else null,
    )
  }
}

/**
 * A utility function that allows to simply remove or add elements from a set.
 * This set with [value] added, or removed when it was already there.
 */
private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
