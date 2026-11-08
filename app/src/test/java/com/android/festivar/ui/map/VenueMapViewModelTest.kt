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
import kotlinx.coroutines.runBlocking
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

/**
 * A [StructuresRepositoryLocal] whose reads and writes can be made to fail, counting the writes
 * that reach it.
 */
private class ControlledStructures(val local: StructuresRepositoryLocal) :
    StructuresRepository by local {
  /** Thrown by [getAllStructures] when not null. */
  var failure: Exception? = null

  /** Thrown by every write when not null, before it reaches [local]. */
  var writeFailure: Exception? = null

  /** How many add, edit and delete calls were made, failed ones included. */
  var writes = 0

  override suspend fun getAllStructures(eventId: String): List<Structure> {
    failure?.let { throw it }
    return local.getAllStructures(eventId)
  }

  override suspend fun addStructure(structure: Structure) {
    writes++
    writeFailure?.let { throw it }
    local.addStructure(structure)
  }

  override suspend fun editStructure(structureId: String, newValue: Structure) {
    writes++
    writeFailure?.let { throw it }
    local.editStructure(structureId, newValue)
  }

  override suspend fun deleteStructure(structureId: String) {
    writes++
    writeFailure?.let { throw it }
    local.deleteStructure(structureId)
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

  /** A loaded view model where the current user is the first member, the organizer. */
  private fun organizer(): VenueMapViewModel {
    events = ControlledEvents(EventsRepositoryLocal(listOf(event.copy(members = listOf(me, sami)))))
    return viewModel().also { it.load() }
  }

  /** A loaded view model where the current user is a member but not the first one. */
  private fun volunteer(): VenueMapViewModel {
    events = ControlledEvents(EventsRepositoryLocal(listOf(event.copy(members = listOf(sami, me)))))
    return viewModel().also { it.load() }
  }

  /** What the repository holds for the event, to compare with the state. */
  private fun stored(): List<Structure> = runBlocking { structures.getAllStructures("e1") }

  private fun VenueMapViewModel.tapAt(x: Double, y: Double) =
      pointAt(x, y).let { placeAt(it.latitude, it.longitude) }

  private fun List<Double>.rounded() = map { Math.round(it * 1000) / 1000.0 }

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

  /** Red if the organizer is not the first member, or anyone else may edit. */
  @Test
  fun isOrganizerIsTrueOnlyForTheFirstMember() {
    assertTrue(organizer().uiState.value.isOrganizer)
    assertFalse(volunteer().uiState.value.isOrganizer)
    assertFalse(viewModel().also { it.load() }.uiState.value.isOrganizer)
  }

  /** Red if a volunteer can pick a tool or change the plan through any edit call. */
  @Test
  fun aVolunteerCannotPickAToolNorWriteAnything() {
    val vm = volunteer()

    vm.selectTool(PaletteTool.STAND)
    vm.tapAt(10.0, 10.0)
    vm.select("s1")
    vm.moveSelected(venue.latitude, venue.longitude)
    vm.resizeSelected(Corner.TOP_LEFT, venue.latitude, venue.longitude)
    vm.startRename()
    vm.rename("Mine now")

    assertNull(vm.uiState.value.tool)
    assertFalse(vm.uiState.value.renaming)
    assertEquals(0, structures.writes)
    assertEquals(listOf(stage, bar), vm.uiState.value.structures)
  }

  /** Red if selecting the picked tool again does not put it down. */
  @Test
  fun selectToolToggles() {
    val vm = organizer()

    vm.selectTool(PaletteTool.STAGE)
    assertEquals(PaletteTool.STAGE, vm.uiState.value.tool)
    vm.selectTool(PaletteTool.STAGE)
    assertNull(vm.uiState.value.tool)
  }

  /** Red if a placed structure is not centred on the snapped tap, not saved, or not selected. */
  @Test
  fun placeAtCentresASnappedFootprintOnTheTapAndSavesIt() {
    val vm = organizer()

    vm.selectTool(PaletteTool.STAND)
    // (10.2, 20.3) snaps to the centre (10, 20.5); a 3 × 3 m stand then starts at (8.5, 19).
    vm.tapAt(10.2, 20.3)

    val state = vm.uiState.value
    val placed = state.selected!!
    assertEquals(StructureKind.STAND, placed.kind)
    assertEquals(
        listOf(8.5, 19.0, 3.0, 3.0),
        listOf(placed.x, placed.y, placed.width, placed.height).rounded(),
    )
    assertNull(state.tool)
    assertFalse(state.isSaving)
    assertEquals(stored(), state.structures)
    assertTrue(placed in stored())
  }

  /** Red if a tap without a tool places something. */
  @Test
  fun placeAtWithoutAToolDoesNothing() {
    val vm = organizer()

    vm.tapAt(10.0, 10.0)

    assertEquals(0, structures.writes)
  }

  /** Red if a second structure of a kind gets the same name as the first. */
  @Test
  fun defaultLabelsNumberRepeats() {
    assertEquals("Stand", defaultLabel(StructureKind.STAND, emptyList()))
    assertEquals("Stand 2", defaultLabel(StructureKind.STAND, listOf(bar)))
    val vm = organizer()

    vm.selectTool(PaletteTool.TABLE)
    vm.tapAt(20.0, 20.0)
    vm.selectTool(PaletteTool.TABLE)
    vm.tapAt(30.0, 30.0)

    val tables = stored().filter { it.kind == StructureKind.TABLE }.map { it.label }
    assertEquals(listOf("Table", "Table 2"), tables)
  }

  /** Red if a move changes the size, lands off the grid, or is not saved. */
  @Test
  fun moveSelectedKeepsTheSizeAndSnaps() {
    val vm = organizer()
    vm.select("s1")

    // (20.2, 30.9) snaps to the centre (20, 31); the 10 × 6 m stage then starts at (15, 28).
    pointAt(20.2, 30.9).let { vm.moveSelected(it.latitude, it.longitude) }

    val moved = stored().first { it.structureId == "s1" }
    assertEquals(
        listOf(15.0, 28.0, 10.0, 6.0),
        listOf(moved.x, moved.y, moved.width, moved.height).rounded(),
    )
    assertEquals(stored(), vm.uiState.value.structures)
    assertEquals("s1", vm.uiState.value.selectedStructureId)
  }

  /** Red if a resize moves the opposite corner, skips the snap, or shrinks below half a metre. */
  @Test
  fun resizeSelectedMovesOneCornerKeepsTheOppositeAndSnaps() {
    val vm = organizer()
    vm.select("s1")
    // The stage spans x -5 to 5 and y -10 to -4.

    pointAt(8.2, -1.1).let { vm.resizeSelected(Corner.BOTTOM_RIGHT, it.latitude, it.longitude) }
    var s = vm.uiState.value.selected!!
    assertEquals(listOf(-5.0, -10.0, 13.0, 9.0), listOf(s.x, s.y, s.width, s.height).rounded())

    // Now x -5 to 8, y -10 to -1. The top left goes to (-7.3, -12.6).
    pointAt(-7.3, -12.6).let { vm.resizeSelected(Corner.TOP_LEFT, it.latitude, it.longitude) }
    s = vm.uiState.value.selected!!
    assertEquals(listOf(-7.5, -12.5, 15.5, 11.5), listOf(s.x, s.y, s.width, s.height).rounded())

    // Dragged past the opposite corner (8, -1): half a metre, the opposite corner kept.
    pointAt(30.0, 30.0).let { vm.resizeSelected(Corner.TOP_LEFT, it.latitude, it.longitude) }
    s = vm.uiState.value.selected!!
    assertEquals(listOf(7.5, -1.5, 0.5, 0.5), listOf(s.x, s.y, s.width, s.height).rounded())
    assertEquals(stored(), vm.uiState.value.structures)
  }

  /** Red if a blank name is saved, or a good one leaves the dialog open. */
  @Test
  fun renameRefusesBlankAndSavesATrimmedName() {
    val vm = organizer()
    vm.select("s1")
    vm.startRename()
    assertTrue(vm.uiState.value.renaming)

    vm.rename("   ")
    assertTrue(vm.uiState.value.renaming)
    assertEquals(0, structures.writes)

    vm.rename("  Main stage ")
    assertFalse(vm.uiState.value.renaming)
    assertEquals("Main stage", stored().first { it.structureId == "s1" }.label)
    assertEquals("Main stage", vm.uiState.value.selected!!.label)
  }

  /** Red if cancelling the rename leaves the dialog open or writes. */
  @Test
  fun cancelRenameClosesTheDialog() {
    val vm = organizer()
    vm.select("s1")
    vm.startRename()

    vm.cancelRename()

    assertFalse(vm.uiState.value.renaming)
    assertEquals(0, structures.writes)
  }

  /**
   * Red if a failed save is silent, or the screen then shows a plan the repository does not hold.
   */
  @Test
  fun aFailingSaveSetsTheErrorAndKeepsTheRepositoryList() {
    val vm = organizer()
    structures.writeFailure = IOException("offline")

    vm.selectTool(PaletteTool.STAND)
    vm.tapAt(10.0, 10.0)
    vm.select("s1")
    vm.moveSelected(venue.latitude, venue.longitude)

    val state = vm.uiState.value
    assertEquals(R.string.map_save_failed, state.errorMsg)
    assertEquals(stored(), state.structures)
    assertEquals(listOf(stage, bar), state.structures)
    assertFalse(state.isSaving)
  }
}
