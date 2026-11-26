// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.components.map.MapCard
import com.android.festivar.ui.components.map.MapPin
import com.android.festivar.ui.components.map.MapPinKind

/** Height of the map card, as in the "map card" of the Event details frame in Figma. */
private val EventLocationCardHeight = 156.dp

/** A pin of the kind [kind] drawn on the map of the event card at [position]. */
internal data class EventMapPin(val kind: MapPinKind, val position: GeoPosition)

/**
 * The pin of the event card: the pin of the event on [location]. It is a pure function because no
 * UI test sees the pins of a map.
 */
internal fun eventMapPin(location: GeoPosition): EventMapPin =
    EventMapPin(MapPinKind.EVENT, location)

/**
 * The map card of the Event Details screen, centred on [location] with the pin of the event on it.
 * The card is not clickable: there is no full-screen map from this screen.
 *
 * @param location Where the event is.
 */
@Composable
internal fun EventLocationCard(location: GeoPosition, modifier: Modifier = Modifier) {
  val pin = eventMapPin(location)
  MapCard(
      locations = listOf(pin.position),
      modifier = modifier.fillMaxWidth().height(EventLocationCardHeight),
  ) {
    MapPin(pin.position, pin.kind)
  }
}
