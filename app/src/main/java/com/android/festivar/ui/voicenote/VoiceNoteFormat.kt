// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatDateWithYear
import com.android.festivar.ui.components.formatTime
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Utility for the screens and components of the voice note section: it turns the [Duration] of a
 * note (the length of the player, the timer of the recorder) into the text they display.
 *
 * The format is "minutes:seconds", like "0:14" or "12:05". From one hour on it becomes
 * "hours:minutes:seconds", like "1:01:01", so the minutes never go over 59. There is no maximum
 * length for a voice note, so every duration is accepted. Seconds are rounded down and a negative
 * duration counts as zero.
 */
internal fun formatVoiceDuration(duration: Duration): String {
  val totalSeconds = if (duration.isNegative) 0L else duration.seconds
  val hours = totalSeconds / 3600
  val minutes = totalSeconds % 3600 / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
  }
}

/**
 * The date and the time at which a note was recorded, like "Sat 17 Oct, 09:12". The date is
 * replaced by [todayLabel] ("Today, 09:12") for a note recorded on [today]. The year is only added
 * when it is not the year of [today], like "Sat 17 Oct 2025, 09:12", since a note of this year does
 * not need it.
 */
internal fun formatRecordedAt(
    createdAt: ZonedDateTime,
    today: LocalDate,
    todayLabel: String,
): String {
  val date = createdAt.toLocalDate()
  val day =
      when {
        date == today -> todayLabel
        date.year == today.year -> formatDate(date)
        else -> formatDateWithYear(date)
      }
  return "$day, ${formatTime(createdAt.toLocalTime())}"
}
