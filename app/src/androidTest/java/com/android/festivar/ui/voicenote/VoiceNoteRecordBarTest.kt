// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.theme.AppTheme
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
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

  // JUnit creates a new instance of the class for every test, so these mocks start every test
  // without any call
  private val onRecordStart = mockk<() -> Unit>(relaxed = true)
  private val onRecordRelease = mockk<() -> Unit>(relaxed = true)
  private val onRetry = mockk<() -> Unit>(relaxed = true)
  private val onOpenSettings = mockk<() -> Unit>(relaxed = true)

  private val callbacks =
      VoiceNoteCallbacks.NoOp.let {
        VoiceNoteCallbacks(
            onTogglePlay = it.onTogglePlay,
            onDelete = it.onDelete,
            onConfirmDelete = it.onConfirmDelete,
            onDismissDelete = it.onDismissDelete,
            onRecordStart = onRecordStart,
            onRecordRelease = onRecordRelease,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
        )
      }

  /** Fails if a callback was called that the test did not verify, so a stray call is caught. */
  private fun assertNoOtherCall() =
      confirmVerified(onRecordStart, onRecordRelease, onRetry, onOpenSettings)

  private fun show(state: VoiceNoteUiState) = composeTestRule.setContent {
    AppTheme {
      FixedBottomBar {
        VoiceNotePlaybackNotice(state, callbacks)
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

  /** Breaks if the idle bar does not show the hint and the record button. */
  @Test
  fun idleBarInvitesToHoldToRecord() {
    show(VoiceNoteUiState(canManage = true))
    composeTestRule.onNodeWithText(text(R.string.voice_note_hold_to_record)).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.RECORDING_BAR).assertIsDisplayed()
  }

  /** Breaks if the idle bar shows the red recording dot. */
  @Test
  fun idleBarHasNoRecordingDot() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.RECORDING_DOT).assertDoesNotExist()
  }

  /** Breaks if the timer or the red dot is missing while recording, or the hint stays. */
  @Test
  fun recordingReplacesTheHintWithTheDotAndTheTimer() {
    show(
        VoiceNoteUiState(
            canManage = true,
            recorder = RecorderState.Recording(Duration.ofSeconds(3)),
        )
    )
    composeTestRule.onNodeWithText(text(R.string.voice_note_recording, "0:03")).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.RECORDING_DOT).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_hold_to_record)).assertDoesNotExist()
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
    verifySequence {
      onRecordStart()
      onRecordRelease()
    }
  }

  /** Breaks if a second press does not record, or if a press calls release more than once. */
  @Test
  fun eachPressCallsStartAndReleaseOnce() {
    show(VoiceNoteUiState(canManage = true))
    repeat(2) {
      tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
        down(center)
        up()
      }
    }
    verifySequence {
      onRecordStart()
      onRecordRelease()
      onRecordStart()
      onRecordRelease()
    }
  }

  /** Breaks if the system taking the gesture away leaves the recorder on. */
  @Test
  fun cancelledGestureStillReleases() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      cancel()
    }
    verifySequence {
      onRecordStart()
      onRecordRelease()
    }
  }

  /** Breaks if the finger sliding off the button leaves the recorder on until the finger lifts. */
  @Test
  fun slidingOffTheButtonReleases() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      moveBy(Offset(-400f, 0f))
      up()
    }
    verifySequence {
      onRecordStart()
      onRecordRelease()
    }
  }

  /** Breaks if the info note about the microphone is missing, or its text is not shown. */
  @Test
  fun deniedMicrophoneShowsTheNote() {
    show(VoiceNoteUiState(canManage = true, micDenied = true))
    // The tag is on the card and the text is in a child node, so each is checked on its own.
    tag(VoiceNoteRecordTestTags.MIC_DENIED).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_mic_denied)).assertIsDisplayed()
  }

  /** Breaks if the button of the info note does not open the settings. */
  @Test
  fun deniedMicrophoneButtonOpensTheSettings() {
    show(VoiceNoteUiState(canManage = true, micDenied = true))
    tag(VoiceNoteRecordTestTags.OPEN_SETTINGS).performClick()
    verify(exactly = 1) { onOpenSettings() }
    assertNoOtherCall()
  }

  /** Breaks if the record button still records without the microphone permission. */
  @Test
  fun deniedMicrophoneDisablesRecording() {
    show(VoiceNoteUiState(canManage = true, micDenied = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      up()
    }
    verify(exactly = 0) { onRecordStart() }
    assertNoOtherCall()
  }

  /** Breaks if a failed send shows no snackbar, or its Retry does nothing. */
  @Test
  fun sendFailedShowsRetry() {
    show(VoiceNoteUiState(canManage = true, sendFailed = true))
    tag(VoiceNoteRecordTestTags.SEND_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_send_failed)).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.SEND_RETRY).performClick()
    verify(exactly = 1) { onRetry() }
    assertNoOtherCall()
  }

  /** Breaks if a failed playback shows no snackbar to a volunteer, or its Retry does nothing. */
  @Test
  fun playbackFailedShowsRetryForVolunteers() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)))
    tag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.PLAYBACK_RETRY).performClick()
    verify(exactly = 1) { onRetry() }
    assertNoOtherCall()
  }

  /** Breaks if the send or microphone notice shows although nothing failed and the mic is on. */
  @Test
  fun noNoticesByDefault() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.SEND_ERROR).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.MIC_DENIED).assertDoesNotExist()
  }

  /** Breaks if the send or microphone notice shows next to a note, where there is no bar. */
  @Test
  fun sendAndMicrophoneNoticesAreHiddenWhenTheBarIsHidden() {
    show(VoiceNoteUiState(note = note, canManage = true, sendFailed = true, micDenied = true))
    tag(VoiceNoteRecordTestTags.SEND_ERROR).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.MIC_DENIED).assertDoesNotExist()
  }

  /** Breaks if a volunteer without an error still sees a record bar or a notice. */
  @Test
  fun nothingIsShownForAVolunteerWithoutAnError() {
    show(VoiceNoteUiState(note = note, canManage = false))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
    tag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertDoesNotExist()
  }

  /** Breaks if a notice is drawn over or under the record bar instead of above it. */
  @Test
  fun noticesAreAboveTheRecordBar() {
    show(VoiceNoteUiState(canManage = true, sendFailed = true))
    val noticeBottom = tag(VoiceNoteRecordTestTags.SEND_ERROR).getUnclippedBoundsInRoot().bottom
    val barTop = tag(VoiceNoteRecordTestTags.RECORDING_BAR).getUnclippedBoundsInRoot().top
    assertTrue("the notice must end above the record bar", noticeBottom <= barTop)
  }

  /**
   * Breaks if one of the two notices hides the other, or if they are not stacked as in the design:
   * the failed send, then the microphone, then the bar.
   */
  @Test
  fun bothNoticesAreShownAboveTheBarInOrder() {
    show(VoiceNoteUiState(canManage = true, sendFailed = true, micDenied = true))
    composeTestRule.onNodeWithText(text(R.string.voice_note_send_failed)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_mic_denied)).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.SEND_RETRY).assertIsDisplayed()
    tag(VoiceNoteRecordTestTags.OPEN_SETTINGS).assertIsDisplayed()

    val sendBottom = tag(VoiceNoteRecordTestTags.SEND_ERROR).getUnclippedBoundsInRoot().bottom
    val micBounds = tag(VoiceNoteRecordTestTags.MIC_DENIED).getUnclippedBoundsInRoot()
    val barTop = tag(VoiceNoteRecordTestTags.RECORDING_BAR).getUnclippedBoundsInRoot().top
    assertTrue("the send notice must end above the microphone notice", sendBottom <= micBounds.top)
    assertTrue("the microphone notice must end above the record bar", micBounds.bottom <= barTop)
  }

  /** Breaks if turning the microphone off in the middle of a recording leaves the recorder on. */
  @Test
  fun interruptedGestureStillReleases() {
    var state by mutableStateOf(VoiceNoteUiState(canManage = true))
    composeTestRule.setContent {
      AppTheme { FixedBottomBar { VoiceNoteRecordBar(state, callbacks) } }
    }
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).performTouchInput { down(center) }
    composeTestRule.runOnIdle { state = VoiceNoteUiState(canManage = true, micDenied = true) }
    composeTestRule.waitForIdle()
    verifySequence {
      onRecordStart()
      onRecordRelease()
    }
  }

  /** Breaks if the record button is not announced as an enabled, labelled button. */
  @Test
  fun recordButtonIsAnEnabledLabelledButtonWithTheMicrophone() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD)
        .assertIsEnabled()
        .assertContentDescriptionEquals(text(R.string.voice_note_record))
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
  }

  /** Breaks if the record button is not announced as a disabled, labelled button without it. */
  @Test
  fun recordButtonIsADisabledLabelledButtonWithoutTheMicrophone() {
    show(VoiceNoteUiState(canManage = true, micDenied = true))
    tag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD)
        .assertIsNotEnabled()
        .assertContentDescriptionEquals(text(R.string.voice_note_record))
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
  }
}
