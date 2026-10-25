// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioRepositoryFirebaseTest {
  private lateinit var repository: AudioRepositoryFirebase
  private val testAudioName = "test_${UUID.randomUUID()}.mp3"

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    repository = AudioRepositoryFirebase(Firebase.storage.reference)
  }

  @Test
  fun getNewUid_generatesUniqueId() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()
    assertNotNull(uid1)
    assertNotNull(uid2)
    assertTrue(uid1.isNotEmpty())
  }

  @Test
  fun addAudioAndGetAudio_worksSuccessfully() = runBlocking {
    val tempFile = File.createTempFile("test_", ".mp3")
    tempFile.writeText("dummy audio content")
    val audio = Audio(testAudioName, tempFile.toUri())

    repository.addAudio(audio)
    assertNotNull(audio.url)
    assertTrue(audio.url.isNotEmpty())

    val retrieved = repository.getAudio(testAudioName)
    assertNotNull(retrieved)
    assertEquals(testAudioName, retrieved.fileName)

    repository.deleteAudio(testAudioName)
  }

  @Test
  fun addAudio_alreadyExistsThrowsIllegalArgumentException(): Unit = runBlocking {
    val tempFile = File.createTempFile("test_", ".mp3")
    tempFile.writeText("dummy audio content")
    val audio = Audio(testAudioName, tempFile.toUri())

    repository.addAudio(audio)

    try {
      assertThrows(IllegalArgumentException::class.java) {
        runBlocking { repository.addAudio(audio) }
      }
    } finally {
      repository.deleteAudio(testAudioName)
    }
  }

  @Test
  fun getAudio_notFoundThrowsNoSuchElementException() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getAudio("nonexistent_${UUID.randomUUID()}.mp3") }
    }
  }

  @Test
  fun deleteAudio_notFoundThrowsNoSuchElementException() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteAudio("nonexistent_${UUID.randomUUID()}.mp3") }
    }
  }

  @Test
  fun editAudio_mismatchedFileNameThrowsIllegalArgumentException() {
    val newValue = Audio("audio2.mp3", Uri.parse("file:///audio2.mp3"))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.editAudio("audio1.mp3", newValue) }
    }
  }

  @After
  fun tearDown() {
    runBlocking {
      try {
        repository.deleteAudio(testAudioName)
      } catch (_: Exception) {}
    }
  }
}
