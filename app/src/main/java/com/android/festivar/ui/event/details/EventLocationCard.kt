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

/**
 * The map card of the Event Details screen, centred on [location] with the pin of the event on it.
 * The card is not clickable: there is no full-screen map from this screen.
 *
 * @param location Where the event is.
 */
@Composable
internal fun EventLocationCard(location: GeoPosition, modifier: Modifier = Modifier) {
  MapCard(
      locations = listOf(location),
      modifier = modifier.fillMaxWidth().height(156.dp),
  ) {
    MapPin(location, MapPinKind.EVENT)
  }
}
