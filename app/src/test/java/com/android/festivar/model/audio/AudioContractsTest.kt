// Co-authored-by: Gemini
package com.android.festivar.model.audio

import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioContractsTest {

  @Test
  fun audio_init_nonPositiveDuration_throwsIllegalArgumentException() {
    val tempFile = File.createTempFile("test", ".m4a")
    assertThrows(IllegalArgumentException::class.java) {
      Audio(file = tempFile, duration = Duration.ZERO)
    }
    assertThrows(IllegalArgumentException::class.java) {
      Audio(file = tempFile, duration = Duration.ofSeconds(-1))
    }
    tempFile.delete()
  }

  @Test
  fun voiceRecorderException_createsCorrectly() {
    val cause = RuntimeException("Cause")
    val exception = VoiceRecorderException("Error message", cause)
    assertEquals("Error message", exception.message)
    assertEquals(cause, exception.cause)

    val exceptionNoCause = VoiceRecorderException("Error message")
    assertEquals("Error message", exceptionNoCause.message)
  }

  @Test
  fun recorderState_subtypesWork() {
    val idle = RecorderState.Idle
    val recording = RecorderState.Recording(Duration.ofSeconds(5))
    assertEquals(RecorderState.Idle, idle)
    assertEquals(5L, recording.elapsed.seconds)
    assertNotNull(recording.toString())
    assertNotNull(recording.hashCode())
  }

  @Test
  fun playerState_subtypesWork() {
    val idle = PlayerState.Idle
    val playing = PlayerState.Playing("url1")
    val failed = PlayerState.Failed("url2")

    assertEquals(PlayerState.Idle, idle)
    assertEquals("url1", playing.audioUrl)
    assertEquals("url2", failed.audioUrl)
    assertNotNull(playing.toString())
    assertNotNull(playing.hashCode())
    assertNotNull(failed.toString())
    assertNotNull(failed.hashCode())
  }
}
