// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioTest {

  private val tempFile = File.createTempFile("sample", ".m4a")

  @Test
  fun init_withValidFile_succeeds() {
    val audio =
        Audio(
            file = tempFile,
            duration = Duration.ofSeconds(5),
            url = "https://example.com/sample.m4a",
        )
    assertEquals(tempFile, audio.file)
    assertEquals(tempFile.name, audio.fileName)
    assertEquals(Duration.ofSeconds(5), audio.duration)
    assertEquals("https://example.com/sample.m4a", audio.url)
  }

  @Test
  fun init_defaultDurationAndUrl_areDefaulted() {
    val audio = Audio(file = tempFile)
    assertEquals(Duration.ZERO, audio.duration)
    assertEquals("", audio.url)
  }

  @Test
  fun init_withNegativeDuration_throwsIllegalArgumentException() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          Audio(file = tempFile, duration = Duration.ofMillis(-1))
        }
    assertEquals("The duration must be non-negative.", exception.message)
  }
}
