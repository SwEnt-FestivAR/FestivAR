// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The voice note of a task, for the Task details screen: everybody can play the note, and only the
 * task owner can record, send, retry and delete it. It gives the [VoiceNoteUiState] of the section
 * ([uiState]) and the actions behind it ([callbacks]), and leaves the work to a class for each of
 * them: [VoiceNoteRecording], [VoiceNotePlayback] and [VoiceNoteDeletion].
 *
 * Without [services] nobody can record or delete the note and it cannot be played: the section is
 * read-only.
 *
 * @param scope where [uiState] is shared and the work runs.
 * @param services what records, keeps and plays the voice note.
 */
class VoiceNoteController
internal constructor(
    scope: CoroutineScope,
    private val host: VoiceNoteHost,
    private val services: VoiceNoteServices?,
) {
  private val recording = services?.let {
    VoiceNoteRecording(scope, host, host.state.value.currentUserId, it)
  }
  private val playback = services?.let {
    VoiceNotePlayback(it.player) { host.state.value.task?.voiceNote?.audioUrl }
  }
  private val deletion = services?.let {
    VoiceNoteDeletion(scope, host, it) { url -> playback?.stop(url) }
  }

  /** The parts of the section that come from neither the task nor what the services do. */
  private data class Flags(val micDenied: Boolean = false, val showDeleteConfirm: Boolean = false)

  private val flags = MutableStateFlow(Flags())

  /** What the voice note section shows, derived from the task, the recording and the player. */
  val uiState: StateFlow<VoiceNoteUiState> =
      combine(
              host.state,
              recording?.stage ?: flowOf(RecordingStage.Idle),
              recording?.recorderState ?: flowOf(RecorderState.Idle),
              playback?.state ?: flowOf(PlayerState.Idle),
              flags,
          ) { state, stage, recorderState, playerState, current ->
            VoiceNoteUiState(
                note = state.task?.voiceNote,
                canManage = services != null && state.canManageVoiceNote,
                recorder =
                    if (stage is RecordingStage.Failed) RecorderState.Failed else recorderState,
                player = playerState,
                sendFailed = stage is RecordingStage.SendFailed,
                micDenied = current.micDenied,
                showDeleteConfirm = current.showDeleteConfirm,
                pending = stage.pending,
            )
          }
          .stateIn(scope, SharingStarted.Eagerly, VoiceNoteUiState())

  /**
   * The actions of the section. The screen gives what only it can do: check the microphone
   * permission ([hasMicrophone]), ask for it ([requestMicrophone]) and open the settings of the app
   * ([onOpenSettings]). The record button asks for the permission instead of recording while it is
   * not granted.
   */
  fun callbacks(
      hasMicrophone: () -> Boolean,
      requestMicrophone: () -> Unit,
      onOpenSettings: () -> Unit,
  ) =
      VoiceNoteCallbacks(
          onTogglePlay = ::togglePlay,
          onDelete = ::requestDelete,
          onConfirmDelete = ::confirmDelete,
          onDismissDelete = ::dismissDelete,
          onRecordStart = { if (hasMicrophone()) startRecording() else requestMicrophone() },
          onRecordRelease = ::finishRecording,
          onConfirmSend = ::confirmSend,
          onCancelSend = ::cancelSend,
          onDismissRecordingFailed = ::dismissRecordingFailed,
          onRetry = ::retry,
          onOpenSettings = onOpenSettings,
      )

  /**
   * Starts recording the voice note, once the microphone permission is granted (see [callbacks]).
   */
  fun startRecording() {
    recording?.start()
  }

  /** Stops recording: the recording then waits for the owner to send it or drop it. */
  fun finishRecording() {
    recording?.finish()
  }

  /** The owner confirms: uploads the waiting recording and attaches it to the task. */
  fun confirmSend() {
    recording?.confirmSend()
  }

  /** The owner cancels: drops the waiting recording and its temporary file. */
  fun cancelSend() {
    recording?.cancelSend()
  }

  /** The owner dismisses the notice of a recording that failed. */
  fun dismissRecordingFailed() {
    recording?.dismissFailure()
  }

  /** Tries again what failed last: sending the recording, or else playing the note. */
  fun retry() {
    if (recording?.hasFailedSend == true) recording.retrySend() else playback?.toggle()
  }

  /** Plays the voice note, or stops it if it is playing. */
  fun togglePlay() {
    playback?.toggle()
  }

  /** The owner taps the trash: asks to confirm. Does nothing for the other users. */
  fun requestDelete() {
    if (deletion != null && host.state.value.canDeleteVoiceNote) {
      flags.update { it.copy(showDeleteConfirm = true) }
    }
  }

  /** The owner keeps the note. */
  fun dismissDelete() = flags.update { it.copy(showDeleteConfirm = false) }

  /** The owner confirms: deletes the note and its audio file. */
  fun confirmDelete() {
    dismissDelete()
    deletion?.delete()
  }

  /**
   * The answer of the microphone permission request, or a check when the user comes back from the
   * settings: refused means the record button is disabled.
   */
  fun onMicPermissionResult(granted: Boolean) = flags.update { it.copy(micDenied = !granted) }

  /** Frees what the screen no longer needs: the held recording, the recorder and the player. */
  fun release() {
    recording?.releaseResources()
    playback?.release()
  }
}
