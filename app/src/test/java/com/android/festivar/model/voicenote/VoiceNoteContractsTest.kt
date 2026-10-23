// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.io.File
import java.io.IOException
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/** The rules of the voice note contracts: what their data types refuse. */
class VoiceNoteContractsTest {
  private val file = File("note.m4a")

  @Test
  fun recordedAudio_acceptsAZeroDuration() {
    assertEquals(Duration.ZERO, RecordedAudio(file, Duration.ZERO).duration)
  }

  @Test
  fun recordedAudio_refusesANegativeDuration() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          RecordedAudio(file, Duration.ofMillis(-1))
        }

    assertEquals("The duration cannot be negative.", exception.message)
  }

  @Test
  fun voiceRecorderException_keepsTheMessageAndTheCause() {
    val cause = IOException("disk full")

    val exception = VoiceRecorderException("cannot record", cause)

    assertEquals("cannot record", exception.message)
    assertSame(cause, exception.cause)
  }
}
