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
 * Formats an event date range, omitting repeated month and year information where possible.
 *
 * For ranges spanning different years, the year is included for both dates.
 */
fun formatEventDate(event: Event): String {
  val start = event.startDate.toLocalDate()
  val end = event.endDate.toLocalDate()
  return when {
    start == end -> formatDate(start)
    start.year == end.year && start.month == end.month ->
        "${formatDay(start)} to ${formatDate(end)}"
    start.year == end.year -> "${formatDate(start)} to ${formatDate(end)}"
    else -> "${formatDateWithYear(start)} to ${formatDateWithYear(end)}"
  }
}

/**
 * Formats the days and the location of an [event], e.g. "Fri 16 to Sun 18 Oct · Parc de Vidy". The
 * days come from [formatEventDate]; a blank location leaves the days alone.
 */
@Composable
fun formatEventDates(event: Event): String {
  val dates = formatEventDate(event)
  return if (event.location.isBlank()) dates
  else stringResource(R.string.event_dates_location, dates, event.location)
}

/** Formats a time the way the forms show it, e.g. `09:30`. */
fun formatTime(time: LocalTime): String = time.format(formatter("HH:mm"))
