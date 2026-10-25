// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.voicenote

import androidx.core.net.toUri
import com.android.festivar.model.audio.Audio
import com.android.festivar.model.audio.AudioRepository
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Firebase implementation of [VoiceNoteStorage] backed by an [AudioRepository]. */
class VoiceNoteStorageFirebase(
    private val audioRepository: AudioRepository,
) : VoiceNoteStorage {

  override suspend fun upload(taskId: String, audio: RecordedAudio): String {
    try {
      val extension = audio.file.extension.let { if (it.isNotBlank()) ".$it" else ".m4a" }
      val fileName = "${taskId}_${audioRepository.getNewUid()}$extension"
      val audioItem = Audio(fileName = fileName, uri = audio.file.toUri())
      audioRepository.addAudio(audioItem)
      return audioItem.url.ifBlank {
        throw IOException("Audio upload did not return a valid download URL.")
      }
    } catch (e: Exception) {
      if (e is IOException) throw e
      throw IOException("Failed to upload voice note audio.", e)
    }
  }

  override suspend fun delete(audioUrl: String) {
    if (audioUrl.isBlank()) return
    try {
      val fileName = extractFileName(audioUrl)
      if (fileName.isNotBlank()) {
        audioRepository.deleteAudio(fileName)
      }
    } catch (_: NoSuchElementException) {
      // Deleting a file that does not exist is not an error.
    } catch (e: Exception) {
      throw IOException("Failed to delete voice note audio.", e)
    }
  }

  /** Extracts the file name from a Firebase Storage URL. */
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
