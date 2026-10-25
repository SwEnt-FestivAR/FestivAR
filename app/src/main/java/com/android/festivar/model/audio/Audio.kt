// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import android.net.Uri

/**
 * Represents an [Audio] item in the FestivAR application.
 *
 * @property fileName The unique file name / identifier of the audio item.
 * @property uri The local or source URI of the audio file.
 * @property url The remote download URL of the audio file.
 */
data class Audio(
    val fileName: String,
    val uri: Uri,
    var url: String = "",
) {
  init {
    require(fileName.isNotBlank()) { "The fileName cannot be empty." }
  }
}
