// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.audio.PendingRecording
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.VoiceRecorderException
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import io.mockk.mockk
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
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
  private val users = UsersRepositoryLocal()
  private val dataRepository =
      DataRepositoryLocal(
          authRepository = mockk(),
          tasksRepository = repository,
          usersRepository = users,
      )
  private val recorder = FakeVoiceRecorder()
  private val storage = FakeAudioRepository()
  private val player = FakeVoicePlayer()
  private val services =
      VoiceNoteServices(recorder, storage, player, newId = { "id-${++ids}" }, now = { recordedAt })
  private val owner = User(uid = "owner")
  private val volunteer = User(uid = "volunteer")
  private val recordedAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC)
  private var ids = 0

  private val note =
      VoiceNote(
          "n1",
          "https://fake/t1/old.m4a",
          Duration.ofSeconds(4),
          ZonedDateTime.of(2026, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC),
      )

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun task(voiceNote: VoiceNote? = null, ownerId: String = "owner") =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          voiceNote = voiceNote,
          ownerId = ownerId,
      )

  private fun viewModel(
      task: Task = task(),
      user: User = owner,
      withServices: Boolean = true,
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
            dataRepository = dataRepository,
            voiceNoteServices = services.takeIf { withServices },
        )
        .also { it.load() }
  }

  private fun stored(): Task = runBlocking { repository.local.getTask("t1") }

  /** What the voice note section shows now. */
  private val TaskDetailsViewModel.section: VoiceNoteUiState
    get() = voiceNote.uiState.value

  /** The owner holds the button and releases it: the recording waits to be sent. */
  private fun TaskDetailsViewModel.record() {
    voiceNote.startRecording()
    voiceNote.finishRecording()
  }

  /** The owner records, then confirms the sending. */
  private fun TaskDetailsViewModel.recordAndSend() {
    record()
    voiceNote.confirmSend()
  }

  // Who can manage the note

  /** Breaks if the owner of a task without a note cannot record it. */
  @Test
  fun owner_canManage_andSeesTheRecordBarWhenThereIsNoNote() {
    val state = viewModel().section
    assertTrue(state.canManage)
    assertTrue(state.showRecordBar)
  }

  /** Breaks if a volunteer can manage the note, or cannot hear it. */
  @Test
  fun volunteer_canOnlyListen() {
    val state = viewModel(task(note), volunteer).section
    assertFalse(state.canManage)
    assertEquals(note, state.note)
    assertFalse(state.showRecordBar)
  }

  /** Breaks if a volunteer sees the section of a task that has no note. */
  @Test
  fun volunteer_seesNothingWithoutANote() {
    assertFalse(viewModel(task(), volunteer).section.isVisible)
  }

  /** Breaks if the owner can manage the note without a recorder and a storage to do it with. */
  @Test
  fun withoutServices_evenTheOwnerIsReadOnly() {
    assertFalse(viewModel(withServices = false).section.canManage)
  }

  // Recording

  /** Breaks if releasing the button sends the recording without asking the owner first. */
  @Test
  fun release_waitsForTheOwnerToSend() {
    val vm = viewModel()

    vm.voiceNote.startRecording()
    assertTrue(vm.section.isRecording)
    vm.voiceNote.finishRecording()

    assertEquals(PendingRecording.Waiting(recorder.duration), vm.section.pending)
    assertTrue(vm.section.showSendDialog)
    assertTrue(storage.uploads.isEmpty())
    assertNull(stored().voiceNote)
  }

  /** Breaks if a volunteer can record. */
  @Test
  fun recording_isIgnoredForAVolunteer() {
    val vm = viewModel(task(), volunteer)
    vm.record()
    assertEquals(0, recorder.starts)
    assertNull(vm.section.pending)
  }

  /** Breaks if the owner can record a second note while the task has one. */
  @Test
  fun recording_isIgnoredWhileTheTaskHasANote() {
    val vm = viewModel(task(note))
    vm.record()
    assertEquals(0, recorder.starts)
    assertEquals(note, stored().voiceNote)
  }

  /** Breaks if pressing the button twice starts two recordings. */
  @Test
  fun startingTwice_startsOnlyOneRecording() {
    val vm = viewModel()
    vm.voiceNote.startRecording()
    vm.voiceNote.startRecording()
    assertEquals(1, recorder.starts)
  }

  /** Breaks if releasing a button that never started a recording shows a waiting recording. */
  @Test
  fun finishWithoutStarting_doesNothing() {
    val vm = viewModel()
    vm.voiceNote.finishRecording()
    assertNull(vm.section.pending)
    assertFalse(vm.section.recordingFailed)
  }

  /** Breaks if the owner can start another recording while one waits for the dialog. */
  @Test
  fun recording_isIgnoredWhileARecordingWaitsToBeSent() {
    val vm = viewModel()
    vm.record()
    vm.voiceNote.startRecording()
    assertEquals(1, recorder.starts)
    assertTrue(vm.section.showSendDialog)
  }

  /** Breaks if a recording that cannot start is not shown as a failed recording. */
  @Test
  fun recorderThatCannotStart_showsAFailedRecording() {
    recorder.startFailure = VoiceRecorderException("busy")
    val vm = viewModel()
    vm.voiceNote.startRecording()
    assertTrue(vm.section.recordingFailed)
    assertFalse(vm.section.isRecording)
  }

  /** Breaks if a recording that is too short is sent, or is not shown as a failed recording. */
  @Test
  fun recordingThatIsTooShort_showsAFailedRecordingAndSendsNothing() {
    recorder.stopFailure = VoiceRecorderException("too short")
    val vm = viewModel()
    vm.record()
    assertTrue(vm.section.recordingFailed)
    assertNull(vm.section.pending)
    assertTrue(storage.uploads.isEmpty())
    assertNull(stored().voiceNote)
  }

  /** Breaks if a recording the recorder reports as failed is not shown, or cannot be dismissed. */
  @Test
  fun recorderThatFailsByItself_isShownUntilTheOwnerDismissesIt() {
    val vm = viewModel()
    vm.voiceNote.startRecording()
    recorder.fail()
    assertTrue(vm.section.recordingFailed)

    vm.voiceNote.dismissRecordingFailed()

    assertFalse(vm.section.recordingFailed)
  }

  /** Breaks if a failed recording is still shown once the owner starts a new one. */
  @Test
  fun newRecording_clearsTheFailedRecording() {
    recorder.startFailure = VoiceRecorderException("busy")
    val vm = viewModel()
    vm.voiceNote.startRecording()
    assertTrue(vm.section.recordingFailed)

    recorder.startFailure = null
    vm.voiceNote.startRecording()

    assertFalse(vm.section.recordingFailed)
    assertTrue(vm.section.isRecording)
  }

  /** Breaks if a paused recording cannot be finished and sent. */
  @Test
  fun pausedRecording_canBeFinishedAndSent() {
    val vm = viewModel()
    vm.voiceNote.startRecording()
    recorder.pause()

    vm.voiceNote.finishRecording()
    vm.voiceNote.confirmSend()

    assertNotNull(stored().voiceNote)
  }

  // Sending

  /** Breaks if the confirmed recording is not uploaded and attached to the task as a note. */
  @Test
  fun confirmSend_uploadsTheAudioAndAttachesTheNoteToTheTask() {
    val vm = viewModel()

    vm.recordAndSend()

    assertEquals(listOf("t1"), storage.uploads)
    val saved = stored().voiceNote
    assertNotNull(saved)
    assertEquals("id-1", saved!!.id)
    assertEquals("https://fake/t1/1.m4a", saved.audioUrl)
    assertEquals(recorder.duration, saved.duration)
    assertEquals(recordedAt, saved.createdAt)
    assertEquals(saved, vm.section.note)
    assertNull(vm.section.pending)
    assertFalse(vm.section.showRecordBar)
    assertFalse(vm.uiState.value.isUpdating)
  }

  /** Breaks if the temporary file of a recording stays on the device once it is sent. */
  @Test
  fun confirmSend_deletesTheTemporaryFile() {
    val vm = viewModel()
    vm.recordAndSend()
    assertFalse(recorder.lastAudio!!.file.exists())
  }

  /** Breaks if the upload does not show as sending, or the other actions are not blocked. */
  @Test
  fun whileUploading_theRecordingIsSending_andTheOtherActionsAreBlocked() {
    val gate = CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val vm = viewModel()

    vm.recordAndSend()

    assertEquals(PendingRecording.Sending(recorder.duration), vm.section.pending)
    assertFalse(vm.section.recordEnabled)
    assertTrue(vm.uiState.value.isUpdating)
    assertFalse(vm.uiState.value.canTake)
    gate.complete(Unit)
    assertNull(vm.section.pending)
    assertFalse(vm.uiState.value.isUpdating)
    assertNotNull(stored().voiceNote)
  }

  /** Breaks if confirming without a waiting recording uploads something. */
  @Test
  fun confirmSend_withoutAWaitingRecording_doesNothing() {
    val vm = viewModel()
    vm.voiceNote.confirmSend()
    assertTrue(storage.uploads.isEmpty())
    assertNull(vm.section.pending)
  }

  /** Breaks if cancelling the dialog keeps the recording, or leaves its file on the device. */
  @Test
  fun cancelSend_dropsTheRecordingAndItsFile() {
    val vm = viewModel()
    vm.record()

    vm.voiceNote.cancelSend()

    assertNull(vm.section.pending)
    assertTrue(vm.section.showRecordBar)
    assertFalse(recorder.lastAudio!!.file.exists())
    assertTrue(storage.uploads.isEmpty())
  }

  /** Breaks if cancelling while the recording is uploading drops it halfway. */
  @Test
  fun cancelSend_whileSending_doesNothing() {
    val gate = CompletableDeferred<Unit>()
    storage.uploadGate = gate
    val vm = viewModel()
    vm.recordAndSend()

    vm.voiceNote.cancelSend()
    gate.complete(Unit)

    assertNotNull(stored().voiceNote)
  }

  /** Breaks if a failed upload loses the recording, or if Retry does not send it again. */
  @Test
  fun uploadThatFails_keepsTheRecordingAndRetrySendsIt() {
    storage.uploadFailure = IOException("offline")
    val vm = viewModel()
    vm.recordAndSend()
    assertTrue(vm.section.sendFailed)
    assertNull(vm.section.pending)
    assertNull(stored().voiceNote)
    assertFalse(vm.uiState.value.isUpdating)
    assertTrue(recorder.lastAudio!!.file.exists())

    storage.uploadFailure = null
    vm.voiceNote.retry()

    assertFalse(vm.section.sendFailed)
    assertNotNull(stored().voiceNote)
    assertFalse(recorder.lastAudio!!.file.exists())
    assertEquals(1, recorder.starts) // retried without recording again
  }

  /** Breaks if a file stays uploaded after the task could not be saved with its note. */
  @Test
  fun saveThatFails_afterTheUpload_removesTheFileAndKeepsTheRecording() {
    repository.editFailure = IOException("offline")
    val vm = viewModel()
    vm.recordAndSend()

    assertTrue(vm.section.sendFailed)
    assertEquals(listOf("https://fake/t1/1.m4a"), storage.deletions)
    assertNull(stored().voiceNote)

    repository.editFailure = null
    vm.voiceNote.retry()
    assertNotNull(stored().voiceNote)
    assertEquals(2, storage.uploads.size)
  }

  /** Breaks if a new recording keeps the failure of the last one on screen. */
  @Test
  fun newRecording_clearsTheFailedSend() {
    storage.uploadFailure = IOException("offline")
    val vm = viewModel()
    vm.recordAndSend()
    assertTrue(vm.section.sendFailed)

    vm.voiceNote.startRecording()

    assertFalse(vm.section.sendFailed)
    assertTrue(vm.section.isRecording)
  }

  /** Breaks if the file of a recording that could not be sent stays when it is replaced. */
  @Test
  fun newRecording_deletesTheFileOfTheRecordingThatCouldNotBeSent() {
    storage.uploadFailure = IOException("offline")
    val vm = viewModel()
    vm.recordAndSend()
    val unsent = recorder.lastAudio!!.file
    assertTrue(unsent.exists())

    vm.voiceNote.startRecording()

    assertFalse(unsent.exists())
  }

  // Deleting

  /** Breaks if deleting does not ask first, or does not remove the note and its audio file. */
  @Test
  fun delete_asksToConfirmThenRemovesTheNoteAndItsFile() {
    val vm = viewModel(task(note))

    vm.voiceNote.requestDelete()
    assertTrue(vm.section.showDeleteConfirm)
    vm.voiceNote.confirmDelete()

    assertFalse(vm.section.showDeleteConfirm)
    assertNull(stored().voiceNote)
    assertNull(vm.section.note)
    assertEquals(listOf(note.audioUrl), storage.deletions)
    assertTrue(vm.section.showRecordBar) // the owner can record again
  }

  /** Breaks if keeping the note still deletes it, or leaves the dialog open. */
  @Test
  fun delete_canBeDismissed() {
    val vm = viewModel(task(note))
    vm.voiceNote.requestDelete()
    vm.voiceNote.dismissDelete()
    assertFalse(vm.section.showDeleteConfirm)
    assertEquals(note, stored().voiceNote)
    assertTrue(storage.deletions.isEmpty())
  }

  /** Breaks if a volunteer can delete the note. */
  @Test
  fun delete_isIgnoredForAVolunteer() {
    val vm = viewModel(task(note), volunteer)
    vm.voiceNote.requestDelete()
    vm.voiceNote.confirmDelete()
    assertFalse(vm.section.showDeleteConfirm)
    assertEquals(note, stored().voiceNote)
    assertTrue(storage.deletions.isEmpty())
  }

  /** Breaks if the owner is asked to confirm the deletion of a note that does not exist. */
  @Test
  fun delete_withoutANote_doesNotAsk() {
    val vm = viewModel(task())
    vm.voiceNote.requestDelete()
    assertFalse(vm.section.showDeleteConfirm)
  }

  /** Breaks if a file that cannot be deleted keeps the note or shows an error. */
  @Test
  fun delete_whoseFileCannotBeRemoved_stillRemovesTheNote() {
    storage.deleteFailure = IOException("offline")
    val vm = viewModel(task(note))
    vm.voiceNote.confirmDelete()
    assertNull(stored().voiceNote)
    assertNull(vm.uiState.value.errorMsg)
  }

  /** Breaks if a failed save removes the note or its file, or does not show the save error. */
  @Test
  fun delete_whoseSaveFails_keepsTheNoteAndItsFile() {
    repository.editFailure = IOException("offline")
    val vm = viewModel(task(note))
    vm.voiceNote.requestDelete()
    vm.voiceNote.confirmDelete()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertEquals(note, stored().voiceNote)
    assertEquals(note, vm.section.note)
    assertTrue(storage.deletions.isEmpty())
    assertFalse(vm.uiState.value.isUpdating)
  }

  /** Breaks if a note keeps playing after it is deleted. */
  @Test
  fun delete_stopsThePlaybackOfTheNote() {
    val vm = viewModel(task(note))
    vm.voiceNote.togglePlay()
    assertTrue(vm.section.isPlaying)

    vm.voiceNote.confirmDelete()

    assertEquals(PlayerState.Idle, vm.section.player)
  }

  /** Breaks if a paused note stays held by the player after it is deleted. */
  @Test
  fun delete_stopsThePausedPlaybackOfTheNote() {
    val vm = viewModel(task(note))
    vm.voiceNote.togglePlay()
    player.pause()
    assertFalse(vm.section.isPlaying)

    vm.voiceNote.confirmDelete()

    assertEquals(PlayerState.Idle, vm.section.player)
  }

  // Playing

  /** Breaks if the play button does not play the note, or does not stop it the second time. */
  @Test
  fun togglePlay_playsTheNoteThenStopsIt() {
    val vm = viewModel(task(note), volunteer)

    vm.voiceNote.togglePlay()
    assertEquals(listOf(note.audioUrl), player.played)
    assertTrue(vm.section.isPlaying)
    vm.voiceNote.togglePlay()

    assertFalse(vm.section.isPlaying)
    assertEquals(1, player.played.size)
  }

  /** Breaks if tapping a paused note plays it again from the start instead of stopping it. */
  @Test
  fun togglePlay_onAPausedNote_stopsIt() {
    val vm = viewModel(task(note), volunteer)
    vm.voiceNote.togglePlay()
    player.pause()

    vm.voiceNote.togglePlay()

    assertEquals(PlayerState.Idle, vm.section.player)
    assertEquals(1, player.played.size)
  }

  /** Breaks if playing does anything without a note or without a player. */
  @Test
  fun togglePlay_doesNothingWithoutANoteOrWithoutServices() {
    viewModel(task()).voiceNote.togglePlay()
    assertTrue(player.played.isEmpty())
    viewModel(task(note), withServices = false).voiceNote.togglePlay()
    assertTrue(player.played.isEmpty())
  }

  /** Breaks if Retry after a failed playback does not play the note again. */
  @Test
  fun retryAfterAPlaybackFailure_playsTheNoteAgain() {
    val vm = viewModel(task(note), volunteer)
    vm.voiceNote.togglePlay()
    player.fail(note.audioUrl)
    assertTrue(vm.section.playbackFailed)

    vm.voiceNote.retry()

    assertEquals(2, player.played.size)
    assertTrue(vm.section.isPlaying)
  }

  // Microphone

  /** Breaks if a refused microphone does not disable the record button, or never enables it. */
  @Test
  fun microphonePermission_disablesTheRecordButtonWhenRefused() {
    val vm = viewModel()
    vm.voiceNote.onMicPermissionResult(granted = false)
    assertTrue(vm.section.micDenied)
    assertFalse(vm.section.recordEnabled)

    vm.voiceNote.onMicPermissionResult(granted = true)
    assertFalse(vm.section.micDenied)
    assertTrue(vm.section.recordEnabled)
  }

  // Who recorded the note

  /** Breaks if a volunteer is not told who recorded the note. */
  @Test
  fun ownerName_isTheNameOfTheOwnerForAVolunteerWithANote() {
    runBlocking { users.addUser(User(uid = "owner", name = "Marie", surname = "Dupont")) }
    val vm = viewModel(task(note), volunteer)
    assertEquals("Marie Dupont", vm.uiState.value.ownerName)
  }

  /** Breaks if the owner is credited by name for their own note, which is shown as "You". */
  @Test
  fun ownerName_isBlankForTheOwner() {
    runBlocking { users.addUser(User(uid = "owner", name = "Marie", surname = "Dupont")) }
    val vm = viewModel(task(note))
    assertEquals("", vm.uiState.value.ownerName)
  }

  /** Breaks if an owner that cannot be read hides the task or shows an error. */
  @Test
  fun ownerName_isBlankWhenTheOwnerCannotBeRead() {
    val vm = viewModel(task(note), volunteer)
    assertEquals("", vm.uiState.value.ownerName)
    assertNotNull(vm.uiState.value.task)
    assertNull(vm.uiState.value.errorMsg)
  }

  /** Breaks if the owner is read for a volunteer's task without a voice note. */
  @Test
  fun ownerName_isBlankForAVolunteerWhenThereIsNoNote() {
    runBlocking { users.addUser(owner) }
    val vm = viewModel(task(), volunteer)

    assertEquals("", vm.uiState.value.ownerName)
  }

  // Callbacks and release

  /** Breaks if the record button records while the microphone is off, instead of asking. */
  @Test
  fun recordButton_asksForTheMicrophoneInsteadOfRecordingWhenItIsOff() {
    var asked = 0
    val vm = viewModel()
    val callbacks =
        vm.voiceNote.callbacks(
            hasMicrophone = { false },
            requestMicrophone = { asked++ },
            onOpenSettings = {},
        )

    callbacks.onRecordStart()

    assertEquals(1, asked)
    assertEquals(0, recorder.starts)
  }

  /** Breaks if the record button asks for the microphone although it is on. */
  @Test
  fun recordButton_recordsWithoutAskingWhenTheMicrophoneIsOn() {
    var asked = 0
    val vm = viewModel()
    val callbacks =
        vm.voiceNote.callbacks(
            hasMicrophone = { true },
            requestMicrophone = { asked++ },
            onOpenSettings = {},
        )

    callbacks.onRecordStart()

    assertEquals(0, asked)
    assertEquals(1, recorder.starts)
  }

  /** Breaks if recording, sending, or settings callbacks are not wired to their actions. */
  @Test
  fun callbacks_drivetheVoiceNote() {
    var settingsOpened = false
    val vm = viewModel()
    val callbacks =
        vm.voiceNote.callbacks(
            hasMicrophone = { true },
            requestMicrophone = {},
            onOpenSettings = { settingsOpened = true },
        )

    callbacks.onRecordStart()
    callbacks.onRecordRelease()
    callbacks.onConfirmSend()
    callbacks.onOpenSettings()

    assertNotNull(stored().voiceNote)
    assertTrue(settingsOpened)
  }

  /** Breaks if closing the screen leaves a waiting recording's file on the device. */
  @Test
  fun release_deletesTheWaitingRecording() {
    val vm = viewModel()
    vm.record()
    val waitingFile = recorder.lastAudio!!.file

    vm.voiceNote.release()

    assertFalse(waitingFile.exists())
  }

  /** Breaks if closing the screen leaves the microphone recording. */
  @Test
  fun release_cancelsTheRecordingInProgress() {
    val vm = viewModel()
    vm.voiceNote.startRecording()

    vm.voiceNote.release()

    assertEquals(1, recorder.cancels)
  }

  /** Breaks if closing the screen keeps the player. */
  @Test
  fun release_releasesThePlayer() {
    viewModel().voiceNote.release()
    assertTrue(player.released)
  }

  /** Breaks if clearing the ViewModel leaves recording or playback resources held. */
  @Test
  fun clearingTheViewModel_releasesVoiceNoteResources() {
    val vm = viewModel()
    vm.voiceNote.startRecording()
    ViewModelStore().apply {
      put("task-details", vm)
      clear()
    }

    assertEquals(1, recorder.cancels)
    assertTrue(player.released)
  }
}
