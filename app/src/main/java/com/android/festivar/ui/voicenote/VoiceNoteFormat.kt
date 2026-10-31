// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import java.time.Duration
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
