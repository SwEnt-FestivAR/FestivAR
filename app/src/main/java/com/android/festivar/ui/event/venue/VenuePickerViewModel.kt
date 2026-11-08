// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.venue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Venue
import com.android.festivar.model.geocoding.GeocodingRepository
import com.android.festivar.model.geocoding.GeocodingResult
import com.android.festivar.ui.map.LatLngPoint
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "VenuePickerViewModel"

/**
 * UI state of the venue picker sheet.
 *
 * @property candidates The places the typed address may mean, best first.
 * @property isSearching True while the address is being looked up.
 * @property searchFailed True when the last lookup could not reach the geocoding backend.
 * @property center Where the camera should go next; null until a search result or the initial venue
 *   gives one.
 * @property bearing The rotation of the plan, in whole degrees clockwise from north, in 0..359.
 */
data class VenuePickerUiState(
    val candidates: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchFailed: Boolean = false,
    val center: LatLngPoint? = null,
    val bearing: Int = 0,
)

/**
 * Holds the state of the venue picker: looks the typed address up, keeps the place the camera
 * should move to and the bearing of the plan, and builds the confirmed [Venue].
 *
 * @param geocoding Where the address is looked up.
 * @param initialVenue The venue already placed, if any. The camera starts on it and a search does
 *   not move it away, so changing a venue keeps the organizer's spot.
 */
class VenuePickerViewModel(
    private val geocoding: GeocodingRepository,
    initialVenue: Venue?,
) : ViewModel() {

  private val keepCenter = initialVenue != null

  private val _uiState =
      MutableStateFlow(
          VenuePickerUiState(
              center = initialVenue?.let { LatLngPoint(it.latitude, it.longitude) },
              bearing = initialVenue?.bearing?.toInt() ?: 0,
          )
      )

  /** The state the sheet shows. */
  val uiState: StateFlow<VenuePickerUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null

  /**
   * Looks [address] up. A blank address clears the candidates without a lookup. Otherwise the
   * candidates are replaced by the results and the center moves to the first one, unless an initial
   * venue was given. A new search cancels the one still running.
   *
   * @param address The address as the organizer typed it.
   */
  fun search(address: String) {
    searchJob?.cancel()
    if (address.isBlank()) {
      _uiState.update { it.copy(candidates = emptyList(), isSearching = false) }
      return
    }
    _uiState.update { it.copy(isSearching = true, searchFailed = false) }
    searchJob = viewModelScope.launch {
      try {
        val results = geocoding.search(address)
        _uiState.update {
          it.copy(
              candidates = results,
              isSearching = false,
              center =
                  if (keepCenter) it.center
                  else
                      results.firstOrNull()?.let { r -> LatLngPoint(r.latitude, r.longitude) }
                          ?: it.center,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: IOException) {
        Log.e(LOG_TAG, "Could not look the address up", e)
        _uiState.update {
          it.copy(candidates = emptyList(), isSearching = false, searchFailed = true)
        }
      }
    }
  }

  /**
   * Moves the camera to [candidate].
   *
   * @param candidate The place the organizer tapped.
   */
  fun moveTo(candidate: GeocodingResult) {
    _uiState.update { it.copy(center = LatLngPoint(candidate.latitude, candidate.longitude)) }
  }

  /**
   * Sets the bearing of the plan.
   *
   * @param degrees The bearing in degrees clockwise from north, coerced into 0..359.
   */
  fun setBearing(degrees: Int) {
    _uiState.update { it.copy(bearing = degrees.coerceIn(0, 359)) }
  }

  /**
   * Builds the venue the organizer confirms.
   *
   * @param latitude The latitude of the camera target, in degrees.
   * @param longitude The longitude of the camera target, in degrees.
   * @return The [Venue] at that point with the current bearing.
   */
  fun venueAt(latitude: Double, longitude: Double): Venue =
      Venue(latitude, longitude, _uiState.value.bearing.toDouble())
}
