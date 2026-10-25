// Co-authored-by: Gemini <gemini@google.com>
package com.android.festivar.model.audio

import androidx.core.net.toUri
import java.io.File
import java.time.Duration

/**
 * Represents an audio recording or file in the FestivAR application.
 *
 * @property file The local file handle of the audio recording.
 * @property duration Length of the recording.
 * @property url The remote download URL of the audio file once uploaded.
 */
data class Audio(
    val file: File,
    val duration: Duration = Duration.ZERO,
    var url: String = "",
) {
  val fileName: String
    get() = file.name

  val uri
    get() = file.toUri()

  init {
    require(duration.isZero || duration.isPositive) { "The duration must be non-negative." }
  }
}
