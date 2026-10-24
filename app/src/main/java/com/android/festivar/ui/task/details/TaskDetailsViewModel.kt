// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import com.android.festivar.model.voicenote.PlayerState
import com.android.festivar.model.voicenote.RecordedAudio
import com.android.festivar.model.voicenote.RecorderState
import com.android.festivar.model.voicenote.VocalNote
import com.android.festivar.model.voicenote.VocalNoteStorage
import com.android.festivar.model.voicenote.VoicePlayer
import com.android.festivar.model.voicenote.VoiceRecorder
import com.android.festivar.model.voicenote.VoiceRecorderException
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "TaskDetailsViewModel"

/** The badge at the top of the Task details screen. */
enum class TaskStatus {
  /** Not done, and a place is left on it. */
  OPEN,

  /** Not done, and every place is held by someone else. */
  TAKEN,

  /** Not done, and the current user holds a place on it. */
  YOURS,

  /** The task is completed. */
  DONE,
}

/**
 * UI state of the Task details screen.
 *
 * @property currentUserId the uid of the signed-in user, to tell if [task] is [TaskStatus.YOURS].
 * @property task the task shown, or null until it is loaded (or if it could not be loaded).
 * @property isLoading true while the task is being loaded.
 * @property isUpdating true while an action is being saved, so a second tap does nothing. Actions
 *   are also disabled while [isLoading], so a reload cannot overwrite a saved change.
 * @property errorMsg the string resource of the last failure, or null.
 */
data class TaskDetailsUiState(
    val currentUserId: String,
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /** The badge of [task], computed from it so the two cannot differ; null while [task] is null. */
  val status: TaskStatus?
    get() = task?.let {
      when {
        it.completed -> TaskStatus.DONE
        it.assignees.any { user -> user.uid == currentUserId } -> TaskStatus.YOURS
        it.assignees.size >= it.maxAssign -> TaskStatus.TAKEN
        else -> TaskStatus.OPEN
      }
    }

  /** The "Take" button of an [TaskStatus.OPEN] task. */
  val canTake: Boolean
    get() = status == TaskStatus.OPEN && !isUpdating && !isLoading

  /** The "Give back" button of a [TaskStatus.YOURS] task. */
  val canGiveBack: Boolean
    get() = status == TaskStatus.YOURS && !isUpdating && !isLoading

  /** The "Mark done" button of a [TaskStatus.YOURS] task. */
  val canMarkDone: Boolean
    get() = status == TaskStatus.YOURS && !isUpdating && !isLoading

  /** Whether the actions bar is shown at all: only open and own tasks have one. */
  val hasActions: Boolean
    get() = status == TaskStatus.OPEN || status == TaskStatus.YOURS
}

/**
 * Holds the state of the Task details screen and applies its three actions: take the task, give it
 * back and mark it done. Each action saves the new version of the task in [repository] and shows it
 * only once saved.
 *
 * It also drives the voice note section ([voiceNote]): everybody can play the note, and only the
 * task owner can record, replace (delete, then record) and delete it.
 *
 * @param taskId the task to show.
 * @param currentUser the signed-in user. The caller must pass it: this class does not check that
 *   someone is signed in.
 * @param recorder records the voice note. Without it, or without [storage], nobody can record or
 *   delete the note: the section is read-only.
 * @param storage keeps the audio file of the voice note.
 * @param player plays the voice note. Without it the note cannot be played.
 * @param newId creates the id of a new voice note.
 * @param now gives the time a voice note is recorded at.
 */
class TaskDetailsViewModel(
    private val taskId: String,
    private val currentUser: User,
    private val repository: TasksRepository = TasksRepositoryProvider.repository,
    private val recorder: VoiceRecorder? = null,
    private val storage: VocalNoteStorage? = null,
    private val player: VoicePlayer? = null,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {
  private val _uiState = MutableStateFlow(TaskDetailsUiState(currentUserId = currentUser.uid))
  val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

  /** The parts of the voice note section that do not come from the task. */
  private data class VoiceNoteFlags(
      val sendFailed: Boolean = false,
      val micDenied: Boolean = false,
      val showDeleteConfirm: Boolean = false,
  )

  private val voiceFlags = MutableStateFlow(VoiceNoteFlags())

  // The recording that could not be uploaded, kept to try again.
  private var pendingAudio: RecordedAudio? = null

  /** What the voice note section shows, derived from the task, the recorder and the player. */
  val voiceNote: StateFlow<VoiceNoteUiState> =
      combine(
              _uiState,
              voiceFlags,
              recorder?.state ?: flowOf(RecorderState.Idle),
              player?.state ?: flowOf(PlayerState.Idle),
          ) { state, flags, recorderState, playerState ->
            VoiceNoteUiState(
                note = state.task?.vocalNote,
                canManage = canManageVoiceNote(state),
                recorder = recorderState,
                player = playerState,
                sendFailed = flags.sendFailed,
                micDenied = flags.micDenied,
                showDeleteConfirm = flags.showDeleteConfirm,
            )
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, VoiceNoteUiState())

  // isLoading starts true, so it cannot tell a running load from the initial state.
  private var loadJob: Job? = null

  init {
    load()
  }

  /**
   * Loads the task; on failure the error is set and the version already shown, if any, stays shown.
   * Does nothing while an action is saving or another load is running.
   */
  fun load() {
    if (_uiState.value.isUpdating || loadJob?.isActive == true) return
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val task = repository.getTask(taskId)
        _uiState.update { it.copy(task = task, isLoading = false, errorMsg = null) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading task $taskId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = R.string.task_details_load_failed) }
      }
    }
  }

  /**
   * Adds the current user to the task's assignees. Does nothing unless
   * [TaskDetailsUiState.canTake].
   */
  fun take() = saveIf({ it.canTake }) { it.addAssignee(currentUser) }

  /** Removes the current user from the assignees. Does nothing unless the task is yours. */
  fun giveBack() = saveIf({ it.canGiveBack }) { it.removeAssignee(currentUser) }

  /** Completes the task. Does nothing unless the task is yours. */
  fun markDone() = saveIf({ it.canMarkDone }) { it.complete() }

  /** Clears the error message. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  // ---- Voice note ----

  /** Only the task owner manages the note, and only if there is something to record and store. */
  private fun canManageVoiceNote(state: TaskDetailsUiState): Boolean =
      recorder != null && storage != null && state.task?.canManageVocalNote(currentUser.uid) == true

  /** The owner can record only while the task has no note, and when nothing else is saving. */
  private fun canRecord(state: TaskDetailsUiState): Boolean =
      canManageVoiceNote(state) &&
          state.task?.vocalNote == null &&
          !state.isUpdating &&
          !state.isLoading

  private fun canDeleteVoiceNote(state: TaskDetailsUiState): Boolean =
      canManageVoiceNote(state) &&
          state.task?.vocalNote != null &&
          !state.isUpdating &&
          !state.isLoading

  /**
   * Starts recording the voice note. The caller checks the microphone permission first. Does
   * nothing unless the user is the owner and the task has no note.
   */
  fun startRecording() {
    val recorder = recorder ?: return
    if (!canRecord(_uiState.value) || recorder.state.value is RecorderState.Recording) return
    pendingAudio = null // a new recording replaces the one that could not be sent
    voiceFlags.update { it.copy(sendFailed = false) }
    try {
      recorder.start()
    } catch (e: VoiceRecorderException) {
      Log.e(LOG_TAG, "Error starting a recording", e)
      _uiState.update { it.copy(errorMsg = R.string.voice_note_record_failed) }
    } catch (e: IllegalStateException) {
      Log.e(LOG_TAG, "Error starting a recording", e)
    }
  }

  /**
   * Stops recording, uploads the audio and attaches the voice note to the task. If the upload or
   * the save fails, the recording is kept and [retryVoiceNote] sends it again.
   */
  fun finishRecording() {
    val recorder = recorder ?: return
    if (recorder.state.value !is RecorderState.Recording) return
    val audio =
        try {
          recorder.stop()
        } catch (e: VoiceRecorderException) {
          Log.e(LOG_TAG, "Error finishing a recording", e)
          _uiState.update { it.copy(errorMsg = R.string.voice_note_record_failed) }
          return
        } catch (e: IllegalStateException) {
          Log.e(LOG_TAG, "Error finishing a recording", e)
          return
        }
    upload(audio)
  }

  /** Drops the recording in progress. */
  fun cancelRecording() {
    recorder?.cancel()
  }

  private fun upload(audio: RecordedAudio) {
    val storage = storage ?: return
    // Checking and setting isUpdating in one step, like the other actions.
    val before = _uiState.getAndUpdate {
      if (canRecord(it)) it.copy(isUpdating = true, errorMsg = null) else it
    }
    val task = before.task?.takeIf { canRecord(before) } ?: return
    voiceFlags.update { it.copy(sendFailed = false) }
    viewModelScope.launch {
      var url: String? = null
      try {
        url = storage.upload(task.taskId, audio)
        val updated =
            task.attachVocalNote(VocalNote(newId(), url, audio.duration, now()), currentUser.uid)
        repository.editTask(taskId, updated)
        pendingAudio = null
        audio.file.delete()
        _uiState.update { it.copy(task = updated, isUpdating = false) }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error sending the voice note of task $taskId", e)
        // The file is useless without the note: remove it, the recording is kept for a retry.
        url?.let { deleteFileQuietly(storage, it) }
        pendingAudio = audio
        voiceFlags.update { it.copy(sendFailed = true) }
        _uiState.update { it.copy(isUpdating = false) }
      }
    }
  }

  private suspend fun deleteFileQuietly(storage: VocalNoteStorage, url: String) {
    try {
      storage.delete(url)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(LOG_TAG, "Could not delete the audio file $url", e)
    }
  }

  /** Plays the voice note, or stops it if it is playing. */
  fun togglePlay() {
    val player = player ?: return
    val url = _uiState.value.task?.vocalNote?.audioUrl ?: return
    if ((player.state.value as? PlayerState.Playing)?.audioUrl == url) player.stop()
    else player.play(url)
  }

  /** Tries again what failed last: sending the recording, or else playing the note. */
  fun retryVoiceNote() {
    val audio = pendingAudio
    if (voiceFlags.value.sendFailed && audio != null) upload(audio) else togglePlay()
  }

  /** The owner taps the trash: asks to confirm. Does nothing for the other users. */
  fun requestDeleteVoiceNote() {
    if (canDeleteVoiceNote(_uiState.value)) voiceFlags.update { it.copy(showDeleteConfirm = true) }
  }

  /** The owner keeps the note. */
  fun dismissDeleteVoiceNote() = voiceFlags.update { it.copy(showDeleteConfirm = false) }

  /**
   * Deletes the voice note: removes it from the task, then its audio file. If the save fails the
   * note stays. A file that cannot be deleted is only logged: the note is already gone.
   */
  fun confirmDeleteVoiceNote() {
    voiceFlags.update { it.copy(showDeleteConfirm = false) }
    val storage = storage ?: return
    val before = _uiState.getAndUpdate {
      if (canDeleteVoiceNote(it)) it.copy(isUpdating = true, errorMsg = null) else it
    }
    val task = before.task?.takeIf { canDeleteVoiceNote(before) } ?: return
    val note = task.vocalNote ?: return
    viewModelScope.launch {
      try {
        val updated = task.removeVocalNote(currentUser.uid)
        repository.editTask(taskId, updated)
        if ((player?.state?.value as? PlayerState.Playing)?.audioUrl == note.audioUrl) {
          player?.stop()
        }
        _uiState.update { it.copy(task = updated, isUpdating = false) }
        deleteFileQuietly(storage, note.audioUrl)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error deleting the voice note of task $taskId", e)
        _uiState.update {
          it.copy(isUpdating = false, errorMsg = R.string.task_details_save_failed)
        }
      }
    }
  }

  /**
   * The answer of the microphone permission request, or a check when the user comes back from the
   * settings: refused means the record button is disabled.
   */
  fun onMicPermissionResult(granted: Boolean) = voiceFlags.update { it.copy(micDenied = !granted) }

  override fun onCleared() {
    recorder?.cancel()
    player?.release()
  }

  // editTask overwrites the whole task, so two users acting at once can lose a change:
  // https://github.com/SwEnt-FestivAR/FestivAR/issues/186
  private fun saveIf(allowed: (TaskDetailsUiState) -> Boolean, change: (Task) -> Task) {
    // Checking and setting isUpdating in one step, so two taps cannot both start a save.
    val canRun = { state: TaskDetailsUiState -> allowed(state) && state.task != null }
    val before = _uiState.getAndUpdate {
      if (canRun(it)) it.copy(isUpdating = true, errorMsg = null) else it
    }
    val task = before.task?.takeIf { canRun(before) } ?: return
    viewModelScope.launch {
      try {
        val updated = change(task)
        repository.editTask(taskId, updated)
        _uiState.update { it.copy(task = updated, isUpdating = false) }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error saving task $taskId", e)
        _uiState.update {
          it.copy(isUpdating = false, errorMsg = R.string.task_details_save_failed)
        }
      }
    }
  }
}
