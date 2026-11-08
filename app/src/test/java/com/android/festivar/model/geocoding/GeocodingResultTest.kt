// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

import org.junit.Assert.assertEquals
import org.junit.Test

class GeocodingResultTest {
  // Breaks if a blank label is accepted: the list would show an empty row.
  @Test(expected = IllegalArgumentException::class)
  fun blankLabel_isRefused() {
    GeocodingResult("  ", 46.5, 6.6)
  }

  // Breaks if the latitude bound is off: the map would be sent past the poles.
  @Test
  fun latitudeOutOfRange_isRefused() {
    for (latitude in listOf(-90.1, 90.1)) {
      assertRefused { GeocodingResult("Lausanne", latitude, 6.6) }
    }
  }

  // Breaks if the longitude bound is off: the map would be sent past the antimeridian.
  @Test
  fun longitudeOutOfRange_isRefused() {
    for (longitude in listOf(-180.1, 180.1)) {
      assertRefused { GeocodingResult("Lausanne", 46.5, longitude) }
    }
  }

  // Breaks if a bound is exclusive: the poles and the antimeridian are real places.
  @Test
  fun boundaryCoordinates_areAccepted() {
    assertEquals(-90.0, GeocodingResult("South", -90.0, -180.0).latitude, 0.0)
    assertEquals(180.0, GeocodingResult("North", 90.0, 180.0).longitude, 0.0)
  }

  private fun assertRefused(block: () -> Unit) {
    try {
      block()
    } catch (_: IllegalArgumentException) {
      return
    }
    throw AssertionError("expected IllegalArgumentException")
  }
}
