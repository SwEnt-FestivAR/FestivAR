// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.components.map.MapCard
import com.android.festivar.ui.components.map.MapPin
import com.android.festivar.ui.components.map.MapPinKind
import com.android.festivar.ui.theme.AppTheme

/** Test tags of [TaskMapCard]. */
object TaskMapCardTestTags {
  const val ADDRESS = "task_map_card_address"
}

/** A pin of the task map: its [kind] and where it stands. */
internal data class TaskMapPin(val kind: MapPinKind, val position: GeoPosition)

/**
 * What the task map draws for a task at [location]: one task pin on it, and nothing else. The map
 * looks at the position of the pins, so the camera is on the pin.
 */
internal fun taskMapPins(location: GeoPosition): List<TaskMapPin> =
    listOf(TaskMapPin(MapPinKind.TASK, location))

/**
 * The where card of a task, as in the "Task details" frames of Figma: a map centred on [location]
 * with the pin of the task on it, and the [address] of the task under the map. The card does not
 * react to touches.
 *
 * @param location Where the task is on the map.
 * @param address The place of the task, as text. With a blank one, the card is the map alone.
 */
@Composable
fun TaskMapCard(location: GeoPosition, address: String, modifier: Modifier = Modifier) {
  val pins = taskMapPins(location)
  val shape = MaterialTheme.shapes.large
  Column(
      modifier
          .fillMaxWidth()
          .clip(shape)
          .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
  ) {
    MapCard(
        locations = pins.map { it.position },
        modifier = Modifier.fillMaxWidth().height(123.dp),
        shape = RectangleShape,
    ) {
      pins.forEach { MapPin(it.position, it.kind) }
    }
    if (address.isNotBlank()) {
      Text(
          address,
          style = MaterialTheme.typography.titleSmall,
          modifier =
              Modifier.fillMaxWidth()
                  .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                  .testTag(TaskMapCardTestTags.ADDRESS),
      )
    }
  }
}

/** Preview of the task map: use "Run Preview on Device", the panel cannot draw Google Maps. */
@Preview(showBackground = true)
@Composable
internal fun TaskMapCardPreview() {
  AppTheme {
    TaskMapCard(EPFL_LOCATION, "Parc de Vidy, Lausanne, Vaud, Switzerland", Modifier.padding(20.dp))
  }
}
