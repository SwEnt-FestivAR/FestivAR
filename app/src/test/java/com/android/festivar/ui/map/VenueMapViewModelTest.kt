// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.location.LocationSource
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.model.structure.StructuresRepository
import com.android.festivar.model.structure.StructuresRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TaskSpot
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import java.io.IOException
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** An [EventsRepositoryLocal] whose [getEvent] can be made to fail or to wait. */
private class ControlledEvents(val local: EventsRepositoryLocal) : EventsRepository by local {
  /** Thrown by [getEvent] when not null. */
  var failure: Exception? = null

  /** While not null, [getEvent] waits for it to complete. */
  var gate: CompletableDeferred<Unit>? = null

  /** How many times [getEvent] was called. */
  var calls = 0

  override suspend fun getEvent(eventId: String): Event {
    calls++
    gate?.await()
    failure?.let { throw it }
    return local.getEvent(eventId)
  }
}

/** A [StructuresRepositoryLocal] whose [getAllStructures] can be made to fail. */
private class ControlledStructures(val local: StructuresRepositoryLocal) :
    StructuresRepository by local {
  /** Thrown by [getAllStructures] when not null. */
  var failure: Exception? = null

  override suspend fun getAllStructures(eventId: String): List<Structure> {
    failure?.let { throw it }
    return local.getAllStructures(eventId)
  }
}

/** A [TasksRepository] serving [tasks], or throwing [failure] when it is not null. */
private class ControlledTasks(var tasks: List<Task>) : TasksRepository by TasksRepositoryLocal() {
  /** Thrown by [getAllTasks] when not null. */
  var failure: Exception? = null

  override suspend fun getAllTasks(eventId: String): List<Task> {
    failure?.let { throw it }
    return tasks.filter { it.eventId == eventId }
  }
}

/** A [LocationSource] that counts how many times [updates] was collected from. */
private class CountingLocationSource : LocationSource {
  val fixes = MutableSharedFlow<LocationFix>()
  var subscriptions = 0

  override fun updates(): Flow<LocationFix> {
    subscriptions++
    return fixes
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VenueMapViewModelTest {
  private val start = ZonedDateTime.parse("2026-07-01T14:00:00+02:00")
  private val venue = Venue(46.5170, 6.5990, 0.0)
  private val event =
      Event(
          eventId = "e1",
          title = "Fête de Vidy",
          location = "Parc de Vidy, Lausanne",
          venue = venue,
          startDate = start,
          endDate = start.plusHours(10),
      )
  private val stage = Structure("s1", "e1", StructureKind.STAGE, "Stage", -5.0, -10.0, 10.0, 6.0)
  private val bar = Structure("s2", "e1", StructureKind.STAND, "Bar", 8.0, 2.0, 3.0, 2.5)
  private val elsewhere = Structure("s3", "e2", StructureKind.TABLE, "Other", 0.0, 0.0, 2.0, 2.0)

  private val me = User("me", "Ada", "Lovelace")
  private val sami = User("u2", "Sami", "Kader")
  // Mine, on the stage, whose centre is (0, -7); starts 40 minutes after the fixed clock.
  private val power =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          location = "Stage, north corner",
          startTime = start.plusMinutes(40),
          maxAssign = 2,
          assignees = listOf(me, sami),
          spot = TaskSpot.onStructure("s1"),
          ownerId = "o",
      )
  private val ice =
      Task(
          taskId = "t2",
          eventId = "e1",
          title = "Ice bags",
          location = "Bar",
          spot = TaskSpot.at(90.0, 120.0),
          ownerId = "o",
      )
  private val gate =
      Task(
          taskId = "t3",
          eventId = "e1",
          title = "Gate",
          assignees = listOf(sami),
          spot = TaskSpot.at(1.0, 1.0),
          ownerId = "o",
      )
  private val flyers =
      Task(
          taskId = "t4",
          eventId = "e1",
          title = "Flyers",
          completed = true,
          spot = TaskSpot.at(2.0, 2.0),
          ownerId = "o",
      )
  private val unplaced = Task(taskId = "t5", eventId = "e1", title = "Unplaced", ownerId = "o")
  private val ghost =
      Task(
          taskId = "t6",
          eventId = "e1",
          title = "Ghost",
          assignees = listOf(me),
          spot = TaskSpot.onStructure("gone"),
          ownerId = "o",
      )
  private val allTasks = listOf(power, ice, gate, flyers, unplaced, ghost)

  private lateinit var events: ControlledEvents
  private lateinit var structures: ControlledStructures
  private lateinit var tasks: ControlledTasks

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    events = ControlledEvents(EventsRepositoryLocal(listOf(event)))
    structures = ControlledStructures(StructuresRepositoryLocal(listOf(stage, bar, elsewhere)))
    tasks = ControlledTasks(allTasks)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(source: LocationSource? = null) =
      VenueMapViewModel("e1", "me", events, structures, source, tasks, clock = { start })

  /** A loaded state with every test task, the filter on ALL so every placed task is a pin. */
  private fun allState() =
      VenueMapUiState(
          isLoading = false,
          venue = venue,
          structures = listOf(stage, bar),
          tasks = allTasks,
          filter = PinFilter.ALL,
          currentUserId = "me",
          now = start,
      )

  private fun pointAt(x: Double, y: Double) =
      VenueGeometry.latLngFrom(venue.latitude, venue.longitude, venue.bearing, x, y)

  /** Red if load drops a field of the event or keeps structures of another event. */
  @Test
  fun loadFillsTitleLocationVenueAndStructures() {
    val vm = viewModel().also { it.load() }

    val state = vm.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Fête de Vidy", state.eventTitle)
    assertEquals("Parc de Vidy, Lausanne", state.eventLocation)
    assertEquals(venue, state.venue)
    assertEquals(listOf(stage, bar), state.structures)
    assertNull(state.errorMsg)
  }

  /** Red if an event without venue leaves the screen loading or invents a venue. */
  @Test
  fun anEventWithoutVenueLeavesVenueNullAndStopsLoading() {
    events = ControlledEvents(EventsRepositoryLocal(listOf(event.copy(venue = null))))
    val vm = viewModel().also { it.load() }

    assertNull(vm.uiState.value.venue)
    assertFalse(vm.uiState.value.isLoading)
  }

  /** Red if a failing event read is swallowed or leaves the spinner on. */
  @Test
  fun aFailingEventsRepositorySetsTheErrorAndStopsLoading() {
    events.failure = IOException("offline")
    val vm = viewModel().also { it.load() }

    assertEquals(R.string.map_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
  }

  /** Red if a structures failure throws away the event that did load. */
  @Test
  fun aFailingStructuresRepositoryKeepsTheEventAndSetsTheError() {
    structures.failure = IOException("offline")
    val vm = viewModel().also { it.load() }

    val state = vm.uiState.value
    assertEquals("Fête de Vidy", state.eventTitle)
    assertEquals(venue, state.venue)
    assertEquals(R.string.map_load_failed, state.errorMsg)
    assertFalse(state.isLoading)
  }

  /** Red if a successful retry keeps showing the old error. */
  @Test
  fun retryAfterAFailureClearsTheError() {
    events.failure = IOException("offline")
    val vm = viewModel().also { it.load() }
    events.failure = null

    vm.load()

    assertNull(vm.uiState.value.errorMsg)
    assertEquals(listOf(stage, bar), vm.uiState.value.structures)
  }

  /** Red if clearError leaves the message. */
  @Test
  fun clearErrorRemovesTheMessage() {
    events.failure = IOException("offline")
    val vm = viewModel().also { it.load() }

    vm.clearError()

    assertNull(vm.uiState.value.errorMsg)
  }

  /** Red if selecting does not reach the derived selected structure, or null does not clear it. */
  @Test
  fun selectAndSelectNull() {
    val vm = viewModel().also { it.load() }

    vm.select("s2")
    assertEquals(bar, vm.uiState.value.selected)

    vm.select(null)
    assertNull(vm.uiState.value.selected)
    assertNull(vm.uiState.value.selectedStructureId)
  }

  /** Red if fixes arrive out of order or a second call subscribes twice. */
  @Test
  fun observeLocationFeedsFixesInOrderAndSubscribesOnce() = runTest {
    val source = CountingLocationSource()
    val vm = viewModel(source)

    vm.observeLocation()
    vm.observeLocation()
    val first = LocationFix(46.5170, 6.5990, 8f)
    val second = LocationFix(46.5171, 6.5991, 4f)
    source.fixes.emit(first)
    assertEquals(first, vm.uiState.value.me)
    source.fixes.emit(second)
    assertEquals(second, vm.uiState.value.me)

    assertEquals(1, source.subscriptions)
    assertEquals(1, source.fixes.subscriptionCount.value)
  }

  /** Red if a missing location source crashes or invents a position. */
  @Test
  fun aNullLocationSourceLeavesMeNull() {
    val vm = viewModel(source = null)

    vm.observeLocation()

    assertNull(vm.uiState.value.me)
  }

  /** Red if a second load starts while the first is still waiting on the event. */
  @Test
  fun loadWhileLoadingDoesNotStartASecondLoad() {
    val gate = CompletableDeferred<Unit>()
    events.gate = gate
    val vm = viewModel()

    vm.load()
    vm.load()
    gate.complete(Unit)

    assertEquals(1, events.calls)
    assertFalse(vm.uiState.value.isLoading)
  }

  /** Red if load skips the tasks or keeps tasks of another event. */
  @Test
  fun loadFillsTheTasksOfTheEvent() {
    tasks.tasks = allTasks + ice.copy(taskId = "other", eventId = "e2")
    val vm = viewModel().also { it.load() }

    assertEquals(allTasks, vm.uiState.value.tasks)
    assertEquals("me", vm.uiState.value.currentUserId)
    assertEquals(start, vm.uiState.value.now)
  }

  /** Red if a failing tasks read throws away the venue and structures that did load. */
  @Test
  fun aFailingTasksRepositoryKeepsTheVenueAndSetsTheError() {
    tasks.failure = IOException("offline")
    val vm = viewModel().also { it.load() }

    val state = vm.uiState.value
    assertEquals(venue, state.venue)
    assertEquals(listOf(stage, bar), state.structures)
    assertEquals(R.string.map_load_failed, state.errorMsg)
    assertFalse(state.isLoading)
  }

  /** Red if a task without spot, or a spot on a deleted structure, is drawn as a pin. */
  @Test
  fun tasksWithoutSpotOrOnAMissingStructureAreNotPins() {
    val ids = allState().pins.map { it.taskId }

    assertEquals(listOf("t1", "t2", "t3", "t4"), ids)
  }

  /** Red if a pin on a structure misses its centre or a free point is moved. */
  @Test
  fun aPinOnAStructureSitsAtItsCentreAndAFreePointAtItsPoint() {
    val pins = allState().pins.associateBy { it.taskId }

    val onStage = pins.getValue("t1").point
    val expectedStage = pointAt(0.0, -7.0)
    assertEquals(expectedStage.latitude, onStage.latitude, 1e-9)
    assertEquals(expectedStage.longitude, onStage.longitude, 1e-9)
    val free = pins.getValue("t2").point
    val expectedFree = pointAt(90.0, 120.0)
    assertEquals(expectedFree.latitude, free.latitude, 1e-9)
    assertEquals(expectedFree.longitude, free.longitude, 1e-9)
  }

  /** Red if a filter keeps the wrong tasks. */
  @Test
  fun mineKeepsTheUsersTasksAllKeepsAllStructuresKeepsNone() {
    val state = allState()

    assertEquals(listOf("t1"), state.copy(filter = PinFilter.MINE).pins.map { it.taskId })
    assertEquals(4, state.copy(filter = PinFilter.ALL).pins.size)
    assertTrue(state.copy(filter = PinFilter.STRUCTURES).pins.isEmpty())
  }

  /** Red if the chip counts follow the filter or the query, or count unplaced tasks. */
  @Test
  fun countsAreThePlacedTasksWhateverTheFilterAndQuery() {
    val state = allState().copy(filter = PinFilter.STRUCTURES, query = "nothing matches")

    assertEquals(1, state.mineCount)
    assertEquals(4, state.allCount)
  }

  /** Red if the search ignores the place, or is case sensitive. */
  @Test
  fun theQueryFiltersByTitleAndByPlaceIgnoringCase() {
    val state = allState()

    assertEquals(listOf("t2"), state.copy(query = "ICE").pins.map { it.taskId })
    assertEquals(listOf("t1"), state.copy(query = "north CORNER").pins.map { it.taskId })
    assertEquals(4, state.copy(query = "  ").pins.size)
  }

  /** Red if a pin is tinted by the wrong rule. */
  @Test
  fun statusFollowsTheTaskDetailsRule() {
    val status = allState().pins.associate { it.taskId to it.status }

    assertEquals(PinStatus.MINE, status["t1"])
    assertEquals(PinStatus.OPEN, status["t2"])
    assertEquals(PinStatus.TAKEN, status["t3"])
    assertEquals(PinStatus.DONE, status["t4"])
  }

  /** Red if the label drops the time or does not cut a long title. */
  @Test
  fun theLabelIsTheTitleAndTimeCutAtEighteenCharacters() {
    val long = ice.copy(taskId = "t7", title = "Unload the delivery truck", startTime = start)
    val labels = allState().copy(tasks = allTasks + long).pins.associate { it.taskId to it.label }

    // "Run power to stage" is exactly 18 characters, so it stays whole.
    assertEquals("Run power to stage · 14:40", labels["t1"])
    assertEquals("Ice bags", labels["t2"])
    assertEquals("Unload the deliver… · 14:00", labels["t7"])
  }

  /** Red if the focused task misses its partner, time or distance. */
  @Test
  fun focusWithAFixGivesTheDistanceAndWalk() {
    // Me at the venue centre, the pin at (90, 120): sqrt(90² + 120²) = 150 m, and
    // 150 m / 1.2 m/s = 125 s, which rounds up to 3 minutes.
    val state =
        allState()
            .copy(selectedTaskId = "t2", me = LocationFix(venue.latitude, venue.longitude, 5f))

    val focus = state.focus!!
    assertEquals("Ice bags", focus.title)
    assertEquals("Bar", focus.place)
    assertNull(focus.withName)
    assertNull(focus.time)
    assertNull(focus.minutesUntil)
    assertEquals(150, focus.distanceMetres)
    assertEquals(3, focus.walkMinutes)
  }

  /** Red if a missing fix invents a distance, or the time and partner are wrong. */
  @Test
  fun focusWithoutAFixHasNoDistanceAndCountsMinutesFromNow() {
    val focus = allState().copy(selectedTaskId = "t1").focus!!

    assertEquals("t1", focus.taskId)
    assertEquals("Stage, north corner", focus.place)
    assertEquals("Sami K.", focus.withName)
    assertEquals("14:40", focus.time)
    assertEquals(40L, focus.minutesUntil)
    assertNull(focus.distanceMetres)
    assertNull(focus.walkMinutes)
  }

  /** Red if the sheet keeps describing a task whose pin is filtered out, or none is selected. */
  @Test
  fun focusIsNullWithoutASelectionOrWhenThePinIsHidden() {
    assertNull(allState().focus)
    assertNull(allState().copy(selectedTaskId = "t2", filter = PinFilter.MINE).focus)
  }

  /** Red if a task and a structure can be selected at once. */
  @Test
  fun selectingATaskClearsTheStructureAndBack() {
    val vm = viewModel().also { it.load() }

    vm.select("s1")
    vm.selectTask("t1")
    assertNull(vm.uiState.value.selectedStructureId)
    assertEquals("t1", vm.uiState.value.selectedTaskId)

    vm.select("s2")
    assertNull(vm.uiState.value.selectedTaskId)
    assertEquals("s2", vm.uiState.value.selectedStructureId)
  }

  /** Red if setFilter or setQuery do not reach the state. */
  @Test
  fun setFilterAndSetQueryReachTheState() {
    val vm = viewModel().also { it.load() }

    vm.setFilter(PinFilter.ALL)
    vm.setQuery("ice")

    assertEquals(listOf("t2"), vm.uiState.value.pins.map { it.taskId })
  }

  /** Red if the short name drops the initial's dot or breaks on a blank surname. */
  @Test
  fun shortNameIsTheNameAndTheSurnameInitial() {
    assertEquals("Sami K.", shortName(sami))
    assertEquals("Sami", shortName(User("u3", "Sami", " ")))
    assertEquals("", shortName(User("u4")))
  }
}
