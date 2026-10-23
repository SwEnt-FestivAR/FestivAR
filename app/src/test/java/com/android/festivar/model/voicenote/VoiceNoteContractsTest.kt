// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.io.File
import java.io.IOException
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/** The data types of the voice note contracts: what they keep and what they refuse. */
class VoiceNoteContractsTest {
  private val file = File("note.m4a")

  // RecordedAudio

  @Test
  fun recordedAudio_keepsTheFileAndTheDuration() {
    val audio = RecordedAudio(file, Duration.ofSeconds(14))

    assertEquals(file, audio.file)
    assertEquals(Duration.ofSeconds(14), audio.duration)
  }

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

  // RecorderState

  @Test
  fun recorderState_recordingKeepsTheElapsedTime() {
    val state = RecorderState.Recording(Duration.ofSeconds(3))

    assertEquals(Duration.ofSeconds(3), state.elapsed)
    assertEquals(RecorderState.Recording(Duration.ofSeconds(3)), state)
    assertNotEquals(RecorderState.Recording(Duration.ofSeconds(4)), state)
  }

  @Test
  fun recorderState_idleIsNotRecording() {
    assertNotEquals(RecorderState.Idle, RecorderState.Recording(Duration.ZERO))
  }

  // VoiceRecorderException

  @Test
  fun voiceRecorderException_keepsTheMessageAndTheCause() {
    val cause = IOException("disk full")

    val exception = VoiceRecorderException("cannot record", cause)

    assertEquals("cannot record", exception.message)
    assertSame(cause, exception.cause)
  }

  @Test
  fun voiceRecorderException_hasNoCauseByDefault() {
    assertNull(VoiceRecorderException("too short").cause)
  }

  // PlayerState

  @Test
  fun playerState_playingAndFailedKeepTheUrlOfTheNote() {
    assertEquals("https://x/n1.m4a", PlayerState.Playing("https://x/n1.m4a").audioUrl)
    assertEquals("https://x/n1.m4a", PlayerState.Failed("https://x/n1.m4a").audioUrl)
  }

  @Test
  fun playerState_dependsOnTheNoteAndOnWhatHappens() {
    assertEquals(PlayerState.Playing("a"), PlayerState.Playing("a"))
    assertNotEquals(PlayerState.Playing("a"), PlayerState.Playing("b"))
    assertNotEquals(PlayerState.Playing("a"), PlayerState.Failed("a"))
    assertNotEquals(PlayerState.Idle, PlayerState.Playing("a"))
  }
}
