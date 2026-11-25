// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPinTest {
  /** Breaks if the event pin stops being 56dp, the task pin 40dp, or the event pin the larger. */
  @Test
  fun pinSizes_followFigma_andEventIsLarger() {
    assertEquals(56f, MapPinKind.EVENT.size.value, 0f)
    assertEquals(40f, MapPinKind.TASK.size.value, 0f)
    assertTrue(MapPinKind.EVENT.size > MapPinKind.TASK.size)
  }

  /** Breaks if the two kinds share a test tag, so a test cannot tell the pins apart. */
  @Test
  fun testTags_differByKind() {
    assertNotEquals(MapPinKind.EVENT.testTag, MapPinKind.TASK.testTag)
  }
}
