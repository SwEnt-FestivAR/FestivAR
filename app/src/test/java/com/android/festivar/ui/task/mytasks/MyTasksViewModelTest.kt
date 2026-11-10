// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import io.mockk.mockk
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyTasksViewModelTest {
  private val clock = Clock.fixed(at(10, 0).toInstant(), ZONE)
  private val me = User("me")

  private val power =
      task(
          "power",
          FETE,
          "Run power to stage",
          at(10, 30),
          Priority.URGENT,
          location = "Main stage",
      )
  private val amps = task("amps", FETE, "Carry the amps", at(10, 30), done = true)
  private val bar = task("bar", FETE, "Clean the bar", at(9, 0), Priority.URGENT, done = true)
  private val tables = task("tables", NOEL, "Set up tables", at(8, 0), Priority.HIGH)
  private val tents = task("tents", FETE, "Fold the tents", at(9, 0).plusDays(1), Priority.LOW)
  private val litter = task("litter", GONE, "Pick up litter", start = null)

  /** In [me]'s tasksAssigned because they created it, but they did not take it. */
  private val created = Task("created", FETE, "Draw the map", ownerId = me.uid)

  private val tasks = TasksRepositoryLocal()
  private val events = EventsRepositoryLocal()
  private val users = UsersRepositoryLocal()

  /**
   * Routes viewModelScope to the test dispatcher and stores the tasks above, [me] listing all of
   * them, and the events [FETE] and [NOEL]; [GONE] is left out, as an event that was deleted.
   */
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    runBlocking {
      val all = listOf(power, amps, bar, tables, tents, litter, created)
      all.forEach { tasks.addTask(it) }
      users.addUser(me.copy(tasksAssigned = all.map { it.taskId }))
      events.addEvent(event(FETE, "Summer fest"))
      events.addEvent(event(NOEL, "Christmas market"))
    }
  }

  /** Gives Dispatchers.Main back, so the next test class starts clean. */
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /**
   * A ViewModel signed in as [me] on the fixed clock, with one [MyTasksViewModel.refresh] called
   * the way the screen's first resume does.
   */
  private fun myTasks(
      usersRepo: UsersRepository = users,
      eventsRepo: EventsRepository = events,
      viewerClock: Clock = clock,
      userId: String = me.uid,
  ) =
      MyTasksViewModel(
              userId,
              DataRepositoryLocal(mockk(), tasks, eventsRepo, usersRepo),
              viewerClock,
          )
          .also { it.refresh() }

  /** The ids of every visible row, in section then row order. */
  private fun MyTasksUiState.visibleIds() = sections.flatMap { section ->
    section.rows.map { it.task.taskId }
  }

  /** Turns red when a task the user created without taking it shows, or the load reports one. */
  @Test
  fun onlyTheTasksTheUserTookAreShown() {
    val state = myTasks().uiState.value

    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(6, state.totalTasks)
    assertFalse("created" in state.visibleIds())
  }

  /**
   * Turns red when the days are out of order, the tasks without a start do not come last, or the
   * rows of a day are not sorted by start then title with done and to do mixed.
   */
  @Test
  fun tasksAreGroupedByDayAndSortedByStartThenTitle() {
    val sections = myTasks().uiState.value.sections

    assertEquals(listOf(DAY, DAY.plusDays(1), null), sections.map { it.day })
    assertEquals(
        listOf(listOf("tables", "bar", "amps", "power"), listOf("tents"), listOf("litter")),
        sections.map { section -> section.rows.map { it.task.taskId } },
    )
  }

  /**
   * Turns red when a row loses its event title or start, or counts down to a task that is done,
   * already started or more than an hour away. An event that no longer exists gives a null title.
   */
  @Test
  fun aRowCarriesItsEventStartAndCountdown() {
    val rows = myTasks().uiState.value.sections.flatMap { it.rows }.associateBy { it.task.taskId }

    val powerRow = rows.getValue("power")
    assertEquals("Summer fest", powerRow.eventTitle)
    assertEquals(LocalTime.of(10, 30), powerRow.startTime)
    assertEquals(30, powerRow.minutesUntilStart)
    assertNull(rows.getValue("amps").minutesUntilStart)
    assertNull(rows.getValue("bar").minutesUntilStart)
    assertNull(rows.getValue("tents").minutesUntilStart)
    assertNull(rows.getValue("litter").eventTitle)
    assertNull(rows.getValue("litter").startTime)
  }

  /** Turns red when a task is grouped or shown in the zone it was saved in, not the viewer's. */
  @Test
  fun tasksAreGroupedAndShownInTheViewersZone() {
    // 08:00 in Zurich is 23:00 the day before in Los Angeles, where the viewer is.
    val viewModel = myTasks(viewerClock = clock.withZone(ZoneId.of("America/Los_Angeles")))

    val first = viewModel.uiState.value.sections.first()
    assertEquals(DAY.minusDays(1), first.day)
    assertEquals(LocalTime.of(23, 0), first.rows.single().startTime)
  }

  /**
   * Turns red when the values of one group stop adding up (OR) or the groups stop narrowing each
   * other (AND), when a second toggle does not unselect, or when the events count as filters.
   */
  @Test
  fun filtersAreOrWithinAGroupAndAndAcrossGroups() {
    val viewModel = myTasks()

    viewModel.togglePriority(Priority.URGENT)
    viewModel.togglePriority(Priority.HIGH)
    assertEquals(listOf("tables", "bar", "power"), viewModel.uiState.value.visibleIds())

    viewModel.toggleEvent(FETE)
    viewModel.toggleStatus(MyTaskStatus.TODO)
    assertEquals(listOf("power"), viewModel.uiState.value.visibleIds())
    assertEquals(3, viewModel.uiState.value.activeFilterCount)

    viewModel.toggleStatus(MyTaskStatus.TODO)
    assertEquals(listOf("bar", "power"), viewModel.uiState.value.visibleIds())
  }

  /**
   * Turns red when a chip's count depends on the other chips of its own group, so that the
   * unselected ones drop to 0, or when the events filter loses its order by title, its missing
   * event last, a count or a checked box.
   */
  @Test
  fun eachCountFollowsTheOtherGroupsButNotItsOwn() {
    val viewModel = myTasks()

    viewModel.togglePriority(Priority.URGENT)
    viewModel.toggleEvent(FETE)

    val state = viewModel.uiState.value
    assertEquals(mapOf(MyTaskStatus.TODO to 1, MyTaskStatus.DONE to 1), state.statusCounts)
    assertEquals(
        mapOf(
            Priority.NONE to 1,
            Priority.LOW to 1,
            Priority.MEDIUM to 0,
            Priority.HIGH to 0,
            Priority.URGENT to 2,
        ),
        state.priorityCounts,
    )
    assertEquals(
        listOf(
            EventFilterOption(NOEL, "Christmas market", 0, selected = false),
            EventFilterOption(FETE, "Summer fest", 2, selected = true),
            EventFilterOption(GONE, null, 0, selected = false),
        ),
        state.events,
    )
  }

  /** Turns red when the search stops narrowing the rows and the counts, or survives its closing. */
  @Test
  fun theSearchNarrowsRowsAndCountsAndClosingItClearsIt() {
    val viewModel = myTasks()
    viewModel.toggleSearch()

    viewModel.setQuery("stage")
    assertEquals(listOf("power"), viewModel.uiState.value.visibleIds())
    assertEquals(
        mapOf(MyTaskStatus.TODO to 1, MyTaskStatus.DONE to 0),
        viewModel.uiState.value.statusCounts,
    )

    viewModel.toggleSearch()
    assertFalse(viewModel.uiState.value.searchOpen)
    assertEquals("", viewModel.uiState.value.query)
    assertEquals(6, viewModel.uiState.value.visibleIds().size)
  }

  /** Turns red when clearing the events touches the other filters, or clearing all misses one. */
  @Test
  fun clearEventsKeepsTheOtherFiltersAndClearFiltersDropsThemAll() {
    val viewModel = myTasks()
    viewModel.togglePriority(Priority.URGENT)
    viewModel.toggleStatus(MyTaskStatus.DONE)
    viewModel.toggleEvent(NOEL)
    viewModel.toggleSearch()
    viewModel.setQuery("bar")

    viewModel.clearEvents()
    var state = viewModel.uiState.value
    assertTrue(state.eventIdsSelected.isEmpty())
    assertEquals(setOf(Priority.URGENT), state.prioritiesSelected)
    assertEquals(listOf("bar"), state.visibleIds())

    viewModel.clearFilters()
    state = viewModel.uiState.value
    assertTrue(state.statusesSelected.isEmpty())
    assertTrue(state.prioritiesSelected.isEmpty())
    assertEquals("", state.query)
    assertEquals(6, state.visibleIds().size)
  }

  /** Turns red when "no tasks yet" and "nothing matches" are mixed up. */
  @Test
  fun noTasksAtAllAndNothingMatchingAreTwoStates() {
    runBlocking { users.addUser(User("newcomer")) }
    val newcomer = myTasks(userId = "newcomer").uiState.value
    assertTrue(newcomer.hasNoTasks)
    assertFalse(newcomer.nothingMatches)

    val viewModel = myTasks()
    viewModel.togglePriority(Priority.MEDIUM)
    assertFalse(viewModel.uiState.value.hasNoTasks)
    assertTrue(viewModel.uiState.value.nothingMatches)
  }

  /**
   * Turns red when a refresh resets the filters, or keeps checked an event the user no longer has
   * any task in, which would hide every task behind an invisible box.
   */
  @Test
  fun aRefreshKeepsTheFiltersButUnchecksTheEventsThatAreGone() {
    val viewModel = myTasks()
    viewModel.togglePriority(Priority.URGENT)
    viewModel.toggleEvent(FETE)
    viewModel.toggleEvent(NOEL)
    runBlocking {
      val withoutTables = users.getUser(me.uid).tasksAssigned - tables.taskId
      users.editUser(me.uid, me.copy(tasksAssigned = withoutTables))
    }

    viewModel.refresh()

    val state = viewModel.uiState.value
    assertEquals(setOf(Priority.URGENT), state.prioritiesSelected)
    assertEquals(setOf(FETE), state.eventIdsSelected)
    assertEquals(listOf("bar", "power"), state.visibleIds())
  }

  /**
   * Turns red when a failed load keeps loading, hides its error, claims the user has no tasks or
   * drops the rows of the last load, or when the error does not go once a load succeeds or the
   * snackbar is dismissed.
   */
  @Test
  fun aFailedLoadSetsTheErrorAndKeepsTheLastRows() {
    val flaky = FlakyUsers()
    flaky.failing = true
    val viewModel = myTasks(usersRepo = flaky)
    var state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Network down", state.errorMsg)
    assertFalse(state.hasNoTasks)

    flaky.failing = false
    viewModel.refresh()
    val loaded = viewModel.uiState.value.sections
    assertNull(viewModel.uiState.value.errorMsg)

    flaky.failing = true
    viewModel.refresh()
    state = viewModel.uiState.value
    assertEquals("Network down", state.errorMsg)
    assertEquals(loaded, state.sections)

    viewModel.dismissError()
    assertNull(viewModel.uiState.value.errorMsg)
    assertEquals(loaded, viewModel.uiState.value.sections)
  }

  /** Turns red when any event read failure is taken for a missing event instead of an error. */
  @Test
  fun anEventThatCannotBeReadFailsTheLoad() {
    val broken =
        object : EventsRepository by events {
          override suspend fun getEvent(eventId: String): Event =
              throw IllegalStateException("Network down")
        }

    assertEquals("Network down", myTasks(eventsRepo = broken).uiState.value.errorMsg)
  }

  /**
   * Turns red when a task still listed in tasksAssigned but deleted fails the load, which would
   * hide the tasks that do exist behind an error no refresh can clear.
   */
  @Test
  fun aTaskThatNoLongerExistsIsSkipped() {
    runBlocking {
      val assigned = users.getUser(me.uid).tasksAssigned + "deleted"
      users.editUser(me.uid, me.copy(tasksAssigned = assigned))
    }

    val state = myTasks().uiState.value

    assertNull(state.errorMsg)
    assertEquals(6, state.totalTasks)
  }

  /** Turns red when the ViewModel swallows a cancellation and reports it as an error. */
  @Test
  fun aCancelledLoadIsNotReportedAsAnError() {
    val cancelling =
        object : UsersRepository by users {
          override suspend fun getUser(uId: String): User =
              throw CancellationException("Left the screen")
        }

    assertNull(myTasks(usersRepo = cancelling).uiState.value.errorMsg)
  }

  /**
   * Turns red when the ViewModel loads at construction, when a refresh does not show it is loading,
   * or when an older load that answers late overwrites a newer one.
   */
  @Test
  fun theNewestRefreshWinsOverAnOlderLoadThatAnswersLate() {
    val slow = SlowUsers()
    val viewModel =
        MyTasksViewModel(me.uid, DataRepositoryLocal(mockk(), tasks, events, slow), clock)
    assertTrue(slow.calls.isEmpty())

    viewModel.refresh()
    viewModel.refresh()
    assertTrue(viewModel.uiState.value.isLoading)

    slow.calls[1].complete(runBlocking { users.getUser(me.uid) })
    slow.calls[0].complete(me.copy(tasksAssigned = listOf(bar.taskId)))

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(6, state.totalTasks)
  }

  /** Reads through [users] until [failing] is set, then every user read throws. */
  private inner class FlakyUsers : UsersRepository by users {
    var failing = false

    override suspend fun getUser(uId: String): User =
        if (failing) throw IllegalStateException("Network down") else users.getUser(uId)
  }

  /** Answers each user read only when the test completes that call's entry in [calls]. */
  private inner class SlowUsers : UsersRepository by users {
    val calls = mutableListOf<CompletableDeferred<User>>()

    override suspend fun getUser(uId: String): User =
        CompletableDeferred<User>().also { calls += it }.await()
  }

  /** A task [me] has taken. */
  private fun task(
      id: String,
      eventId: String,
      title: String,
      start: ZonedDateTime?,
      priority: Priority = Priority.NONE,
      done: Boolean = false,
      location: String = "",
  ) =
      Task(
          taskId = id,
          eventId = eventId,
          title = title,
          startTime = start,
          location = location,
          priority = priority,
          assignees = listOf(me),
          completed = done,
          ownerId = "owner-1",
      )

  private fun event(id: String, title: String) =
      Event(eventId = id, title = title, startDate = at(0, 0), endDate = at(0, 0).plusDays(3))

  private companion object {
    const val FETE = "fete"
    const val NOEL = "noel"
    const val GONE = "gone"
    val DAY: LocalDate = LocalDate.of(2026, 7, 1)
    val ZONE: ZoneId = ZoneId.of("Europe/Zurich")

    /** A time on [DAY] in [ZONE]. */
    fun at(hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(DAY, LocalTime.of(hour, minute), ZONE)
  }
}
