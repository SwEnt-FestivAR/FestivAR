// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.ui.components.map.MapPinKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventLocationCardTest {
  /** Breaks if the event card draws a pin that is not the event's, or not at the location. */
  @Test
  fun eventMapPin_isTheEventPinAtTheLocation() {
    assertEquals(EventMapPin(MapPinKind.EVENT, EPFL_LOCATION), eventMapPin(EPFL_LOCATION))
  }
}
