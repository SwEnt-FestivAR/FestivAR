// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.map

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPinTest {
  /** Breaks if the event pin is not larger than the task pin, so an event hides among its tasks. */
  @Test
  fun eventPin_isLargerThanTaskPin() {
    assertTrue(MapPinKind.EVENT.size > MapPinKind.TASK.size)
  }

  /** Breaks if the two kinds share a test tag, so a test cannot tell the pins apart. */
  @Test
  fun testTags_differByKind() {
    assertNotEquals(MapPinKind.EVENT.testTag, MapPinKind.TASK.testTag)
  }
}
