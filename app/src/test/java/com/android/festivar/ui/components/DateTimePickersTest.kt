// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateTimePickersTest {
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

  /** Red if the initially empty start date/time fields report an error. */
  @Test
  fun startDateError_initialState_returnsNoError() {
    assertNull(startDateError(startDate = null, startTime = null))
  }

  /** Red if a start date without a start time incorrectly reports an error. */
  @Test
  fun startDateError_dateWithoutTime_returnsNoError() {
    assertNull(startDateError(startDate = LocalDate.now(), startTime = null))
  }

  /** Red if a start time without a start date does not report a missing-date error. */
  @Test
  fun startDateError_timeWithoutDate_returnsMissingDate() {
    assertEquals(
        FieldError.MISSING_DATE,
        startDateError(startDate = null, startTime = LocalTime.NOON),
    )
  }

  /** Red if the initially empty end date/time fields report an error. */
  @Test
  fun endDateError_initialState_returnsNoError() {
    assertNull(endDateError(DateTimeRange(endDate = null, endTime = null)))
  }

  /** Red if an end date without an end time incorrectly reports an error. */
  @Test
  fun endDateError_dateWithoutTime_returnsNoError() {
    assertNull(endDateError(DateTimeRange(endDate = LocalDate.now().plusDays(1))))
  }

  /** Red if a valid end date/time range incorrectly reports an error. */
  @Test
  fun endDateError_validRangeWithDatesAndTimes_returnsNoError() {
    val date = LocalDate.now().plusDays(1)
    val range =
        DateTimeRange(
            startDate = date,
            startTime = LocalTime.of(9, 0),
            endDate = date,
            endTime = LocalTime.of(17, 0),
        )

    assertNull(endDateError(range))
  }

  /** Red if a valid date-only range incorrectly reports an error. */
  @Test
  fun endDateError_validRangeWithDatesOnly_returnsNoError() {
    val date = LocalDate.now().plusDays(1)

    assertNull(endDateError(DateTimeRange(startDate = date, endDate = date)))
  }

  /** Red if an end time without an end date does not report a missing-date error. */
  @Test
  fun endDateError_timeWithoutDate_returnsMissingDate() {
    assertEquals(
        FieldError.MISSING_DATE,
        endDateError(DateTimeRange(endTime = LocalTime.NOON)),
    )
  }

  /** Red if an end before start does not report an end-before-start error. */
  @Test
  fun endDateError_endBeforeStart_returnsEndBeforeStart() {
    val date = LocalDate.now().plusDays(1)
    val range =
        DateTimeRange(
            startDate = date,
            startTime = LocalTime.of(17, 0),
            endDate = date,
            endTime = LocalTime.of(9, 0),
        )

    assertEquals(FieldError.END_BEFORE_START, endDateError(range))
  }

  /** Red if a range in the past does not report an outdated error. */
  @Test
  fun endDateError_pastRange_returnsOutdated() {
    val date = LocalDate.now().minusDays(1)
    val range =
        DateTimeRange(
            startDate = date,
            startTime = LocalTime.of(9, 0),
            endDate = date,
            endTime = LocalTime.of(17, 0),
        )

    assertEquals(FieldError.OUTDATED, endDateError(range))
  }
}
