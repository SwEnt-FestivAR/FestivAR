// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.io.File
import java.time.Duration
import kotlinx.coroutines.flow.StateFlow

/*
 * Contracts between the voice note modules. Each module implements or uses only these types, so
 * the modules never depend on each other's code:
 *
 * - recording (issue 208) implements [VoiceRecorder];
 * - storage (issue 209) implements [VocalNoteStorage];
 * - the UI (issue 207) implements [VoicePlayer] for playback and shows [RecorderState] and
 *   [PlayerState];
 * - the task details view model wires them together with the TasksRepository.
 */

/** A finished recording, waiting to be uploaded. */
data class RecordedAudio(val file: File, val duration: Duration) {
  init {
    require(!duration.isNegative) { "The duration cannot be negative." }
  }
}

/** What a [VoiceRecorder] is doing. */
sealed interface RecorderState {
  /** Not recording. */
  data object Idle : RecorderState

  /** Recording since [elapsed]. */
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
   * Starts recording into a new temporary file.
   *
   * @throws IllegalStateException if a recording is already running.
   * @throws VoiceRecorderException if the recording cannot start.
   */
  fun start()

  /**
   * Stops the recording and returns it.
   *
   * @throws IllegalStateException if nothing is being recorded.
   * @throws VoiceRecorderException if the recording is unusable (for example too short).
   */
  fun stop(): RecordedAudio

  /** Stops the recording and deletes its file. Does nothing if nothing is being recorded. */
  fun cancel()
}

/** Keeps the audio files of the voice notes in a remote storage. */
interface VocalNoteStorage {
  /**
   * Uploads [audio] for the task [taskId] and returns the URL where it can be played from.
   *
   * Implementations backed by a remote storage throw when it cannot be reached.
   */
  suspend fun upload(taskId: String, audio: RecordedAudio): String

  /**
   * Deletes the audio file at [audioUrl], a URL returned by [upload].
   *
   * Deleting a file that does not exist is not an error.
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
