// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoPositionMapTest {

  private val epfl = GeoPosition(46.5191, 6.5668)

  /** Fails unless reading [stored] throws an [IllegalArgumentException] saying [message]. */
  private fun assertRefused(message: String, stored: Map<String, Any?>) {
    val exception =
        assertThrows(IllegalArgumentException::class.java) { GeoPosition.fromMap(stored) }
    assertEquals(message, exception.message)
  }

  /**
   * Breaks if the stored names change: documents already saved would stop loading. They are written
   * as text here on purpose.
   */
  @Test
  fun toMap_usesLatitudeAndLongitude() {
    assertEquals(mapOf("latitude" to 46.5191, "longitude" to 6.5668), epfl.toMap())
  }

  /** Breaks if a position is not read back as it was stored, so a saved place moves. */
  @Test
  fun fromMap_readsWhatToMapStored() {
    assertEquals(epfl, GeoPosition.fromMap(epfl.toMap()))
  }

  /** Breaks if a whole number, which Firestore may return as a Long, is refused. */
  @Test
  fun fromMap_readsWholeNumbers() {
    val stored = mapOf("latitude" to 46L, "longitude" to 6L)

    assertEquals(GeoPosition(46.0, 6.0), GeoPosition.fromMap(stored))
  }

  /** Breaks if a position without a latitude is read as a position. */
  @Test
  fun fromMap_refusesAMissingLatitude() {
    assertRefused("The latitude must be a number.", mapOf("longitude" to 6.5))
  }

  /** Breaks if a position without a longitude is read as a position. */
  @Test
  fun fromMap_refusesAMissingLongitude() {
    assertRefused("The longitude must be a number.", mapOf("latitude" to 46.5))
  }

  /** Breaks if a coordinate stored as text is read as a number. */
  @Test
  fun fromMap_refusesATextCoordinate() {
    assertRefused(
        "The latitude must be a number.",
        mapOf("latitude" to "46.5", "longitude" to 6.5),
    )
  }

  /** Breaks if the rules of [GeoPosition] are skipped when a position is read. */
  @Test
  fun fromMap_refusesACoordinateOutOfRange() {
    val tooFarNorth = mapOf("latitude" to 91.0, "longitude" to 0.0)
    val tooFarEast = mapOf("latitude" to 0.0, "longitude" to 181.0)

    assertThrows(IllegalArgumentException::class.java) { GeoPosition.fromMap(tooFarNorth) }
    assertThrows(IllegalArgumentException::class.java) { GeoPosition.fromMap(tooFarEast) }
  }

  /** Breaks if a stored NaN or infinity is read as a position. */
  @Test
  fun fromMap_refusesACoordinateThatIsNotFinite() {
    val notANumber = mapOf("latitude" to Double.NaN, "longitude" to 0.0)
    val infinite = mapOf("latitude" to 0.0, "longitude" to Double.POSITIVE_INFINITY)

    assertThrows(IllegalArgumentException::class.java) { GeoPosition.fromMap(notANumber) }
    assertThrows(IllegalArgumentException::class.java) { GeoPosition.fromMap(infinite) }
  }
}
