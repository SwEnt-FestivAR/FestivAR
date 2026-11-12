// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.util.Log
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.audio.VoiceRecorderException
import com.android.festivar.model.voicenote.VoiceNote
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_TAG = "VoiceNoteRecording"

/**
 * The owner recording a voice note, step by step: they hold the button ([start]) and release it
 * ([finish]); the recording then waits for them to send ([confirmSend]) or drop ([cancelSend]) it.
 * Sending uploads the audio, then saves the task with the note. If either fails, the recording is
 * kept for [retrySend] and a file already uploaded is removed.
 *
 * Where the owner is in these steps is the [stage]: there is no other state.
 *
 * @param scope where the uploads run.
 * @param host the task shown, and the way to save it.
 * @param currentUserId the user recording, who must own the task.
 * @param services what records the audio and keeps it.
 */
internal class VoiceNoteRecording(
    private val scope: CoroutineScope,
    private val host: VoiceNoteHost,
    private val currentUserId: String,
    private val services: VoiceNoteServices,
) {
  private val recorder = services.recorder
  private val _stage = MutableStateFlow<RecordingStage>(RecordingStage.Idle)

  /** What became of the last recording. */
  val stage: StateFlow<RecordingStage> = _stage.asStateFlow()

  /** What the recorder is doing, to show the timer. */
  val recorderState: StateFlow<RecorderState>
    get() = recorder.state

  /** Whether the last recording could not be sent: [retrySend] sends it again. */
  val hasFailedSend: Boolean
    get() = _stage.value is RecordingStage.SendFailed

  /**
   * Starts recording after the caller has requested and received microphone permission. Does
   * nothing unless the user can add a voice note and no recording waits to be sent or is being
   * sent.
   */
  fun start() {
    val busy = _stage.value is RecordingStage.Waiting || _stage.value is RecordingStage.Sending
    val recording = recorder.state.value is RecorderState.Recording
    if (busy || recording || !host.state.value.canAddVoiceNote) return
    drop() // a new recording replaces the one that could not be sent
    try {
      recorder.start()
    } catch (e: VoiceRecorderException) {
      Log.e(LOG_TAG, "Error starting a recording", e)
      _stage.value = RecordingStage.Failed
    } catch (e: IllegalStateException) {
      Log.e(LOG_TAG, "Error starting a recording", e)
      _stage.value = RecordingStage.Failed
    }
  }

  /**
   * Stops recording: the recording then waits for the owner to send it or drop it. A recording that
   * cannot be finished (too short...) is dropped and shown as a failed recording.
   */
  fun finish() {
    if (recorder.state.value !is RecorderState.Recording) return
    val audio =
        try {
          recorder.stop()
        } catch (e: VoiceRecorderException) {
          Log.e(LOG_TAG, "Error finishing a recording", e)
          _stage.value = RecordingStage.Failed
          return
        } catch (e: IllegalStateException) {
          Log.e(LOG_TAG, "Error finishing a recording", e)
          _stage.value = RecordingStage.Failed
          return
        }
    _stage.value = RecordingStage.Waiting(audio)
  }

  /** The owner confirms: uploads the waiting recording and attaches it to the task. */
  fun confirmSend() {
    (_stage.value as? RecordingStage.Waiting)?.let(::send)
  }

  /** The owner cancels: drops the waiting recording and its temporary file. */
  fun cancelSend() {
    if (_stage.value is RecordingStage.Waiting) drop()
  }

  /** Sends again the recording that could not be sent. */
  fun retrySend() {
    (_stage.value as? RecordingStage.SendFailed)?.let(::send)
  }

  /** The owner dismisses the notice of a recording that failed, whoever reported the failure. */
  fun dismissFailure() {
    if (_stage.value is RecordingStage.Failed) _stage.value = RecordingStage.Idle
    if (recorder.state.value is RecorderState.Failed) recorder.cancel()
  }

  /**
   * Frees what the screen no longer needs when its lifecycle ends.
   *
   * A waiting or failed recording is dropped immediately. A send in progress is left alone so its
   * upload can finish without the temporary file being deleted underneath it; cancellation of the
   * send cleans up both the uploaded file and the local file.
   */
  fun releaseResources() {
    if (_stage.value !is RecordingStage.Sending) drop()
    recorder.cancel()
  }

  private fun send(held: RecordingStage.Held) {
    val task = host.startSaving { it.canAddVoiceNote } ?: return
    _stage.value = RecordingStage.Sending(held.audio)
    scope.launch {
      var url: String? = null
      try {
        url = services.storage.upload(held.audio, task.taskId)
        val note = VoiceNote(services.newId(), url, held.audio.duration, services.now())
        val updated = task.attachVoiceNote(note, currentUserId)
        host.saveTask(updated)
        drop()
        host.finishSaving(updated)
      } catch (e: CancellationException) {
        url?.let { uploadedUrl ->
          withContext(NonCancellable) { services.deleteAudioQuietly(uploadedUrl) }
        }
        drop()
        host.finishSaving(null)
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error sending the voice note of task ${task.taskId}", e)
        // The file is useless without the note: remove it, the recording is kept for another try.
        url?.let { services.deleteAudioQuietly(it) }
        _stage.value = RecordingStage.SendFailed(held.audio)
        host.finishSaving(null)
      }
    }
  }

  /** Gives up the recording that is held: its temporary file is no longer needed. */
  private fun drop() {
    (_stage.value as? RecordingStage.Held)?.audio?.file?.delete()
    _stage.value = RecordingStage.Idle
  }
}
