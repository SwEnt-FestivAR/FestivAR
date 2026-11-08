// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.location.LocationSource
import com.android.festivar.model.location.LocationSourceProvider
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructuresRepository
import com.android.festivar.model.structure.StructuresRepositoryProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "VenueMapViewModel"

/**
 * UI state of the read only venue map.
 *
 * @property isLoading true while the event and its structures are being loaded.
 * @property eventTitle the title of the event, shown in the title pill.
 * @property eventLocation the free text location of the event, shown in the sheet.
 * @property venue the centre and bearing of the venue, or null when the organizer has not placed
 *   it.
 * @property structures the footprints drawn on the venue.
 * @property me the last position fix of the device, or null while there is none.
 * @property selectedStructureId the structure the sheet describes, or null for the venue summary.
 * @property errorMsg the string resource of the last load failure, or null.
 */
data class VenueMapUiState(
    val isLoading: Boolean = true,
    val eventTitle: String = "",
    val eventLocation: String = "",
    val venue: Venue? = null,
    val structures: List<Structure> = emptyList(),
    val me: LocationFix? = null,
    val selectedStructureId: String? = null,
    @StringRes val errorMsg: Int? = null,
) {
  /** The structure matching [selectedStructureId], or null when none is selected or it is gone. */
  val selected: Structure?
    get() = structures.find { it.structureId == selectedStructureId }
}

/**
 * Holds the state of the read only venue map: the event's venue, its structures and the device's
 * position.
 *
 * Nothing loads until [load] is called, and the position is only followed once [observeLocation] is
 * called, after the location permission is granted.
 *
 * @param eventId the event whose venue is shown.
 * @param eventsRepository where the event is read.
 * @param structuresRepository where the structures of the event are read.
 * @param locationSource the device's position, or null when none is available.
 */
class VenueMapViewModel(
    private val eventId: String,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.repository,
    private val structuresRepository: StructuresRepository =
        StructuresRepositoryProvider.repository,
    private val locationSource: LocationSource? = LocationSourceProvider.source,
) : ViewModel() {
  private val _uiState = MutableStateFlow(VenueMapUiState())

  /** The state the screen shows. */
  val uiState: StateFlow<VenueMapUiState> = _uiState.asStateFlow()

  // isLoading starts true, so it cannot tell a running load from the initial state.
  private var loadJob: Job? = null
  private var locationJob: Job? = null

  /**
   * Loads the event, then its structures. A failure sets [VenueMapUiState.errorMsg] and keeps what
   * already loaded; a success clears it. Does nothing while another load is running.
   */
  fun load() {
    if (loadJob?.isActive == true) return
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update {
          it.copy(eventTitle = event.title, eventLocation = event.location, venue = event.venue)
        }
        val structures = structuresRepository.getAllStructures(eventId)
        _uiState.update { it.copy(structures = structures, isLoading = false, errorMsg = null) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading the venue map of event $eventId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = R.string.map_load_failed) }
      }
    }
  }

  /**
   * Selects the structure with [structureId], or clears the selection when null.
   *
   * @param structureId the structure the sheet should describe, or null for the venue summary.
   */
  fun select(structureId: String?) = _uiState.update { it.copy(selectedStructureId = structureId) }

  /**
   * Starts following the device's position into [VenueMapUiState.me]. Only the first call
   * subscribes; later calls, and a null location source, do nothing.
   */
  fun observeLocation() {
    val source = locationSource ?: return
    if (locationJob != null) return
    locationJob = viewModelScope.launch {
      try {
        source.updates().collect { fix -> _uiState.update { it.copy(me = fix) } }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // A lost provider is the no fix state, not an error worth a message.
        Log.w(LOG_TAG, "Location updates stopped", e)
        _uiState.update { it.copy(me = null) }
      }
    }
  }

  /** Clears the error message. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }
}
