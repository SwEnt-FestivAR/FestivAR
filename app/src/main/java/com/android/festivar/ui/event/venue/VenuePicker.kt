// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.venue

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.model.event.Venue
import com.android.festivar.model.geocoding.GeocodingRepository
import com.android.festivar.model.geocoding.GeocodingRepositoryLocal
import com.android.festivar.model.geocoding.GeocodingResult
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import kotlin.math.roundToInt

private const val VENUE_ZOOM = 17f

/** Test tags of the venue field and its picker sheet. */
object VenuePickerTestTags {
  /** The "Place on the map" button of an event without a venue. */
  const val PLACE_BUTTON = "venuePlaceButton"

  /** The "Change" button of an event with a venue. */
  const val CHANGE_BUTTON = "venueChangeButton"

  /** The "Remove" button of an event with a venue. */
  const val REMOVE_BUTTON = "venueRemoveButton"

  /** The line giving the coordinates and the bearing of the venue. */
  const val SUMMARY = "venueSummary"

  /** The picker sheet. */
  const val SHEET = "venueSheet"

  /** The fixed pin over the centre of the map. */
  const val CENTER_PIN = "venueCenterPin"

  /** The bearing slider. */
  const val BEARING_SLIDER = "venueBearingSlider"

  /** The line shown while the address is looked up. */
  const val SEARCHING = "venueSearching"

  /** The line shown when the address matches no place. */
  const val NO_RESULTS = "venueNoResults"

  /** The line shown when the address could not be looked up. */
  const val SEARCH_ERROR = "venueSearchError"

  /** The button that closes the sheet without a change. */
  const val CANCEL_BUTTON = "venueCancelButton"

  /** The button that keeps the spot under the pin. */
  const val CONFIRM_BUTTON = "venueConfirmButton"

  /**
   * The chip of one geocoding candidate.
   *
   * @param index The position of the candidate, from 0.
   */
  fun candidate(index: Int) = "venueCandidate$index"
}

/**
 * The venue field of the event form: shows whether the event is placed on the map, and opens the
 * [VenuePickerSheet] to place it, change it or remove it.
 *
 * @param address The address the organizer typed, looked up when the sheet opens.
 * @param venue The venue placed so far, or null.
 * @param onVenueChange Called with the confirmed venue, or null when it is removed.
 * @param geocoding Where the address is looked up.
 */
@Composable
fun VenueField(
    address: String,
    venue: Venue?,
    onVenueChange: (Venue?) -> Unit,
    geocoding: GeocodingRepository,
    modifier: Modifier = Modifier,
) {
  var open by remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth()) {
    SectionLabel(stringResource(R.string.venue_label))
    if (venue == null) {
      Text(
          text = stringResource(R.string.venue_not_placed),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
          onClick = { open = true },
          enabled = address.isNotBlank(),
          modifier = Modifier.padding(top = 8.dp).testTag(VenuePickerTestTags.PLACE_BUTTON),
      ) {
        Text(stringResource(R.string.venue_place))
      }
    } else {
      Text(
          text =
              stringResource(
                  R.string.venue_summary,
                  String.format(Locale.US, "%.5f", venue.latitude),
                  String.format(Locale.US, "%.5f", venue.longitude),
                  venue.bearing.toInt(),
              ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(VenuePickerTestTags.SUMMARY),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(VenuePickerTestTags.CHANGE_BUTTON),
        ) {
          Text(stringResource(R.string.venue_change))
        }
        TextButton(
            onClick = { onVenueChange(null) },
            modifier = Modifier.testTag(VenuePickerTestTags.REMOVE_BUTTON),
        ) {
          Text(stringResource(R.string.venue_remove))
        }
      }
    }
  }
  if (open) {
    // The venue is part of the key: a sheet reopened after a change starts from the new venue.
    val viewModel: VenuePickerViewModel =
        viewModel(key = "venuePicker:$address:$venue") { VenuePickerViewModel(geocoding, venue) }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(address) { viewModel.search(address) }
    VenuePickerSheet(
        address = address,
        state = state,
        onCandidate = viewModel::moveTo,
        onBearing = viewModel::setBearing,
        onConfirm = { latitude, longitude ->
          onVenueChange(viewModel.venueAt(latitude, longitude))
          open = false
        },
        onDismiss = { open = false },
    )
  }
}

/**
 * The sheet where the organizer places the venue: the map moves under a fixed pin, so the venue
 * centre is wherever the camera ends, and a slider sets the bearing of the plan.
 *
 * @param address The address being placed, echoed at the top.
 * @param state What the sheet shows.
 * @param onCandidate Called with the candidate the organizer tapped.
 * @param onBearing Called with the new bearing, in whole degrees.
 * @param onConfirm Called with the camera target when the organizer keeps the spot.
 * @param onDismiss Called when the organizer cancels or closes the sheet.
 * @param mapContent The map drawn under the pin, given the camera; tests pass an empty one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuePickerSheet(
    address: String,
    state: VenuePickerUiState,
    onCandidate: (GeocodingResult) -> Unit,
    onBearing: (Int) -> Unit,
    onConfirm: (latitude: Double, longitude: Double) -> Unit,
    onDismiss: () -> Unit,
    mapContent: @Composable BoxScope.(CameraPositionState) -> Unit = { camera -> VenueMap(camera) },
) {
  val camera = rememberCameraPositionState()
  LaunchedEffect(state.center) {
    state.center?.let {
      camera.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), VENUE_ZOOM)
    }
  }
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      modifier = Modifier.testTag(VenuePickerTestTags.SHEET),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(text = address, style = MaterialTheme.typography.titleMedium)
      if (state.candidates.isNotEmpty()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          state.candidates.forEachIndexed { index, candidate ->
            AssistChip(
                onClick = { onCandidate(candidate) },
                label = { Text(candidate.label) },
                modifier = Modifier.testTag(VenuePickerTestTags.candidate(index)),
            )
          }
        }
      }
      StatusLine(state)
      Box(
          modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
      ) {
        mapContent(camera)
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp).testTag(VenuePickerTestTags.CENTER_PIN),
        )
      }
      Text(
          text = stringResource(R.string.venue_bearing, state.bearing),
          style = MaterialTheme.typography.bodyMedium,
      )
      Slider(
          value = state.bearing.toFloat(),
          onValueChange = { onBearing(it.roundToInt()) },
          valueRange = 0f..359f,
          modifier = Modifier.testTag(VenuePickerTestTags.BEARING_SLIDER),
      )
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
      ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(VenuePickerTestTags.CANCEL_BUTTON),
        ) {
          Text(stringResource(R.string.venue_cancel))
        }
        Button(
            onClick = {
              val target = camera.position.target
              onConfirm(target.latitude, target.longitude)
            },
            modifier = Modifier.testTag(VenuePickerTestTags.CONFIRM_BUTTON),
        ) {
          Text(stringResource(R.string.venue_confirm))
        }
      }
    }
  }
}

/** The line under the candidates: the search in progress, its failure, or no match. */
@Composable
private fun StatusLine(state: VenuePickerUiState) {
  val (text, tag) =
      when {
        state.isSearching -> R.string.venue_searching to VenuePickerTestTags.SEARCHING
        state.searchFailed -> R.string.venue_search_error to VenuePickerTestTags.SEARCH_ERROR
        state.candidates.isEmpty() -> R.string.venue_no_results to VenuePickerTestTags.NO_RESULTS
        else -> return
      }
  Text(
      text = stringResource(text),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag(tag),
  )
}

/** The satellite map the camera moves over, without zoom buttons. */
@Composable
private fun BoxScope.VenueMap(camera: CameraPositionState) {
  GoogleMap(
      modifier = Modifier.matchParentSize(),
      cameraPositionState = camera,
      properties = MapProperties(mapType = MapType.SATELLITE),
      uiSettings = MapUiSettings(zoomControlsEnabled = false),
  )
}

@Preview(showBackground = true)
@Composable
internal fun VenueFieldNotPlacedPreview() {
  AppTheme {
    VenueField(
        address = "EPFL, Lausanne",
        venue = null,
        onVenueChange = {},
        geocoding = GeocodingRepositoryLocal(),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun VenueFieldPlacedPreview() {
  AppTheme {
    VenueField(
        address = "EPFL, Lausanne",
        venue = Venue(46.517, 6.599, 12.0),
        onVenueChange = {},
        geocoding = GeocodingRepositoryLocal(),
    )
  }
}
