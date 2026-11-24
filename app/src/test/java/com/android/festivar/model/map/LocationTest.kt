// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map

import com.android.festivar.model.map.geometry.GeoPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationTest {

  private val vidy = GeoPosition(46.5137, 6.6012)

  /** Breaks if a location loses its name or its position. */
  @Test
  fun aLocationKeepsItsNameAndItsPosition() {
    val location = Location("Parc de Vidy", vidy)

    assertEquals("Parc de Vidy", location.name)
    assertEquals(vidy, location.position)
  }

  /** Breaks if a location without a name is accepted: a list would show an empty row for it. */
  @Test
  fun aBlankNameIsRefused() {
    for (name in listOf("", "   ")) {
      val exception = assertThrows(IllegalArgumentException::class.java) { Location(name, vidy) }
      assertEquals("The name of a location cannot be blank.", exception.message)
    }
  }
}
