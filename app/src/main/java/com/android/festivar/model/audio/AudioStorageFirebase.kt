// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AudioStorageFirebase(private val storageRef: StorageReference) : AudioRepository {
  private val audioRef = storageRef.child("audio").child("voice-note")

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun upload(audio: Audio, taskId: String): String {
    try {
      val extension = audio.file.extension.let { if (it.isNotBlank()) ".$it" else ".m4a" }
      val fileName = "${getNewUid()}$extension"
      val ref = audioRef.child(taskId).child(fileName)
      ref.putFile(audio.uri).await()
      val downloadUrl = ref.downloadUrl.await().toString()
      return downloadUrl.ifBlank {
        throw IOException("Audio upload did not return a valid download URL.")
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (e is IOException) throw e
      throw IOException("Failed to upload audio file.", e)
    }
  }

  override suspend fun delete(audioUrl: String) {
    if (audioUrl.isBlank()) return
    try {
      val ref = storageRef.storage.getReferenceFromUrl(audioUrl)
      ref.delete().await()
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
        // Deleting a non-existent file is non-fatal per contract.
        return
      }
      throw IOException("Failed to delete audio file.", e)
    }
  }
}
