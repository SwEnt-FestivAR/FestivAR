// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.PillStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnouncementPriorityUiTest {
  // Breaks if a priority gets the pill of another one, e.g. an urgent announcement in amber.
  @Test
  fun eachPriorityHasItsPillStyle() {
    assertEquals(PillStyle.ERROR, announcementPriorityPillStyle(AnnouncementPriority.URGENT))
    assertEquals(PillStyle.AMBER, announcementPriorityPillStyle(AnnouncementPriority.WARNING))
    assertEquals(PillStyle.MUTED, announcementPriorityPillStyle(AnnouncementPriority.INFO))
  }
}
