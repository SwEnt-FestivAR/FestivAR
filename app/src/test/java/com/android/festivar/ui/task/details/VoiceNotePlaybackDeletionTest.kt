// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.audio.VoiceRecorderException
import com.android.festivar.model.task.Task
import com.android.festivar.model.voicenote.VoiceNote
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNotePlaybackDeletionTest {
  private val ownerId = "owner"
  private val note =
      VoiceNote(
          id = "note",
          audioUrl = "https://fake/task/note.m4a",
          duration = Duration.ofSeconds(4),
          createdAt = recordedAt,
      )
  private val task =
      Task(
          taskId = "task",
          eventId = "event",
          title = "Task",
          voiceNote = note,
          ownerId = ownerId,
      )
  private val host = PlaybackHost(task)
  private val recorder = FakeVoiceRecorder()
  private val storage = FakeAudioRepository()
  private val player = FakeVoicePlayer()
  private val services = VoiceNoteServices(recorder, storage, player)
  private val scope =
      CoroutineScope(
          Dispatchers.Unconfined + CoroutineExceptionHandler { _, _ -> },
      )

  /** Breaks if playback cannot start or stop the current note. */
  @Test
  fun playback_togglePlaysThenStopsTheCurrentNote() {
    val subject = VoiceNotePlayback(player) { note.audioUrl }

    subject.toggle()
    assertEquals(listOf(note.audioUrl), player.played)
    assertEquals(PlayerState.Playing(note.audioUrl), subject.state.value)

    subject.toggle()

    assertEquals(PlayerState.Idle, subject.state.value)
    assertEquals(1, player.played.size)
  }

  /** Breaks if playback tries to play when the task has no note. */
  @Test
  fun playback_toggleDoesNothingWithoutANote() {
    val subject = VoiceNotePlayback(player) { null }

    subject.toggle()

    assertTrue(player.played.isEmpty())
    assertEquals(PlayerState.Idle, subject.state.value)
  }

  /** Breaks if stopping ignores paused notes or stops a different note. */
  @Test
  fun playback_stopStopsPlayingAndPausedMatchingNotesOnly() {
    val subject = VoiceNotePlayback(player) { note.audioUrl }
    subject.toggle()
    subject.stop("other")
    assertEquals(PlayerState.Playing(note.audioUrl), subject.state.value)

    subject.stop(note.audioUrl)
    assertEquals(PlayerState.Idle, subject.state.value)
    subject.toggle()
    player.pause()

    subject.stop(note.audioUrl)

    assertEquals(PlayerState.Idle, subject.state.value)
  }

  /** Breaks if releasing playback does not release the player. */
  @Test
  fun playback_releaseReleasesThePlayer() {
    VoiceNotePlayback(player) { note.audioUrl }.release()

    assertTrue(player.released)
  }

  /** Breaks if a non-owner deletion can save or delete the note. */
  @Test
  fun deletion_isIgnoredWhenUserCannotDeleteTheNote() {
    val nonOwnerHost = PlaybackHost(task, currentUserId = "someone")
    val subject =
        VoiceNoteDeletion(
            scope,
            nonOwnerHost,
            services,
        ) {}

    subject.delete()

    assertNull(nonOwnerHost.savedTask)
    assertTrue(storage.deletions.isEmpty())
    assertFalse(nonOwnerHost.finishedWithFailure)
  }

  /** Breaks if deletion does not stop playback before removing remote audio. */
  @Test
  fun deletion_savesTheTaskStopsPlaybackAndDeletesAudio() {
    val calls = mutableListOf<String>()
    storage.onDelete = { calls += "delete" }
    val subject = VoiceNoteDeletion(scope, host, services) { url -> calls += "stop:$url" }

    subject.delete()

    assertNull(host.savedTask!!.voiceNote)
    assertEquals(listOf("stop:${note.audioUrl}", "delete"), calls)
    assertEquals(listOf(note.audioUrl), storage.deletions)
    assertFalse(host.finishedWithFailure)
  }

  /** Breaks if a storage failure makes an already-saved deletion fail. */
  @Test
  fun deletion_survivesAStorageFailureAfterSavingTheTask() {
    storage.deleteFailure = IOException("offline")
    val subject = VoiceNoteDeletion(scope, host, services) {}

    subject.delete()

    assertNull(host.savedTask!!.voiceNote)
    assertFalse(host.finishedWithFailure)
  }

  /** Breaks if leaving the screen cancels remote audio cleanup. */
  @Test
  fun deletion_cancellationDuringStorageCleanupStillDeletesAudio() {
    val deleteGate = kotlinx.coroutines.CompletableDeferred<Unit>()
    storage.deleteGate = deleteGate
    val deletionJob = Job()
    val deletionScope =
        CoroutineScope(
            Dispatchers.Unconfined + deletionJob + CoroutineExceptionHandler { _, _ -> },
        )
    val subject = VoiceNoteDeletion(deletionScope, host, services) {}

    subject.delete()
    deletionJob.cancel()
    deleteGate.complete(Unit)

    assertEquals(listOf(note.audioUrl), storage.deletions)
    assertFalse(host.finishedWithFailure)
  }

  /** Breaks if cancellation during cleanup reports a deletion error. */
  @Test
  fun deletion_cancellationFinishesWithoutShowingADeletionError() {
    storage.deleteFailure = CancellationException("cancelled")
    val subject = VoiceNoteDeletion(scope, host, services) {}

    subject.delete()

    assertFalse(host.finishedWithFailure)
    assertEquals(listOf(note.audioUrl), storage.deletions)
  }

  /** Breaks if a task-save failure removes the note or audio anyway. */
  @Test
  fun deletion_keepsTheNoteWhenSavingFails() {
    host.saveFailure = IOException("offline")
    var stopped = false
    val subject = VoiceNoteDeletion(scope, host, services) { stopped = true }

    subject.delete()

    assertNull(host.savedTask)
    assertFalse(stopped)
    assertTrue(host.finishedWithFailure)
    assertTrue(storage.deletions.isEmpty())
  }

  /** Breaks if callbacks expose recording or deletion controls without services. */
  @Test
  fun controller_exposesReadOnlyStateWithoutServices() {
    val controller = VoiceNoteController(scope, host, null)
    var requested = false
    val callbacks =
        controller.callbacks(
            hasMicrophone = { false },
            requestMicrophone = { requested = true },
            onOpenSettings = {},
        )

    callbacks.onRecordStart()
    controller.requestDelete()

    assertTrue(requested)
    assertFalse(controller.uiState.value.canManage)
    assertFalse(controller.uiState.value.showDeleteConfirm)
  }

  /** Breaks if a non-owner can request or confirm voice-note deletion. */
  @Test
  fun controller_blocksNonOwnerDeletion() {
    val nonOwnerHost = PlaybackHost(task, currentUserId = "volunteer")
    val controller = VoiceNoteController(scope, nonOwnerHost, services)

    controller.requestDelete()
    controller.confirmDelete()

    assertFalse(controller.uiState.value.showDeleteConfirm)
    assertNull(nonOwnerHost.savedTask)
    assertTrue(storage.deletions.isEmpty())
    assertFalse(nonOwnerHost.finishedWithFailure)
  }

  /** Breaks if the controller does not route playback, retry, and deletion callbacks. */
  @Test
  fun controller_routesPlaybackRetryAndDeleteCallbacks() {
    val controller = VoiceNoteController(scope, host, services)
    var settingsOpened = false
    val callbacks =
        controller.callbacks(
            hasMicrophone = { true },
            requestMicrophone = {},
            onOpenSettings = { settingsOpened = true },
        )

    callbacks.onTogglePlay()
    assertTrue(controller.uiState.value.isPlaying)
    player.fail(note.audioUrl)
    callbacks.onRetry()
    assertTrue(controller.uiState.value.isPlaying)
    callbacks.onOpenSettings()
    assertTrue(settingsOpened)

    callbacks.onDelete()
    assertTrue(controller.uiState.value.showDeleteConfirm)
    callbacks.onDismissDelete()
    assertFalse(controller.uiState.value.showDeleteConfirm)
    callbacks.onDelete()
    callbacks.onConfirmDelete()

    assertNull(host.savedTask!!.voiceNote)
    assertFalse(controller.uiState.value.isPlaying)
    assertEquals(PlayerState.Idle, player.state.value)
    assertFalse(controller.uiState.value.showDeleteConfirm)
  }

  /** Breaks if permission state or resource release is not routed by the controller. */
  @Test
  fun controller_routesMicrophonePermissionAndRelease() {
    val controller = VoiceNoteController(scope, PlaybackHost(task.copy(voiceNote = null)), services)
    var asked = false
    val callbacks =
        controller.callbacks(
            hasMicrophone = { false },
            requestMicrophone = { asked = true },
            onOpenSettings = {},
        )

    callbacks.onRecordStart()
    assertTrue(asked)
    controller.startRecording()
    controller.onMicPermissionResult(false)
    assertTrue(controller.uiState.value.micDenied)
    controller.onMicPermissionResult(true)
    assertFalse(controller.uiState.value.micDenied)
    controller.release()

    assertFalse(controller.uiState.value.showSendDialog)
    assertEquals(1, recorder.cancels)
    assertTrue(player.released)
  }

  /** Breaks if recording cancellation and send retry are not routed by the controller. */
  @Test
  fun controller_routesRecordingCancellationAndSendRetry() {
    val emptyHost = PlaybackHost(task.copy(voiceNote = null))
    val controller = VoiceNoteController(scope, emptyHost, services)

    controller.startRecording()
    controller.finishRecording()
    assertTrue(controller.uiState.value.showSendDialog)
    controller.cancelSend()
    assertFalse(controller.uiState.value.showSendDialog)

    storage.uploadFailure = IOException("offline")
    controller.startRecording()
    controller.finishRecording()
    controller.confirmSend()
    assertTrue(controller.uiState.value.sendFailed)

    storage.uploadFailure = null
    controller.retry()

    assertFalse(controller.uiState.value.sendFailed)
    assertEquals(1, storage.uploads.size)
  }

  /** Breaks if callbacks cannot complete recording or dismiss recorder failures. */
  @Test
  fun controller_callbacksRouteRecordingSuccessAndFailureDismissal() {
    val emptyHost = PlaybackHost(task.copy(voiceNote = null))
    val controller = VoiceNoteController(scope, emptyHost, services)
    val callbacks =
        controller.callbacks(
            hasMicrophone = { true },
            requestMicrophone = {},
            onOpenSettings = {},
        )

    callbacks.onRecordStart()
    assertEquals(1, recorder.starts)
    callbacks.onRecordRelease()
    callbacks.onConfirmSend()
    assertFalse(controller.uiState.value.showSendDialog)
    assertNotNull(emptyHost.savedTask!!.voiceNote)

    recorder.startFailure = VoiceRecorderException("microphone unavailable")
    val failureController =
        VoiceNoteController(scope, PlaybackHost(task.copy(voiceNote = null)), services)
    val failureCallbacks =
        failureController.callbacks(
            hasMicrophone = { true },
            requestMicrophone = {},
            onOpenSettings = {},
        )
    failureCallbacks.onRecordStart()
    assertEquals(RecorderState.Failed, failureController.uiState.value.recorder)
    failureCallbacks.onDismissRecordingFailed()
    assertEquals(RecorderState.Idle, failureController.uiState.value.recorder)
  }

  private class PlaybackHost(
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
    var finishedWithFailure = false
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
      finishedWithFailure = failed
      _state.value =
          _state.value.copy(
              task = updated ?: _state.value.task,
              isUpdating = false,
          )
    }
  }

  companion object {
    private val recordedAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC)
  }
}
