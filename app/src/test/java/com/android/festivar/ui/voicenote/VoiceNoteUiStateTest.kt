// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import com.android.festivar.model.voicenote.PlayerState
import com.android.festivar.model.voicenote.RecorderState
import com.android.festivar.model.voicenote.VoiceNote
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            onRecordCancel = { calls += "cancel" },
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
      onRecordCancel()
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
            "cancel",
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
      onRecordCancel()
      onRetry()
      onOpenSettings()
    }
  }

  @Test
  fun noticesFollowTheFlagsAndTheRole() {
    assertFalse(VoiceNoteUiState(canManage = true).hasNotices)
    assertTrue(VoiceNoteUiState(canManage = true, sendFailed = true).hasNotices)
    assertTrue(VoiceNoteUiState(canManage = true, micDenied = true).hasNotices)
    // Send and microphone notices only matter while the record bar is there.
    assertFalse(VoiceNoteUiState(note = note, canManage = true, sendFailed = true).hasNotices)
    assertFalse(VoiceNoteUiState(canManage = false, micDenied = true).hasNotices)
    assertTrue(VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)).hasNotices)
  }
}
