// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.venue

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Venue
import com.android.festivar.model.geocoding.GeocodingRepository
import com.android.festivar.model.geocoding.GeocodingRepositoryLocal
import com.android.festivar.model.geocoding.GeocodingResult
import com.android.festivar.ui.map.LatLngPoint
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.runner.RunWith

/**
 * A [GeocodingRepositoryLocal] whose searches can be made to fail or to wait, to test how the view
 * model behaves while a search runs or when it fails.
 */
private class ControlledGeocoding(val local: GeocodingRepositoryLocal) : GeocodingRepository {
  /** Thrown by [search] when not null. */
  var failure: Exception? = null

  /** Searches for a query listed here wait for its gate to complete. */
  val gates = mutableMapOf<String, CompletableDeferred<Unit>>()

  /** How many times [search] was called. */
  var calls = 0

  override suspend fun search(query: String, limit: Int): List<GeocodingResult> {
    calls++
    gates[query]?.await()
    failure?.let { throw it }
    return local.search(query, limit)
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VenuePickerViewModelTest {
  private val rolex = GeocodingResult("Rolex Learning Center, Lausanne", 46.5185, 6.5684)
  private val satellite = GeocodingResult("Satellite, Lausanne", 46.5205, 6.5650)
  private val zurich = GeocodingResult("Hauptbahnhof, Zurich", 47.3779, 8.5403)
  private val geocoding =
      ControlledGeocoding(
          GeocodingRepositoryLocal(
              mapOf("EPFL" to listOf(rolex, satellite), "Zurich" to listOf(zurich))
          )
      )

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** Catches a blank address reaching the backend or leaving stale candidates. */
  @Test
  fun blankAddressClearsCandidatesWithoutCallingTheBackend() {
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.search("EPFL")
    viewModel.search("   ")
    val state = viewModel.uiState.value
    assertTrue(state.candidates.isEmpty())
    assertFalse(state.isSearching)
    assertEquals(1, geocoding.calls)
  }

  /** Catches results not shown or the camera not sent to the best match. */
  @Test
  fun searchFillsCandidatesAndCentresOnTheFirst() {
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.search("EPFL, Lausanne")
    val state = viewModel.uiState.value
    assertEquals(listOf(rolex, satellite), state.candidates)
    assertEquals(LatLngPoint(rolex.latitude, rolex.longitude), state.center)
    assertFalse(state.isSearching)
    assertFalse(state.searchFailed)
  }

  /** Catches a search throwing away the venue the organizer already placed. */
  @Test
  fun initialVenueKeepsTheCentreAfterASearch() {
    val venue = Venue(46.517, 6.599, 12.0)
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = venue)
    viewModel.search("EPFL")
    val state = viewModel.uiState.value
    assertEquals(listOf(rolex, satellite), state.candidates)
    assertEquals(LatLngPoint(46.517, 6.599), state.center)
    assertEquals(12, state.bearing)
  }

  /** Catches an empty result moving the camera or keeping old candidates. */
  @Test
  fun noMatchEmptiesCandidatesAndKeepsTheCentre() {
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.search("EPFL")
    viewModel.search("Nowhere at all")
    val state = viewModel.uiState.value
    assertTrue(state.candidates.isEmpty())
    assertTrue(state.searched)
    assertEquals(LatLngPoint(rolex.latitude, rolex.longitude), state.center)
    assertFalse(state.searchFailed)
  }

  /** Catches a network failure leaving the spinner on or going unreported. */
  @Test
  fun ioExceptionSetsSearchFailedAndStopsSearching() {
    geocoding.failure = IOException("offline")
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.search("EPFL")
    val state = viewModel.uiState.value
    assertTrue(state.searchFailed)
    assertFalse(state.isSearching)
    assertNull(state.center)
  }

  /** Catches a slow earlier search overwriting the results of a newer one. */
  @Test
  fun secondSearchCancelsTheFirst() {
    val gate = CompletableDeferred<Unit>()
    geocoding.gates["EPFL"] = gate
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.search("EPFL")
    assertTrue(viewModel.uiState.value.isSearching)
    viewModel.search("Zurich")
    gate.complete(Unit)
    val state = viewModel.uiState.value
    assertEquals(listOf(zurich), state.candidates)
    assertEquals(LatLngPoint(zurich.latitude, zurich.longitude), state.center)
    assertFalse(state.isSearching)
  }

  /** Catches a tapped candidate not moving the camera. */
  @Test
  fun moveToCentresOnTheCandidate() {
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.moveTo(satellite)
    assertEquals(
        LatLngPoint(satellite.latitude, satellite.longitude),
        viewModel.uiState.value.center,
    )
  }

  /** Catches a bearing outside 0..359, which [Venue] would reject. */
  @Test
  fun setBearingCoercesIntoRange() {
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.setBearing(-10)
    assertEquals(0, viewModel.uiState.value.bearing)
    viewModel.setBearing(400)
    assertEquals(359, viewModel.uiState.value.bearing)
  }

  /** Catches the confirmed venue losing the chosen bearing. */
  @Test
  fun venueAtBuildsTheVenueWithTheBearing() {
    val viewModel = VenuePickerViewModel(geocoding, initialVenue = null)
    viewModel.setBearing(45)
    assertEquals(Venue(46.5, 6.6, 45.0), viewModel.venueAt(46.5, 6.6))
  }
}
