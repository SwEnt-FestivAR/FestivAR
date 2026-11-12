// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Draws the previews of the record bar and of its notices once. The behaviour is tested in
 * [VoiceNoteRecordBarTest]; this only catches a preview that crashes when drawn.
 */
@RunWith(AndroidJUnit4::class)
class VoiceNoteRecordBarPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Breaks if one of the previews crashes when drawn, which Android Studio alone would not catch.
   */
  @Test
  fun previewsDraw() {
    composeTestRule.setContent {
      Column {
        VoiceNoteRecordBarIdlePreview()
        VoiceNoteRecordBarRecordingPreview()
        VoiceNoteRecordBarWaitingPreview()
        VoiceNoteRecordBarSendingPreview()
        VoiceNoteRecordBarSendFailedPreview()
        VoiceNoteRecordBarRecordingFailedPreview()
        VoiceNoteRecordBarMicDeniedPreview()
        VoiceNotePlaybackFailedPreview()
      }
    }

    // Each preview is checked on its own tag, not on its position: they do not all fit the screen.
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.SEND_ERROR).assertExists()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.MIC_DENIED).assertExists()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.PLAYBACK_ERROR).assertExists()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.VOICE_NOTE_SENDING).assertExists()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DURATION).assertExists()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.RECORDING_ERROR).assertExists()
  }
}
