// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.flow.StateFlow

/*
 * Contracts between the voice note modules. Each module implements or uses only these types, so
 * the modules never depend on each other's code:
 *
 * - the recorder implements [VoiceRecorder];
 * - the storage implements [AudioRepository];
 * - the player implements [VoicePlayer] for playback, and the UI shows [RecorderState] and
 *   [PlayerState];
 * - the task details view model wires them together with the TasksRepository.
 *
 * To add a voice note to a task (the caller is the task details view model):
 * 1. check the microphone permission, then call [VoiceRecorder.start];
 * 2. call [VoiceRecorder.stop] to get the [Audio] (or [VoiceRecorder.cancel] to drop it);
 * 3. call [AudioRepository.upload] with the task id and the [Audio] to get the audio URL;
 * 4. delete the temporary [Audio.file], whether the upload succeeded or failed;
 * 5. build a [VoiceNote] with that URL and call [Task.attachVoiceNote];
 * 6. save the returned [Task] with the TasksRepository.
 *
 * To remove it: call [Task.removeVoiceNote], save the returned [Task], then call
 * [AudioRepository.delete] with the old audio URL. To listen to it: call [VoicePlayer.play] with
 * the audio URL, and call [VoicePlayer.release] when the screen is closed.
 */

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

/** What a [VoiceRecorder] is doing. */
sealed interface RecorderState {
  /** Not recording. */
  data object Idle : RecorderState

  /** Recording for [elapsed]. */
  data class Recording(val elapsed: Duration) : RecorderState
}

interface AudioRepository {

  /** Generates and returns a new unique identifier for an audio item. */
  fun getNewUid(): String

  /**
   * Uploads [audio] into remote storage and returns the URL where it can be played from.
   *
   * @throws IOException if the storage cannot be reached or refuses the file.
   */
  suspend fun upload(audio: Audio, taskId: String): String

  /**
   * Deletes the audio file at [audioUrl].
   *
   * Deleting a file that does not exist is not an error.
   *
   * @throws IOException if the storage cannot be reached or refuses the deletion.
   */
  suspend fun delete(audioUrl: String)
}

/** A recording could not be started or finished (microphone busy, no space, too short...). */
class VoiceRecorderException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Records one voice note at a time. The microphone permission is checked by the caller before
 * [start]; this class only records.
 */
interface VoiceRecorder {
  /** The current state, updated while recording so the screen can show the timer. */
  val state: StateFlow<RecorderState>

  /**
   * Starts recording into a new temporary file. If the recording fails, the recorder deletes that
   * file itself.
   *
   * @throws IllegalStateException if a recording is already running.
   * @throws VoiceRecorderException if the recording cannot start.
   */
  fun start()

  /**
   * Stops the recording and returns it. The caller then owns the temporary file (see [Audio]). If
   * the recording is unusable, the recorder deletes the file and throws.
   *
   * @throws IllegalStateException if nothing is being recorded.
   * @throws VoiceRecorderException if the recording is unusable (for example too short).
   */
  fun stop(): Audio

  /** Stops the recording and deletes its file. Does nothing if nothing is being recorded. */
  fun cancel()
}

/** What a [VoicePlayer] is doing. */
sealed interface PlayerState {
  /** Nothing is playing. */
  data object Idle : PlayerState

  /** Playing the note at [audioUrl]. */
  data class Playing(val audioUrl: String) : PlayerState

  /** The note at [audioUrl] could not be played. */
  data class Failed(val audioUrl: String) : PlayerState
}

/** Plays one voice note at a time. */
interface VoicePlayer {
  /** The current state. [PlayerState.Idle] again when a note ends. */
  val state: StateFlow<PlayerState>

  /** Plays the note at [audioUrl], stopping any other one. */
  fun play(audioUrl: String)

  /** Stops playing. Does nothing if nothing is playing. */
  fun stop()

  /** Frees the underlying player. The instance must not be used afterwards. */
  fun release()
}
