// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot App
package com.android.festivar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.festivar.R
import com.android.festivar.model.event.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatter(pattern: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())

/** Formats a date the way the forms show it, e.g. `Sat 17 Oct`. */
fun formatDate(date: LocalDate): String = date.format(formatter("EEE d MMM"))

/** Formats a date with its year, e.g. `Sat 17 Oct 2026`. */
fun formatDateWithYear(date: LocalDate): String = date.format(formatter("EEE d MMM yyyy"))

/** Formats only the day of the week of a date, e.g. `Sat`. */
fun formatWeekday(date: LocalDate): String = date.format(formatter("EEE"))

/** Formats a date without its month, e.g. `Sat 17`, for a range ending in the same month. */
fun formatDay(date: LocalDate): String = date.format(formatter("EEE d"))

/**
 * Formats the first and the last day of a range, omitting repeated month and year information where
 * possible, e.g. `Fri 16 to Sun 18 Oct`.
 *
 * For ranges spanning different years, the year is included for both dates.
 *
 * @param join The word between the two days in the language of the user, with a placeholder for
 *   each day, e.g. `%1$s to %2$s` (`R.string.date_range_join`).
 */
fun formatDateRange(start: LocalDate, end: LocalDate, join: String): String =
    when {
      start == end -> formatDate(start)
      start.year == end.year && start.month == end.month ->
          join.format(formatDay(start), formatDate(end))
      start.year == end.year -> join.format(formatDate(start), formatDate(end))
      else -> join.format(formatDateWithYear(start), formatDateWithYear(end))
    }

/** Formats the days of an [event] with [formatDateRange]. */
fun formatEventDate(event: Event, join: String): String =
    formatDateRange(event.startDate.toLocalDate(), event.endDate.toLocalDate(), join)

/**
 * Formats the days and the location of an [event] on one line, e.g. "Fri 16 to Sun 18 Oct · Parc de
 * Vidy". The days come from [formatEventDate]; a blank location leaves the days alone.
 */
@Composable
fun formatEventDateAndLocation(event: Event): String {
  val dates = formatEventDate(event, stringResource(R.string.date_range_join))
  return if (event.location.isBlank()) dates
  else stringResource(R.string.event_dates_location, dates, event.location)
}

/** Formats a time the way the forms show it, e.g. `09:30`. */
fun formatTime(time: LocalTime): String = time.format(formatter("HH:mm"))
