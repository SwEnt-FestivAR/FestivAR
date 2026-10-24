// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.model.voicenote.PlayerState
import com.android.festivar.model.voicenote.RecorderState
import com.android.festivar.model.voicenote.VocalNote
import com.android.festivar.model.voicenote.VoiceRecorderException
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The voice note part of [TaskDetailsViewModel], with fakes for the recorder, storage, player. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TaskDetailsViewModelVoiceNoteTest {
  private val repository = EditFailingRepository()
  private val recorder = FakeVoiceRecorder()
  private val storage = FakeVocalNoteStorage()
  private val player = FakeVoicePlayer()
  private val owner = User(uid = "owner")
  private val volunteer = User(uid = "volunteer")
  private val recordedAt = LocalDateTime.of(2026, 10, 17, 9, 12)
  private var ids = 0

  private val note =
      VocalNote(
          "n1",
          "https://fake/t1/old.m4a",
          Duration.ofSeconds(4),
          LocalDateTime.of(2026, 1, 1, 8, 0),
      )

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun task(vocalNote: VocalNote? = null, ownerId: String? = "owner") =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          vocalNote = vocalNote,
          ownerId = ownerId,
      )

  private fun viewModel(
      task: Task = task(),
      user: User = owner,
      withRecorder: Boolean = true,
      withStorage: Boolean = true,
      withPlayer: Boolean = true,
  ): TaskDetailsViewModel {
    runBlocking {
      // A test can build several view models: the task of the last one replaces the first.
      if (repository.local.getAllTasks(task.eventId).any { it.taskId == task.taskId }) {
        repository.local.editTask(task.taskId, task)
      } else {
        repository.local.addTask(task)
      }
    }
    return TaskDetailsViewModel(
        taskId = task.taskId,
        currentUser = user,
        repository = repository,
        recorder = recorder.takeIf { withRecorder },
        storage = storage.takeIf { withStorage },
        player = player.takeIf { withPlayer },
        newId = { "id-${++ids}" },
        now = { recordedAt },
    )
  }

  private fun stored(): Task = runBlocking { repository.local.getTask("t1") }

  private fun TaskDetailsViewModel.record() {
    startRecording()
    finishRecording()
  }

  // Who can manage the note

  @Test
  fun owner_canManage_andSeesTheRecordBarWhenThereIsNoNote() {
    val state = viewModel().voiceNote.value
    assertTrue(state.canManage)
    assertTrue(state.showRecordBar)
    assertTrue(state.showEmpty)
  }

  @Test
  fun volunteer_canOnlyListen() {
    val state = viewModel(task(note), volunteer).voiceNote.value
    assertFalse(state.canManage)
    assertEquals(note, state.note)
    assertFalse(state.showRecordBar)
  }

  @Test
  fun volunteer_seesNothingWithoutANote() {
    assertFalse(viewModel(task(), volunteer).voiceNote.value.isVisible)
  }

  @Test
  fun taskWithoutOwner_hasNoManager() {
    assertFalse(viewModel(task(ownerId = null), owner).voiceNote.value.canManage)
  }

  @Test
  fun withoutRecorderOrStorage_evenTheOwnerIsReadOnly() {
    assertFalse(viewModel(withRecorder = false).voiceNote.value.canManage)
    assertFalse(viewModel(withStorage = false).voiceNote.value.canManage)
  }

  // Recording and sending

  @Test
  fun recording_uploadsTheAudioAndAttachesTheNoteToTheTask() {
    val vm = viewModel()

    vm.startRecording()
    assertTrue(vm.voiceNote.value.isRecording)
    vm.finishRecording()

    assertEquals(listOf("t1"), storage.uploads)
    val saved = stored().vocalNote
    assertNotNull(saved)
    assertEquals("id-1", saved!!.id)
    assertEquals("https://fake/t1/1.m4a", saved.audioUrl)
    assertEquals(recorder.duration, saved.duration)
    assertEquals(recordedAt, saved.createdAt)
    assertEquals(saved, vm.voiceNote.value.note)
    assertFalse(vm.voiceNote.value.showRecordBar)
    assertFalse(vm.uiState.value.isUpdating)
    assertFalse(recorder.lastAudio!!.file.exists())
  }

  @Test
  fun recording_isIgnoredForAVolunteer() {
    val vm = viewModel(task(), volunteer)
    vm.record()
    assertEquals(0, recorder.starts)
    assertTrue(storage.uploads.isEmpty())
  }

  @Test
  fun recording_isIgnoredWhileTheTaskHasANote() {
    val vm = viewModel(task(note))
    vm.record()
    assertEquals(0, recorder.starts)
    assertEquals(note, stored().vocalNote)
  }

  @Test
  fun startingTwice_startsOnlyOneRecording() {
    val vm = viewModel()
    vm.startRecording()
    vm.startRecording()
    assertEquals(1, recorder.starts)
  }

  @Test
  fun finishWithoutStarting_doesNothing() {
    val vm = viewModel()
    vm.finishRecording()
    assertTrue(storage.uploads.isEmpty())
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun recorderThatCannotStart_showsAnError() {
    recorder.startFailure = VoiceRecorderException("busy")
    val vm = viewModel()
    vm.startRecording()
    assertEquals(R.string.voice_note_record_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.voiceNote.value.isRecording)
  }

  @Test
  fun recordingThatIsTooShort_showsAnErrorAndSendsNothing() {
    recorder.stopFailure = VoiceRecorderException("too short")
    val vm = viewModel()
    vm.record()
    assertEquals(R.string.voice_note_record_failed, vm.uiState.value.errorMsg)
    assertTrue(storage.uploads.isEmpty())
    assertNull(stored().vocalNote)
  }

  @Test
  fun cancel_dropsTheRecordingWithoutSendingIt() {
    val vm = viewModel()
    vm.startRecording()
    vm.cancelRecording()
    assertEquals(RecorderState.Idle, vm.voiceNote.value.recorder)
    assertEquals(1, recorder.cancels)
    assertTrue(storage.uploads.isEmpty())
  }

  @Test
  fun uploadThatFails_keepsTheRecordingAndRetrySendsIt() {
    storage.uploadFailure = IOException("offline")
    val vm = viewModel()
    vm.record()
    assertTrue(vm.voiceNote.value.sendFailed)
    assertTrue(vm.voiceNote.value.hasNotices)
    assertNull(stored().vocalNote)
    assertFalse(vm.uiState.value.isUpdating)
    assertTrue(recorder.lastAudio!!.file.exists())

    storage.uploadFailure = null
    vm.retryVoiceNote()

    assertFalse(vm.voiceNote.value.sendFailed)
    assertNotNull(stored().vocalNote)
    assertFalse(recorder.lastAudio!!.file.exists())
    assertEquals(1, recorder.starts) // retried without recording again
  }

  @Test
  fun saveThatFails_afterTheUpload_removesTheFileAndKeepsTheRecording() {
    repository.editFailure = IOException("offline")
    val vm = viewModel()
    vm.record()

    assertTrue(vm.voiceNote.value.sendFailed)
    assertEquals(listOf("https://fake/t1/1.m4a"), storage.deletions)
    assertNull(stored().vocalNote)

    repository.editFailure = null
    vm.retryVoiceNote()
    assertNotNull(stored().vocalNote)
    assertEquals(2, storage.uploads.size)
  }

  @Test
  fun newRecording_clearsTheFailureOfTheLastOne() {
    storage.uploadFailure = IOException("offline")
    val vm = viewModel()
    vm.record()
    assertTrue(vm.voiceNote.value.sendFailed)

    vm.startRecording()

    assertFalse(vm.voiceNote.value.sendFailed)
    assertTrue(vm.voiceNote.value.isRecording)
  }

  @Test
  fun whileUploading_theOtherActionsAreBlocked() {
    val gate = CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val vm = viewModel()

    vm.record()

    assertTrue(vm.uiState.value.isUpdating)
    assertFalse(vm.uiState.value.canTake)
    gate.complete(Unit)
    assertFalse(vm.uiState.value.isUpdating)
    assertNotNull(stored().vocalNote)
  }

  // Deleting

  @Test
  fun delete_asksToConfirmThenRemovesTheNoteAndItsFile() {
    val vm = viewModel(task(note))

    vm.requestDeleteVoiceNote()
    assertTrue(vm.voiceNote.value.showDeleteConfirm)
    vm.confirmDeleteVoiceNote()

    assertFalse(vm.voiceNote.value.showDeleteConfirm)
    assertNull(stored().vocalNote)
    assertNull(vm.voiceNote.value.note)
    assertEquals(listOf(note.audioUrl), storage.deletions)
    assertTrue(vm.voiceNote.value.showRecordBar) // the owner can record again
  }

  @Test
  fun delete_canBeDismissed() {
    val vm = viewModel(task(note))
    vm.requestDeleteVoiceNote()
    vm.dismissDeleteVoiceNote()
    assertFalse(vm.voiceNote.value.showDeleteConfirm)
    assertEquals(note, stored().vocalNote)
    assertTrue(storage.deletions.isEmpty())
  }

  @Test
  fun delete_isIgnoredForAVolunteerAndForATaskWithoutNote() {
    val volunteerVm = viewModel(task(note), volunteer)
    volunteerVm.requestDeleteVoiceNote()
    volunteerVm.confirmDeleteVoiceNote()
    assertFalse(volunteerVm.voiceNote.value.showDeleteConfirm)
    assertEquals(note, stored().vocalNote)
    assertTrue(storage.deletions.isEmpty())
  }

  @Test
  fun delete_withoutANote_doesNotAsk() {
    val vm = viewModel(task())
    vm.requestDeleteVoiceNote()
    assertFalse(vm.voiceNote.value.showDeleteConfirm)
  }

  @Test
  fun delete_whoseFileCannotBeRemoved_stillRemovesTheNote() {
    storage.deleteFailure = IOException("offline")
    val vm = viewModel(task(note))
    vm.confirmDeleteVoiceNote()
    assertNull(stored().vocalNote)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun delete_whoseSaveFails_keepsTheNoteAndItsFile() {
    repository.editFailure = IOException("offline")
    val vm = viewModel(task(note))
    vm.requestDeleteVoiceNote()
    vm.confirmDeleteVoiceNote()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertEquals(note, stored().vocalNote)
    assertEquals(note, vm.voiceNote.value.note)
    assertTrue(storage.deletions.isEmpty())
    assertFalse(vm.uiState.value.isUpdating)
  }

  @Test
  fun delete_stopsThePlaybackOfTheNote() {
    val vm = viewModel(task(note))
    vm.togglePlay()
    assertTrue(vm.voiceNote.value.isPlaying)

    vm.confirmDeleteVoiceNote()

    assertEquals(PlayerState.Idle, vm.voiceNote.value.player)
  }

  // Playing

  @Test
  fun togglePlay_playsTheNoteThenStopsIt() {
    val vm = viewModel(task(note), volunteer)

    vm.togglePlay()
    assertEquals(listOf(note.audioUrl), player.played)
    assertTrue(vm.voiceNote.value.isPlaying)
    vm.togglePlay()

    assertFalse(vm.voiceNote.value.isPlaying)
    assertEquals(1, player.played.size)
  }

  @Test
  fun togglePlay_doesNothingWithoutANoteOrWithoutAPlayer() {
    viewModel(task()).togglePlay()
    assertTrue(player.played.isEmpty())
    viewModel(task(note), withPlayer = false).togglePlay()
    assertTrue(player.played.isEmpty())
  }

  @Test
  fun retryAfterAPlaybackFailure_playsTheNoteAgain() {
    val vm = viewModel(task(note), volunteer)
    vm.togglePlay()
    player.fail(note.audioUrl)
    assertTrue(vm.voiceNote.value.playbackFailed)

    vm.retryVoiceNote()

    assertEquals(2, player.played.size)
    assertTrue(vm.voiceNote.value.isPlaying)
  }

  // Microphone

  @Test
  fun microphonePermission_disablesTheRecordButtonWhenRefused() {
    val vm = viewModel()
    vm.onMicPermissionResult(granted = false)
    assertTrue(vm.voiceNote.value.micDenied)
    assertFalse(vm.voiceNote.value.recordEnabled)

    vm.onMicPermissionResult(granted = true)
    assertFalse(vm.voiceNote.value.micDenied)
    assertTrue(vm.voiceNote.value.recordEnabled)
  }
}
