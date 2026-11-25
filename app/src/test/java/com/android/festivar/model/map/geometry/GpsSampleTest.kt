// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GpsSampleTest {

  private fun assertInvalid(expectedMessage: String, build: () -> Any) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  /** Breaks if a perfect reading (accuracy 0) is refused. */
  @Test
  fun aGpsSampleKeepsItsPositionAndAccuracy() {
    val position = GeoPosition(46.5191, 6.5668)

    assertEquals(position, GpsSample(position, 4.9).position)
    assertEquals(0.0, GpsSample(position, 0.0).accuracyMeters, 0.0)
  }

  /** Breaks if a negative or undefined accuracy is accepted, which would pass every filter. */
  @Test
  fun aGpsSampleWithABadAccuracyIsRefused() {
    val message = "The accuracy must be finite and not negative."
    val position = GeoPosition(46.5191, 6.5668)
    assertInvalid(message) { GpsSample(position, -0.1) }
    assertInvalid(message) { GpsSample(position, Double.NaN) }
    assertInvalid(message) { GpsSample(position, Double.POSITIVE_INFINITY) }
  }
}
