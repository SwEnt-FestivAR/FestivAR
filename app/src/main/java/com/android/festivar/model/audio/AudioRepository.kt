// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import java.io.IOException

interface AudioRepository {

  /** Generates and returns a new unique identifier for an audio item. */
  fun getNewUid(): String

  /**
   * Uploads [audio] into remote storage and returns the URL where it can be played from.
   *
   * @throws IOException if the storage cannot be reached or refuses the file.
   */
  suspend fun upload(audio: Audio): String

  /**
   * Deletes the audio file at [audioUrl].
   *
   * Deleting a file that does not exist is not an error.
   *
   * @throws IOException if the storage cannot be reached or refuses the deletion.
   */
  suspend fun delete(audioUrl: String)
}
