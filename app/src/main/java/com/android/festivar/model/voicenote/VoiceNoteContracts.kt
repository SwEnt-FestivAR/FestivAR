// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.io.File
import java.time.Duration
import kotlinx.coroutines.flow.StateFlow

/*
 * Contracts between the voice note modules. Each module implements or uses only these types, so
 * the modules never depend on each other's code:
 *
 * - the recorder implements [VoiceRecorder];
 * - the storage implements [VoiceNoteStorage];
 * - the player implements [VoicePlayer] for playback, and the UI shows [RecorderState] and
 *   [PlayerState];
 * - the task details view model wires them together with the TasksRepository.
 *
 * To add a voice note to a task (the caller is the task details view model):
 * 1. check the microphone permission, then call [VoiceRecorder.start];
 * 2. call [VoiceRecorder.stop] to get the [RecordedAudio] (or [VoiceRecorder.cancel] to drop it);
 * 3. call [VoiceNoteStorage.upload] with the task id and the [RecordedAudio] to get the audio URL;
 * 4. delete the temporary [RecordedAudio.file], whether the upload succeeded or failed;
 * 5. build a [VoiceNote] with that URL and call [Task.attachVoiceNote];
 * 6. save the returned [Task] with the TasksRepository.
 *
 * To remove it: call [Task.removeVoiceNote], save the returned [Task], then call
 * [VoiceNoteStorage.delete] with the old audio URL. To listen to it: call [VoicePlayer.play] with
 * the audio URL, and call [VoicePlayer.release] when the screen is closed.
 */

/**
 * A finished recording, waiting to be uploaded. The [file] is only a handle: the model never opens
 * it. The [duration] is positive: the recorder refuses a recording that is too short.
 *
 * The [file] is temporary. Whoever received this [RecordedAudio] from [VoiceRecorder.stop] owns it
 * and must delete it once the upload has succeeded or been given up.
 */
data class RecordedAudio(val file: File, val duration: Duration) {
  init {
    require(duration.isPositive()) { "The duration must be positive." }
  }
}

/** What a [VoiceRecorder] is doing. */
sealed interface RecorderState {
  /** Not recording. */
  data object Idle : RecorderState

  /** Recording for [elapsed]. */
  data class Recording(val elapsed: Duration) : RecorderState
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
   * Stops the recording and returns it. The caller then owns the temporary file (see
   * [RecordedAudio]). If the recording is unusable, the recorder deletes the file and throws.
   *
   * @throws IllegalStateException if nothing is being recorded.
   * @throws VoiceRecorderException if the recording is unusable (for example too short).
   */
  fun stop(): RecordedAudio

  /** Stops the recording and deletes its file. Does nothing if nothing is being recorded. */
  fun cancel()
}

/** Keeps the audio files of the voice notes in a remote storage. */
interface VoiceNoteStorage {
  /**
   * Uploads [audio] for the task [taskId] and returns the URL where it can be played from.
   *
   * @throws java.io.IOException if the storage cannot be reached or refuses the file. An
   *   implementation converts its own exceptions (for example Firebase's `StorageException`) to an
   *   [java.io.IOException], so callers do not depend on the storage used.
   */
  suspend fun upload(taskId: String, audio: RecordedAudio): String

  /**
   * Deletes the audio file at [audioUrl], a URL returned by [upload].
   *
   * Deleting a file that does not exist is not an error.
   *
   * @throws java.io.IOException if the storage cannot be reached or refuses the deletion. Other
   *   exceptions are converted as for [upload].
   */
  suspend fun delete(audioUrl: String)
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
