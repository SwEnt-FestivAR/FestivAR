// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.android.festivar.model.map.geometry.GeoPosition

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
 * @param shape The outline of the card, rounded on all four corners by default. A screen that puts
 *   the card inside a larger one passes a shape that rounds only the corners that show.
 * @param content Markers and polygons, drawn on the map.
 */
@Composable
fun MapCard(
    locations: List<GeoPosition>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit = {},
) {
  Box(
      modifier = modifier.clip(shape).testTag(MapCardTestTags.CARD),
  ) {
    FestivarMap(
        locations = locations,
        modifier = Modifier.matchParentSize(),
        gesturesEnabled = false,
        content = content,
    )
    // The map's gestures are off, but a tap on a marker still moves its camera and opens its info
    // window. This layer sits above the map so that no touch reaches it. It leaves the touches
    // unconsumed, so a screen can still make the whole card clickable.
    Box(Modifier.matchParentSize().testTag(MapCardTestTags.TOUCH_BLOCKER).pointerInput(Unit) {})
  }
}
