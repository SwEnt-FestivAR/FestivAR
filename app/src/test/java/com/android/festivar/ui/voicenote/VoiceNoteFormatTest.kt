// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceNoteFormatTest {
  @Test
  fun formatsSecondsWithTwoDigits() {
    assertEquals("0:14", formatVoiceDuration(Duration.ofSeconds(14)))
    assertEquals("0:03", formatVoiceDuration(Duration.ofSeconds(3)))
    assertEquals("0:00", formatVoiceDuration(Duration.ZERO))
  }

  @Test
  fun formatsMinutes() {
    assertEquals("1:05", formatVoiceDuration(Duration.ofSeconds(65)))
    assertEquals("12:00", formatVoiceDuration(Duration.ofMinutes(12)))
  }

  @Test
  fun keepsCountingMinutesPastAnHour() {
    assertEquals("61:01", formatVoiceDuration(Duration.ofSeconds(61 * 60 + 1)))
  }

  @Test
  fun roundsSecondsDown() {
    assertEquals("0:01", formatVoiceDuration(Duration.ofMillis(1999)))
  }

  @Test
  fun negativeCountsAsZero() {
    assertEquals("0:00", formatVoiceDuration(Duration.ofSeconds(-5)))
  }
}
