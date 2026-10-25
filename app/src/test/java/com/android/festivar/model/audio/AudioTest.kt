// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioTest {

  private val sampleUri = "file:///tmp/sample.mp3".toUri()

  @Test
  fun init_withValidFileName_succeeds() {
    val audio =
        Audio(fileName = "sample.mp3", uri = sampleUri, url = "https://example.com/sample.mp3")
    assertEquals("sample.mp3", audio.fileName)
    assertEquals(sampleUri, audio.uri)
    assertEquals("https://example.com/sample.mp3", audio.url)
  }

  @Test
  fun init_defaultUrl_isEmpty() {
    val audio = Audio(fileName = "sample.mp3", uri = sampleUri)
    assertEquals("", audio.url)
  }

  @Test
  fun init_withEmptyFileName_throwsIllegalArgumentException() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) { Audio(fileName = "", uri = sampleUri) }
    assertEquals("The fileName cannot be empty.", exception.message)
  }

  @Test
  fun init_withBlankFileName_throwsIllegalArgumentException() {
    listOf(" ", "\t", "\n", "   ").forEach { blankName ->
      val exception =
          assertThrows(IllegalArgumentException::class.java) {
            Audio(fileName = blankName, uri = sampleUri)
          }
      assertEquals("The fileName cannot be empty.", exception.message)
    }
  }
}
