// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.voicenote.PlayerState
import com.android.festivar.model.voicenote.RecordedAudio
import com.android.festivar.model.voicenote.RecorderState
import com.android.festivar.model.voicenote.VocalNoteStorage
import com.android.festivar.model.voicenote.VoicePlayer
import com.android.festivar.model.voicenote.VoiceRecorder
import java.io.File
import java.nio.file.Files
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A [VoiceRecorder] that records nothing: it hands out empty files and can be made to fail. */
class FakeVoiceRecorder : VoiceRecorder {
  private val directory = Files.createTempDirectory("fake-recorder").toFile()
  private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
  override val state: StateFlow<RecorderState> = _state

  /** Thrown by [start] when not null. */
  var startFailure: Exception? = null

  /** Thrown by [stop] when not null. */
  var stopFailure: Exception? = null

  /** The duration of the next recordings. */
  var duration: Duration = Duration.ofSeconds(5)

  var starts = 0
  var cancels = 0

  /** The last audio handed out by [stop]. */
  var lastAudio: RecordedAudio? = null

  override fun start() {
    startFailure?.let { throw it }
    check(_state.value !is RecorderState.Recording) { "Already recording." }
    starts++
    _state.value = RecorderState.Recording(Duration.ZERO)
  }

  override fun stop(): RecordedAudio {
    check(_state.value is RecorderState.Recording) { "Not recording." }
    _state.value = RecorderState.Idle
    stopFailure?.let { throw it }
    return RecordedAudio(File.createTempFile("note", ".m4a", directory), duration).also {
      lastAudio = it
    }
  }

  override fun cancel() {
    if (_state.value is RecorderState.Recording) cancels++
    _state.value = RecorderState.Idle
  }
}

/** A [VocalNoteStorage] in memory: remembers the calls, can fail and can be made to wait. */
class FakeVocalNoteStorage : VocalNoteStorage {
  /** Thrown by [upload] when not null. */
  var uploadFailure: Exception? = null

  /** Thrown by [delete] when not null. */
  var deleteFailure: Exception? = null

  /** While not null, [upload] waits for it to complete. */
  var uploadGate: CompletableDeferred<Unit>? = null

  /** The task ids of the uploads, in order. */
  val uploads = mutableListOf<String>()

  /** The URLs of the deletions asked, in order. */
  val deletions = mutableListOf<String>()

  override suspend fun upload(taskId: String, audio: RecordedAudio): String {
    uploadGate?.await()
    uploadFailure?.let { throw it }
    uploads += taskId
    return "https://fake/$taskId/${uploads.size}.m4a"
  }

  override suspend fun delete(audioUrl: String) {
    deletions += audioUrl
    deleteFailure?.let { throw it }
  }
}

/** A [VoicePlayer] that plays nothing but keeps the state a real one would have. */
class FakeVoicePlayer : VoicePlayer {
  private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
  override val state: StateFlow<PlayerState> = _state

  /** The URLs asked to play, in order. */
  val played = mutableListOf<String>()

  var released = false

  override fun play(audioUrl: String) {
    played += audioUrl
    _state.value = PlayerState.Playing(audioUrl)
  }

  override fun stop() {
    _state.value = PlayerState.Idle
  }

  override fun release() {
    released = true
  }

  /** Simulates a note that cannot be played. */
  fun fail(audioUrl: String) {
    _state.value = PlayerState.Failed(audioUrl)
  }
}

/** A [TasksRepository] whose [editTask] can be made to fail, to test a save that goes wrong. */
class EditFailingRepository(val local: TasksRepositoryLocal = TasksRepositoryLocal()) :
    TasksRepository by local {
  /** Thrown by [editTask] when not null. */
  var editFailure: Exception? = null

  override suspend fun editTask(taskId: String, newValue: Task) {
    editFailure?.let { throw it }
    local.editTask(taskId, newValue)
  }
}
