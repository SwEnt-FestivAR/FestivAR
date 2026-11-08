// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Test tags of the venue map, named after the "Map, volunteer" frame in Figma. */
object VenueMapScreenTestTags {
  /** The Google map itself. */
  const val MAP = "mapMap"
  /** The round back button at the top left. */
  const val BACK = "mapBack"
  /** The pill with the event title at the top centre. */
  const val TITLE_PILL = "mapTitlePill"
  /** The bottom sheet with the venue summary or the selected structure. */
  const val SHEET = "mapSheet"
  /** The pill shown while the device has no position fix. */
  const val NO_GPS = "mapNoGps"
  /** The state shown when the organizer has not placed the venue yet. */
  const val EMPTY = "mapEmpty"
  /** The spinner shown while the event loads. */
  const val LOADING = "mapLoading"
  /** The load error message. */
  const val ERROR = "mapError"
  /** The retry button of the load error. */
  const val RETRY = "mapRetry"
  /** The button of the sheet that clears the selected structure. */
  const val CLEAR_SELECTION = "mapClearSelection"
}

/** Zoom level the camera opens on: a festival site fills the screen. */
private const val VENUE_ZOOM = 18f

/**
 * The read only venue map of an event, reached from its details: the venue on a satellite map, the
 * organizer's structures on it and the user's own position.
 *
 * Asks for the fine location permission on first composition and starts following the position once
 * it is granted; on denial the "No GPS fix" pill simply stays.
 *
 * @param viewModel holds the event, its structures and the position.
 * @param onBack called when the back button is clicked.
 */
@Composable
fun VenueMapScreen(viewModel: VenueMapViewModel, onBack: () -> Unit) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.observeLocation()
      }
  LaunchedEffect(Unit) {
    viewModel.load()
    val permission = Manifest.permission.ACCESS_FINE_LOCATION
    if (
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    ) {
      viewModel.observeLocation()
    } else {
      launcher.launch(permission)
    }
  }
  VenueMapContent(
      state = state,
      onBack = onBack,
      onSelect = viewModel::select,
      onRetry = viewModel::load,
      onDismissError = viewModel::clearError,
  )
}

/**
 * The stateless venue map: everything it shows comes from [state].
 *
 * @param state what to show.
 * @param onBack called when the back button is clicked.
 * @param onSelect called with a structure id when its footprint is tapped, null to clear.
 * @param onRetry called when the retry button of a load error is clicked.
 * @param onDismissError called when the error shown over the map is dismissed.
 * @param mapContent draws the map under the overlays; tests and previews swap the Google map out
 *   since it needs Play services.
 */
@Composable
fun VenueMapContent(
    state: VenueMapUiState,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    mapContent: @Composable BoxScope.(VenueMapUiState, (String?) -> Unit) -> Unit = { s, select ->
      VenueGoogleMap(s, select)
    },
) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Box(Modifier.fillMaxSize()) {
      when {
        state.isLoading ->
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).testTag(VenueMapScreenTestTags.LOADING)
            )
        state.venue == null && state.errorMsg != null ->
            LoadError(state.errorMsg, onRetry, Modifier.align(Alignment.Center).padding(20.dp))
        state.venue == null -> NoVenue(Modifier.align(Alignment.Center))
        else -> {
          mapContent(state, onSelect)
          TopOverlay(state, Modifier.align(Alignment.TopCenter))
          if (state.errorMsg != null) {
            MapErrorCard(
                state.errorMsg,
                onRetry,
                onDismissError,
                Modifier.align(Alignment.Center).padding(20.dp),
            )
          }
          MapSheet(state, onSelect, Modifier.align(Alignment.BottomCenter))
        }
      }
      BackDisc(onBack, Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))
    }
  }
}

/**
 * The satellite map centred on the venue, with each structure as a tappable footprint and label,
 * and the user's fix as an accuracy ring and dot. Draws nothing without a venue.
 *
 * @param state the venue, structures, selection and fix to draw.
 * @param onSelect called with the id of a tapped structure.
 */
@Composable
fun VenueGoogleMap(state: VenueMapUiState, onSelect: (String?) -> Unit) {
  val venue = state.venue ?: return
  val colors = MaterialTheme.colorScheme
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(LatLng(venue.latitude, venue.longitude), VENUE_ZOOM)
  }
  val properties = remember {
    MapProperties(mapType = MapType.SATELLITE, isMyLocationEnabled = false)
  }
  val uiSettings = remember {
    MapUiSettings(
        zoomControlsEnabled = false,
        mapToolbarEnabled = false,
        compassEnabled = true,
        myLocationButtonEnabled = false,
    )
  }
  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(VenueMapScreenTestTags.MAP),
      cameraPositionState = cameraPositionState,
      properties = properties,
      uiSettings = uiSettings,
  ) {
    state.structures.forEach { structure ->
      val corners =
          VenueGeometry.corners(
                  venue.latitude,
                  venue.longitude,
                  venue.bearing,
                  structure.x,
                  structure.y,
                  structure.width,
                  structure.height,
              )
              .map { LatLng(it.latitude, it.longitude) }
      val centre =
          VenueGeometry.latLngFrom(
              venue.latitude,
              venue.longitude,
              venue.bearing,
              structure.centerX,
              structure.centerY,
          )
      Polygon(
          points = corners,
          fillColor = colors.surface.copy(alpha = 0.92f),
          strokeColor =
              if (structure.structureId == state.selectedStructureId) colors.primary
              else colors.onSurface,
          strokeWidth = 4f,
          clickable = true,
          onClick = { onSelect(structure.structureId) },
      )
      MarkerComposable(
          structure.label,
          state = rememberUpdatedMarkerState(position = LatLng(centre.latitude, centre.longitude)),
          anchor = Offset(0.5f, 0.5f),
      ) {
        Text(
            text = structure.label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
        )
      }
    }
    state.me?.let { fix ->
      val centre = LatLng(fix.latitude, fix.longitude)
      Circle(
          center = centre,
          radius = fix.accuracyMetres.toDouble(),
          fillColor = colors.primary.copy(alpha = 0.2f),
          strokeColor = colors.primary,
          strokeWidth = 2f,
      )
      Circle(
          center = centre,
          radius = 2.0,
          fillColor = colors.onSurface,
          strokeColor = colors.surface,
          strokeWidth = 4f,
      )
    }
  }
}

/** The 44dp round back button floating over the map. */
@Composable
private fun BackDisc(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier.size(44.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 2.dp,
  ) {
    IconButton(onClick = onBack, modifier = Modifier.testTag(VenueMapScreenTestTags.BACK)) {
      Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.map_navigate_back),
          tint = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

/** The title pill and, while there is no fix, the "No GPS fix" pill under it on the right. */
@Composable
private fun TopOverlay(state: VenueMapUiState, modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(start = 72.dp, end = 72.dp, top = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
        modifier = Modifier.testTag(VenueMapScreenTestTags.TITLE_PILL),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
      Text(
          text = state.eventTitle,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
    if (state.me == null) {
      Surface(
          modifier = Modifier.align(Alignment.End).testTag(VenueMapScreenTestTags.NO_GPS),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
      ) {
        Text(
            text = stringResource(R.string.map_no_gps),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/** The bottom sheet: the venue summary, or the selected structure with a button to clear it. */
@Composable
private fun MapSheet(
    state: VenueMapUiState,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
  val selected = state.selected
  Surface(
      modifier = modifier.fillMaxWidth().testTag(VenueMapScreenTestTags.SHEET),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 4.dp,
  ) {
    Row(
        modifier = Modifier.navigationBarsPadding().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = selected?.label ?: stringResource(R.string.map_venue),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                if (selected != null)
                    stringResource(
                        R.string.map_structure_size,
                        VenueGeometry.metresText(selected.width),
                        VenueGeometry.metresText(selected.height),
                    )
                else summaryLine(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (selected != null) {
        TextButton(
            onClick = { onSelect(null) },
            modifier = Modifier.testTag(VenueMapScreenTestTags.CLEAR_SELECTION),
        ) {
          Text(stringResource(R.string.map_clear))
        }
      }
    }
  }
}

/** "3 structures · Parc de Vidy", or only the count when the event has no location text. */
@Composable
private fun summaryLine(state: VenueMapUiState): String {
  val count =
      pluralStringResource(R.plurals.map_structures, state.structures.size, state.structures.size)
  return if (state.eventLocation.isBlank()) count else "$count · ${state.eventLocation}"
}

/** The state of an event whose venue is not placed yet. */
@Composable
private fun NoVenue(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.padding(20.dp).testTag(VenueMapScreenTestTags.EMPTY),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
        text = stringResource(R.string.map_no_venue_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.map_no_venue_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
  }
}

/** The load error shown in place of the map, with a retry button. */
@Composable
private fun LoadError(message: Int, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.testTag(VenueMapScreenTestTags.ERROR),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = onRetry, modifier = Modifier.testTag(VenueMapScreenTestTags.RETRY)) {
      Text(stringResource(R.string.action_retry))
    }
  }
}

/** The load error shown over the map when the venue loaded but its structures did not. */
@Composable
private fun MapErrorCard(
    message: Int,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier.testTag(VenueMapScreenTestTags.ERROR),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.errorContainer,
  ) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = stringResource(message),
          modifier = Modifier.weight(1f, fill = false),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onErrorContainer,
      )
      TextButton(onClick = onRetry, modifier = Modifier.testTag(VenueMapScreenTestTags.RETRY)) {
        Text(stringResource(R.string.action_retry))
      }
      IconButton(onClick = onDismiss) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.map_dismiss_error),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }
  }
}

private val previewVenue = Venue(latitude = 46.5170, longitude = 6.5990, bearing = 0.0)

private val previewStructures =
    listOf(
        Structure("s1", "e1", StructureKind.STAGE, "Main stage", -6.0, -20.0, 12.0, 8.0),
        Structure("s2", "e1", StructureKind.MARQUEE, "Marquee A", -18.0, 0.0, 12.0, 8.0),
        Structure("s3", "e1", StructureKind.STAND, "Bar", 8.0, 4.0, 3.0, 3.0),
    )

@Preview
@Composable
internal fun VenueMapPreview() {
  AppTheme {
    VenueMapContent(
        state =
            VenueMapUiState(
                isLoading = false,
                eventTitle = "Fête de Vidy",
                eventLocation = "Parc de Vidy, Lausanne",
                venue = previewVenue,
                structures = previewStructures,
                me = LocationFix(46.5169, 6.5991, 6f),
            ),
        onBack = {},
        onSelect = {},
        onRetry = {},
        onDismissError = {},
        mapContent = { _, _ -> },
    )
  }
}

@Preview
@Composable
internal fun VenueMapNoVenuePreview() {
  AppTheme {
    VenueMapContent(
        state = VenueMapUiState(isLoading = false, eventTitle = "Fête de Vidy"),
        onBack = {},
        onSelect = {},
        onRetry = {},
        onDismissError = {},
        mapContent = { _, _ -> },
    )
  }
}

@Preview
@Composable
internal fun VenueMapErrorPreview() {
  AppTheme {
    VenueMapContent(
        state = VenueMapUiState(isLoading = false, errorMsg = R.string.map_load_failed),
        onBack = {},
        onSelect = {},
        onRetry = {},
        onDismissError = {},
        mapContent = { _, _ -> },
    )
  }
}
