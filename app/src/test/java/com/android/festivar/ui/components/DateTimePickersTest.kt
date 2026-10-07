// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimePickersTest {
  @Test
  fun pickerMillis_roundTripsTheSameDate() {
    listOf(LocalDate.of(2026, 10, 17), LocalDate.of(2028, 2, 29), LocalDate.of(1999, 12, 31))
        .forEach { assertEquals(it, it.toPickerMillis().toPickerDate()) }
  }

  @Test
  fun pickerMillis_isUtcMidnight() {
    assertEquals(0L, LocalDate.of(1970, 1, 1).toPickerMillis())
    assertEquals(86_400_000L, LocalDate.of(1970, 1, 2).toPickerMillis())
  }

  @Test
  fun pickerDate_ignoresTimeOfDayWithinTheUtcDay() {
    val lateOnTheDay = LocalDate.of(2026, 10, 17).toPickerMillis() + 86_399_999L
    assertEquals(LocalDate.of(2026, 10, 17), lateOnTheDay.toPickerDate())
  }
}
