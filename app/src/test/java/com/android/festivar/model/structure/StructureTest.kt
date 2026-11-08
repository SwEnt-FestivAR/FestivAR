// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StructureTest {

  private val structure =
      Structure(
          structureId = "s1",
          eventId = "event1",
          kind = StructureKind.STAGE,
          label = "Main stage",
          x = -4.0,
          y = 6.0,
          width = 10.0,
          height = 5.0,
      )

  private fun assertInvalid(expectedMessage: String, build: () -> Structure) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  /** Breaks if a blank structureId stops being refused. */
  @Test
  fun blankStructureId_isRefused() {
    for (blank in listOf("", " ", "\t", "\n")) {
      assertInvalid("The structureId cannot be empty.") { structure.copy(structureId = blank) }
    }
  }

  /** Breaks if a blank eventId stops being refused. */
  @Test
  fun blankEventId_isRefused() {
    for (blank in listOf("", " ", "\t", "\n")) {
      assertInvalid("The eventId cannot be empty.") { structure.copy(eventId = blank) }
    }
  }

  /** Breaks if a blank label stops being refused. */
  @Test
  fun blankLabel_isRefused() {
    for (blank in listOf("", " ", "\t", "\n")) {
      assertInvalid("The label cannot be empty.") { structure.copy(label = blank) }
    }
  }

  /** Breaks if a zero or negative width is accepted. */
  @Test
  fun widthNotAboveZero_isRefused() {
    assertInvalid("The width must be above 0.") { structure.copy(width = 0.0) }
    assertInvalid("The width must be above 0.") { structure.copy(width = -1.5) }
  }

  /** Breaks if a zero or negative height is accepted. */
  @Test
  fun heightNotAboveZero_isRefused() {
    assertInvalid("The height must be above 0.") { structure.copy(height = 0.0) }
    assertInvalid("The height must be above 0.") { structure.copy(height = -1.5) }
  }

  /** Breaks if a tiny positive size is wrongly refused, or a negative origin is. */
  @Test
  fun smallPositiveSizeAndNegativeOrigin_areAccepted() {
    val s = structure.copy(x = -100.0, y = -0.5, width = 0.01, height = 0.01)
    assertEquals(0.01, s.width, 0.0)
  }

  /** Breaks if the centre is not the left/top edge plus half the size. */
  @Test
  fun center_isEdgePlusHalfTheSize() {
    assertEquals(1.0, structure.centerX, 0.0)
    assertEquals(8.5, structure.centerY, 0.0)
  }

  /** Breaks if copy loses a field it was not asked to change. */
  @Test
  fun copy_keepsTheOtherFields() {
    val moved = structure.copy(x = 3.0)

    assertEquals(3.0, moved.x, 0.0)
    assertEquals(structure.copy(x = 3.0), moved)
    assertEquals(structure.structureId, moved.structureId)
    assertEquals(structure.eventId, moved.eventId)
    assertEquals(structure.kind, moved.kind)
    assertEquals(structure.label, moved.label)
    assertEquals(structure.y, moved.y, 0.0)
    assertEquals(structure.width, moved.width, 0.0)
    assertEquals(structure.height, moved.height, 0.0)
  }
}
