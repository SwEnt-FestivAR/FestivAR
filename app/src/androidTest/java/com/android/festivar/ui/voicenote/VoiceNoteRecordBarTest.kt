// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The record bar and the notices above it for every state, with no ViewModel. */
@RunWith(AndroidJUnit4::class)
class VoiceNoteRecordBarTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val note =
      VoiceNote(
          "n1",
          "https://x/n1.m4a",
          Duration.ofSeconds(14),
          ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC),
      )

  private fun text(id: Int, vararg args: Any) =
      ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

  private val events = mutableListOf<String>()

  private val callbacks =
      VoiceNoteCallbacks(
          onTogglePlay = { events += "toggle" },
          onDelete = { events += "delete" },
          onConfirmDelete = { events += "confirm" },
          onDismissDelete = { events += "dismiss" },
          onRecordStart = { events += "start" },
          onRecordRelease = { events += "release" },
          onRecordCancel = { events += "cancel" },
          onConfirmSend = { events += "send" },
          onCancelSend = { events += "cancelSend" },
          onDismissRecordingFailed = { events += "dismissFailed" },
          onRetry = { events += "retry" },
          onOpenSettings = { events += "settings" },
      )

  private fun show(state: VoiceNoteUiState) = composeTestRule.setContent {
    AppTheme {
      Column {
        VoiceNoteNotices(state, callbacks)
        VoiceNoteRecordBar(state, callbacks)
      }
    }
  }

  private fun tag(tag: String) = composeTestRule.onNodeWithTag(tag)

  /** Breaks if the bar shows next to a note, or to somebody who cannot record. */
  @Test
  fun barIsHiddenWhenANoteExists() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.RECORDING_BAR).assertDoesNotExist()
  }

  /** Breaks if a volunteer can see the record bar. */
  @Test
  fun barIsHiddenForVolunteers() {
    show(VoiceNoteUiState(canManage = false))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
  }

  /** Breaks if the bar is not the 56dp of the design, or does not invite to hold to record. */
  @Test
  fun idleBarInvitesToHoldToRecord() {
    show(VoiceNoteUiState(canManage = true))
    composeTestRule.onNodeWithText(text(R.string.voice_note_hold_to_record)).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.RECORDING_BAR).assertHeightIsEqualTo(56.dp)
    tag(VoiceNoteRecordTestTags.RECORDING_HINT).assertDoesNotExist()
  }

  /** Breaks if the timer or the hint is missing while recording. */
  @Test
  fun recordingShowsTimerAndHint() {
    show(
        VoiceNoteUiState(
            canManage = true,
            recorder = RecorderState.Recording(Duration.ofSeconds(3)),
        )
    )
    composeTestRule.onNodeWithText(text(R.string.voice_note_recording, "0:03")).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.RECORDING_HINT)
        .assertTextContains(text(R.string.voice_note_recording_hint))
    tag(VoiceNoteRecordTestTags.RECORDING_BAR).assertIsDisplayed()
  }

  /** Breaks if releasing the button does not call the release callback. */
  @Test
  fun holdAndReleaseCallsRelease() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      up()
    }
    assertEquals(listOf("start", "release"), events)
  }

  /** Breaks if sliding left and releasing keeps the recording instead of dropping it. */
  @Test
  fun slideLeftThenReleaseCancels() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      moveBy(Offset(-400f, 0f))
      up()
    }
    assertEquals(listOf("start", "cancel"), events)
  }

  /** Breaks if the info note or its button is missing, or the button still records. */
  @Test
  fun deniedMicrophoneShowsTheNoteAndDisablesRecording() {
    show(VoiceNoteUiState(canManage = true, micDenied = true))
    // The tag is on the card and the text is in a child node, so each is checked on its own.
    tag(VoiceNoteRecordTestTags.MIC_DENIED).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_mic_denied)).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.OPEN_SETTINGS).performClick()
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      up()
    }
    assertEquals(listOf("settings"), events)
  }

  /** Breaks if a failed send shows no snackbar, or its Retry does nothing. */
  @Test
  fun sendFailedShowsRetry() {
    show(VoiceNoteUiState(canManage = true, sendFailed = true))
    tag(VoiceNoteRecordTestTags.SEND_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_send_failed)).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.SEND_RETRY).performClick()
    assertEquals(listOf("retry"), events)
  }

  /** Breaks if a failed playback shows no snackbar to a volunteer, or its Retry does nothing. */
  @Test
  fun playbackFailedShowsRetryForVolunteers() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)))
    tag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.PLAYBACK_RETRY).performClick()
    assertEquals(listOf("retry"), events)
  }

  /** Breaks if a notice shows without a reason. */
  @Test
  fun noNoticesByDefault() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.SEND_ERROR).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.MIC_DENIED).assertDoesNotExist()
  }
}
