// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.android.festivar.R
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.model.map.geometry.barycenter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/** Test tags of [FestivarMap]. */
object FestivarMapTestTags {
  const val MAP = "festivar_map"
}

/** Zoom level that shows the grounds of an event, not the whole city. */
private const val MAP_ZOOM = 16f

/**
 * The camera of the shared map: on the barycenter of [locations], or on the EPFL campus when there
 * is none.
 */
internal fun cameraPositionFor(locations: List<GeoPosition>): CameraPosition {
  val center = barycenter(locations)
  return CameraPosition.fromLatLngZoom(LatLng(center.latitude, center.longitude), MAP_ZOOM)
}

/** The dark style on a dark theme, and the default style of Google Maps otherwise. */
internal fun mapPropertiesFor(darkTheme: Boolean, context: Context): MapProperties =
    MapProperties(
        mapStyleOptions =
            if (darkTheme) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
            else null
    )

/** The map shows no control of its own, and moves only when [gesturesEnabled]. */
internal fun mapUiSettingsFor(gesturesEnabled: Boolean): MapUiSettings =
    MapUiSettings(
        compassEnabled = false,
        mapToolbarEnabled = false,
        myLocationButtonEnabled = false,
        zoomControlsEnabled = false,
        scrollGesturesEnabled = gesturesEnabled,
        zoomGesturesEnabled = gesturesEnabled,
        rotationGesturesEnabled = gesturesEnabled,
        tiltGesturesEnabled = gesturesEnabled,
    )

/**
 * A Google map centred on the barycenter of [locations], with nothing drawn on it: no marker, no
 * polygon and no user location. What goes on the map is passed as [content]. The map does not ask
 * for the location permission.
 *
 * It follows the light and dark theme. It takes the size of its [modifier], so a screen can put it
 * in a card or give it the whole screen.
 *
 * @param locations Where the camera looks, at their barycenter. With none, it looks at EPFL.
 * @param gesturesEnabled Whether the user can move, zoom, rotate and tilt the map.
 * @param darkTheme Whether the map uses its dark style.
 * @param content Markers and polygons, drawn on the map.
 */
@Composable
fun FestivarMap(
    locations: List<GeoPosition>,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit = {},
) {
  val context = LocalContext.current
  val target = cameraPositionFor(locations)
  val cameraPositionState = rememberCameraPositionState { position = target }
  // The state is built once, so the camera is put back on the new barycenter when the locations
  // change.
  LaunchedEffect(target) { cameraPositionState.position = target }
  val properties = remember(darkTheme) { mapPropertiesFor(darkTheme, context) }
  val uiSettings = remember(gesturesEnabled) { mapUiSettingsFor(gesturesEnabled) }
  GoogleMap(
      modifier = modifier.testTag(FestivarMapTestTags.MAP),
      cameraPositionState = cameraPositionState,
      properties = properties,
      uiSettings = uiSettings,
      content = content,
  )
}
