// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.ui.tasks

import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
class TaskOverviewViewModelTest {
  private val zone = ZoneId.of("Europe/Zurich")
  private val clock = SettableClock(at(10, 0).atZone(zone).toInstant(), zone)
  private val dispatcher = UnconfinedTestDispatcher()
  private val me = User("me")
  private val ana = User("ana")

  private val power =
      Task(
          taskId = "power",
          eventId = EVENT,
          title = "Run power to stage",
          startTime = at(10, 30),
          estimatedTime = Duration.ofMinutes(20),
          location = "Stage, north corner",
          maxAssign = 2,
          assignees = listOf(me),
      )
  private val tables = Task("tables", EVENT, "Set up tables", startTime = at(8, 0))
  private val bar = Task("bar", EVENT, "Clean the bar", startTime = at(9, 0), completed = true)
  private val bunting =
      Task(
          "bunting",
          EVENT,
          "Hang bunting",
          startTime = at(11, 30),
          estimatedTime = Duration.ofMinutes(90),
          location = "Marquee A",
      )
  private val sound =
      Task("sound", EVENT, "Sound check", startTime = at(15, 0), assignees = listOf(ana))
  private val gate = Task("gate", EVENT, "Close the gate", startTime = at(20, 0))
  private val litter = Task("litter", EVENT, "Pick up litter")
  private val elsewhere =
      Task("elsewhere", "other-event", "Other event's task", startTime = at(10, 30))

  private lateinit var repository: TasksRepositoryLocal

  /** Routes viewModelScope to the test dispatcher and seeds the eight tasks above. */
  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = TasksRepositoryLocal()
    runBlocking {
      listOf(power, tables, bar, bunting, sound, gate, litter, elsewhere).forEach {
        repository.addTask(it)
      }
    }
  }

  /** Gives Dispatchers.Main back, so the next test class starts clean. */
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** A ViewModel on [EVENT] with the settable clock, signed in as [me] unless told otherwise. */
  private fun overview(userId: String? = me.uid, repo: TasksRepository = repository) =
      TaskOverviewViewModel(EVENT, userId, repo, clock)

  /** The ids of every visible row, in section then row order. */
  private fun TaskOverviewUiState.visibleIds() = sections.flatMap { section ->
    section.rows.map { it.task.taskId }
  }

  /** The ids of the rows under [group], across days, in section then row order. */
  private fun TaskOverviewUiState.idsIn(group: OverviewGroup) =
      sections
          .filter { it.group == group }
          .flatMap { section -> section.rows.map { it.task.taskId } }

  /** Collects [TaskOverviewViewModel.uiState] the way a screen does; cancel it to leave. */
  private fun watch(viewModel: TaskOverviewViewModel): Job =
      CoroutineScope(dispatcher).launch { viewModel.uiState.collect {} }

  /** Moves the clock and the coroutine scheduler together, as wall time would. */
  private fun tick(by: Duration) {
    clock.advance(by)
    dispatcher.scheduler.advanceTimeBy(by.toMillis())
    dispatcher.scheduler.runCurrent()
  }

  /** Turns red when the ViewModel stops asking the repository for [EVENT]'s tasks only. */
  @Test
  fun onlyThisEventsTasksAppear() {
    val viewModel = overview()
    viewModel.selectFilter(OverviewFilter.ALL)

    val ids = viewModel.uiState.value.visibleIds()

    assertEquals(7, ids.size)
    assertFalse("elsewhere" in ids)
  }

  /** Turns red when a filter rule (open, mine, done, all) or the counts change. */
  @Test
  fun eachFilterShowsItsTasksAndTheCountsHoldOpenMineAndDone() {
    val viewModel = overview()
    val state = viewModel.uiState.value

    assertEquals(OverviewFilter.OPEN, state.filter)
    assertFalse(state.isLoading)
    assertEquals(
        mapOf(
            OverviewFilter.OPEN to 5,
            OverviewFilter.MINE to 1,
            OverviewFilter.DONE to 1,
            OverviewFilter.ALL to 7,
        ),
        state.counts,
    )
    assertEquals(
        setOf("tables", "power", "bunting", "gate", "litter"),
        state.visibleIds().toSet(),
    )

    viewModel.selectFilter(OverviewFilter.MINE)
    assertEquals(listOf("power"), viewModel.uiState.value.visibleIds())

    viewModel.selectFilter(OverviewFilter.DONE)
    assertEquals(listOf("bar"), viewModel.uiState.value.visibleIds())

    viewModel.selectFilter(OverviewFilter.ALL)
    assertEquals(7, viewModel.uiState.value.visibleIds().size)
  }

  /** Turns red when MINE matches tasks without a signed-in user's uid. */
  @Test
  fun mineNeedsTheUsersUid() {
    val viewModel = overview(userId = null)
    viewModel.selectFilter(OverviewFilter.MINE)

    val state = viewModel.uiState.value

    assertEquals(0, state.counts[OverviewFilter.MINE])
    assertTrue(state.sections.isEmpty())
  }

  /** Turns red when search stops matching title or location case-insensitively, or blank. */
  @Test
  fun searchMatchesTitleOrLocationIgnoringCaseAndBlankResets() {
    val viewModel = overview()
    viewModel.toggleSearch()

    viewModel.setQuery("  POWER ")
    assertEquals(listOf("power"), viewModel.uiState.value.visibleIds())

    viewModel.setQuery("marquee")
    assertEquals(listOf("bunting"), viewModel.uiState.value.visibleIds())
    assertEquals(5, viewModel.uiState.value.counts[OverviewFilter.OPEN])

    viewModel.setQuery("   ")
    assertEquals(5, viewModel.uiState.value.visibleIds().size)
  }

  /** Turns red when closing the search keeps the old query filtering the rows. */
  @Test
  fun closingTheSearchClearsTheQuery() {
    val viewModel = overview()
    viewModel.toggleSearch()
    viewModel.setQuery("power")

    viewModel.toggleSearch()

    assertFalse(viewModel.uiState.value.searchOpen)
    assertEquals("", viewModel.uiState.value.query)
    assertEquals(5, viewModel.uiState.value.visibleIds().size)
  }

  /**
   * Turns red when a group boundary (12:00, 18:00), the NOW rule (an unfinished task from yesterday
   * is not NOW), the start-then-title sort, the section order, or the day split changes:
   * yesterday's and tomorrow's 09:00 tasks must not share today's MORNING section.
   */
  @Test
  fun tasksAreGroupedByDayThenIntoTheFiveGroupsInOrder() {
    runBlocking {
      listOf(
              Task("yesterday", EVENT, "Fold the tents", startTime = at(9, 0).minusDays(1)),
              Task("tomorrow", EVENT, "Unfold the tents", startTime = at(9, 0).plusDays(1)),
              Task("chairs", EVENT, "Arrange chairs", startTime = at(11, 30)),
              Task("noon", EVENT, "Serve lunch", startTime = at(12, 0)),
              Task("late", EVENT, "Clear the tables", startTime = at(17, 59)),
              Task("six", EVENT, "Light the lanterns", startTime = at(18, 0)),
          )
          .forEach { repository.addTask(it) }
    }
    val viewModel = overview()
    viewModel.selectFilter(OverviewFilter.ALL)

    val sections = viewModel.uiState.value.sections

    val yesterday = DAY.minusDays(1)
    val tomorrow = DAY.plusDays(1)
    assertEquals(
        listOf(
            yesterday to OverviewGroup.MORNING,
            DAY to OverviewGroup.NOW,
            DAY to OverviewGroup.MORNING,
            DAY to OverviewGroup.AFTERNOON,
            DAY to OverviewGroup.EVENING,
            tomorrow to OverviewGroup.MORNING,
            null to OverviewGroup.ANYTIME,
        ),
        sections.map { it.day to it.group },
    )
    assertEquals(
        listOf(
            listOf("yesterday"),
            listOf("tables", "power"),
            listOf("bar", "chairs", "bunting"),
            listOf("noon", "sound", "late"),
            listOf("six", "gate"),
            listOf("tomorrow"),
            listOf("litter"),
        ),
        sections.map { section -> section.rows.map { it.task.taskId } },
    )
  }

  /**
   * Turns red when the NOW group stops following the clock while the screen watches, or regroups
   * before a task actually changes group: bunting (11:30) comes within the hour at 10:30 exactly.
   */
  @Test
  fun nowFollowsTheClockWhileTheScreenWatches() {
    val viewModel = overview()
    val screen = watch(viewModel)
    assertEquals(listOf("tables", "power"), viewModel.uiState.value.idsIn(OverviewGroup.NOW))

    tick(Duration.ofMinutes(29))
    assertEquals(listOf("tables", "power"), viewModel.uiState.value.idsIn(OverviewGroup.NOW))

    tick(Duration.ofMinutes(1))
    assertEquals(
        listOf("tables", "power", "bunting"),
        viewModel.uiState.value.idsIn(OverviewGroup.NOW),
    )
    screen.cancel()
  }

  /**
   * Turns red when the clock is followed with nobody collecting the state, which would keep a
   * backgrounded screen's ViewModel working, or when a returning screen is not regrouped at once.
   */
  @Test
  fun nothingRegroupsWhileNobodyWatchesAndAReturningScreenCatchesUp() {
    val viewModel = overview()

    tick(Duration.ofHours(2))
    assertEquals(listOf("tables", "power"), viewModel.uiState.value.idsIn(OverviewGroup.NOW))

    val screen = watch(viewModel)
    assertEquals(
        listOf("tables", "power", "bunting"),
        viewModel.uiState.value.idsIn(OverviewGroup.NOW),
    )
    screen.cancel()
  }

  /**
   * Turns red when a started task without an end leaves NOW before midnight, or stays in NOW past
   * it: at 23:59:59 every unfinished task started today is NOW, full or not, one second later none
   * is.
   */
  @Test
  fun aTaskWithoutAnEndIsNowUntilMidnightOfItsDay() {
    val viewModel = overview()
    val screen = watch(viewModel)
    viewModel.selectFilter(OverviewFilter.ALL)

    tick(Duration.ofHours(13).plusMinutes(59).plusSeconds(59))
    assertEquals(
        listOf("tables", "power", "bunting", "sound", "gate"),
        viewModel.uiState.value.idsIn(OverviewGroup.NOW),
    )

    tick(Duration.ofSeconds(1))
    val sections = viewModel.uiState.value.sections
    assertEquals(
        listOf(
            DAY to OverviewGroup.MORNING,
            DAY to OverviewGroup.AFTERNOON,
            DAY to OverviewGroup.EVENING,
            null to OverviewGroup.ANYTIME,
        ),
        sections.map { it.day to it.group },
    )
    assertEquals(
        listOf("tables", "bar", "power", "bunting"),
        sections.first().rows.map { it.task.taskId },
    )
    screen.cancel()
  }

  /**
   * Turns red when the day does not turn for the NOW section itself: a task starting at 00:15
   * tomorrow is NOW from 23:15, under today, and must sit under tomorrow once midnight has passed.
   */
  @Test
  fun theNowSectionMovesToTheNewDayAtMidnight() {
    val onlyDawn = TasksRepositoryLocal()
    runBlocking {
      onlyDawn.addTask(Task("dawn", EVENT, "Open the gates", startTime = at(0, 15).plusDays(1)))
    }
    val viewModel = overview(repo = onlyDawn)
    val screen = watch(viewModel)

    tick(Duration.ofHours(13).plusMinutes(30))
    val beforeMidnight = viewModel.uiState.value.sections.single()
    assertEquals(OverviewGroup.NOW to DAY, beforeMidnight.group to beforeMidnight.day)

    tick(Duration.ofMinutes(30))
    val afterMidnight = viewModel.uiState.value.sections.single()
    assertEquals(OverviewGroup.NOW to DAY.plusDays(1), afterMidnight.group to afterMidnight.day)
    assertEquals(listOf("dawn"), afterMidnight.rows.map { it.task.taskId })
    screen.cancel()
  }

  /**
   * Turns red when a task with an end time stays in NOW after it has ended (bunting, 11:30, is
   * rightly within the hour).
   */
  @Test
  fun aTaskWithAnEndLeavesNowWhenItEnds() {
    runBlocking {
      repository.addTask(
          Task("lights", EVENT, "Focus the lights", startTime = at(10, 30), endTime = at(10, 45))
      )
    }
    val viewModel = overview()
    val screen = watch(viewModel)
    assertEquals(
        listOf("tables", "lights", "power"),
        viewModel.uiState.value.idsIn(OverviewGroup.NOW),
    )

    tick(Duration.ofMinutes(45))

    assertEquals(
        listOf("tables", "power", "bunting"),
        viewModel.uiState.value.idsIn(OverviewGroup.NOW),
    )
    assertEquals(listOf("lights"), viewModel.uiState.value.idsIn(OverviewGroup.MORNING))
    screen.cancel()
  }

  /** Turns red when a row stops carrying the task's place, people, length, start or state. */
  @Test
  fun aFullTaskRowCarriesItsData() {
    val viewModel = overview()
    viewModel.selectFilter(OverviewFilter.ALL)
    val rows = viewModel.uiState.value.sections.flatMap { it.rows }.associateBy { it.task.taskId }

    val powerRow = rows.getValue("power")
    assertEquals("Stage, north corner", powerRow.location)
    assertEquals(2, powerRow.peopleNeeded)
    assertEquals(20, powerRow.estimatedMinutes)
    assertEquals(LocalTime.of(10, 30), powerRow.startTime)
    assertEquals(1, powerRow.assigned)
    assertTrue(powerRow.isMine)
    assertFalse(powerRow.isFull)

    assertEquals(90, rows.getValue("bunting").estimatedMinutes)
    assertTrue(rows.getValue("sound").isFull)
    assertFalse(rows.getValue("sound").isMine)
  }

  /** Turns red when the OPEN chip and the full badge disagree on when a task is full. */
  @Test
  fun aFullTaskIsNeverOpen() {
    val viewModel = overview()
    viewModel.selectFilter(OverviewFilter.ALL)
    val full = viewModel.uiState.value.sections.flatMap { it.rows }.filter { it.isFull }

    viewModel.selectFilter(OverviewFilter.OPEN)

    assertEquals(listOf("sound"), full.map { it.task.taskId })
    assertTrue(full.none { it.task.taskId in viewModel.uiState.value.visibleIds() })
  }

  /** Turns red when a task without start or estimate gets invented values. */
  @Test
  fun aTaskWithNullsHasNullStartAndLength() {
    val litterRow = overview().uiState.value.sections.last().rows.single()

    assertEquals("", litterRow.location)
    assertEquals(1, litterRow.peopleNeeded)
    assertNull(litterRow.estimatedMinutes)
    assertNull(litterRow.startTime)
    assertEquals(0, litterRow.assigned)
    assertFalse(litterRow.isMine)
    assertFalse(litterRow.isFull)
  }

  /** Turns red when a failed load drops the sections, hides the message or keeps loading. */
  @Test
  fun aThrowingRepositoryKeepsTheLastSectionsAndSetsTheError() {
    val flaky = FlakyRepository(repository)
    val viewModel = overview(repo = flaky)
    val before = viewModel.uiState.value.sections

    flaky.failing = true
    viewModel.refresh()

    val state = viewModel.uiState.value
    assertEquals("Network down", state.errorMsg)
    assertFalse(state.isLoading)
    assertEquals(before, state.sections)
  }

  /** Turns red when a failing first load stays loading, hides the error or shows rows. */
  @Test
  fun aThrowingFirstLoadEndsLoadingWithTheErrorAndNoSections() {
    val viewModel = overview(repo = FlakyRepository(repository).apply { failing = true })

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Network down", state.errorMsg)
    assertTrue(state.sections.isEmpty())
  }

  /** Turns red when the ViewModel swallows cancellation and reports it as an error. */
  @Test
  fun aCancelledLoadIsNotReportedAsAnError() {
    val cancelling =
        object : TasksRepository by repository {
          override suspend fun getAllTasks(eventId: String): List<Task> =
              throw CancellationException("Left the screen")
        }

    val state = overview(repo = cancelling).uiState.value

    assertNull(state.errorMsg)
    assertTrue(state.sections.isEmpty())
  }

  /** Turns red when refresh stops reading the repository again. */
  @Test
  fun refreshShowsATaskAddedAfterTheFirstLoad() {
    val viewModel = overview()
    runBlocking { repository.addTask(Task("water", EVENT, "Fill the water tanks")) }

    viewModel.refresh()

    assertTrue("water" in viewModel.uiState.value.visibleIds())
    assertEquals(6, viewModel.uiState.value.counts[OverviewFilter.OPEN])
  }

  /** Turns red when a manual refresh runs without showing progress, or drops the last rows. */
  @Test
  fun aRefreshIsLoadingUntilTheRepositoryAnswers() {
    val slow = SlowRepository()
    val viewModel = overview(repo = slow)
    slow.calls[0].complete(listOf(power))
    assertFalse(viewModel.uiState.value.isLoading)

    viewModel.refresh()

    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(listOf("power"), viewModel.uiState.value.visibleIds())

    slow.calls[1].complete(listOf(power, tables))
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(listOf("tables", "power"), viewModel.uiState.value.visibleIds())
  }

  /** Turns red when an older, slower load lands after a newer one and overwrites its rows. */
  @Test
  fun theNewestRefreshWinsOverAnOlderLoadThatAnswersLate() {
    val slow = SlowRepository()
    val viewModel = overview(repo = slow)

    viewModel.refresh()
    slow.calls[1].complete(listOf(power, tables))
    slow.calls[0].complete(listOf(litter))

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(listOf("tables", "power"), state.visibleIds())
  }

  /** Turns red when an older load that fails late puts its error over a newer, successful one. */
  @Test
  fun anOlderLoadThatFailsLateDoesNotSetTheError() {
    val slow = SlowRepository()
    val viewModel = overview(repo = slow)

    viewModel.refresh()
    slow.calls[1].complete(listOf(power))
    slow.calls[0].completeExceptionally(IllegalStateException("Network down"))

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(listOf("power"), state.visibleIds())
  }

  /** Reads through [inner] until [failing] is set, then every load throws. */
  private class FlakyRepository(private val inner: TasksRepositoryLocal) :
      TasksRepository by inner {
    var failing = false

    override suspend fun getAllTasks(eventId: String): List<Task> =
        if (failing) throw IllegalStateException("Network down") else inner.getAllTasks(eventId)
  }

  /** Answers each load only when the test completes that call's entry in [calls]. */
  private inner class SlowRepository : TasksRepository by repository {
    val calls = mutableListOf<CompletableDeferred<List<Task>>>()

    override suspend fun getAllTasks(eventId: String): List<Task> =
        CompletableDeferred<List<Task>>().also { calls += it }.await()
  }

  /** A clock the test moves by hand, so the NOW group can be watched over time. */
  private class SettableClock(private var now: Instant, private val zone: ZoneId) : Clock() {
    /** Moves the clock forward; the coroutine scheduler is moved separately by [tick]. */
    fun advance(by: Duration) {
      now += by
    }

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SettableClock(now, zone)

    override fun instant(): Instant = now
  }

  private companion object {
    const val EVENT = "fete"
    val DAY: LocalDate = LocalDate.of(2026, 7, 1)

    /** A time on [DAY], the day every seeded task lives on. */
    fun at(hour: Int, minute: Int): LocalDateTime = DAY.atTime(hour, minute)
  }
}
