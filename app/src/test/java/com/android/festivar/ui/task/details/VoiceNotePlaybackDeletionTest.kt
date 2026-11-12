// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.task.Task
import com.android.festivar.model.voicenote.VoiceNote
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
          Dispatchers.Unconfined +
              CoroutineExceptionHandler { _, _ -> },
      )

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

  @Test
  fun playback_toggleDoesNothingWithoutANote() {
    val subject = VoiceNotePlayback(player) { null }

    subject.toggle()

    assertTrue(player.played.isEmpty())
    assertEquals(PlayerState.Idle, subject.state.value)
  }

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

  @Test
  fun playback_releaseReleasesThePlayer() {
    VoiceNotePlayback(player) { note.audioUrl }.release()

    assertTrue(player.released)
  }

  @Test
  fun deletion_isIgnoredWhenUserCannotDeleteTheNote() {
    val subject =
        VoiceNoteDeletion(
            scope,
            PlaybackHost(task.copy(ownerId = "someone")),
            ownerId,
            services,
        ) {}

    subject.delete()

    assertNull(host.savedTask)
    assertTrue(storage.deletions.isEmpty())
  }

  @Test
  fun deletion_savesTheTaskStopsPlaybackAndDeletesAudio() {
    var stopped: String? = null
    val subject = VoiceNoteDeletion(scope, host, ownerId, services) { stopped = it }

    subject.delete()

    assertNull(host.savedTask!!.voiceNote)
    assertEquals(note.audioUrl, stopped)
    assertEquals(listOf(note.audioUrl), storage.deletions)
    assertFalse(host.finishedWithFailure)
  }

  @Test
  fun deletion_survivesAStorageFailureAfterSavingTheTask() {
    storage.deleteFailure = IOException("offline")
    val subject = VoiceNoteDeletion(scope, host, ownerId, services) {}

    subject.delete()

    assertNull(host.savedTask!!.voiceNote)
    assertFalse(host.finishedWithFailure)
  }

  @Test
  fun deletion_cancellationFinishesWithoutShowingADeletionError() {
    storage.deleteFailure = CancellationException("cancelled")
    val subject = VoiceNoteDeletion(scope, host, ownerId, services) {}

    subject.delete()

    assertFalse(host.finishedWithFailure)
    assertEquals(listOf(note.audioUrl), storage.deletions)
  }

  @Test
  fun deletion_keepsTheNoteWhenSavingFails() {
    host.saveFailure = IOException("offline")
    var stopped = false
    val subject = VoiceNoteDeletion(scope, host, ownerId, services) { stopped = true }

    subject.delete()

    assertNull(host.savedTask)
    assertFalse(stopped)
    assertTrue(host.finishedWithFailure)
    assertTrue(storage.deletions.isEmpty())
  }

  @Test
  fun deletion_reportsFailureWhenTheTaskHasNoNote() {
    val emptyHost = PlaybackHost(task.copy(voiceNote = null))
    emptyHost.allowDeleteWithoutNote = true
    val subject = VoiceNoteDeletion(scope, emptyHost, ownerId, services) {}

    subject.delete()

    assertTrue(emptyHost.finishedWithFailure)
    assertNull(emptyHost.savedTask)
  }

  @Test
  fun controller_exposesReadOnlyStateWithoutServices() {
    val controller = VoiceNoteController(scope, host, "volunteer", null)
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

  @Test
  fun controller_routesPlaybackRetryAndDeleteCallbacks() {
    val controller = VoiceNoteController(scope, host, ownerId, services)
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
    assertFalse(controller.uiState.value.showDeleteConfirm)
  }

  @Test
  fun controller_routesMicrophonePermissionAndRelease() {
    val controller = VoiceNoteController(scope, host, ownerId, services)
    var asked = false
    val callbacks =
        controller.callbacks(
            hasMicrophone = { false },
            requestMicrophone = { asked = true },
            onOpenSettings = {},
        )

    callbacks.onRecordStart()
    assertTrue(asked)
    controller.onMicPermissionResult(false)
    assertTrue(controller.uiState.value.micDenied)
    controller.onMicPermissionResult(true)
    assertFalse(controller.uiState.value.micDenied)
    controller.release()

    assertTrue(player.released)
  }

  @Test
  fun controller_routesRecordingCancellationAndSendRetry() {
    val emptyHost = PlaybackHost(task.copy(voiceNote = null))
    val controller = VoiceNoteController(scope, emptyHost, ownerId, services)

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

  private class PlaybackHost(initialTask: Task) : VoiceNoteHost {
    private val _state =
        MutableStateFlow(
            TaskDetailsUiState(
                currentUserId = initialTask.ownerId,
                task = initialTask,
                isLoading = false,
            ),
        )
    var saveFailure: Exception? = null
    var savedTask: Task? = null
    var finishedWithFailure = false
    var allowDeleteWithoutNote = false
    override val state: StateFlow<TaskDetailsUiState> = _state

    override fun startSaving(allowed: (TaskDetailsUiState) -> Boolean): Task? {
      val current = _state.value
      if (current.isUpdating || (!allowDeleteWithoutNote && !allowed(current))) return null
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
    private val recordedAt =
        ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC)
  }
}
