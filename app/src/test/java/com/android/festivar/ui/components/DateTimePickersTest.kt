// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot App
package com.android.festivar.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateTimePickersTest {
  private val zone = ZoneId.of("UTC")
  private val now = ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0, zone)

  // Converting a date to picker milliseconds and back gives the same date, leap day included.
  @Test
  fun pickerMillis_roundTripsTheSameDate() {
    listOf(LocalDate.of(2026, 10, 17), LocalDate.of(2028, 2, 29), LocalDate.of(1999, 12, 31))
        .forEach { assertEquals(it, it.toPickerMillis().toPickerDate()) }
  }

  // The picker value of a date is midnight UTC, as the Material date picker expects.
  @Test
  fun pickerMillis_isUtcMidnight() {
    assertEquals(0L, LocalDate.of(1970, 1, 1).toPickerMillis())
    assertEquals(86_400_000L, LocalDate.of(1970, 1, 2).toPickerMillis())
  }

  // Any moment within a UTC day maps back to that same day, so the time of day is ignored.
  @Test
  fun pickerDate_ignoresTimeOfDayWithinTheUtcDay() {
    val lateOnTheDay = LocalDate.of(2026, 10, 17).toPickerMillis() + 86_399_999L
    assertEquals(LocalDate.of(2026, 10, 17), lateOnTheDay.toPickerDate())
  }

  @Test
  fun startDateErrorReportsOnlyTimeWithoutDate() {
    assertNull(startDateError(null, null))
    assertNull(startDateError(LocalDate.now(), null))
    assertEquals(FieldError.MISSING_DATE, startDateError(null, LocalTime.NOON))
  }

  @Test
  fun endDateErrorReportsMissingDateForEndTimeWithoutDate() {
    assertNull(endDateError(DateTimeRange(), zone, now))
    assertEquals(
        FieldError.MISSING_DATE,
        endDateError(DateTimeRange(endTime = LocalTime.NOON), zone, now),
    )
  }

  @Test
  fun endDateErrorAcceptsFutureRange() {
    val date = LocalDate.of(2026, 10, 30)
    assertNull(
        endDateError(
            DateTimeRange(date, LocalTime.of(9, 0), date, LocalTime.of(17, 0)),
            zone,
            now,
        )
    )
  }

  @Test
  fun endDateErrorRejectsEqualOrBeforeRanges() {
    val date = LocalDate.of(2026, 10, 30)
    val equal = DateTimeRange(date, LocalTime.of(9, 0), date, LocalTime.of(9, 0))
    val before = DateTimeRange(date, LocalTime.of(17, 0), date, LocalTime.of(9, 0))

    assertEquals(FieldError.END_BEFORE_START, endDateError(equal, zone, now))
    assertEquals(FieldError.END_BEFORE_START, endDateError(before, zone, now))
  }

  @Test
  fun endDateErrorRejectsPastRangeEvenWithoutStartDate() {
    val past = LocalDate.of(2020, 1, 1)
    val range = DateTimeRange(endDate = past, endTime = LocalTime.NOON)

    assertEquals(FieldError.OUTDATED, endDateError(range, zone, now))
  }
}
