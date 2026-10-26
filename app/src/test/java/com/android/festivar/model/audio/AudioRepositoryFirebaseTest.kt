// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioRepositoryFirebaseTest {

  private lateinit var storageRef: StorageReference
  private lateinit var audioRef: StorageReference
  private lateinit var voiceNoteRef: StorageReference
  private lateinit var fileRef: StorageReference
  private lateinit var repository: AudioStorageFirebase
  private val tempFile = File.createTempFile("test_audio", ".m4a")
  private val audio = Audio(tempFile, Duration.ofSeconds(5))

  @Before
  fun setUp() {
    storageRef = mockk()
    audioRef = mockk()
    voiceNoteRef = mockk()
    fileRef = mockk()

    every { storageRef.child("audio") } returns audioRef
    every { audioRef.child("voice-note") } returns voiceNoteRef
    every { voiceNoteRef.child(any()) } returns fileRef
    every { fileRef.child(any()) } returns fileRef

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
  }

  @Test
  fun upload_success() = runTest {
    val expectedUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2F12345.m4a?alt=media"
    val uploadTask: UploadTask = mockk()
    every { uploadTask.isComplete } returns true
    every { uploadTask.isCanceled } returns false
    every { uploadTask.isSuccessful } returns true
    every { uploadTask.result } returns mockk()
    every { uploadTask.exception } returns null

    every { fileRef.putFile(any()) } returns uploadTask
    every { fileRef.downloadUrl } returns Tasks.forResult(Uri.parse(expectedUrl))

    val url = repository.upload(audio, "task1")

    assertEquals(expectedUrl, url)
    assertEquals(expectedUrl, audio.url)
  }

  @Test
  fun upload_failure_throwsIOException() {
    val uploadTask: UploadTask = mockk()
    every { uploadTask.isComplete } returns true
    every { uploadTask.isCanceled } returns false
    every { uploadTask.isSuccessful } returns false
    every { uploadTask.exception } returns RuntimeException("Network error")

    every { fileRef.putFile(any()) } returns uploadTask

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

    every { fileRef.putFile(any()) } returns uploadTask
    every { fileRef.downloadUrl } returns Tasks.forResult(Uri.parse(""))

    val exception =
        assertThrows(IOException::class.java) { runBlocking { repository.upload(audio, "task1") } }
    assertEquals("Audio upload did not return a valid download URL.", exception.message)
  }

  @Test
  fun delete_success() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1_12345.m4a?alt=media"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_actualFirebaseUrl_deletesCorrectFileName() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/festivar-9a339.firebasestorage.app/o/audio%2Fvoice-note%2Ftask1_12345.m4a?alt=media&token=7d4d9a9g9j2k4k"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_actualFirebaseUrlWithoutAudioPrefix_deletesCorrectFileName() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/festivar-9a339.firebasestorage.app/o/task1_12345.m4a?alt=media&token=7d4d9a9g9j2k4k"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_urlWithoutOParam_deletesCorrectFileName() = runTest {
    val audioUrl = "https://example.com/audio/task1_12345.m4a?alt=media"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_plainFileName_deletesCorrectFileName() = runTest {
    val audioUrl = "task1_12345.m4a"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_malformedUrlWithInvalidEncoding_fallsBackToLastSegment() = runTest {
    val audioUrl = "https://example.com/o/audio%2"
    every { voiceNoteRef.child("audio%2") } returns fileRef
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_urlExtractingToBlankFileName_doesNotCallDelete() = runTest {
    val audioUrl = "https://example.com/o/audio%2F?alt=media"

    repository.delete(audioUrl)

    verify(exactly = 0) { voiceNoteRef.child(any()) }
  }

  @Test
  fun delete_blankUrl_doesNothing() = runTest {
    repository.delete("")
    repository.delete("   ")

    verify(exactly = 0) { voiceNoteRef.child(any()) }
  }

  @Test
  fun delete_notFound_doesNotThrow() = runTest {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1_12345.m4a?alt=media"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns
        Tasks.forException(NoSuchElementException("Object does not exist"))

    repository.delete(audioUrl)

    verify { fileRef.delete() }
  }

  @Test
  fun delete_failure_throwsIOException() {
    val audioUrl =
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/audio%2Fvoice-note%2Ftask1_12345.m4a?alt=media"
    every { voiceNoteRef.child("task1_12345.m4a") } returns fileRef
    every { fileRef.delete() } returns Tasks.forException(RuntimeException("Error"))

    assertThrows(IOException::class.java) { runBlocking { repository.delete(audioUrl) } }
  }
}
