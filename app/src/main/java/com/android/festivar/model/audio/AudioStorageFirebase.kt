// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.tasks.await

class AudioStorageFirebase(storageRef: StorageReference) : AudioRepository {
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
      audio.url = downloadUrl
      return downloadUrl.ifBlank {
        throw IOException("Audio upload did not return a valid download URL.")
      }
    } catch (e: Exception) {
      if (e is IOException) throw e
      throw IOException("Failed to upload audio file.", e)
    }
  }

  override suspend fun delete(audioUrl: String) {
    if (audioUrl.isBlank()) return
    try {
      val fileName = extractFileName(audioUrl)
      if (fileName.isNotBlank()) {
        val ref = audioRef.child(fileName)
        ref.delete().await()
      }
    } catch (e: Exception) {
      if (
          e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND ||
              e.message?.contains("Object does not exist") == true ||
              e is NoSuchElementException
      ) {
        // Deleting a non-existent file is non-fatal per contract.
        return
      }
      throw IOException("Failed to delete audio file.", e)
    }
  }

  private fun extractFileName(url: String): String {
    return try {
      val withoutQuery = url.substringBefore('?')
      val oIndex = withoutQuery.indexOf("/o/")
      val path =
          if (oIndex != -1) {
            withoutQuery.substring(oIndex + 3)
          } else {
            withoutQuery.substringAfterLast('/')
          }
      val decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name())
      if (decoded.startsWith("audio/")) {
        decoded.removePrefix("audio/")
      } else {
        decoded
      }
    } catch (_: Exception) {
      url.substringAfterLast('/')
    }
  }
}
