// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.components.map.MapPinKind
import org.junit.Assert.assertEquals
import org.junit.Test

class EventLocationCardTest {
  /**
   * Breaks if the pin is not the event pin, or is not at the given location, for example when the
   * function ignores its parameter and always returns EPFL.
   */
  @Test
  fun eventMapPin_isTheEventPinAtTheGivenLocation() {
    listOf(EPFL_LOCATION, GeoPosition(46.2044, 6.1432)).forEach { location ->
      val pin = eventMapPin(location)

      assertEquals(MapPinKind.EVENT, pin.kind)
      assertEquals(location, pin.position)
    }
  }
}
