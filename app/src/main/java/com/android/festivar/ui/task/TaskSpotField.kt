// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.Venue
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.task.TaskSpot
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.map.VenueGeometry
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Zoom level the picker opens on: a festival site fills the map. */
private const val PICKER_ZOOM = 18f

/**
 * The map of the spot picker: draws the venue and calls the tap lambda it is given with the
 * latitude and longitude of each tap. Tests swap the Google map for their own content.
 */
typealias SpotMapContent = @Composable BoxScope.(onTap: (lat: Double, lng: Double) -> Unit) -> Unit

/**
 * The spot a tap at ([lat], [lng]) picks: the tap in plan metres from the venue centre, snapped to
 * half a metre, becomes the first structure whose footprint holds it, edges included ("pins snap to
 * the nearest structure"), or else a free point.
 *
 * @param venue The venue the plan is measured from, with its bearing.
 * @param structures The structures a tap can land on.
 * @param lat Latitude of the tap, in degrees.
 * @param lng Longitude of the tap, in degrees.
 */
fun spotForTap(venue: Venue, structures: List<Structure>, lat: Double, lng: Double): TaskSpot {
  val (east, south) =
      VenueGeometry.metresFrom(venue.latitude, venue.longitude, venue.bearing, lat, lng)
  val x = VenueGeometry.snap(east)
  val y = VenueGeometry.snap(south)
  val hit = structures.firstOrNull { x in it.x..(it.x + it.width) && y in it.y..(it.y + it.height) }
  return if (hit != null) TaskSpot.onStructure(hit.structureId) else TaskSpot.at(x, y)
}

/**
 * The "Spot on the map" field of the task forms: a dropdown to choose no spot, one of the
 * [structures], or, once the event has a [venue], a point picked on the map in
 * [TaskSpotPickerSheet].
 *
 * @param spot The current spot, or null for none.
 * @param venue The venue of the event, or null when it is not placed or could not be loaded; the
 *   map option is then absent.
 * @param structures The structures of the event.
 * @param onSpotChange Called with the chosen spot, or null for "No spot".
 * @param mapContent The map of the picker sheet; the Google map by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSpotField(
    spot: TaskSpot?,
    venue: Venue?,
    structures: List<Structure>,
    onSpotChange: (TaskSpot?) -> Unit,
    modifier: Modifier = Modifier,
    mapContent: SpotMapContent = { onTap -> venue?.let { SpotPickerMap(it, structures, onTap) } },
) {
  var expanded by remember { mutableStateOf(false) }
  var picking by remember { mutableStateOf(false) }
  Column(modifier = modifier) {
    SectionLabel(stringResource(R.string.task_spot_label))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
      val accent = MaterialTheme.colorScheme.onPrimaryContainer
      OutlinedTextField(
          value = spotText(spot, structures),
          onValueChange = {},
          readOnly = true,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
          shape = RoundedCornerShape(12.dp),
          colors =
              OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = accent,
                  focusedLabelColor = accent,
                  cursorColor = accent,
              ),
          modifier =
              Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth()
                  .testTag(CreateTaskScreenTestTags.SPOT_DROPDOWN),
      )
      ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.task_spot_none)) },
            onClick = {
              expanded = false
              onSpotChange(null)
            },
            modifier = Modifier.testTag(CreateTaskScreenTestTags.SPOT_NONE),
        )
        structures.forEach { structure ->
          DropdownMenuItem(
              text = { Text(stringResource(R.string.task_spot_on_structure, structure.label)) },
              onClick = {
                expanded = false
                onSpotChange(TaskSpot.onStructure(structure.structureId))
              },
              modifier =
                  Modifier.testTag(CreateTaskScreenTestTags.spotStructure(structure.structureId)),
          )
        }
        if (venue != null) {
          DropdownMenuItem(
              text = { Text(stringResource(R.string.task_spot_pick_point)) },
              onClick = {
                expanded = false
                picking = true
              },
              modifier = Modifier.testTag(CreateTaskScreenTestTags.SPOT_PICK_POINT),
          )
        }
      }
    }
  }
  if (picking && venue != null) {
    TaskSpotPickerSheet(
        venue = venue,
        structures = structures,
        onConfirm = {
          picking = false
          onSpotChange(it)
        },
        onDismiss = { picking = false },
        mapContent = mapContent,
    )
  }
}

/**
 * The bottom sheet that picks a spot on the map: a tap on the map becomes a pending spot, described
 * under the map, and "Use this spot" confirms it.
 *
 * @param venue The venue the map is centred on and the taps are measured from.
 * @param structures The structures drawn on the map, which a tap snaps to.
 * @param onConfirm Called with the pending spot when "Use this spot" is pressed.
 * @param onDismiss Called on Cancel or when the sheet is dismissed; nothing is chosen.
 * @param mapContent The map, given the tap lambda; the Google map by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSpotPickerSheet(
    venue: Venue,
    structures: List<Structure>,
    onConfirm: (TaskSpot) -> Unit,
    onDismiss: () -> Unit,
    mapContent: SpotMapContent = { onTap -> SpotPickerMap(venue, structures, onTap) },
) {
  var pending by remember { mutableStateOf<TaskSpot?>(null) }
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      sheetGesturesEnabled = false,
      modifier = Modifier.testTag(CreateTaskScreenTestTags.SPOT_SHEET),
  ) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = stringResource(R.string.task_spot_pick_hint),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(12.dp))) {
        mapContent { lat, lng -> pending = spotForTap(venue, structures, lat, lng) }
      }
      pending?.let {
        Text(
            text = spotText(it, structures),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(CreateTaskScreenTestTags.SPOT_PENDING),
        )
      }
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
      ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(CreateTaskScreenTestTags.SPOT_CANCEL),
        ) {
          Text(stringResource(R.string.task_spot_cancel))
        }
        Button(
            onClick = { pending?.let(onConfirm) },
            enabled = pending != null,
            modifier = Modifier.testTag(CreateTaskScreenTestTags.SPOT_CONFIRM),
        ) {
          Text(stringResource(R.string.task_spot_confirm))
        }
      }
    }
  }
}

/**
 * The satellite map of the picker, centred on the venue, with the structures as footprints and a
 * marker on the spot the last tap picks.
 */
@Composable
private fun SpotPickerMap(
    venue: Venue,
    structures: List<Structure>,
    onTap: (lat: Double, lng: Double) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(LatLng(venue.latitude, venue.longitude), PICKER_ZOOM)
  }
  val properties = remember { MapProperties(mapType = MapType.SATELLITE) }
  val uiSettings = remember {
    MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
  }
  var tap by remember { mutableStateOf<LatLng?>(null) }
  GoogleMap(
      modifier = Modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      properties = properties,
      uiSettings = uiSettings,
      onMapClick = {
        tap = it
        onTap(it.latitude, it.longitude)
      },
  ) {
    structures.forEach { structure ->
      Polygon(
          points =
              VenueGeometry.corners(
                      venue.latitude,
                      venue.longitude,
                      venue.bearing,
                      structure.x,
                      structure.y,
                      structure.width,
                      structure.height,
                  )
                  .map { LatLng(it.latitude, it.longitude) },
          fillColor = colors.surface.copy(alpha = 0.92f),
          strokeColor = colors.onSurface,
          strokeWidth = 4f,
      )
    }
    tap?.let { t ->
      markerPosition(venue, structures, spotForTap(venue, structures, t.latitude, t.longitude))
          ?.let { Marker(state = rememberUpdatedMarkerState(position = it)) }
    }
  }
}

/** Where the marker of [spot] goes: the middle of its structure, or its point. */
private fun markerPosition(venue: Venue, structures: List<Structure>, spot: TaskSpot): LatLng? {
  val structure = structures.find { it.structureId == spot.structureId }
  val x = structure?.centerX ?: spot.x ?: return null
  val y = structure?.centerY ?: spot.y ?: return null
  val point = VenueGeometry.latLngFrom(venue.latitude, venue.longitude, venue.bearing, x, y)
  return LatLng(point.latitude, point.longitude)
}

/** "No spot", "On Marquee A" or "Point at 2.5 m east, 7 m south". */
@Composable
private fun spotText(spot: TaskSpot?, structures: List<Structure>): String {
  val structureId = spot?.structureId
  val x = spot?.x
  val y = spot?.y
  return when {
    structureId != null ->
        stringResource(
            R.string.task_spot_on_structure,
            structures.find { it.structureId == structureId }?.label ?: structureId,
        )
    x != null && y != null ->
        stringResource(
            R.string.task_spot_point,
            VenueGeometry.metresText(x),
            VenueGeometry.metresText(y),
        )
    else -> stringResource(R.string.task_spot_none)
  }
}
