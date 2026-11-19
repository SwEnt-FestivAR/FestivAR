// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PositionsTest {

  private fun assertInvalid(expectedMessage: String, build: () -> Any) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  // Breaks if a valid position is refused, the bounds included.
  @Test
  fun aGeoPositionOnEarthIsValid() {
    val lausanne = GeoPosition(46.5191, 6.5668)

    assertEquals(46.5191, lausanne.latitude, 0.0)
    assertEquals(6.5668, lausanne.longitude, 0.0)
    GeoPosition(90.0, 180.0)
    GeoPosition(-90.0, -180.0)
  }

  // Breaks if a latitude beyond a pole is accepted.
  @Test
  fun aLatitudeOutOfRangeIsRefused() {
    val message = "The latitude must be between -90 and 90."
    assertInvalid(message) { GeoPosition(90.1, 0.0) }
    assertInvalid(message) { GeoPosition(-90.1, 0.0) }
  }

  // Breaks if a longitude beyond the antimeridian is accepted.
  @Test
  fun aLongitudeOutOfRangeIsRefused() {
    val message = "The longitude must be between -180 and 180."
    assertInvalid(message) { GeoPosition(0.0, 180.1) }
    assertInvalid(message) { GeoPosition(0.0, -180.1) }
  }

  // Breaks if NaN or an infinity gets into a position, where every later computation would be NaN.
  @Test
  fun aGeoPositionThatIsNotFiniteIsRefused() {
    val message = "The latitude and the longitude must be finite."
    assertInvalid(message) { GeoPosition(Double.NaN, 0.0) }
    assertInvalid(message) { GeoPosition(0.0, Double.NaN) }
    assertInvalid(message) { GeoPosition(Double.POSITIVE_INFINITY, 0.0) }
    assertInvalid(message) { GeoPosition(0.0, Double.NEGATIVE_INFINITY) }
  }

  // Breaks if a local position refuses a point west or south of the origin.
  @Test
  fun aLocalPositionKeepsItsMetres() {
    val position = LocalPosition(-12.5, 40.0)

    assertEquals(-12.5, position.x, 0.0)
    assertEquals(40.0, position.y, 0.0)
  }

  // Breaks if NaN or an infinity gets into a local position.
  @Test
  fun aLocalPositionThatIsNotFiniteIsRefused() {
    val message = "x and y must be finite."
    assertInvalid(message) { LocalPosition(Double.NaN, 0.0) }
    assertInvalid(message) { LocalPosition(0.0, Double.NaN) }
    assertInvalid(message) { LocalPosition(Double.POSITIVE_INFINITY, 0.0) }
    assertInvalid(message) { LocalPosition(0.0, Double.NEGATIVE_INFINITY) }
  }
}
