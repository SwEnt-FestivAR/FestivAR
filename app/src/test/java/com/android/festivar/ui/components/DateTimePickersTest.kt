// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)
    val range =
        DateTimeRange(
            startDate = startDate,
            startTime = LocalTime.of(9, 0),
            endDate = endDate,
            endTime = LocalTime.of(17, 0),
        )

    assertNull(endDateError(range))
  }

  /** Red if a one-day range with its end time after its start time reports an error. */
  @Test
  fun endDateError_oneDayRangeWithEndAfterStart_returnsNoError() {
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

  /**
   * Red if a one-day range with its end time before its start time reports no end-before-start
   * error.
   */
  @Test
  fun endDateError_oneDayRangeWithEndBeforeStart_returnsEndBeforeStart() {
    val date = LocalDate.now().plusDays(1)
    val range =
        DateTimeRange(
            startDate = date,
            startTime = LocalTime.of(9, 0),
            endDate = date,
            endTime = LocalTime.of(7, 0),
        )

    assertEquals(FieldError.END_BEFORE_START, endDateError(range))
  }

  /** Red if a range with equal start and end reports no end-before-start error. */
  @Test
  fun endDateError_equalStartAndEnd_returnsEndBeforeStart() {
    val date = LocalDate.now().plusDays(1)
    val range =
        DateTimeRange(
            startDate = date,
            startTime = LocalTime.of(9, 0),
            endDate = date,
            endTime = LocalTime.of(9, 0),
        )

    assertEquals(FieldError.END_BEFORE_START, endDateError(range))
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
    val now = ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0, ZoneId.systemDefault())
    val date = now.toLocalDate().minusDays(1)
    val range =
        DateTimeRange(
            startDate = date,
            startTime = LocalTime.of(9, 0),
            endDate = date,
            endTime = LocalTime.of(17, 0),
        )

    assertEquals(FieldError.OUTDATED, endDateError(range, now = now))
  }

  /** Red if a past start date is rejected when the end date is still in the future. */
  @Test
  fun endDateError_pastStartAndFutureEnd_returnsNoError() {
    val now = ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0, ZoneId.systemDefault())
    val range =
        DateTimeRange(
            startDate = now.toLocalDate().minusDays(1),
            endDate = now.toLocalDate().plusDays(1),
        )

    assertNull(endDateError(range, now = now))
  }

  /** Breaks if the prefilled range is not today from the next full hour, for two hours. */
  @Test
  fun prefilled_startsAtTheNextFullHourAndLastsTwoHours() {
    val range = DateTimeRange.prefilled(LocalDateTime.of(2026, 10, 17, 14, 20))

    assertEquals(
        DateTimeRange(
            LocalDate.of(2026, 10, 17),
            LocalTime.of(15, 0),
            LocalDate.of(2026, 10, 17),
            LocalTime.of(17, 0),
        ),
        range,
    )
  }

  /** Breaks if exactly on the hour counts as the next full hour, or the end ignores midnight. */
  @Test
  fun prefilled_onTheHourTakesTheFollowingHourAndMayEndTomorrow() {
    val range = DateTimeRange.prefilled(LocalDateTime.of(2026, 10, 17, 22, 0))

    assertEquals(LocalTime.of(23, 0), range.startTime)
    assertEquals(LocalDate.of(2026, 10, 17), range.startDate)
    assertEquals(LocalTime.of(1, 0), range.endTime)
    assertEquals(LocalDate.of(2026, 10, 18), range.endDate)
  }

  /** Breaks if moving the start time does not move the end time by the same amount. */
  @Test
  fun withStartTime_movesTheEndToKeepTheDuration() {
    val day = LocalDate.of(2026, 10, 17)
    val range = DateTimeRange(day, LocalTime.of(18, 0), day, LocalTime.of(20, 0))

    val moved = range.withStartTime(LocalTime.of(10, 30))

    assertEquals(DateTimeRange(day, LocalTime.of(10, 30), day, LocalTime.of(12, 30)), moved)
  }

  /** Breaks if the duration of a range over several days is not kept across the days. */
  @Test
  fun withStartTime_keepsTheDurationOfARangeOverSeveralDays() {
    val range =
        DateTimeRange(
            LocalDate.of(2026, 11, 20),
            LocalTime.of(18, 0),
            LocalDate.of(2026, 11, 22),
            LocalTime.of(12, 0),
        )

    val moved = range.withStartTime(LocalTime.of(20, 0))

    assertEquals(LocalDate.of(2026, 11, 22), moved.endDate)
    assertEquals(LocalTime.of(14, 0), moved.endTime)
  }

  /** Breaks if an end pushed past midnight stays on the start day. */
  @Test
  fun withStartTime_movesTheEndToTheNextDayPastMidnight() {
    val day = LocalDate.of(2026, 10, 17)
    val range = DateTimeRange(day, LocalTime.of(18, 0), day, LocalTime.of(20, 0))

    val moved = range.withStartTime(LocalTime.of(23, 0))

    assertEquals(day.plusDays(1), moved.endDate)
    assertEquals(LocalTime.of(1, 0), moved.endTime)
  }

  /**
   * Breaks if a range already ending before its start drags its end along instead of keeping it.
   */
  @Test
  fun withStartTime_keepsTheEndOfARangeThatEndsBeforeItsStart() {
    val day = LocalDate.of(2026, 10, 17)
    val range = DateTimeRange(day, LocalTime.of(20, 0), day, LocalTime.of(18, 0))

    val moved = range.withStartTime(LocalTime.of(17, 0))

    assertEquals(DateTimeRange(day, LocalTime.of(17, 0), day, LocalTime.of(18, 0)), moved)
  }

  /** Breaks if a range without an end time gets one invented when the start time is set. */
  @Test
  fun withStartTime_onAnIncompleteRangeOnlySetsTheStartTime() {
    val day = LocalDate.of(2026, 10, 17)

    assertEquals(
        DateTimeRange(day, LocalTime.NOON),
        DateTimeRange(day).withStartTime(LocalTime.NOON),
    )
    assertEquals(
        DateTimeRange(startTime = LocalTime.NOON),
        DateTimeRange().withStartTime(LocalTime.NOON),
    )
  }

  /** Breaks if a one-day range whose end is not after its start is accepted. */
  @Test
  fun timesError_sameDayEndNotAfterStart_returnsEndBeforeStart() {
    val day = LocalDate.of(2026, 10, 17)

    listOf(LocalTime.of(18, 0), LocalTime.of(17, 0)).forEach { end ->
      val range = DateTimeRange(day, LocalTime.of(18, 0), day, end)
      assertEquals(FieldError.END_BEFORE_START, range.timesError())
    }
  }

  /** Breaks if a later end, or an end on a later day, is reported as an error. */
  @Test
  fun timesError_laterEndOrLaterDay_returnsNoError() {
    val day = LocalDate.of(2026, 10, 17)

    assertNull(
        DateTimeRange(day, LocalTime.of(18, 0), day, LocalTime.of(18, 1)).timesError(),
    )
    assertNull(
        DateTimeRange(day, LocalTime.of(20, 0), day.plusDays(1), LocalTime.of(2, 0)).timesError(),
    )
  }

  /** Breaks if a range that is not filled in yet reports an error. */
  @Test
  fun timesError_incompleteRange_returnsNoError() {
    val day = LocalDate.of(2026, 10, 17)

    assertNull(DateTimeRange().timesError())
    assertNull(DateTimeRange(day, LocalTime.NOON, day).timesError())
  }
}
