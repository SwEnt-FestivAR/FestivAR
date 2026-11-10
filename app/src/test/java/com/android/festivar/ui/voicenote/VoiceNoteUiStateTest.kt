// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.voicenote.VoiceNote
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNoteUiStateTest {
  private val note =
      VoiceNote(
          "n1",
          "https://x/n1.m4a",
          Duration.ofSeconds(14),
          ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC),
      )

  /** Breaks if the record bar shows to a volunteer, or to the owner of a task that has a note. */
  @Test
  fun recordBarOnlyForTheOwnerWithoutNote() {
    assertTrue(VoiceNoteUiState(canManage = true).showRecordBar)
    assertFalse(VoiceNoteUiState(note = note, canManage = true).showRecordBar)
    assertFalse(VoiceNoteUiState(canManage = false).showRecordBar)
    assertFalse(VoiceNoteUiState(note = note, canManage = false).showRecordBar)
  }

  /** Breaks if the delete dialog opens for a volunteer, for a task without a note, or unasked. */
  @Test
  fun deleteDialogOnlyForTheOwnerOfANote() {
    assertTrue(
        VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true).showDeleteDialog
    )
    assertFalse(VoiceNoteUiState(note = note, canManage = true).showDeleteDialog)
    assertFalse(VoiceNoteUiState(note = note, showDeleteConfirm = true).showDeleteDialog)
    assertFalse(VoiceNoteUiState(canManage = true, showDeleteConfirm = true).showDeleteDialog)
  }

  /** Breaks if the owner can press the record button while the microphone is refused. */
  @Test
  fun recordIsDisabledWithoutTheMicrophonePermission() {
    assertTrue(VoiceNoteUiState(canManage = true).recordEnabled)
    assertFalse(VoiceNoteUiState(canManage = true, micDenied = true).recordEnabled)
  }

  /** Breaks if a volunteer sees an empty section, or somebody loses the section of a note. */
  @Test
  fun sectionIsHiddenForAVolunteerWithoutNote() {
    assertFalse(VoiceNoteUiState().isVisible)
    assertTrue(VoiceNoteUiState(note = note).isVisible)
    assertTrue(VoiceNoteUiState(canManage = true).isVisible)
  }

  /** Breaks if a note looks playing while the player plays another note or nothing. */
  @Test
  fun isPlayingOnlyForTheNoteOfTheTask() {
    assertTrue(VoiceNoteUiState(note = note, player = PlayerState.Playing(note.audioUrl)).isPlaying)
    assertFalse(VoiceNoteUiState(note = note, player = PlayerState.Playing("other")).isPlaying)
    assertFalse(VoiceNoteUiState(note = note).isPlaying)
    assertFalse(VoiceNoteUiState(player = PlayerState.Playing(note.audioUrl)).isPlaying)
  }

  /** Breaks if the playback failure of another note is shown on this note. */
  @Test
  fun playbackFailedOnlyForTheNoteOfTheTask() {
    assertTrue(
        VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)).playbackFailed
    )
    assertFalse(VoiceNoteUiState(note = note, player = PlayerState.Failed("other")).playbackFailed)
    assertFalse(VoiceNoteUiState(note = note).playbackFailed)
  }

  /** Breaks if the timer does not follow the recorder, or does not start at zero. */
  @Test
  fun elapsedFollowsTheRecorder() {
    assertEquals(Duration.ZERO, VoiceNoteUiState().elapsed)
    val state = VoiceNoteUiState(recorder = RecorderState.Recording(Duration.ofSeconds(3)))
    assertTrue(state.isRecording)
    assertEquals(Duration.ofSeconds(3), state.elapsed)
  }

  private val waiting = PendingRecording.Waiting(Duration.ofSeconds(7))
  private val uploading = PendingRecording.Sending(Duration.ofSeconds(7))

  /** Breaks if the owner can record while the recording is sent, or loses the button before. */
  @Test
  fun recordingIsDisabledWhileSending() {
    assertTrue(VoiceNoteUiState(canManage = true, pending = waiting).recordEnabled)
    assertFalse(VoiceNoteUiState(canManage = true, pending = uploading).recordEnabled)
  }

  /** Breaks if a recording that waits is taken for one that is sent, or the other way round. */
  @Test
  fun sendingOnlyForARecordingThatIsUploaded() {
    assertEquals(uploading, VoiceNoteUiState(pending = uploading).sending)
    assertNull(VoiceNoteUiState(pending = waiting).sending)
    assertNull(VoiceNoteUiState(canManage = true).sending)
  }

  /** Breaks if the send dialog shows to somebody else than the owner, or at the wrong moment. */
  @Test
  fun sendDialogShowsWhileTheRecordingWaitsForTheOwner() {
    assertTrue(VoiceNoteUiState(canManage = true, pending = waiting).showSendDialog)
    assertFalse(VoiceNoteUiState(canManage = true, pending = uploading).showSendDialog)
    assertFalse(VoiceNoteUiState(canManage = true).showSendDialog)
    assertFalse(VoiceNoteUiState(canManage = false, pending = waiting).showSendDialog)
  }

  /** Breaks if the send dialog opens over a task that already has a voice note. */
  @Test
  fun noSendDialogForATaskThatHasANote() {
    assertFalse(VoiceNoteUiState(note = note, canManage = true, pending = waiting).showSendDialog)
  }

  /** Breaks if a recorder that failed by itself is not reported as a failed recording. */
  @Test
  fun recordingFailedOnlyForAFailedRecorder() {
    assertTrue(VoiceNoteUiState(recorder = RecorderState.Failed).recordingFailed)
    assertFalse(VoiceNoteUiState(recorder = RecorderState.Idle).recordingFailed)
  }

  /** Breaks if a callback is wired to another action than the one the caller gave. */
  @Test
  fun everyCallbackIsGivenByTheCaller() {
    val calls = mutableListOf<String>()
    val callbacks =
        VoiceNoteCallbacks(
            onTogglePlay = { calls += "play" },
            onDelete = { calls += "delete" },
            onConfirmDelete = { calls += "confirm" },
            onDismissDelete = { calls += "dismiss" },
            onRecordStart = { calls += "start" },
            onRecordRelease = { calls += "release" },
            onConfirmSend = { calls += "send" },
            onCancelSend = { calls += "cancelSend" },
            onDismissRecordingFailed = { calls += "dismissFailed" },
            onRetry = { calls += "retry" },
            onOpenSettings = { calls += "settings" },
        )

    with(callbacks) {
      onTogglePlay()
      onDelete()
      onConfirmDelete()
      onDismissDelete()
      onRecordStart()
      onRecordRelease()
      onConfirmSend()
      onCancelSend()
      onDismissRecordingFailed()
      onRetry()
      onOpenSettings()
    }

    assertEquals(
        listOf(
            "play",
            "delete",
            "confirm",
            "dismiss",
            "start",
            "release",
            "send",
            "cancelSend",
            "dismissFailed",
            "retry",
            "settings",
        ),
        calls,
    )
  }

  /** Breaks if a NoOp callback throws when it is called. */
  @Test
  fun noOpCallbacksDoNothing() {
    with(VoiceNoteCallbacks.NoOp) {
      onTogglePlay()
      onDelete()
      onConfirmDelete()
      onDismissDelete()
      onRecordStart()
      onRecordRelease()
      onConfirmSend()
      onCancelSend()
      onDismissRecordingFailed()
      onRetry()
      onOpenSettings()
    }
  }
}
