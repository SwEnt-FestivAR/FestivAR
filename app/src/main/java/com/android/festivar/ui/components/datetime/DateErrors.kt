// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.datetime

import com.android.festivar.ui.components.FieldError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Validates the start date/time fields.
 *
 * A start time may only be selected after a start date; selecting a time without a date reports
 * [FieldError.MISSING_DATE]. All other combinations are valid for this field.
 */
fun startDateError(startDate: LocalDate?, startTime: LocalTime?): FieldError? =
    if (startTime != null && startDate == null) FieldError.MISSING_DATE else null

/**
 * Validates the end date/time fields against the start and the current time.
 *
 * A selected end time requires an end date. When both dates are present, missing start and end
 * times default to [START_OF_DAY] and [END_OF_DAY]. The end must be strictly after the start, and
 * it must not be in the past unless [allowPast], which the screens that edit an item that may have
 * ended turn on. A start in the past is allowed as long as the end is still in the future.
 */
fun endDateError(
    range: DateTimeRange,
    zone: ZoneId = ZoneId.systemDefault(),
    now: ZonedDateTime = ZonedDateTime.now(),
    endZone: ZoneId = zone,
    allowPast: Boolean = false,
): FieldError? {
  if (range.endTime != null && range.endDate == null) return FieldError.MISSING_DATE

  val start = range.startDate?.atTime(range.startTime ?: START_OF_DAY)?.atZone(zone)
  val end = range.endDate?.atTime(range.endTime ?: END_OF_DAY)?.atZone(endZone)

  return if (end != null) {
    when {
      start != null && !end.isAfter(start) -> FieldError.END_BEFORE_START
      !allowPast && end.isBefore(now) -> FieldError.OUTDATED
      else -> null
    }
  } else {
    null
  }
}
