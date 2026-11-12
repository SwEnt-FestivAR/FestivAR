// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.audio.VoiceRecorderException
import com.android.festivar.model.task.Task
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            ownerId,
            services,
        )

  @Test
  fun start_isIgnoredWhenUserCannotManageTheTask() {
    val other =
        VoiceNoteRecording(
            CoroutineScope(Dispatchers.Unconfined),
            host.copy(ownerId = "someone"),
            ownerId,
            services,
        )

    other.start()

    assertEquals(0, recorder.starts)
    assertEquals(RecordingStage.Idle, other.stage.value)
  }

  @Test
  fun startAndFinish_holdsTheTemporaryRecording() {
    val subject = recording

    subject.start()
    subject.finish()

    assertEquals(1, recorder.starts)
    assertTrue(subject.stage.value is RecordingStage.Waiting)
    assertTrue(recorder.lastAudio!!.file.exists())
  }

  @Test
  fun start_isFailedWhenRecorderRejectsIt() {
    recorder.startFailure = VoiceRecorderException("busy")
    val subject = recording

    subject.start()

    assertEquals(RecordingStage.Failed, subject.stage.value)
  }

  @Test
  fun start_ignoresAnIllegalRecorderState() {
    recorder.start()
    val subject = recording

    subject.start()

    assertEquals(1, recorder.starts)
    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  @Test
  fun finish_isIgnoredWithoutAnActiveRecording() {
    val subject = recording

    subject.finish()

    assertEquals(RecordingStage.Idle, subject.stage.value)
  }

  @Test
  fun finish_isFailedWhenRecorderCannotProduceAudio() {
    recorder.start()
    recorder.stopFailure = VoiceRecorderException("too short")
    val subject = recording

    subject.finish()

    assertEquals(RecordingStage.Failed, subject.stage.value)
  }

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

  @Test
  fun confirmSend_isIgnoredWithoutAWaitingRecording() = runTest {
    recording.confirmSend()

    assertTrue(storage.uploads.isEmpty())
    assertNull(host.savedTask)
  }

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

  @Test
  fun cancelledUpload_finishesTheSaveWithoutShowingAnError() = runTest {
    storage.uploadFailure = CancellationException("cancelled")
    val subject = recording
    subject.start()
    subject.finish()

    subject.confirmSend()

    assertTrue(subject.stage.value is RecordingStage.Sending)
    assertFalse(subject.hasFailedSend)
  }

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

  @Test
  fun release_dropsHeldAudioAndCancelsRecorder() {
    val subject = recording
    subject.start()
    subject.finish()
    val file = recorder.lastAudio!!.file

    subject.release()

    assertFalse(file.exists())
    assertEquals(1, recorder.cancelCalls)
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
    var savedTask: Task? = null
    override val state: StateFlow<TaskDetailsUiState> = _state

    override fun startSaving(allowed: (TaskDetailsUiState) -> Boolean): Task? {
      val current = _state.value
      if (current.isUpdating || !allowed(current)) return null
      _state.value = current.copy(isUpdating = true)
      return current.task
    }

    override suspend fun saveTask(updated: Task) {
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
  }

  companion object {
    private val recordedAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC)
  }
}
