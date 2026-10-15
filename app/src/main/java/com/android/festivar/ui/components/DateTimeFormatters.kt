// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val dayFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** Formats a date the way the forms show it, e.g. `Sat 17 Oct`. */
fun formatDate(date: LocalDate): String = date.format(dateFormatter)

/** Formats a date without its month, e.g. `Sat 17`, for a range ending in the same month. */
fun formatDay(date: LocalDate): String = date.format(dayFormatter)

/** Formats a time the way the forms show it, e.g. `09:30`. */
fun formatTime(time: LocalTime): String = time.format(timeFormatter)
