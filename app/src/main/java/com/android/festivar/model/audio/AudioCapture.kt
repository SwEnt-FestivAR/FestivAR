// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/** One recording of the microphone into a file: the small part of [MediaRecorder] that is used. */
internal interface AudioCapture {
  /**
   * Starts recording into [outputFile], then calls [onError] if the recording fails later on.
   *
   * @throws IOException if the file cannot be written.
   * @throws RuntimeException if the microphone is busy or the permission is missing.
   */
  fun start(outputFile: File, onError: () -> Unit)

  /**
   * Pauses recording, so that nothing is added to the file until [resume].
   *
   * @throws RuntimeException if the recording cannot be paused.
   */
  fun pause()

  /**
   * Resumes recording after [pause].
   *
   * @throws RuntimeException if the recording cannot be resumed.
   */
  fun resume()

  /**
   * Stops recording, even if it is paused, and finishes the file.
   *
   * @throws RuntimeException if no audio was captured.
   */
  fun stop()

  /** Frees the capture. It must not be used afterwards. */
  fun release()
}

/** The [AudioCapture] backed by a [MediaRecorder] that writes AAC audio into an MPEG-4 file. */
internal class MediaRecorderCapture(private val recorder: MediaRecorder) : AudioCapture {

  constructor(context: Context) : this(newRecorder(context))

  override fun start(outputFile: File, onError: () -> Unit) {
    recorder.setOnErrorListener { _, what, extra ->
      Log.e(LOG_TAG, "MediaRecorder error what=$what extra=$extra")
      onError()
    }
    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    recorder.setAudioSamplingRate(SAMPLING_RATE_HZ)
    recorder.setAudioEncodingBitRate(BIT_RATE)
    recorder.setOutputFile(outputFile.absolutePath)
    recorder.prepare()
    recorder.start()
  }

  override fun pause() = recorder.pause()

  override fun resume() = recorder.resume()

  override fun stop() = recorder.stop()

  override fun release() = recorder.release()

  private companion object {
    // Without these, the default AAC settings of many devices sound muffled.
    const val SAMPLING_RATE_HZ = 44_100
    const val BIT_RATE = 64_000
    const val LOG_TAG = "MediaRecorderCapture"

    fun newRecorder(context: Context): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          MediaRecorder(context)
        } else {
          @Suppress("DEPRECATION") MediaRecorder()
        }
  }
}
