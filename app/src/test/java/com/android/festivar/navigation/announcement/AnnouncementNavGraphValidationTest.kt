// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.announcement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AnnouncementNavGraphValidationTest {
  @Test
  fun requireEventId_withEventId_returnsEventId() {
    assertEquals("event-1", requireEventId("event-1"))
  }

  @Test
  fun requireEventId_withoutArguments_throws() {
    assertThrowsWithMessage("eventId argument is required") { requireEventId(null) }
  }

  @Test
  fun requireAnnouncementDetailsArguments_withArguments_returnsIds() {
    assertEquals(
        "event-1" to "announcement-1",
        requireAnnouncementDetailsArguments("event-1", "announcement-1"),
    )
  }

  @Test
  fun requireAnnouncementDetailsArguments_withoutAnnouncementId_throws() {
    assertThrowsWithMessage("announcementId argument is required") {
      requireAnnouncementDetailsArguments("event-1", null)
    }
  }

  @Test
  fun requireAnnouncementDetailsArguments_withBlankEventId_throws() {
    assertThrowsWithMessage("eventId must not be blank") {
      requireAnnouncementDetailsArguments(" ", "announcement-1")
    }
  }

  @Test
  fun requireAnnouncementDetailsArguments_withBlankAnnouncementId_throws() {
    assertThrowsWithMessage("announcementId must not be blank") {
      requireAnnouncementDetailsArguments("event-1", "\t")
    }
  }

  private fun assertThrowsWithMessage(expectedMessage: String, block: () -> Unit) {
    val exception = assertThrows(IllegalArgumentException::class.java, block)
    assertEquals(expectedMessage, exception.message)
  }
}
