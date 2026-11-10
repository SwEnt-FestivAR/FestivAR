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

  @Test
  fun recordBarOnlyForTheOwnerWithoutNote() {
    assertTrue(VoiceNoteUiState(canManage = true).showRecordBar)
    assertFalse(VoiceNoteUiState(note = note, canManage = true).showRecordBar)
    assertFalse(VoiceNoteUiState(canManage = false).showRecordBar)
    assertFalse(VoiceNoteUiState(note = note, canManage = false).showRecordBar)
  }

  @Test
  fun deleteDialogOnlyForTheOwnerOfANote() {
    assertTrue(
        VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true).showDeleteDialog
    )
    assertFalse(VoiceNoteUiState(note = note, canManage = true).showDeleteDialog)
    assertFalse(VoiceNoteUiState(note = note, showDeleteConfirm = true).showDeleteDialog)
    assertFalse(VoiceNoteUiState(canManage = true, showDeleteConfirm = true).showDeleteDialog)
  }

  @Test
  fun recordIsDisabledWithoutTheMicrophonePermission() {
    assertTrue(VoiceNoteUiState(canManage = true).recordEnabled)
    assertFalse(VoiceNoteUiState(canManage = true, micDenied = true).recordEnabled)
  }

  @Test
  fun sectionIsHiddenForAVolunteerWithoutNote() {
    assertFalse(VoiceNoteUiState().isVisible)
    assertTrue(VoiceNoteUiState(note = note).isVisible)
    assertTrue(VoiceNoteUiState(canManage = true).isVisible)
  }

  @Test
  fun isPlayingOnlyForTheNoteOfTheTask() {
    assertTrue(VoiceNoteUiState(note = note, player = PlayerState.Playing(note.audioUrl)).isPlaying)
    assertFalse(VoiceNoteUiState(note = note, player = PlayerState.Playing("other")).isPlaying)
    assertFalse(VoiceNoteUiState(note = note).isPlaying)
    assertFalse(VoiceNoteUiState(player = PlayerState.Playing(note.audioUrl)).isPlaying)
  }

  @Test
  fun playbackFailedOnlyForTheNoteOfTheTask() {
    assertTrue(
        VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)).playbackFailed
    )
    assertFalse(VoiceNoteUiState(note = note, player = PlayerState.Failed("other")).playbackFailed)
    assertFalse(VoiceNoteUiState(note = note).playbackFailed)
  }

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

  /** Breaks if a recorder that failed by itself is not reported as a failed recording. */
  @Test
  fun recordingFailedOnlyForAFailedRecorder() {
    assertTrue(VoiceNoteUiState(recorder = RecorderState.Failed).recordingFailed)
    assertFalse(VoiceNoteUiState(recorder = RecorderState.Idle).recordingFailed)
  }

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
