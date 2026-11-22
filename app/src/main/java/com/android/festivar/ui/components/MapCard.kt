// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val BORDER_WIDTH = 1.dp
private val DASH_LENGTH = 6.dp
private val DASH_GAP = 4.dp

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

/** Test tags of [MapCardEmpty]. */
object MapCardEmptyTestTags {
  const val CARD = "map_card_empty"
  const val TITLE = "map_card_empty_title"
  const val SUBTITLE = "map_card_empty_subtitle"
  const val BUTTON = "map_card_empty_button"
}

/**
 * The dashed card that stands in for [MapCard] while nothing has a location yet, with a button to
 * add one. The event and the task screens use it with their own texts, so that both look the same.
 *
 * It takes the size of its [modifier]; by default it is as tall as its content.
 *
 * @param title What is missing, e.g. "Not on the map yet".
 * @param subtitle Why a location is worth adding.
 * @param buttonLabel The label of the button, e.g. "Pin it".
 * @param onButtonClick Called when the button is clicked.
 */
@Composable
fun MapCardEmpty(
    title: String,
    subtitle: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val shape = MaterialTheme.shapes.large
  val dashColor = MaterialTheme.colorScheme.outlineVariant
  Row(
      modifier =
          modifier
              .testTag(MapCardEmptyTestTags.CARD)
              .clip(shape)
              .background(MaterialTheme.colorScheme.surfaceContainerLow)
              .drawBehind {
                // The card clips what is drawn outside of it, so only the inner half of the line
                // shows: it is drawn twice as thick as the border it must look like.
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = dashColor,
                    style =
                        Stroke(
                            width = 2 * BORDER_WIDTH.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(DASH_LENGTH.toPx(), DASH_GAP.toPx())
                                ),
                        ),
                )
              }
              .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag(MapCardEmptyTestTags.TITLE),
      )
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(MapCardEmptyTestTags.SUBTITLE),
      )
    }
    Button(
        onClick = onButtonClick,
        modifier = Modifier.height(52.dp).testTag(MapCardEmptyTestTags.BUTTON),
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
      Text(text = buttonLabel, style = MaterialTheme.typography.labelLarge)
    }
  }
}

/** Preview of the empty card, with the wording of the task screen. */
@Preview(showBackground = true)
@Composable
internal fun MapCardEmptyPreview() {
  AppTheme {
    MapCardEmpty(
        title = "Not on the map yet",
        subtitle = "Volunteers find a pinned task faster.",
        buttonLabel = "Pin it",
        onButtonClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

/** Preview of the card: use "Run Preview on Device", the preview panel cannot draw Google Maps. */
@Preview(showBackground = true)
@Composable
internal fun MapCardPreview() {
  AppTheme {
    MapCard(listOf(EPFL_LOCATION), Modifier.fillMaxWidth().height(156.dp)) {
      Marker(
          state =
              rememberUpdatedMarkerState(
                  position = LatLng(EPFL_LOCATION.latitude, EPFL_LOCATION.longitude)
              )
      )
    }
  }
}
