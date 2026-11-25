// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.components.FestivarMap
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * What a pin on the map stands for, as in the `Pin` component of the Figma. The event pin is larger
 * than the task pin, so an event stays visible among its tasks.
 *
 * @property drawable The pennant of the design: a pole, a flag and a ring on the ground.
 * @property size Width and height of the pin.
 * @property testTag Test tag of the pin.
 */
enum class MapPinKind(val drawable: Int, val size: Dp, val testTag: String) {
  /** The location of an event: a red flag. */
  EVENT(R.drawable.ic_map_pin_event, size = 56.dp, testTag = "map_pin_event"),
  /** The location of a task: a lime flag with an ink outline. */
  TASK(R.drawable.ic_map_pin_task, size = 40.dp, testTag = "map_pin_task"),
}

/**
 * Where the ring at the foot of the pole is in a pin, as a fraction of its width and of its height
 * (13.33 and 26.67 in the 32 units of the drawables). A marker is anchored on it, so the ring is on
 * the location and the pin does not hang below it.
 */
internal val MapPinAnchor = Offset(13.3333f / 32f, 26.6667f / 32f)

/** The pin of this [kind], as designed in Figma. */
@Composable
fun MapPin(kind: MapPinKind, modifier: Modifier = Modifier) {
  Image(
      painter = painterResource(kind.drawable),
      contentDescription = null,
      modifier = modifier.size(kind.size).testTag(kind.testTag),
  )
}

/**
 * A pin of the given [kind] on [location], to be put in the content of a `FestivarMap`. The ring at
 * the foot of the pin is on the location.
 */
@Composable
fun LocationPin(location: GeoPosition, kind: MapPinKind) {
  MarkerComposable(
      kind,
      state = rememberUpdatedMarkerState(LatLng(location.latitude, location.longitude)),
      anchor = MapPinAnchor,
  ) {
    MapPin(kind)
  }
}

/** Preview of the two pins, side by side. */
@Preview(showBackground = true)
@Composable
internal fun MapPinPreview() {
  AppTheme {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      MapPin(MapPinKind.EVENT)
      MapPin(MapPinKind.TASK)
    }
  }
}

/**
 * Preview of a map with both pins: the camera is on the event, and the tasks of the event are drawn
 * around it. Use "Run Preview on Device", the panel cannot draw Google Maps.
 */
@Preview(showBackground = true)
@Composable
internal fun LocationPinPreview() {
  val event = EPFL_LOCATION
  val task = GeoPosition(event.latitude + 0.002, event.longitude + 0.003)
  AppTheme {
    FestivarMap(listOf(event), Modifier.fillMaxSize()) {
      LocationPin(event, MapPinKind.EVENT)
      LocationPin(task, MapPinKind.TASK)
    }
  }
}
