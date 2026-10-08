// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeFormattersTest {
  @Test
  fun formatDate_usesShortWeekdayDayAndMonth() {
    assertEquals("Sat 17 Oct", formatDate(LocalDate.of(2026, 10, 17)))
  }

  @Test
  fun formatTime_usesTwentyFourHourClockWithPadding() {
    assertEquals("09:30", formatTime(LocalTime.of(9, 30)))
    assertEquals("00:05", formatTime(LocalTime.of(0, 5)))
    assertEquals("21:00", formatTime(LocalTime.of(21, 0)))
  }
}
