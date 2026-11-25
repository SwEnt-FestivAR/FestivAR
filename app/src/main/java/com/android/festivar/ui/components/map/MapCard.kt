// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Test tags of [MapCard]. */
object MapCardTestTags {
  const val CARD = "map_card"
  const val TOUCH_BLOCKER = "map_card_touch_blocker"
}

/**
 * A [FestivarMap] in a rounded card, shared by the event and the task screens so that both look the
 * same. The card does not react to touches and the map does not move inside it, not even when a
 * marker of [content] is tapped; what surrounds the card belongs to the screen. The map keeps its
 * Google credit visible in the bottom corner.
 *
 * It takes the size of its [modifier], so the screen decides how tall the card is.
 *
 * @param locations Where the map looks, at their barycenter. With none, it looks at EPFL.
 * @param shape The outline of the card. With none (`null`), the card takes the usual rounded
 *   corners of the theme (`MaterialTheme.shapes.large`), on all four corners. A screen that puts
 *   the card inside a larger one passes a shape that rounds only the corners that show.
 * @param content Markers and polygons, drawn on the map.
 */
@Composable
fun MapCard(
    locations: List<GeoPosition>,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    content: @Composable () -> Unit = {},
) {
  // null and not MaterialTheme.shapes.large in the signature: a default that reads the theme adds
  // compiler-generated branches that no test can reach.
  val cardShape = shape ?: MaterialTheme.shapes.large
  Box(
      modifier = modifier.clip(cardShape).testTag(MapCardTestTags.CARD),
  ) {
    FestivarMap(
        locations = locations,
        modifier = Modifier.matchParentSize(),
        gesturesEnabled = false,
        content = content,
    )
    // The map's gestures are off, but a tap on a marker still moves its camera and opens its info
    // window. This layer sits above the map so that no touch reaches it.
    TouchBlocker(Modifier.matchParentSize().testTag(MapCardTestTags.TOUCH_BLOCKER))
  }
}

/**
 * A transparent layer that keeps the touches from reaching what is drawn under it. It leaves the
 * touches unconsumed, so a screen can still make the whole card clickable.
 */
@Composable
internal fun TouchBlocker(modifier: Modifier) {
  Box(modifier.pointerInput(Unit) {})
}

/** Preview of the card: use "Run Preview on Device", the preview panel cannot draw Google Maps. */
@Preview(showBackground = true)
@Composable
internal fun MapCardPreview() {
  // Two titled markers around the center of the card: tapping one is how to check by hand that the
  // card neither moves its camera nor opens an info window.
  val locations =
      listOf(
          EPFL_LOCATION,
          GeoPosition(EPFL_LOCATION.latitude - 0.0004, EPFL_LOCATION.longitude - 0.0008),
      )
  AppTheme {
    MapCard(locations, Modifier.fillMaxWidth().padding(20.dp).height(156.dp)) {
      locations.forEachIndexed { index, location ->
        Marker(
            state =
                rememberUpdatedMarkerState(
                    position = LatLng(location.latitude, location.longitude)
                ),
            title = "Marker ${index + 1}",
        )
      }
    }
  }
}
