// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
  private lateinit var fileRef: StorageReference
  private lateinit var repository: AudioRepositoryFirebase

  @Before
  fun setUp() {
    storageRef = mockk()
    audioRef = mockk()
    fileRef = mockk()

    every { storageRef.child("audio") } returns audioRef
    repository = AudioRepositoryFirebase(storageRef)
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
  fun deleteAudio_success() = runTest {
    val fileName = "test.mp3"
    every { audioRef.child(fileName) } returns fileRef
    every { fileRef.metadata } returns Tasks.forResult(mockk())
    every { fileRef.delete() } returns Tasks.forResult(null)

    repository.deleteAudio(fileName)

    verify { fileRef.delete() }
  }

  @Test
  fun deleteAudio_notFoundThrowsNoSuchElementException() {
    val fileName = "nonexistent.mp3"
    every { audioRef.child(fileName) } returns fileRef
    every { fileRef.metadata } returns Tasks.forException(RuntimeException("Object does not exist"))

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteAudio(fileName) }
    }
  }

  @Test
  fun editAudio_mismatchedFileNameThrowsIllegalArgumentException() {
    val fileName = "audio1.mp3"
    val newValue = Audio("audio2.mp3", Uri.parse("file:///audio2.mp3"))

    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.editAudio(fileName, newValue) }
    }
  }

  @Test
  fun addAudio_alreadyExistsThrowsIllegalArgumentException() {
    val audio = Audio("existing.mp3", Uri.parse("file:///existing.mp3"))
    every { audioRef.child(audio.fileName) } returns fileRef
    every { fileRef.metadata } returns Tasks.forResult(mockk())

    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.addAudio(audio) }
    }
  }
}
