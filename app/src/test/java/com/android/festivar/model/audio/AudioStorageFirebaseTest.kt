// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioStorageFirebaseTest {

  private lateinit var storageRef: StorageReference
  private lateinit var firebaseStorage: FirebaseStorage
  private lateinit var audioRef: StorageReference
  private lateinit var voiceNoteRef: StorageReference
  private lateinit var taskRef: StorageReference
  private lateinit var fileRef: StorageReference
  private lateinit var repository: AudioStorageFirebase
  private val tempFile = File.createTempFile("test_audio", ".m4a")
  private val audio = Audio(tempFile, Duration.ofSeconds(5))

  @Before
  fun setUp() {
    storageRef = mockk()
    firebaseStorage = mockk()
    audioRef = mockk()
    voiceNoteRef = mockk()
    taskRef = mockk()
    fileRef = mockk()

    every { storageRef.storage } returns firebaseStorage
    every { storageRef.child("audio") } returns audioRef
    every { audioRef.child("voice-note") } returns voiceNoteRef
    every { voiceNoteRef.child("task1") } returns taskRef
    every { voiceNoteRef.child(any()) } returns taskRef
    every { taskRef.child(any()) } returns fileRef
    every { fileRef.child(any()) } returns fileRef
    every { firebaseStorage.getReferenceFromUrl(any()) } returns fileRef
    every { fileRef.putFile(any()) } returns mockk(relaxed = true)
    every { fileRef.putFile(any(), any()) } returns mockk(relaxed = true)

    repository = AudioStorageFirebase(storageRef)
  }

  @Test
  fun getNewUid_generatesUniqueId() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()
    assertNotNull(uid1)
    assertNotNull(uid2)
    assertTrue(uid1.isNotEmpty())
    assertTrue(uid2.isNotEmpty())
    assertNotEquals(uid1, uid2)
  }

  @Test
  fun upload_success() = runTest {
    val expectedUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1%2F12345.m4a?alt=media"
    val uploadTask: UploadTask = mockk()
    every { uploadTask.isComplete } returns true
    every { uploadTask.isCanceled } returns false
    every { uploadTask.isSuccessful } returns true
    every { uploadTask.result } returns mockk()
    every { uploadTask.exception } returns null

    every { fileRef.putFile(audio.uri, any()) } returns uploadTask
    every { fileRef.downloadUrl } returns Tasks.forResult(Uri.parse(expectedUrl))

    val url = repository.upload(audio, "task1")

    assertEquals(expectedUrl, url)
    verify { voiceNoteRef.child("task1") }
    verify { fileRef.putFile(audio.uri, any()) }
  }

  @Test
  fun upload_failure_throwsIOException() {
    val uploadTask: UploadTask = mockk()
    every { uploadTask.isComplete } returns true
    every { uploadTask.isCanceled } returns false
    every { uploadTask.isSuccessful } returns false
    every { uploadTask.exception } returns RuntimeException("Network error")

    every { fileRef.putFile(any(), any()) } returns uploadTask

    assertThrows(IOException::class.java) { runBlocking { repository.upload(audio, "task1") } }
  }

  @Test
  fun upload_blankUrlReturned_throwsIOException() {
    val uploadTask: UploadTask = mockk()
    every { uploadTask.isComplete } returns true
    every { uploadTask.isCanceled } returns false
    every { uploadTask.isSuccessful } returns true
    every { uploadTask.result } returns mockk()
    every { uploadTask.exception } returns null

    every { fileRef.putFile(any(), any()) } returns uploadTask
    every { fileRef.downloadUrl } returns Tasks.forResult(Uri.parse(""))

    val exception =
        assertThrows(IOException::class.java) { runBlocking { repository.upload(audio, "task1") } }
    assertEquals("Audio upload did not return a valid download URL.", exception.message)
  }

  @Test
  fun upload_cancellation_propagatesCancellationException() {
    val uploadTask: UploadTask = mockk()
    every { uploadTask.isComplete } returns true
    every { uploadTask.isCanceled } returns true
    every { uploadTask.isSuccessful } returns false
    every { uploadTask.exception } returns CancellationException("Upload cancelled")

    every { fileRef.putFile(any(), any()) } returns uploadTask

    assertThrows(CancellationException::class.java) {
      runBlocking { repository.upload(audio, "task1") }
    }
  }

  @Test
  fun delete_success() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1%2F12345.m4a?alt=media"
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { firebaseStorage.getReferenceFromUrl(audioUrl) }
    verify { fileRef.delete() }
  }

  @Test
  fun delete_emulatorUrl_success() = runTest {
    val emulatorUrl =
        "http://10.0.2.2:9199/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1%2F12345.m4a?alt=media"
    every { firebaseStorage.getReferenceFromUrl(emulatorUrl) } throws
        IllegalArgumentException("Invalid URL")
    every { firebaseStorage.reference } returns storageRef
    every { storageRef.child("audio/voice-note/task1/12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(emulatorUrl)

    verify { firebaseStorage.getReferenceFromUrl(emulatorUrl) }
    verify { storageRef.child("audio/voice-note/task1/12345.m4a") }
    verify { fileRef.delete() }
  }

  @Test
  fun delete_invalidUrl_throwsIOException() = runTest {
    val invalidUrl = "https://example.com/song.m4a"
    every { firebaseStorage.getReferenceFromUrl(invalidUrl) } throws
        IllegalArgumentException("Invalid URL")

    assertThrows(IOException::class.java) { runBlocking { repository.delete(invalidUrl) } }
  }

  @Test
  fun delete_blankUrl_doesNothing() = runTest {
    repository.delete("")
    repository.delete("   ")

    verify(exactly = 0) { firebaseStorage.getReferenceFromUrl(any()) }
  }

  @Test
  fun delete_notFound_doesNotThrow() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1%2F12345.m4a?alt=media"
    val notFoundException = mockk<StorageException>()
    every { notFoundException.errorCode } returns StorageException.ERROR_OBJECT_NOT_FOUND
    every { fileRef.delete() } returns Tasks.forException(notFoundException)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_failure_throwsIOException() {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1%2F12345.m4a?alt=media"
    every { fileRef.delete() } returns Tasks.forException(RuntimeException("Error"))

    assertThrows(IOException::class.java) { runBlocking { repository.delete(audioUrl) } }
  }

  @Test
  fun delete_cancellation_propagatesCancellationException() {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1%2F12345.m4a?alt=media"
    every { fileRef.delete() } returns Tasks.forException(CancellationException("Cancelled"))

    assertThrows(CancellationException::class.java) { runBlocking { repository.delete(audioUrl) } }
  }
}
