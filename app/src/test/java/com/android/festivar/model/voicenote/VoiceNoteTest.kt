// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VoiceNoteTest {

  private val created = ZonedDateTime.of(2026, 10, 17, 9, 30, 15, 0, ZoneId.of("UTC"))

  private fun note(
      id: String = "n1",
      audioUrl: String = "https://example.com/n1.m4a",
      duration: Duration = Duration.ofSeconds(14),
  ) = VoiceNote(id, audioUrl, duration, created)

  private fun assertInvalid(expectedMessage: String, build: () -> Any?) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  // ----------------------------------- creation ------------------------------------------- //

  @Test
  fun init_rejects_zeroDuration() {
    assertInvalid("The duration must be positive.") { note(duration = Duration.ZERO) }
  }

  @Test
  fun init_rejects_blankId() {
    listOf("", " ", "\t\n").forEach { assertInvalid("The id cannot be empty.") { note(id = it) } }
  }

  @Test
  fun init_rejects_blankAudioUrl() {
    listOf("", " ", "\t\n").forEach {
      assertInvalid("The audioUrl cannot be empty.") { note(audioUrl = it) }
    }
  }

  @Test
  fun init_rejects_negativeDuration() {
    assertInvalid("The duration must be positive.") { note(duration = Duration.ofMillis(-1)) }
  }
}
