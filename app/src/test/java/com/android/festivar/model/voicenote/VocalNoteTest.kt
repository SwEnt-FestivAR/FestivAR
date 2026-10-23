// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VocalNoteTest {

  private val created = LocalDateTime.of(2026, 10, 17, 9, 30, 15)

  private fun note(
      id: String = "n1",
      audioUrl: String = "https://example.com/n1.m4a",
      duration: Duration = Duration.ofSeconds(14),
  ) = VocalNote(id, audioUrl, duration, created)

  private fun assertInvalid(expectedMessage: String, build: () -> Any?) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  // ----------------------------------- creation ------------------------------------------- //

  @Test
  fun init_accepts_zeroDuration() {
    assertEquals(Duration.ZERO, note(duration = Duration.ZERO).duration)
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
    assertInvalid("The duration cannot be negative.") { note(duration = Duration.ofMillis(-1)) }
  }
}
