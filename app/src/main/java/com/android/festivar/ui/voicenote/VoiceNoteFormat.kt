// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import java.time.Duration
import java.util.Locale

/**
 * Formats a [duration] as minutes and seconds, like "0:14" or "12:05". Seconds are rounded down and
 * a negative duration counts as zero.
 */
internal fun formatVoiceDuration(duration: Duration): String {
  val totalSeconds = if (duration.isNegative) 0L else duration.seconds
  return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
