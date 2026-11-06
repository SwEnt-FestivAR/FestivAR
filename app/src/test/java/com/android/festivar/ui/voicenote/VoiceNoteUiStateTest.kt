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

  /** Breaks if a failed recording counts as recording, or if only a failed one is reported. */
  @Test
  fun recordingFailedOnlyForAFailedRecorder() {
    assertTrue(VoiceNoteUiState(recorder = RecorderState.Failed).recordingFailed)
    assertFalse(VoiceNoteUiState(recorder = RecorderState.Failed).isRecording)
    assertFalse(VoiceNoteUiState().recordingFailed)
    assertFalse(
        VoiceNoteUiState(recorder = RecorderState.Recording(Duration.ofSeconds(3))).recordingFailed
    )
  }

  /** Breaks if a paused recording stops counting as a recording or loses its recorded time. */
  @Test
  fun aPausedRecordingIsStillRecording() {
    val state =
        VoiceNoteUiState(recorder = RecorderState.Recording(Duration.ofSeconds(7), isPaused = true))
    assertTrue(state.isRecording)
    assertEquals(Duration.ofSeconds(7), state.elapsed)
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
            onRecordLock = { calls += "lock" },
            onRecordPause = { calls += "pause" },
            onRecordResume = { calls += "resume" },
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
      onRecordLock()
      onRecordPause()
      onRecordResume()
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
            "lock",
            "pause",
            "resume",
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
      onRecordLock()
      onRecordPause()
      onRecordResume()
      onRetry()
      onOpenSettings()
    }
  }

  /**
   * Breaks if the controls show without a recording, or do not show once it is locked or paused.
   */
  @Test
  fun recordControlsShowForALockedOrPausedRecording() {
    val recording = RecorderState.Recording(Duration.ofSeconds(3))
    val paused = RecorderState.Recording(Duration.ofSeconds(3), isPaused = true)

    assertFalse(VoiceNoteUiState(recorder = recording).showRecordControls)
    assertTrue(VoiceNoteUiState(recorder = recording, recordLocked = true).showRecordControls)
    assertTrue(VoiceNoteUiState(recorder = paused).showRecordControls)
    // A lock left over from a recording that ended does not show the controls.
    assertFalse(VoiceNoteUiState(recordLocked = true).showRecordControls)
    assertFalse(
        VoiceNoteUiState(recorder = RecorderState.Failed, recordLocked = true).showRecordControls
    )
  }

  /** Breaks if isPaused reports a pause for a recording that runs or for no recording. */
  @Test
  fun isPausedOnlyForAPausedRecording() {
    assertTrue(
        VoiceNoteUiState(recorder = RecorderState.Recording(Duration.ofSeconds(3), true)).isPaused
    )
    assertFalse(
        VoiceNoteUiState(recorder = RecorderState.Recording(Duration.ofSeconds(3))).isPaused
    )
    assertFalse(VoiceNoteUiState().isPaused)
  }

  @Test
  fun noticesFollowTheFlagsAndTheRole() {
    assertFalse(VoiceNoteUiState(canManage = true).hasNotices)
    assertTrue(VoiceNoteUiState(canManage = true, sendFailed = true).hasNotices)
    assertTrue(VoiceNoteUiState(canManage = true, micDenied = true).hasNotices)
    assertTrue(VoiceNoteUiState(canManage = true, recorder = RecorderState.Failed).hasNotices)
    // Send, recording and microphone notices only matter while the record bar is there.
    assertFalse(VoiceNoteUiState(note = note, canManage = true, sendFailed = true).hasNotices)
    assertFalse(
        VoiceNoteUiState(note = note, canManage = true, recorder = RecorderState.Failed).hasNotices
    )
    assertFalse(VoiceNoteUiState(canManage = false, micDenied = true).hasNotices)
    assertTrue(VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)).hasNotices)
  }
}
