package com.android.festivar.ui.tasks

import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.temporary.User
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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
class TaskOverviewViewModelTest {
  private val zone = ZoneId.of("Europe/Zurich")
  private val clock = Clock.fixed(at(10, 0).atZone(zone).toInstant(), zone)
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

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    repository = TasksRepositoryLocal()
    runBlocking {
      listOf(power, tables, bar, bunting, sound, gate, litter, elsewhere).forEach {
        repository.addTask(it)
      }
    }
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun overview(userId: String? = me.uid, repo: TasksRepository = repository) =
      TaskOverviewViewModel(EVENT, userId, repo, clock)

  private fun TaskOverviewUiState.visibleIds() = sections.flatMap { section ->
    section.rows.map { it.task.taskId }
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
        mapOf(OverviewFilter.OPEN to 5, OverviewFilter.MINE to 1, OverviewFilter.DONE to 1),
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
   * is not NOW), the start-then-title sort or the section order changes.
   */
  @Test
  fun tasksAreGroupedIntoTheFiveGroupsInOrder() {
    runBlocking {
      listOf(
              Task("yesterday", EVENT, "Fold the tents", startTime = at(9, 0).minusDays(1)),
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

    assertEquals(OverviewGroup.entries.toList(), sections.map { it.group })
    assertEquals(
        listOf(
            listOf("tables", "power"),
            listOf("yesterday", "bar", "chairs", "bunting"),
            listOf("noon", "sound", "late"),
            listOf("six", "gate"),
            listOf("litter"),
        ),
        sections.map { section -> section.rows.map { it.task.taskId } },
    )
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

  /** Turns red when refresh stops reading the repository again. */
  @Test
  fun refreshShowsATaskAddedAfterTheFirstLoad() {
    val viewModel = overview()
    runBlocking { repository.addTask(Task("water", EVENT, "Fill the water tanks")) }

    viewModel.refresh()

    assertTrue("water" in viewModel.uiState.value.visibleIds())
    assertEquals(6, viewModel.uiState.value.counts[OverviewFilter.OPEN])
  }

  /** Reads through [inner] until [failing] is set, then every load throws. */
  private class FlakyRepository(private val inner: TasksRepositoryLocal) :
      TasksRepository by inner {
    var failing = false

    override suspend fun getAllTasks(eventId: String): List<Task> =
        if (failing) throw IllegalStateException("Network down") else inner.getAllTasks(eventId)
  }

  private companion object {
    const val EVENT = "fete"

    fun at(hour: Int, minute: Int): LocalDateTime = LocalDateTime.of(2026, 7, 1, hour, minute)
  }
}
