// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests of the line showing the days and the place of an event. */
@RunWith(AndroidJUnit4::class)
class EventDateAndLocationTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun lineOf(location: String): String {
    val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    val event =
        Event(
            eventId = "event",
            ownerId = "owner",
            title = "Event",
            startDate = start,
            endDate = start.plusDays(2),
            location = location,
        )
    var line = ""
    composeTestRule.setContent { line = formatEventDateAndLocation(event) }
    composeTestRule.waitForIdle()
    return line
  }

  /** Breaks if the place is not joined to the days with a middle dot on one line. */
  @Test
  fun joinsDaysAndLocation() {
    assertEquals("Fri 16 to Sun 18 Oct \u00b7 Parc de Vidy", lineOf("Parc de Vidy"))
  }

  /** Breaks if an event without a place keeps a dangling separator or a placeholder. */
  @Test
  fun emptyLocationLeavesOnlyTheDays() {
    assertEquals("Fri 16 to Sun 18 Oct", lineOf(""))
  }

  /** Breaks if a whitespace-only place is treated as a real place. */
  @Test
  fun whitespaceLocationLeavesOnlyTheDays() {
    assertEquals("Fri 16 to Sun 18 Oct", lineOf("   "))
  }
}
