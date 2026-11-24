// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.map

import com.android.festivar.model.map.geometry.GeoPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationMapTest {

  private val vidy = Location("Parc de Vidy", GeoPosition(46.5137, 6.6012))
  private val vidyPosition = mapOf("latitude" to 46.5137, "longitude" to 6.6012)

  /** Fails unless reading [stored] throws an [IllegalArgumentException] saying [message]. */
  private fun assertRefused(message: String, stored: Map<String, Any?>) {
    val exception = assertThrows(IllegalArgumentException::class.java) { Location.fromMap(stored) }
    assertEquals(message, exception.message)
  }

  /**
   * Breaks if the stored names change: documents already saved would stop loading. They are written
   * as text here on purpose.
   */
  @Test
  fun toMap_usesNameAndPosition() {
    assertEquals(mapOf("name" to "Parc de Vidy", "position" to vidyPosition), vidy.toMap())
  }

  /** Breaks if a location is not read back as it was stored, so a saved place changes. */
  @Test
  fun fromMap_readsWhatToMapStored() {
    assertEquals(vidy, Location.fromMap(vidy.toMap()))
  }

  /** Breaks if a location without a name is read as a location. */
  @Test
  fun fromMap_refusesAMissingName() {
    assertRefused("The name must be a text.", mapOf("position" to vidyPosition))
  }

  /** Breaks if a location with a blank name is read as a location. */
  @Test
  fun fromMap_refusesABlankName() {
    assertRefused(
        "The name of a location cannot be blank.",
        mapOf("name" to " ", "position" to vidyPosition),
    )
  }

  /** Breaks if a location without a position is read as a location. */
  @Test
  fun fromMap_refusesAMissingPosition() {
    assertRefused("The position must be a map.", mapOf("name" to "Parc de Vidy"))
  }

  /** Breaks if an invalid position is accepted because it is inside a location. */
  @Test
  fun fromMap_refusesAnInvalidPosition() {
    val stored = mapOf("name" to "Parc de Vidy", "position" to mapOf("latitude" to 46.5))

    assertRefused("The longitude must be a number.", stored)
  }

  /** Breaks if a document saved before the field existed, or with a null field, stops loading. */
  @Test
  fun fromStoredOrNull_givesNoLocationWhenAbsent() {
    assertNull(Location.fromStoredOrNull(null))
  }

  /** Breaks if the optional read does not return the stored location. */
  @Test
  fun fromStoredOrNull_readsAStoredLocation() {
    assertEquals(vidy, Location.fromStoredOrNull(vidy.toMap()))
  }

  /** Breaks if a field that is not a map, such as an old text, is hidden as no location. */
  @Test
  fun fromStoredOrNull_refusesAValueThatIsNotAMap() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) { Location.fromStoredOrNull("Vidy") }

    assertEquals("A stored location must be a map.", exception.message)
  }
}
