// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.voicenote

import com.android.festivar.model.audio.Audio
import com.android.festivar.model.audio.AudioRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceNoteStorageFirebaseTest {

  private lateinit var audioRepository: AudioRepository
  private lateinit var storage: VoiceNoteStorageFirebase
  private val tempFile = File.createTempFile("test_audio", ".m4a")
  private val recordedAudio = RecordedAudio(tempFile, Duration.ofSeconds(5))

  @Before
  fun setUp() {
    audioRepository = mockk(relaxed = true)
    storage = VoiceNoteStorageFirebase(audioRepository)
  }

  @Test
  fun upload_success() = runTest {
    every { audioRepository.getNewUid() } returns "12345"
    coEvery { audioRepository.addAudio(any()) } answers
        {
          val audio = arg<Audio>(0)
          audio.url =
              "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Ftask1_12345.m4a?alt=media"
        }

    val url = storage.upload("task1", recordedAudio)

    assertEquals(
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Ftask1_12345.m4a?alt=media",
        url,
    )
    coVerify { audioRepository.addAudio(any()) }
  }

  @Test
  fun upload_failureThrowsIOException() = runTest {
    every { audioRepository.getNewUid() } returns "12345"
    coEvery { audioRepository.addAudio(any()) } throws RuntimeException("Network error")

    assertThrows(IOException::class.java) { runBlocking { storage.upload("task1", recordedAudio) } }
  }

  @Test
  fun upload_blankUrlReturnedThrowsIOException() = runTest {
    every { audioRepository.getNewUid() } returns "12345"
    coEvery { audioRepository.addAudio(any()) } answers
        {
          val audio = arg<Audio>(0)
          audio.url = ""
        }

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { storage.upload("task1", recordedAudio) }
        }
    assertEquals("Audio upload did not return a valid download URL.", exception.message)
  }

  @Test
  fun delete_success() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Ftask1_12345.m4a?alt=media"
    coEvery { audioRepository.deleteAudio("task1_12345.m4a") } returns Unit

    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("task1_12345.m4a") }
  }

  @Test
  fun delete_actualFirebaseUrl_deletesCorrectFileName() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/festivar-9a339.firebasestorage.app/o/audio%2Ftask1_12345.m4a?alt=media&token=7d4d9a9g9j2k4k"

    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("task1_12345.m4a") }
  }

  @Test
  fun delete_actualFirebaseUrlWithoutAudioPrefix_deletesCorrectFileName() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/festivar-9a339.firebasestorage.app/o/task1_12345.m4a?alt=media&token=7d4d9a9g9j2k4k"

    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("task1_12345.m4a") }
  }

  @Test
  fun delete_urlWithoutOParam_deletesCorrectFileName() = runTest {
    val audioUrl = "https://example.com/audio/task1_12345.m4a?alt=media"

    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("task1_12345.m4a") }
  }

  @Test
  fun delete_plainFileName_deletesCorrectFileName() = runTest {
    val audioUrl = "task1_12345.m4a"

    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("task1_12345.m4a") }
  }

  @Test
  fun delete_malformedUrlWithInvalidEncoding_fallsBackToLastSegment() = runTest {
    val audioUrl = "https://example.com/o/audio%2"

    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("audio%2") }
  }

  @Test
  fun delete_urlExtractingToBlankFileName_doesNotCallDeleteAudio() = runTest {
    val audioUrl = "https://example.com/o/audio%2F?alt=media"

    storage.delete(audioUrl)

    coVerify(exactly = 0) { audioRepository.deleteAudio(any()) }
  }

  @Test
  fun delete_blankUrlDoesNothing() = runTest {
    storage.delete("")
    storage.delete("   ")

    coVerify(exactly = 0) { audioRepository.deleteAudio(any()) }
  }

  @Test
  fun delete_notFoundDoesNotThrow() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Ftask1_12345.m4a?alt=media"
    coEvery { audioRepository.deleteAudio("task1_12345.m4a") } throws
        NoSuchElementException("Not found")

    // Deleting a non-existent file should not throw an error according to VoiceNoteStorage contract
    storage.delete(audioUrl)

    coVerify { audioRepository.deleteAudio("task1_12345.m4a") }
  }

  @Test
  fun delete_failureThrowsIOException() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Ftask1_12345.m4a?alt=media"
    coEvery { audioRepository.deleteAudio("task1_12345.m4a") } throws RuntimeException("Error")

    assertThrows(IOException::class.java) { runBlocking { storage.delete(audioUrl) } }
  }
}
