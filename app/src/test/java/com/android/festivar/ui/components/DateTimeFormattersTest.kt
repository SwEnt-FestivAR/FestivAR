// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import com.android.festivar.model.event.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeFormattersTest {
  @Test
  fun formatDate_usesShortWeekdayDayAndMonth() {
    assertEquals("Sat 17 Oct", formatDate(LocalDate.of(2026, 10, 17)))
  }

  @Test
  fun formatDay_usesShortWeekdayAndDayWithoutMonth() {
    assertEquals("Fri 16", formatDay(LocalDate.of(2026, 10, 16)))
  }

  @Test
  fun formatEventDate_accountsForMonthAndYearBoundaries() {
    val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    val sameDayEnd = start.plusHours(8)
    val sameMonthEnd = start.plusDays(2)
    val sameYearDifferentMonthEnd = start.plusMonths(1)
    val differentYearEnd = start.plusYears(1)

    assertEquals(
        "Fri 16 Oct",
        formatEventDate(
            Event(
                eventId = "same-day",
                title = "Event",
                startDate = start,
                endDate = sameDayEnd,
            )
        ),
    )
    assertEquals(
        "Fri 16 to Sun 18 Oct",
        formatEventDate(
            Event(
                eventId = "same-month",
                title = "Event",
                startDate = start,
                endDate = sameMonthEnd,
            )
        ),
    )
    assertEquals(
        "Fri 16 Oct to Mon 16 Nov",
        formatEventDate(
            Event(
                eventId = "same-year-different-month",
                title = "Event",
                startDate = start,
                endDate = sameYearDifferentMonthEnd,
            )
        ),
    )
    assertEquals(
        "Fri 16 Oct 2026 to Sat 16 Oct 2027",
        formatEventDate(
            Event(
                eventId = "different-year",
                title = "Event",
                startDate = start,
                endDate = differentYearEnd,
            )
        ),
    )
  }

  @Test
  fun formatTime_usesTwentyFourHourClockWithPadding() {
    assertEquals("09:30", formatTime(LocalTime.of(9, 30)))
    assertEquals("00:05", formatTime(LocalTime.of(0, 5)))
    assertEquals("21:00", formatTime(LocalTime.of(21, 0)))
  }
}
