// Co-authored-by: Gemini
package com.android.festivar.model.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.storage
import java.io.File
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioStorageFirebaseTest {

  private lateinit var repository: AudioStorageFirebase
  private lateinit var tempFile: File
  private lateinit var audio: Audio

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    runBlocking { Firebase.auth.signInAnonymously().await() }
    tempFile = File.createTempFile("test_audio_", ".m4a")
    tempFile.writeText("test audio content")
    audio = Audio(file = tempFile, duration = Duration.ofSeconds(3))
    repository = AudioStorageFirebase(Firebase.storage.reference)
  }

  @After
  fun tearDown() {
    if (::tempFile.isInitialized && tempFile.exists()) {
      tempFile.delete()
    }
    runBlocking { Firebase.auth.signOut() }
  }

  @Test
  fun getNewUid_returnsUniqueIds() {
    val id1 = repository.getNewUid()
    val id2 = repository.getNewUid()

    assertTrue(id1.isNotBlank())
    assertTrue(id2.isNotBlank())
    assertNotEquals(id1, id2)
  }

  @Test
  fun uploadAndDelete_succeedsAgainstStorageEmulator() = runBlocking {
    val taskId = "testTask_${System.currentTimeMillis()}"
    val downloadUrl = repository.upload(audio, taskId)

    assertTrue(downloadUrl.isNotBlank())

    repository.delete(downloadUrl)

    // Deleting again (already deleted object) is non-fatal
    repository.delete(downloadUrl)
  }
}
