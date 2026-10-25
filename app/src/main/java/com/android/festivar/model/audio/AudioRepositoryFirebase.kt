// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import com.google.firebase.storage.StorageReference
import java.util.UUID
import kotlinx.coroutines.tasks.await

class AudioRepositoryFirebase(storageRef: StorageReference) : AudioRepository {
  val audioRef = storageRef.child("audio")

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
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
