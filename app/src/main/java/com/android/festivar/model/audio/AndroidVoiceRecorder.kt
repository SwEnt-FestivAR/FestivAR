// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

/** Creates the [AudioCapture] of each recording, so the tests can replace [MediaRecorder]. */
internal fun interface AudioCaptureFactory {
  fun create(): AudioCapture
}

/** The [AudioCapture] backed by a [MediaRecorder] that writes AAC audio into an MPEG-4 file. */
internal class MediaRecorderCapture(private val recorder: MediaRecorder) : AudioCapture {

  constructor(context: Context) : this(newRecorder(context))

  override fun start(outputFile: File, onError: () -> Unit) {
    recorder.setOnErrorListener { _, _, _ -> onError() }
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

    fun newRecorder(context: Context): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          MediaRecorder(context)
        } else {
          @Suppress("DEPRECATION") MediaRecorder()
        }
  }
}

/**
 * The Android [VoiceRecorder]: records the microphone into a temporary `.m4a` file with a
 * [MediaRecorder]. It records one note at a time and must be used from the main thread, where the
 * [MediaRecorder] reports back.
 *
 * @param factory Creates the capture of each recording.
 * @param createOutputFile Creates the new temporary file of each recording.
 * @param clock The current time in milliseconds. It must never go backwards.
 * @param timerScope Runs the timer behind [RecorderState.Recording]. Its dispatcher must use the
 *   same thread as the callers.
 */
class AndroidVoiceRecorder
internal constructor(
    private val factory: AudioCaptureFactory,
    private val createOutputFile: () -> File,
    private val clock: () -> Long,
    private val timerScope: CoroutineScope,
) : VoiceRecorder {

  /** Creates a recorder that uses the application context of [context]. */
  constructor(
      context: Context
  ) : this(
      factory = AudioCaptureFactory { MediaRecorderCapture(context.applicationContext) },
      createOutputFile = {
        File.createTempFile(FILE_PREFIX, FILE_SUFFIX, context.applicationContext.cacheDir)
      },
      clock = SystemClock::elapsedRealtime,
      timerScope = MainScope(),
  )

  private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
  override val state: StateFlow<RecorderState> = _state.asStateFlow()

  private var current: ActiveRecording? = null

  override fun start() {
    check(current == null) { "A recording is already running." }
    val file = startStep(undo = {}) { createOutputFile() }
    val capture = startStep(undo = { deleteFile(file) }) { factory.create() }
    startStep(
        undo = {
          release(capture)
          deleteFile(file)
        }
    ) {
      capture.start(file) { onCaptureError(capture) }
    }
    val recording = ActiveRecording(capture, file, runningSince = clock())
    current = recording
    _state.value = RecorderState.Recording(Duration.ZERO)
    launchTimer(recording)
  }

  override fun pause() {
    val recording = current
    check(recording != null && !recording.isPaused) { "No running recording to pause." }
    changeCapture(recording, "The recording could not be paused.") { recording.capture.pause() }
    recording.timer?.cancel()
    recording.recordedBefore = elapsed(recording)
    recording.runningSince = null
    _state.value = RecorderState.Recording(recording.recordedBefore, isPaused = true)
  }

  override fun resume() {
    val recording = current
    check(recording != null && recording.isPaused) { "No paused recording to resume." }
    changeCapture(recording, "The recording could not be resumed.") { recording.capture.resume() }
    recording.runningSince = clock()
    _state.value = RecorderState.Recording(recording.recordedBefore)
    launchTimer(recording)
  }

  override fun stop(): Audio {
    val recording = checkNotNull(current) { "No recording is running." }
    val duration = elapsed(recording)
    try {
      recording.capture.stop()
    } catch (e: RuntimeException) {
      end(recording)
      deleteFile(recording.file)
      throw VoiceRecorderException("The recording could not be finished.", e)
    }
    end(recording)
    val problem =
        when {
          duration < MIN_DURATION -> "The recording is too short."
          recording.file.length() == 0L -> "The recording is empty."
          else -> null
        }
    if (problem != null) {
      deleteFile(recording.file)
      throw VoiceRecorderException(problem)
    }
    return Audio(recording.file, duration)
  }

  override fun cancel() {
    current?.let { drop(it) }
  }

  /**
   * Runs one [step] of [start]. Anything that goes wrong, including the [IllegalStateException] of
   * a busy [MediaRecorder], is reported as a [VoiceRecorderException] after running [undo].
   */
  private inline fun <T> startStep(undo: () -> Unit, step: () -> T): T =
      try {
        step()
      } catch (e: IOException) {
        undo()
        throw VoiceRecorderException(START_FAILED, e)
      } catch (e: RuntimeException) {
        undo()
        throw VoiceRecorderException(START_FAILED, e)
      }

  /**
   * Runs [action] on the capture of the [recording]. If it fails, including with the
   * [IllegalStateException] of [MediaRecorder], the recording is dropped and the [failure] message
   * is reported as a [VoiceRecorderException].
   */
  private inline fun changeCapture(
      recording: ActiveRecording,
      failure: String,
      action: () -> Unit,
  ) {
    try {
      action()
    } catch (e: RuntimeException) {
      drop(recording)
      throw VoiceRecorderException(failure, e)
    }
  }

  private fun launchTimer(recording: ActiveRecording) {
    recording.timer = timerScope.launch {
      while (isActive) {
        delay(TICK_INTERVAL_MILLIS)
        _state.value = RecorderState.Recording(elapsed(recording))
      }
    }
  }

  /** The time recorded so far by the [recording], without the time it spent paused. */
  private fun elapsed(recording: ActiveRecording): Duration {
    val runningSince = recording.runningSince ?: return recording.recordedBefore
    return recording.recordedBefore + Duration.ofMillis(clock() - runningSince)
  }

  /** Drops the recording after an error, unless [capture] has already been replaced or ended. */
  private fun onCaptureError(capture: AudioCapture) {
    val recording = current?.takeIf { it.capture === capture } ?: return
    Log.e(LOG_TAG, "The recording failed and was dropped")
    drop(recording)
  }

  /** Ends the [recording] and deletes its file. It never throws, as `onCleared` relies on it. */
  private fun drop(recording: ActiveRecording) {
    try {
      recording.capture.stop()
    } catch (e: RuntimeException) {
      // Expected when nothing was captured yet, and the file is deleted anyway.
      Log.w(LOG_TAG, "Error stopping a dropped recording", e)
    }
    end(recording)
    deleteFile(recording.file)
  }

  private fun end(recording: ActiveRecording) {
    recording.timer?.cancel()
    current = null
    _state.value = RecorderState.Idle
    release(recording.capture)
  }

  private fun release(capture: AudioCapture) {
    try {
      capture.release()
    } catch (e: RuntimeException) {
      // Nothing more to free: the capture is already unusable.
      Log.w(LOG_TAG, "Error releasing the recorder", e)
    }
  }

  private fun deleteFile(file: File) {
    if (!file.delete()) Log.w(LOG_TAG, "Could not delete the temporary recording")
  }

  /**
   * The recording in progress.
   *
   * @property runningSince When it last started or resumed, or null while it is paused.
   * @property recordedBefore The time recorded before [runningSince].
   * @property timer The timer of the current run. It is cancelled while the recording is paused.
   */
  private class ActiveRecording(
      val capture: AudioCapture,
      val file: File,
      var runningSince: Long?,
      var recordedBefore: Duration = Duration.ZERO,
      var timer: Job? = null,
  ) {
    val isPaused: Boolean
      get() = runningSince == null
  }

  internal companion object {
    /** Shorter recordings are unusable, so [stop] deletes them and throws. */
    val MIN_DURATION: Duration = Duration.ofSeconds(1)

    /** How often [RecorderState.Recording] is updated. */
    const val TICK_INTERVAL_MILLIS = 200L

    private const val FILE_PREFIX = "voice_note_"
    private const val FILE_SUFFIX = ".m4a"
    private const val START_FAILED = "The recording could not start."
    private const val LOG_TAG = "AndroidVoiceRecorder"
  }
}
