// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.R
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.model.map.geometry.barycenter
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

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

/** Whether this colour scheme is a dark one: its background is closer to black than to white. */
internal fun ColorScheme.isDark(): Boolean = background.luminance() < 0.5f

/**
 * The dark style on a dark theme, and the default style of Google Maps otherwise.
 *
 * The dark style (`res/raw/map_style_dark.json`) is the night mode sample of the Google Maps
 * documentation, https://developers.google.com/maps/documentation/android-sdk/styling, taken from
 * `tutorials/java/StyledMap/app/src/main/res/raw/style_json.json` of
 * https://github.com/googlemaps-samples/android-samples, which is licensed under the Apache License
 * 2.0 (https://www.apache.org/licenses/LICENSE-2.0).
 */
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
 * The camera state of the shared map, on the barycenter of [locations]. The state is built once, so
 * the camera is put back on the new barycenter when the locations change, for example when an event
 * is loaded after its screen is shown.
 */
@Composable
internal fun rememberFestivarCameraState(locations: List<GeoPosition>): CameraPositionState {
  val target = cameraPositionFor(locations)
  val cameraPositionState = rememberCameraPositionState { position = target }
  LaunchedEffect(target) { cameraPositionState.position = target }
  return cameraPositionState
}

/**
 * A Google map centred on the barycenter of [locations], with nothing drawn on it: no marker, no
 * polygon and no user location. What goes on the map is passed as [content]. The map does not ask
 * for the location permission.
 *
 * It follows the light and dark theme of [MaterialTheme]. It takes the size of its [modifier], so a
 * screen can put it in a card or give it the whole screen.
 *
 * @param locations Where the camera looks, at their barycenter. With none, it looks at EPFL.
 * @param gesturesEnabled Whether the user can move, zoom, rotate and tilt the map.
 * @param content Markers and polygons, drawn on the map.
 */
@Composable
fun FestivarMap(
    locations: List<GeoPosition>,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = false,
    content: @Composable () -> Unit = {},
) {
  val context = LocalContext.current
  val cameraPositionState = rememberFestivarCameraState(locations)
  val darkTheme = MaterialTheme.colorScheme.isDark()
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

/** Preview of the map: use "Run Preview on Device", the preview panel cannot draw Google Maps. */
@Preview(showBackground = true)
@Composable
private fun FestivarMapPreview() {
  AppTheme {
    FestivarMap(listOf(EPFL_LOCATION), Modifier.fillMaxSize(), gesturesEnabled = true) {
      Marker(
          state =
              rememberUpdatedMarkerState(
                  position = LatLng(EPFL_LOCATION.latitude, EPFL_LOCATION.longitude)
              )
      )
    }
  }
}
