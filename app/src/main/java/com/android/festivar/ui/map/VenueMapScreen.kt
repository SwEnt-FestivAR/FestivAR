// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Test tags of the venue map, named after the "Map, volunteer" frame in Figma. */
object VenueMapScreenTestTags {
  /** The Google map itself. */
  const val MAP = "mapMap"
  /** The round back button at the top left. */
  const val BACK = "mapBack"
  /** The search field in the pill at the top. */
  const val SEARCH = "mapSearch"
  /** The "My pins" filter chip. */
  const val CHIP_MINE = "mapChipMine"
  /** The "All" filter chip. */
  const val CHIP_ALL = "mapChipAll"
  /** The "Structures" filter chip. */
  const val CHIP_STRUCTURES = "mapChipStructures"
  /** The "Open task" button of the sheet describing a task. */
  const val OPEN_TASK = "mapOpenTask"
  /** The line of the sheet shown when nothing is placed on the venue. */
  const val EMPTY_PINS = "mapEmptyPins"
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
  /** The "Editing" badge in the search row, shown to the organizer only. */
  const val EDITING_BADGE = "mapEditingBadge"
  /** The hint line under the organizer's palette. */
  const val PALETTE_HINT = "mapPaletteHint"
  /** The label of the selected structure in the sheet. */
  const val SELECTED_LABEL = "mapSelectedLabel"
  /** The pencil of the sheet that opens the rename dialog. */
  const val RENAME = "mapRename"
  /** The text field of the rename dialog. */
  const val RENAME_FIELD = "mapRenameField"
  /** The Save button of the rename dialog. */
  const val RENAME_SAVE = "mapRenameSave"
  /** The Cancel button of the rename dialog. */
  const val RENAME_CANCEL = "mapRenameCancel"
  /** The round undo button right of the search pill, shown to the organizer only. */
  const val UNDO = "mapUndo"
  /** The delete button of the sheet describing a structure. */
  const val DELETE = "mapDelete"
  /** The Delete button of the delete confirmation. */
  const val DELETE_CONFIRM = "mapDeleteConfirm"
  /** The Cancel button of the delete confirmation. */
  const val DELETE_DISMISS = "mapDeleteDismiss"
  /** The dialog asking the name and size of a custom element. */
  const val CUSTOM_DIALOG = "mapCustomDialog"
  /** The name field of the custom element dialog. */
  const val CUSTOM_LABEL = "mapCustomLabel"
  /** The width field of the custom element dialog. */
  const val CUSTOM_WIDTH = "mapCustomWidth"
  /** The height field of the custom element dialog. */
  const val CUSTOM_HEIGHT = "mapCustomHeight"
  /** The OK button of the custom element dialog. */
  const val CUSTOM_OK = "mapCustomOk"
  /** The Cancel button of the custom element dialog. */
  const val CUSTOM_CANCEL = "mapCustomCancel"

  /**
   * The palette item placing [tool].
   *
   * @param tool what the item places.
   * @return its test tag.
   */
  fun palette(tool: PaletteTool): String = "mapPalette_${tool.name}"
}

/** Zoom level the camera opens on: a festival site fills the screen. */
private const val VENUE_ZOOM = 18f

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

/**
 * The read only venue map of an event, reached from its details: the venue on a satellite map, the
 * organizer's structures on it and the user's own position.
 *
 * Asks for the fine location permission on first composition and starts following the position once
 * it is granted; on denial the "No GPS fix" pill simply stays.
 *
 * @param viewModel holds the event, its structures, its tasks and the position.
 * @param onBack called when the back button is clicked.
 * @param onOpenTask called with a task id when the sheet's "Open task" button is clicked.
 */
@Composable
fun VenueMapScreen(
    viewModel: VenueMapViewModel,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
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
      onSelectTask = viewModel::selectTask,
      onFilter = viewModel::setFilter,
      onQuery = viewModel::setQuery,
      onOpenTask = onOpenTask,
      onSelectTool = viewModel::selectTool,
      onPlace = viewModel::placeAt,
      onMove = viewModel::moveSelected,
      onResize = viewModel::resizeSelected,
      onStartRename = viewModel::startRename,
      onRename = viewModel::rename,
      onCancelRename = viewModel::cancelRename,
      onDelete = viewModel::deleteSelected,
      onUndo = viewModel::undo,
      onUpdateCustomDraft = viewModel::updateCustomDraft,
      onConfirmCustomDraft = viewModel::confirmCustomDraft,
      onDismissCustomDraft = viewModel::dismissCustomDraft,
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
 * @param onSelectTask called with a task id when its pin is tapped, null to clear.
 * @param onFilter called with the filter chip that is clicked.
 * @param onQuery called with the search text as it is typed.
 * @param onOpenTask called with the task id when the sheet's "Open task" button is clicked.
 * @param onSelectTool called with the palette item the organizer taps.
 * @param onPlace called with the point the organizer taps on the bare map while a tool is picked.
 * @param onMove called with the point the organizer long presses while a structure is selected.
 * @param onResize called with the corner the organizer dragged and where it was dropped.
 * @param onStartRename called when the rename pencil of the sheet is clicked.
 * @param onRename called with the typed name when the rename dialog is saved.
 * @param onCancelRename called when the rename dialog is cancelled.
 * @param onDelete called once the organizer confirms deleting the selected structure.
 * @param onUndo called when the undo button is clicked.
 * @param onUpdateCustomDraft called with the name, width and height typed in the custom dialog.
 * @param onConfirmCustomDraft called when the custom dialog is confirmed.
 * @param onDismissCustomDraft called when the custom dialog is cancelled.
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
    onSelectTask: (String?) -> Unit = {},
    onFilter: (PinFilter) -> Unit = {},
    onQuery: (String) -> Unit = {},
    onOpenTask: (String) -> Unit = {},
    onSelectTool: (PaletteTool?) -> Unit = {},
    onPlace: (Double, Double) -> Unit = { _, _ -> },
    onMove: (Double, Double) -> Unit = { _, _ -> },
    onResize: (Corner, Double, Double) -> Unit = { _, _, _ -> },
    onStartRename: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onCancelRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onUndo: () -> Unit = {},
    onUpdateCustomDraft: (String, String, String) -> Unit = { _, _, _ -> },
    onConfirmCustomDraft: () -> Unit = {},
    onDismissCustomDraft: () -> Unit = {},
    mapContent: @Composable BoxScope.(VenueMapUiState, (String?) -> Unit) -> Unit = { s, select ->
      VenueGoogleMap(s, select, onSelectTask, onPlace, onMove, onResize)
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
          TopOverlay(state, onFilter, onQuery, onUndo, Modifier.align(Alignment.TopCenter))
          if (state.errorMsg != null) {
            MapErrorCard(
                state.errorMsg,
                onRetry,
                onDismissError,
                Modifier.align(Alignment.Center).padding(20.dp),
            )
          }
          val focus = state.focus
          if (focus != null) {
            TaskSheet(focus, onOpenTask, Modifier.align(Alignment.BottomCenter))
          } else {
            Column(Modifier.align(Alignment.BottomCenter)) {
              if (state.isOrganizer) Palette(state.tool, onSelectTool)
              MapSheet(state, onSelect, onStartRename, onDelete)
            }
          }
          val selected = state.selected
          if (state.isOrganizer && state.renaming && selected != null) {
            RenameDialog(selected.label, onRename, onCancelRename)
          }
          val draft = state.customDraft
          if (state.isOrganizer && draft != null) {
            CustomDialog(draft, onUpdateCustomDraft, onConfirmCustomDraft, onDismissCustomDraft)
          }
        }
      }
      BackDisc(onBack, Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))
    }
  }
}

/**
 * The satellite map centred on the venue, with each structure as a tappable footprint and label,
 * the task pins as pennants with their label, and the user's fix as an accuracy ring and dot. Draws
 * nothing without a venue. A tap on the bare map clears both selections.
 *
 * For the organizer, a tap on the bare map with a tool picked places it instead, a long press moves
 * the selected structure there, and the selected structure gets a draggable handle on each corner.
 *
 * @param state the venue, structures, pins, selection and fix to draw.
 * @param onSelect called with the id of a tapped structure.
 * @param onSelectTask called with the id of a tapped task pin.
 * @param onPlace called with the tapped point while the organizer has a tool picked.
 * @param onMove called with the long pressed point while the organizer has a structure selected.
 * @param onResize called with a corner handle and where it was dropped.
 */
@Composable
fun VenueGoogleMap(
    state: VenueMapUiState,
    onSelect: (String?) -> Unit,
    onSelectTask: (String?) -> Unit = {},
    onPlace: (Double, Double) -> Unit = { _, _ -> },
    onMove: (Double, Double) -> Unit = { _, _ -> },
    onResize: (Corner, Double, Double) -> Unit = { _, _, _ -> },
) {
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
      onMapClick = {
        if (state.isOrganizer && state.tool != null) {
          onPlace(it.latitude, it.longitude)
        } else {
          onSelect(null)
          onSelectTask(null)
        }
      },
      onMapLongClick = {
        if (state.isOrganizer && state.selected != null) onMove(it.latitude, it.longitude)
      },
  ) {
    val selected = state.selected
    if (state.isOrganizer && selected != null) {
      CornerHandles(venue, selected, onResize)
    }
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
    state.pins.forEach { pin ->
      MarkerComposable(
          pin.taskId,
          pin.status,
          pin.label,
          state =
              rememberUpdatedMarkerState(
                  position = LatLng(pin.point.latitude, pin.point.longitude)
              ),
          anchor = Offset(0f, 1f),
          onClick = {
            onSelectTask(pin.taskId)
            true
          },
      ) {
        TaskPin(pin)
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

/** The corners of [VenueGeometry.corners], in its order: clockwise from the top left. */
private val CORNER_ORDER =
    listOf(Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT)

/** Side of a corner handle, in pixels. */
private const val HANDLE_PX = 24

/** Width of the ink border of a corner handle, in pixels. */
private const val HANDLE_BORDER_PX = 3f

/** A draggable handle on each corner of [structure]; dropping one calls [onResize]. */
@Composable
@GoogleMapComposable
private fun CornerHandles(
    venue: Venue,
    structure: Structure,
    onResize: (Corner, Double, Double) -> Unit,
) {
  val icon = rememberHandleIcon()
  val points =
      VenueGeometry.corners(
          venue.latitude,
          venue.longitude,
          venue.bearing,
          structure.x,
          structure.y,
          structure.width,
          structure.height,
      )
  CORNER_ORDER.zip(points).forEach { (corner, point) ->
    key(structure.structureId, corner) {
      CornerHandle(corner, LatLng(point.latitude, point.longitude), icon, onResize)
    }
  }
}

/**
 * One corner handle at [point]. Once a drag ends away from [point], calls [onResize] with the drop
 * and puts the handle back; a saved resize then redraws it at the new corner.
 */
@Composable
@GoogleMapComposable
private fun CornerHandle(
    corner: Corner,
    point: LatLng,
    icon: BitmapDescriptor,
    onResize: (Corner, Double, Double) -> Unit,
) {
  val markerState = remember(point) { MarkerState(point) }
  val resize by rememberUpdatedState(onResize)
  LaunchedEffect(markerState) {
    snapshotFlow { markerState.isDragging to markerState.position }
        .collect { (dragging, position) ->
          if (!dragging && position != point) {
            resize(corner, position.latitude, position.longitude)
            markerState.position = point
          }
        }
  }
  Marker(state = markerState, icon = icon, anchor = Offset(0.5f, 0.5f), draggable = true)
}

/** The corner handle icon, drawn once per theme: a 24 px primary square with an ink border. */
@Composable
private fun rememberHandleIcon(): BitmapDescriptor {
  val fill = MaterialTheme.colorScheme.primary.toArgb()
  val ink = MaterialTheme.colorScheme.onSurface.toArgb()
  return remember(fill, ink) {
    val bitmap = Bitmap.createBitmap(HANDLE_PX, HANDLE_PX, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint()
    val side = HANDLE_PX.toFloat()
    paint.color = ink
    canvas.drawRect(0f, 0f, side, side, paint)
    paint.color = fill
    canvas.drawRect(
        HANDLE_BORDER_PX,
        HANDLE_BORDER_PX,
        side - HANDLE_BORDER_PX,
        side - HANDLE_BORDER_PX,
        paint,
    )
    BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}

/** A task pin: the pennant tinted by its status, then a pill with its label. */
@Composable
private fun TaskPin(pin: MapPin) {
  val colors = MaterialTheme.colorScheme
  val (tint, description) =
      when (pin.status) {
        PinStatus.OPEN -> colors.onSurface to R.string.pin_open
        PinStatus.MINE -> colors.primary to R.string.pin_mine
        PinStatus.TAKEN -> colors.tertiary to R.string.pin_taken
        PinStatus.DONE -> colors.onSurfaceVariant to R.string.pin_done
      }
  Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
      if (pin.status == PinStatus.MINE) {
        // The ink outline: a full size ink pennant under a smaller primary one.
        Icon(
            painter = painterResource(R.drawable.ic_pennant),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = colors.onSurface,
        )
      }
      Icon(
          painter = painterResource(R.drawable.ic_pennant),
          contentDescription = stringResource(description),
          modifier = Modifier.size(if (pin.status == PinStatus.MINE) 20.dp else 24.dp),
          tint = tint,
      )
    }
    Surface(shape = CircleShape, color = colors.surface) {
      Text(
          text = pin.label,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          style = MaterialTheme.typography.labelMedium,
          color = colors.onSurface,
          maxLines = 1,
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

/**
 * The search pill right of the back button, the filter chips under it and, while there is no fix,
 * the "No GPS fix" pill on the right.
 */
@Composable
private fun TopOverlay(
    state: VenueMapUiState,
    onFilter: (PinFilter) -> Unit,
    onQuery: (String) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
        modifier = Modifier.padding(start = 72.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
          modifier = Modifier.weight(1f),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
      ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().testTag(VenueMapScreenTestTags.SEARCH),
            placeholder = { Text(stringResource(R.string.map_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (state.isOrganizer) ({ EditingBadge() }) else null,
            singleLine = true,
            shape = CircleShape,
        )
      }
      if (state.isOrganizer) UndoDisc(state.canUndo, onUndo)
    }
    Row(
        modifier = Modifier.padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      MapChip(
          stringResource(R.string.map_chip_mine, state.mineCount),
          state.filter == PinFilter.MINE,
          { onFilter(PinFilter.MINE) },
          VenueMapScreenTestTags.CHIP_MINE,
      )
      MapChip(
          stringResource(R.string.map_chip_all, state.allCount),
          state.filter == PinFilter.ALL,
          { onFilter(PinFilter.ALL) },
          VenueMapScreenTestTags.CHIP_ALL,
      )
      MapChip(
          stringResource(R.string.map_chip_structures),
          state.filter == PinFilter.STRUCTURES,
          { onFilter(PinFilter.STRUCTURES) },
          VenueMapScreenTestTags.CHIP_STRUCTURES,
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

/** The 44dp round undo button right of the search pill, enabled when [canUndo]. */
@Composable
private fun UndoDisc(canUndo: Boolean, onUndo: () -> Unit) {
  Surface(
      modifier = Modifier.size(44.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 2.dp,
  ) {
    IconButton(
        onClick = onUndo,
        enabled = canUndo,
        modifier = Modifier.testTag(VenueMapScreenTestTags.UNDO),
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Rounded.Undo,
          contentDescription = stringResource(R.string.map_undo),
      )
    }
  }
}

/** The "Editing" pill telling the organizer that taps change the plan. */
@Composable
private fun EditingBadge() {
  Surface(
      modifier = Modifier.padding(end = 8.dp).testTag(VenueMapScreenTestTags.EDITING_BADGE),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary,
  ) {
    Text(
        text = stringResource(R.string.map_editing),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimary,
    )
  }
}

/** The label of each palette item. */
private fun toolLabel(tool: PaletteTool): Int =
    when (tool) {
      PaletteTool.MARQUEE -> R.string.map_tool_marquee
      PaletteTool.STAGE -> R.string.map_tool_stage
      PaletteTool.STAND -> R.string.map_tool_stand
      PaletteTool.TABLE -> R.string.map_tool_table
      PaletteTool.CUSTOM -> R.string.map_tool_custom
    }

/** The organizer's palette over the sheet, the picked tool outlined in primary, and its hint. */
@Composable
private fun Palette(tool: PaletteTool?, onSelectTool: (PaletteTool?) -> Unit) {
  val colors = MaterialTheme.colorScheme
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PaletteTool.entries.forEach { item ->
        val picked = item == tool
        Surface(
            onClick = { onSelectTool(item) },
            modifier =
                Modifier.testTag(VenueMapScreenTestTags.palette(item)).semantics {
                  selected = picked
                },
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border =
                if (picked) BorderStroke(2.dp, colors.primary)
                else BorderStroke(1.dp, colors.outline),
            shadowElevation = 2.dp,
        ) {
          Text(
              text = stringResource(toolLabel(item)),
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              style = MaterialTheme.typography.labelLarge,
              color = colors.onSurface,
          )
        }
      }
    }
    Surface(shape = CircleShape, color = colors.surface) {
      Text(
          text = stringResource(R.string.map_palette_hint),
          modifier =
              Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                  .testTag(VenueMapScreenTestTags.PALETTE_HINT),
          style = MaterialTheme.typography.labelMedium,
          color = colors.onSurfaceVariant,
      )
    }
  }
}

/**
 * The rename dialog, its field starting on [current]; Save is enabled once the name is not blank.
 */
@Composable
private fun RenameDialog(current: String, onRename: (String) -> Unit, onCancel: () -> Unit) {
  var text by remember { mutableStateOf(current) }
  AlertDialog(
      onDismissRequest = onCancel,
      title = { Text(stringResource(R.string.map_rename_title)) },
      text = {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.testTag(VenueMapScreenTestTags.RENAME_FIELD),
            label = { Text(stringResource(R.string.map_rename_label)) },
            singleLine = true,
        )
      },
      confirmButton = {
        TextButton(
            onClick = { onRename(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.testTag(VenueMapScreenTestTags.RENAME_SAVE),
        ) {
          Text(stringResource(R.string.map_save))
        }
      },
      dismissButton = {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.testTag(VenueMapScreenTestTags.RENAME_CANCEL),
        ) {
          Text(stringResource(R.string.map_cancel))
        }
      },
  )
}

/** The custom element dialog: a name and two sizes, OK enabled once [draft] is valid. */
@Composable
private fun CustomDialog(
    draft: CustomDraft,
    onUpdate: (String, String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag(VenueMapScreenTestTags.CUSTOM_DIALOG),
      title = { Text(stringResource(R.string.map_custom_title)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
              value = draft.label,
              onValueChange = { onUpdate(it, draft.width, draft.height) },
              modifier = Modifier.testTag(VenueMapScreenTestTags.CUSTOM_LABEL),
              label = { Text(stringResource(R.string.map_custom_label)) },
              singleLine = true,
          )
          OutlinedTextField(
              value = draft.width,
              onValueChange = { onUpdate(draft.label, it, draft.height) },
              modifier = Modifier.testTag(VenueMapScreenTestTags.CUSTOM_WIDTH),
              label = { Text(stringResource(R.string.map_custom_width)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
          )
          OutlinedTextField(
              value = draft.height,
              onValueChange = { onUpdate(draft.label, draft.width, it) },
              modifier = Modifier.testTag(VenueMapScreenTestTags.CUSTOM_HEIGHT),
              label = { Text(stringResource(R.string.map_custom_height)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
          )
        }
      },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            enabled = draft.isValid,
            modifier = Modifier.testTag(VenueMapScreenTestTags.CUSTOM_OK),
        ) {
          Text(stringResource(android.R.string.ok))
        }
      },
      dismissButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(VenueMapScreenTestTags.CUSTOM_CANCEL),
        ) {
          Text(stringResource(R.string.map_cancel))
        }
      },
  )
}

/** A filter chip on a surface background, so it stays readable over the satellite map. */
@Composable
private fun MapChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
  Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
    FilterChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
  }
}

/**
 * The bottom sheet: the venue summary, or the selected structure with a button to clear it. The
 * organizer also sees how many tasks are pinned on the structure, a rename pencil and a delete
 * button that asks for confirmation first.
 */
@Composable
private fun MapSheet(
    state: VenueMapUiState,
    onSelect: (String?) -> Unit,
    onStartRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
  var confirmDelete by remember { mutableStateOf(false) }
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
            modifier =
                if (selected != null) Modifier.testTag(VenueMapScreenTestTags.SELECTED_LABEL)
                else Modifier,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                when {
                  selected == null -> summaryLine(state)
                  state.isOrganizer -> {
                    val pinned = state.tasks.count { it.spot?.structureId == selected.structureId }
                    stringResource(
                        R.string.map_structure_summary,
                        VenueGeometry.metresText(selected.width),
                        VenueGeometry.metresText(selected.height),
                        pluralStringResource(R.plurals.map_tasks, pinned, pinned),
                    )
                  }
                  else ->
                      stringResource(
                          R.string.map_structure_size,
                          VenueGeometry.metresText(selected.width),
                          VenueGeometry.metresText(selected.height),
                      )
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selected == null && state.pins.isEmpty() && state.structures.isEmpty()) {
          Text(
              text = stringResource(R.string.map_empty),
              modifier = Modifier.testTag(VenueMapScreenTestTags.EMPTY_PINS),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (selected != null && state.isOrganizer) {
        IconButton(
            onClick = onStartRename,
            modifier = Modifier.testTag(VenueMapScreenTestTags.RENAME),
        ) {
          Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.map_rename_title),
              tint = MaterialTheme.colorScheme.onSurface,
          )
        }
        IconButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.testTag(VenueMapScreenTestTags.DELETE),
        ) {
          Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = stringResource(R.string.map_delete),
              tint = MaterialTheme.colorScheme.error,
          )
        }
        if (confirmDelete) {
          FestivarConfirmDialog(
              title = stringResource(R.string.map_delete_title, selected.label),
              message = stringResource(R.string.map_delete_message),
              confirmText = stringResource(R.string.map_delete),
              dismissText = stringResource(R.string.map_cancel),
              onConfirm = {
                confirmDelete = false
                onDelete()
              },
              onDismiss = { confirmDelete = false },
              confirmModifier = Modifier.testTag(VenueMapScreenTestTags.DELETE_CONFIRM),
              dismissModifier = Modifier.testTag(VenueMapScreenTestTags.DELETE_DISMISS),
          )
        }
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

/**
 * "Fête de Vidy · 3 structures · Parc de Vidy": the event title, the count and the location, the
 * blank ones left out.
 */
@Composable
private fun summaryLine(state: VenueMapUiState): String {
  val count =
      pluralStringResource(R.plurals.map_structures, state.structures.size, state.structures.size)
  return listOf(state.eventTitle, count, state.eventLocation)
      .filter { it.isNotBlank() }
      .joinToString(" · ")
}

/**
 * The sheet describing a tapped task pin: title, place and partner, start time and how long until
 * it, the walk from the user's position, and the button to open the task.
 */
@Composable
private fun TaskSheet(focus: FocusTask, onOpenTask: (String) -> Unit, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Surface(
      modifier = modifier.fillMaxWidth().testTag(VenueMapScreenTestTags.SHEET),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      color = colors.surface,
      shadowElevation = 4.dp,
  ) {
    Column(
        modifier = Modifier.navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
              text = focus.title,
              style = MaterialTheme.typography.titleLarge,
              color = colors.onSurface,
          )
          Text(
              text =
                  focus.withName?.let { stringResource(R.string.map_place_with, focus.place, it) }
                      ?: focus.place,
              style = MaterialTheme.typography.bodyMedium,
              color = colors.onSurfaceVariant,
          )
        }
        if (focus.time != null) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
                text = focus.time,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            focus.minutesUntil?.let {
              Text(
                  text = untilText(it),
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.onSurfaceVariant,
              )
            }
          }
        }
      }
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.onSurfaceVariant,
        )
        Text(
            text =
                if (focus.distanceMetres != null && focus.walkMinutes != null)
                    stringResource(R.string.map_distance, focus.distanceMetres, focus.walkMinutes)
                else stringResource(R.string.map_distance_unknown),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
      }
      Button(
          onClick = { onOpenTask(focus.taskId) },
          modifier = Modifier.fillMaxWidth().testTag(VenueMapScreenTestTags.OPEN_TASK),
      ) {
        Text(stringResource(R.string.map_open_task))
      }
    }
  }
}

/** "Started" once begun, else "in 40 min", "in 3 h" or "in 2 days". */
@Composable
private fun untilText(minutes: Long): String =
    when {
      minutes < 0 -> stringResource(R.string.map_started)
      minutes < MINUTES_PER_HOUR -> stringResource(R.string.map_in_minutes, minutes)
      minutes < MINUTES_PER_DAY -> stringResource(R.string.map_in_hours, minutes / MINUTES_PER_HOUR)
      else -> {
        val days = (minutes / MINUTES_PER_DAY).toInt()
        pluralStringResource(R.plurals.map_in_days, days, days)
      }
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
