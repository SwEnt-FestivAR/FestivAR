// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the previews of the record bar and of its notices, so that a preview that no longer
 * compiles or crashes when drawn is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class VoiceNoteRecordBarPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the idle preview no longer shows the bar that invites to hold to record. */
  @Test
  fun idlePreview_showsTheBar() {
    composeTestRule.setContent { VoiceNoteRecordBarIdlePreview() }

    composeTestRule.onNodeWithText("Hold to record").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertIsDisplayed()
  }

  /** Breaks if the recording preview no longer shows the timer and the hint. */
  @Test
  fun recordingPreview_showsTheTimer() {
    composeTestRule.setContent { VoiceNoteRecordBarRecordingPreview() }

    composeTestRule.onNodeWithText("Recording 0:03 · Release to stop").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.RECORDING_HINT).assertIsDisplayed()
  }

  /** Breaks if the send failed preview no longer shows the snackbar with Retry. */
  @Test
  fun sendFailedPreview_showsTheSnackbar() {
    composeTestRule.setContent { VoiceNoteRecordBarSendFailedPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.SEND_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.SEND_RETRY).assertIsDisplayed()
  }

  /** Breaks if the microphone preview no longer shows the info note and its button. */
  @Test
  fun micDeniedPreview_showsTheInfoNote() {
    composeTestRule.setContent { VoiceNoteRecordBarMicDeniedPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.MIC_DENIED).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.OPEN_SETTINGS).assertIsDisplayed()
  }

  /** Breaks if the playback preview no longer shows the snackbar of a volunteer. */
  @Test
  fun playbackFailedPreview_showsTheSnackbar() {
    composeTestRule.setContent { VoiceNotePlaybackFailedPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertIsDisplayed()
  }
}
