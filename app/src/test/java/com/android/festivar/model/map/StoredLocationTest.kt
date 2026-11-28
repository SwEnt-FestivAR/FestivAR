package com.android.festivar.model.map

// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import com.android.festivar.model.map.geometry.GeoPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredLocationTest {
  private val epfl = Location("EPFL", GeoPosition(46.5191, 6.5668))

  /** Breaks if a document saved without a location, or with a null one, reads with a place. */
  @Test
  fun anAbsentLocationReadsAsNoLocation() {
    assertNull(readStoredLocation(null, "event-1"))
  }

  /** Breaks if a document saved before the coordinates (its location a text) stops loading. */
  @Test
  fun aTextLocationReadsAsNoLocation() {
    assertNull(readStoredLocation("EPFL", "event-1"))
    assertNull(readStoredLocation("", "event-1"))
  }

  /** Breaks if the stored map of a location does not give back the location that was saved. */
  @Test
  fun aStoredLocationReadsBackAsSaved() {
    assertEquals(epfl, readStoredLocation(epfl.toMap(), "event-1"))
  }

  /**
   * Breaks if a number, an empty map or a map missing its name or position is read as a place, or
   * hides which document is wrong, or loses the reason of the refusal.
   */
  @Test
  fun anInvalidLocationIsRefusedWithTheOwner() {
    val blankName = epfl.toMap() + ("name" to " ")
    val noName = epfl.toMap() - "name"
    val noPosition = epfl.toMap() - "position"

    for (stored in listOf(42, blankName, emptyMap<String, Any>(), noName, noPosition)) {
      val error =
          assertThrows(IllegalArgumentException::class.java) {
            readStoredLocation(stored, "event-1")
          }
      assertTrue(error.message.orEmpty().contains("event-1"))
      assertNotNull(error.cause)
    }
  }
}
