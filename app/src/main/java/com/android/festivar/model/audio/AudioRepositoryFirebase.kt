// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import androidx.core.net.toUri
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AudioRepositoryFirebase(storageRef: StorageReference) : AudioRepository {
  val audioRef = storageRef.child("audio")

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAudio(fileName: String): Audio {
    val ref = audioRef.child(fileName)
    val localFile = withContext(IO) { File.createTempFile("audio_", ".mp3") }
    try {
      ref.getFile(localFile).await()
      val downloadUrl = ref.downloadUrl.await().toString()
      return Audio(fileName, localFile.toUri(), downloadUrl)
    } catch (e: Exception) {
      if (
          e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND ||
              e.message?.contains("Object does not exist") == true
      ) {
        throw NoSuchElementException("Audio '$fileName' does not exist.")
      }
      throw e
    }
  }

  override suspend fun addAudio(audio: Audio) {
    val ref = audioRef.child(audio.fileName)
    try {
      ref.metadata.await()
      throw IllegalArgumentException("An Audio with fileName '${audio.fileName}' already exists.")
    } catch (e: Exception) {
      if (e is IllegalArgumentException) throw e
      // If object not found exception, file does not exist yet, which is expected for add
    }
    ref.putFile(audio.uri).await()
    audio.url = ref.downloadUrl.await().toString()
  }

  override suspend fun editAudio(
      fileName: String,
      newValue: Audio,
  ) {
    require(fileName == newValue.fileName) { "The edited Audio must keep its existing fileName." }
    val ref = audioRef.child(fileName)
    try {
      ref.metadata.await()
    } catch (_: Exception) {
      throw NoSuchElementException("Audio '$fileName' does not exist.")
    }
    ref.putFile(newValue.uri).await()
    newValue.url = ref.downloadUrl.await().toString()
  }

  override suspend fun deleteAudio(fileName: String) {
    val ref = audioRef.child(fileName)
    try {
      ref.metadata.await()
    } catch (_: Exception) {
      throw NoSuchElementException("Audio '$fileName' does not exist.")
    }
    ref.delete().await()
  }
}
