// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.audio.VoiceRecorderException
import com.android.festivar.model.task.Task
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNoteRecordingTest {
  private val ownerId = "owner"
  private val task = Task(taskId = "task", eventId = "event", title = "Task", ownerId = ownerId)
  private val host = RecordingHost(task)
  private val recorder = FakeVoiceRecorder()
  private val storage = FakeAudioRepository()
  private val player = FakeVoicePlayer()
  private val services =
      VoiceNoteServices(
          recorder,
          storage,
          player,
          newId = { "note-id" },
          now = { recordedAt },
      )
  private val scope =
      CoroutineScope(
          Dispatchers.Unconfined + CoroutineExceptionHandler { _, _ -> },
      )
  private val recording
    get() =
        VoiceNoteRecording(
            scope,
            host,
            services,
        )

  /** Breaks if recording starts for a user who cannot manage the task. */
  @Test
  fun start_isIgnoredWhenUserCannotManageTheTask() {
    val other =
        VoiceNoteRecording(
            CoroutineScope(Dispatchers.Unconfined),
            host.copy(ownerId = "someone"),
            services,
        )

    other.start()

    assertEquals(0, recorder.starts)
    assertEquals(RecordingStage.Idle, other.stage.value)
  }

  /** Breaks if a completed recording is not held for confirmation. */
  @Test
  fun startAndFinish_holdsTheTemporaryRecording() {
    val subject = recording

    subject.start()
    subject.finish()

    assertEquals(1, recorder.starts)
    assertTrue(subject.stage.value is RecordingStage.Waiting)
    assertTrue(recorder.lastAudio!!.file.exists())
  }

  /** Breaks if a recorder failure is not exposed as a failed stage. */
  @Test
  fun start_isFailedWhenRecorderRejectsIt() {
    recorder.startFailure = VoiceRecorderException("busy")
    val subject = recording

    subject.start()

    assertEquals(RecordingStage.Failed, subject.stage.value)
  }

  /** Breaks if an illegal recorder state is silently ignored while starting. */
  @Test
  fun start_isFailedWhenRecorderThrowsIllegalStateException() {
    recorder.startFailure = IllegalStateException("recorder unavailable")
    val subject = recording

    subject.start()

    assertEquals(RecordingStage.Failed, subject.stage.value)
  }

  /** Breaks if a failed replacement start drops the recording that could still be retried. */
  @Test
  fun start_preservesAnUnsentRecordingWhenRecorderRejectsIt() = runTest {
    storage.uploadFailure = IOException("offline")
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file
    subject.confirmSend()
    testScheduler.advanceUntilIdle()
    assertTrue(subject.stage.value is RecordingStage.SendFailed)

    recorder.startFailure = IllegalStateException("recorder unavailable")
    subject.start()

    assertTrue(file.exists())
    assertTrue(subject.stage.value is RecordingStage.SendFailed)

    recorder.startFailure = null
    storage.uploadFailure = null
    subject.retrySend()
    testScheduler.advanceUntilIdle()

    assertEquals(1, storage.uploads.size)
    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertFalse(file.exists())
  }

  /** Breaks if a recorder failure drops a sendable recording instead of preserving its retry. */
  @Test
  fun start_preservesAnUnsentRecordingWhenRecorderFails() = runTest {
    storage.uploadFailure = IOException("offline")
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file
    subject.confirmSend()
    testScheduler.advanceUntilIdle()
    assertTrue(subject.stage.value is RecordingStage.SendFailed)

    recorder.startFailure = VoiceRecorderException("microphone busy")
    subject.start()

    assertTrue(file.exists())
    assertTrue(subject.stage.value is RecordingStage.SendFailed)

    recorder.startFailure = null
    storage.uploadFailure = null
    subject.retrySend()
    testScheduler.advanceUntilIdle()

    assertEquals(1, storage.uploads.size)
    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertFalse(file.exists())
  }

  /** Breaks if starting replaces a recording that is waiting for confirmation. */
  @Test
  fun start_isIgnoredWhileWaitingToSend() {
    val subject = recording
    subject.start()
    subject.finish()

    subject.start()

    assertEquals(1, recorder.starts)
    assertTrue(subject.stage.value is RecordingStage.Waiting)
  }

  /** Breaks if starting replaces a recording that is being sent. */
  @Test
  fun start_isIgnoredWhileSending() = runTest {
    val gate = CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val subject = recording
    subject.start()
    subject.finish()
    subject.confirmSend()

    subject.start()

    assertEquals(1, recorder.starts)
    assertTrue(subject.stage.value is RecordingStage.Sending)
    gate.complete(Unit)
  }

  /** Breaks if starting while the recorder is active calls the recorder twice. */
  @Test
  fun start_isIgnoredWhileRecorderIsAlreadyRecording() {
    recorder.start()
    val subject = recording

    subject.start()

    assertEquals(1, recorder.starts)
    assertEquals(RecorderState.Recording(Duration.ZERO), recorder.state.value)
  }

  /** Breaks if finishing without an active recording changes the workflow state. */
  @Test
  fun finish_isIgnoredWithoutAnActiveRecording() {
    val subject = recording

    subject.finish()

    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  /** Breaks if an error producing audio is not exposed as a failed stage. */
  @Test
  fun finish_isFailedWhenRecorderCannotProduceAudio() {
    recorder.start()
    recorder.stopFailure = VoiceRecorderException("too short")
    val subject = recording

    subject.finish()

    assertEquals(RecordingStage.Failed, subject.stage.value)
  }

  /** Breaks if an illegal recorder state is silently ignored while finishing. */
  @Test
  fun finish_isFailedWhenRecorderThrowsIllegalStateException() {
    recorder.start()
    recorder.stopFailure = IllegalStateException("recorder unavailable")
    val subject = recording

    subject.finish()

    assertEquals(RecordingStage.Failed, subject.stage.value)
  }

  /** Breaks if cancelling leaves the temporary recording file behind. */
  @Test
  fun cancelSend_deletesTheWaitingTemporaryFile() {
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file

    subject.cancelSend()

    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertFalse(file.exists())
  }

  /** Breaks if a temporary file that cannot be deleted keeps the recording from being dropped. */
  @Test
  fun cancelSend_dropsTheRecordingWhenItsFileCannotBeDeleted() {
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file
    // A non-empty directory in place of the file makes File.delete() return false
    assertTrue(file.delete())
    assertTrue(file.mkdir())
    assertTrue(file.resolve("child").createNewFile())

    subject.cancelSend()

    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertTrue(file.exists())
    file.deleteRecursively()
  }

  /** Breaks if dropping a recording whose file is already gone still returns to idle. */
  @Test
  fun cancelSend_dropsTheRecordingWhenItsFileIsAlreadyGone() {
    val subject = recording
    subject.start()
    subject.finish()
    assertTrue(recorder.lastAudio!!.file.delete())

    subject.cancelSend()

    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  /** Breaks if cancelling outside the waiting stage changes the recording workflow. */
  @Test
  fun cancelSend_isIgnoredWithoutAWaitingRecording() {
    val subject = recording

    subject.cancelSend()

    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  /** Breaks if cancelling a send in progress drops its recording before the upload finishes. */
  @Test
  fun cancelSend_isIgnoredWhileSending() = runTest {
    val gate = CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file
    subject.confirmSend()

    subject.cancelSend()

    assertTrue(subject.stage.value is RecordingStage.Sending)
    assertTrue(file.exists())
    gate.complete(Unit)
    testScheduler.advanceUntilIdle()
  }

  /** Breaks if cancelling a failed send drops its recording instead of preserving retry. */
  @Test
  fun cancelSend_isIgnoredAfterASendFailure() = runTest {
    storage.uploadFailure = IOException("offline")
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file
    subject.confirmSend()
    testScheduler.advanceUntilIdle()

    subject.cancelSend()

    assertTrue(subject.stage.value is RecordingStage.SendFailed)
    assertTrue(file.exists())
  }

  /** Breaks if confirming a recording does not upload and save its voice note. */
  @Test
  fun confirmSend_uploadsAndSavesTheVoiceNote() = runTest {
    val subject = recording
    subject.start()
    subject.finish()

    subject.confirmSend()

    assertEquals(listOf("task"), storage.uploads)
    assertEquals("note-id", host.savedTask!!.voiceNote!!.id)
    assertEquals(recordedAt, host.savedTask!!.voiceNote!!.createdAt)
    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  /** Breaks if confirming without a waiting recording starts an upload or save. */
  @Test
  fun confirmSend_isIgnoredWithoutAWaitingRecording() = runTest {
    recording.confirmSend()

    assertTrue(storage.uploads.isEmpty())
    assertNull(host.savedTask)
  }

  /** Breaks if sending bypasses the screen save gate and starts a concurrent upload. */
  @Test
  fun confirmSend_isIgnoredWhileTheScreenIsSaving() = runTest {
    val subject = recording
    subject.start()
    subject.finish()
    host.blockSaving()

    subject.confirmSend()

    assertTrue(storage.uploads.isEmpty())
    assertTrue(subject.stage.value is RecordingStage.Waiting)
    assertTrue(host.state.value.isUpdating)
  }

  /** Breaks if retrying from idle starts an upload without a failed recording. */
  @Test
  fun retrySend_isIgnoredFromIdle() = runTest {
    val subject = recording

    subject.retrySend()

    assertTrue(storage.uploads.isEmpty())
    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  /** Breaks if retrying a waiting recording bypasses the send-stage guard. */
  @Test
  fun retrySend_isIgnoredFromWaiting() = runTest {
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file

    subject.retrySend()

    assertTrue(storage.uploads.isEmpty())
    assertTrue(subject.stage.value is RecordingStage.Waiting)
    assertTrue(file.exists())
  }

  /** Breaks if an in-flight upload loses its recording before it finishes. */
  @Test
  fun sending_keepsTheRecordingUntilTheUploadFinishes() = runTest {
    val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val subject = recording
    subject.start()
    subject.finish()

    subject.confirmSend()

    assertTrue(subject.stage.value is RecordingStage.Sending)
    gate.complete(Unit)
    testScheduler.advanceUntilIdle()
    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  /** Breaks if a failed upload cannot be retried with the held recording. */
  @Test
  fun failedUpload_keepsTheRecordingAndRetrySendsIt() = runTest {
    storage.uploadFailure = IOException("offline")
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file

    subject.confirmSend()
    testScheduler.advanceUntilIdle()

    assertTrue(subject.hasFailedSend)
    assertTrue(file.exists())
    storage.uploadFailure = null
    subject.retrySend()
    testScheduler.advanceUntilIdle()

    assertEquals(1, storage.uploads.size)
    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertFalse(file.exists())
  }

  /** Breaks if a failed save leaves uploaded audio or prevents retrying. */
  @Test
  fun failedSave_deletesUploadedAudioAndAllowsRetry() = runTest {
    host.saveFailure = IOException("offline")
    val subject = recording
    subject.start()
    subject.finish()

    subject.confirmSend()
    testScheduler.advanceUntilIdle()

    assertTrue(subject.hasFailedSend)
    assertEquals(listOf("https://fake/task/1.m4a"), storage.deletions)
    host.saveFailure = null
    subject.retrySend()
    testScheduler.advanceUntilIdle()
    assertEquals(2, storage.uploads.size)
  }

  /** Breaks if cancellation leaves the screen saving, reports an error, or leaks uploaded audio. */
  @Test
  fun cancelledUpload_finishesTheSaveWithoutShowingAnError() = runTest {
    host.saveFailure = CancellationException("cancelled")
    val subject = recording
    subject.start()
    subject.finish()

    subject.confirmSend()

    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertFalse(subject.hasFailedSend)
    assertFalse(host.state.value.isUpdating)
    assertEquals(listOf("https://fake/task/1.m4a"), storage.deletions)
  }

  /** Breaks if cancelling the send job leaves uploaded audio or the screen save gate active. */
  @Test
  fun cancellingTheSendJob_cleansUpAndFinishesSaving() = runTest {
    val saveGate = CompletableDeferred<Unit>()
    host.saveGate = saveGate
    val subject = recording
    subject.start()
    subject.finish()

    subject.confirmSend()
    assertTrue(subject.stage.value is RecordingStage.Sending)
    scope.cancel()

    assertEquals(RecordingStage.Idle, subject.stage.value)
    assertFalse(host.state.value.isUpdating)
    assertEquals(listOf("https://fake/task/1.m4a"), storage.deletions)
  }

  /** Breaks if a new recording cannot replace an unsent failed recording. */
  @Test
  fun newRecording_dropsAnUnsentRecording() {
    storage.uploadFailure = IOException("offline")
    val subject = recording
    subject.start()
    subject.finish()
    val oldFile = recorder.lastAudio!!.file
    runTest { subject.confirmSend() }
    assertTrue(oldFile.exists())

    storage.uploadFailure = null
    subject.start()

    assertFalse(oldFile.exists())
    assertEquals(RecorderState.Recording(Duration.ZERO), recorder.state.value)
  }

  /** Breaks if dismissing a failure leaves the recording or recorder failed. */
  @Test
  fun dismissFailure_clearsRecordingAndRecorderFailure() {
    recorder.startFailure = VoiceRecorderException("busy")
    val subject = recording
    subject.start()
    subject.dismissFailure()
    assertEquals(RecordingStage.Idle, subject.stage.value)

    recorder.startFailure = null
    recorder.start()
    recorder.fail()
    subject.dismissFailure()

    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  /** Breaks if releasing screen resources leaves held audio or the recorder active. */
  @Test
  fun releaseResources_dropsHeldAudioAndCancelsRecorder() {
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file

    subject.releaseResources()

    assertFalse(file.exists())
    assertEquals(1, recorder.cancelCalls)
  }

  /** Breaks if releasing during a send deletes the file before the upload finishes. */
  @Test
  fun releaseResources_keepsAudioWhileSendingUntilTheUploadFinishes() = runTest {
    val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file

    subject.confirmSend()
    subject.releaseResources()

    assertTrue(file.exists())
    gate.complete(Unit)
    testScheduler.advanceUntilIdle()
    assertFalse(file.exists())
  }

  private class RecordingHost(
      initialTask: Task,
      currentUserId: String = initialTask.ownerId,
  ) : VoiceNoteHost {
    private val _state =
        MutableStateFlow(
            TaskDetailsUiState(
                currentUserId = currentUserId,
                task = initialTask,
                isLoading = false,
            ),
        )
    var saveFailure: Exception? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var savedTask: Task? = null
    override val state: StateFlow<TaskDetailsUiState> = _state

    override fun startSaving(allowed: (TaskDetailsUiState) -> Boolean): Task? {
      val current = _state.value
      if (current.isUpdating || !allowed(current)) return null
      _state.value = current.copy(isUpdating = true)
      return current.task
    }

    override suspend fun saveTask(updated: Task) {
      saveGate?.await()
      saveFailure?.let { throw it }
      savedTask = updated
    }

    override fun finishSaving(updated: Task?, failed: Boolean) {
      _state.value =
          _state.value.copy(
              task = updated ?: _state.value.task,
              isUpdating = false,
          )
    }

    fun copy(ownerId: String): RecordingHost =
        RecordingHost(_state.value.task!!.copy(ownerId = ownerId), _state.value.currentUserId)

    fun blockSaving() {
      _state.value = _state.value.copy(isUpdating = true)
    }
  }

  companion object {
    private val recordedAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC)
  }
}
