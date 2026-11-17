// Co-authored-by: Gemini
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
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

  /** Stores a task owned by the signed-in user, which is what the Storage rules look up. */
  private suspend fun createOwnedTask(): String {
    val taskId = "testTask_${System.nanoTime()}"
    val ownerId = Firebase.auth.currentUser!!.uid
    Firebase.firestore.collection("tasks").document(taskId).set(mapOf("ownerId" to ownerId)).await()
    return taskId
  }

  /** Signs in as a different user, keeping the emulator users apart. */
  private suspend fun signInAsAnotherUser() {
    Firebase.auth.signOut()
    Firebase.auth.signInAnonymously().await()
  }

  private val audioMetadata = StorageMetadata.Builder().setContentType("audio/m4a").build()

  /** Uploads a voice note of the signed-in user and returns its storage reference. */
  private suspend fun uploadVoiceNote(taskId: String) =
      Firebase.storage.getReferenceFromUrl(repository.upload(audio, taskId))

  /**
   * Breaks if the Storage rules let a signed-in user who does not own the task overwrite its voice
   * note.
   */
  @Test
  fun aUserWhoDoesNotOwnTheTaskCannotOverwriteItsVoiceNote() = runBlocking {
    val taskId = createOwnedTask()
    val ref = uploadVoiceNote(taskId)
    signInAsAnotherUser()

    val exception =
        assertThrows(StorageException::class.java) {
          runBlocking { ref.putBytes(ByteArray(4), audioMetadata).await() }
        }
    assertEquals(StorageException.ERROR_NOT_AUTHORIZED, exception.errorCode)
  }

  /**
   * Breaks if the Storage rules let a signed-in user who does not own the task delete its voice
   * note.
   */
  @Test
  fun aUserWhoDoesNotOwnTheTaskCannotDeleteItsVoiceNote() = runBlocking {
    val taskId = createOwnedTask()
    val ref = uploadVoiceNote(taskId)
    signInAsAnotherUser()

    val exception =
        assertThrows(StorageException::class.java) { runBlocking { ref.delete().await() } }
    assertEquals(StorageException.ERROR_NOT_AUTHORIZED, exception.errorCode)
  }

  /**
   * Breaks if the Storage rules let a signed-in user who does not own the task upload a voice note
   * for it.
   */
  @Test
  fun aUserWhoDoesNotOwnTheTaskCannotUploadItsVoiceNote() = runBlocking {
    val taskId = createOwnedTask()
    signInAsAnotherUser()

    val exception =
        assertThrows(IOException::class.java) { runBlocking { repository.upload(audio, taskId) } }
    assertEquals(
        StorageException.ERROR_NOT_AUTHORIZED,
        (exception.cause as StorageException).errorCode,
    )
  }

  /** Breaks if the Storage rules stop letting another signed-in user play the voice note. */
  @Test
  fun aUserWhoDoesNotOwnTheTaskCanStillReadItsVoiceNote() = runBlocking {
    val taskId = createOwnedTask()
    val ref = uploadVoiceNote(taskId)
    signInAsAnotherUser()

    assertTrue(ref.getBytes(1024).await().isNotEmpty())
  }

  /** Breaks if the Storage rules accept a voice note for a task that does not exist. */
  @Test
  fun aVoiceNoteCannotBeUploadedForATaskThatDoesNotExist() {
    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.upload(audio, "missingTask_${System.nanoTime()}") }
        }
    assertEquals(
        StorageException.ERROR_NOT_AUTHORIZED,
        (exception.cause as StorageException).errorCode,
    )
  }

  /** Breaks if the Storage rules stop letting the task owner replace their own voice note. */
  @Test
  fun theTaskOwnerCanOverwriteTheirVoiceNote() = runBlocking {
    val ref = uploadVoiceNote(createOwnedTask())

    ref.putBytes(ByteArray(4), audioMetadata).await()
  }

  /** Breaks if the Storage rules accept a file that is not audio, even from the task owner. */
  @Test
  fun theTaskOwnerCannotUploadAFileThatIsNotAudio() = runBlocking {
    val ref = Firebase.storage.reference.child("audio/voice-note/${createOwnedTask()}/note.txt")
    val metadata = StorageMetadata.Builder().setContentType("text/plain").build()

    val exception =
        assertThrows(StorageException::class.java) {
          runBlocking { ref.putBytes(ByteArray(4), metadata).await() }
        }
    assertEquals(StorageException.ERROR_NOT_AUTHORIZED, exception.errorCode)
  }

  /** Breaks if the Storage rules let a signed-out user play a voice note. */
  @Test
  fun aSignedOutUserCannotReadAVoiceNote() = runBlocking {
    val ref = uploadVoiceNote(createOwnedTask())
    Firebase.auth.signOut()

    val exception =
        assertThrows(StorageException::class.java) { runBlocking { ref.getBytes(1024).await() } }
    assertEquals(StorageException.ERROR_NOT_AUTHORIZED, exception.errorCode)
  }

  /** Breaks if the Storage rules stop letting the task owner upload and delete the voice note. */
  @Test
  fun uploadAndDelete_succeedsAgainstStorageEmulator() = runBlocking {
    val taskId = createOwnedTask()
    val downloadUrl = repository.upload(audio, taskId)

    // URL is only valid if upload exists
    assertTrue(downloadUrl.isNotBlank())

    repository.delete(downloadUrl)

    // Check that the file is deleted after the first delete
    val ref =
        try {
          Firebase.storage.getReferenceFromUrl(downloadUrl)
        } catch (e: IllegalArgumentException) {
          val pathPart = downloadUrl.substringAfter("/o/").substringBefore("?")
          val decodedPath = URLDecoder.decode(pathPart, "UTF-8")
          Firebase.storage.reference.child(decodedPath)
        }
    val exception =
        assertThrows(StorageException::class.java) { runBlocking { ref.metadata.await() } }
    assertEquals(StorageException.ERROR_OBJECT_NOT_FOUND, exception.errorCode)

    // Deleting again (already deleted object) is non-fatal
    repository.delete(downloadUrl)
  }
}
