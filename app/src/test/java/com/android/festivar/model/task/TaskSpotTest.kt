// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskSpotTest {

  private val eitherMessage = "A spot is a structure or a point, not both and not neither."

  private fun assertInvalid(expectedMessage: String, build: () -> TaskSpot) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  // Breaks if a spot on a structure is refused.
  @Test
  fun aStructureAloneIsValid() {
    val spot = TaskSpot(structureId = "stage")

    assertEquals("stage", spot.structureId)
    assertNull(spot.x)
    assertNull(spot.y)
  }

  // Breaks if a free point is refused, or a negative coordinate with it.
  @Test
  fun aPointAloneIsValid() {
    val spot = TaskSpot(x = 2.5, y = -7.0)

    assertNull(spot.structureId)
    assertEquals(2.5, spot.x!!, 0.0)
    assertEquals(-7.0, spot.y!!, 0.0)
  }

  // Breaks if a spot can be on a structure and at a point at once.
  @Test
  fun aStructureAndAPointAreRefused() {
    assertInvalid(eitherMessage) { TaskSpot(structureId = "stage", x = 1.0, y = 2.0) }
  }

  // Breaks if a spot can name nothing.
  @Test
  fun neitherIsRefused() {
    assertInvalid(eitherMessage) { TaskSpot() }
  }

  // Breaks if a point with only one coordinate is accepted.
  @Test
  fun xWithoutYIsRefused() {
    assertInvalid("x and y go together.") { TaskSpot(structureId = "stage", x = 1.0) }
    assertInvalid(eitherMessage) { TaskSpot(x = 1.0) }
  }

  // Breaks if a blank structure id is accepted.
  @Test
  fun aBlankStructureIdIsRefused() {
    assertInvalid("The structureId cannot be blank.") { TaskSpot(structureId = " ") }
  }

  // Breaks if the factories build another spot than the constructor.
  @Test
  fun theFactoriesBuildTheMatchingSpots() {
    assertEquals(TaskSpot(structureId = "bar"), TaskSpot.onStructure("bar"))
    assertEquals(TaskSpot(x = 3.0, y = 4.0), TaskSpot.at(3.0, 4.0))
  }
}
