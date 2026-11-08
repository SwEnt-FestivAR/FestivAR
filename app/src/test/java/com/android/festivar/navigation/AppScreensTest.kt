// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppScreensTest {
  /** Red if the venue map route string stops matching its nav graph pattern. */
  @Test
  fun venueMapRouteCarriesTheEventId() {
    assertEquals("VenueMap/e1", AppScreens.venueMap("e1"))
  }
}
