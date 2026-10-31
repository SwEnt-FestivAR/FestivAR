// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.net.URLDecoder
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
      val metadata = StorageMetadata.Builder().setContentType("audio/m4a").build()
      ref.putFile(audio.uri, metadata).await()
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
      val ref =
          try {
            storageRef.storage.getReferenceFromUrl(audioUrl)
          } catch (e: IllegalArgumentException) {
            // getReferenceFromUrl rejects emulator URLs, so we read the object path ourselves.
            val pathPart = audioUrl.substringAfter("/o/").substringBefore("?")
            val decodedPath = withContext(IO) { URLDecoder.decode(pathPart, "UTF-8") }
            require(decodedPath.startsWith("audio/voice-note/")) {
              "Not a voice note URL: $audioUrl"
            }
            storageRef.storage.reference.child(decodedPath)
          }
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
