// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot App
package com.android.festivar.ui.components

import com.android.festivar.model.event.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val dateWithYearFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
private val dayFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** Formats a date the way the forms show it, e.g. `Sat 17 Oct`. */
fun formatDate(date: LocalDate): String = date.format(dateFormatter)

/** Formats a date with its year, e.g. `Sat 17 Oct 2026`. */
fun formatDateWithYear(date: LocalDate): String = date.format(dateWithYearFormatter)

/** Formats a date without its month, e.g. `Sat 17`, for a range ending in the same month. */
fun formatDay(date: LocalDate): String = date.format(dayFormatter)

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

/** Formats a time the way the forms show it, e.g. `09:30`. */
fun formatTime(time: LocalTime): String = time.format(timeFormatter)
