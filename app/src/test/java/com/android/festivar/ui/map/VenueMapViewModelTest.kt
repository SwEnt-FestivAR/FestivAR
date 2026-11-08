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

  private lateinit var events: ControlledEvents
  private lateinit var structures: ControlledStructures

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    events = ControlledEvents(EventsRepositoryLocal(listOf(event)))
    structures = ControlledStructures(StructuresRepositoryLocal(listOf(stage, bar, elsewhere)))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(source: LocationSource? = null) =
      VenueMapViewModel("e1", events, structures, source)

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
}
