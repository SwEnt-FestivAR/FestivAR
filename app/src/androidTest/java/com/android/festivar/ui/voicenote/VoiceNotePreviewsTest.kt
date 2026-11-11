// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the previews of the voice note section, so that a preview that no longer compiles or
 * crashes when drawn is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class VoiceNotePreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun text(id: Int, vararg args: Any) =
      ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

  /** Breaks if the owner preview no longer shows the note with its author line and the trash. */
  @Test
  fun ownerPreview_showsTheNoteWithTrash() {
    composeTestRule.setContent { VoiceNoteOwnerPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  /** Breaks if the playing preview no longer offers to stop the note. */
  @Test
  fun ownerPlayingPreview_showsTheStopButton() {
    composeTestRule.setContent { VoiceNoteOwnerPlayingPreview() }

    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_stop)).assertIsDisplayed()
  }

  /** Breaks if the empty preview no longer shows the empty state. */
  @Test
  fun emptyPreview_showsTheEmptyState() {
    composeTestRule.setContent { VoiceNoteEmptyPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
  }

  /** Breaks if the delete preview no longer shows the confirmation dialog. */
  @Test
  fun deleteConfirmPreview_showsTheDialog() {
    composeTestRule.setContent { VoiceNoteDeleteConfirmPreview() }

    composeTestRule.onNodeWithText(text(R.string.voice_note_delete_title)).assertIsDisplayed()
  }

  /** Breaks if the send preview no longer shows the dialog that asks to send the recording. */
  @Test
  fun sendConfirmPreview_showsTheDialog() {
    composeTestRule.setContent { VoiceNoteSendConfirmPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.CONFIRM_SEND).assertIsDisplayed()
  }

  /** Breaks if the send confirm preview no longer shows the waiting recording behind the dialog. */
  @Test
  fun sendConfirmPreview_showsTheWaitingRecordingBehindTheDialog() {
    composeTestRule.setContent { VoiceNoteSendConfirmPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DURATION).assertExists()
  }

  /** Breaks if the sending preview no longer shows the greyed-out bar with the recording length. */
  @Test
  fun sendingPreview_showsTheSendingBar() {
    composeTestRule.setContent { VoiceNoteSendingPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.VOICE_NOTE_SENDING).assertIsDisplayed()
  }

  /** Breaks if the recording failed preview no longer shows the notice with its Dismiss action. */
  @Test
  fun recordingFailedPreview_showsTheNotice() {
    composeTestRule.setContent { VoiceNoteRecordingFailedPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.RECORDING_ERROR).assertIsDisplayed()
  }

  /** Breaks if the volunteer preview no longer shows the note without the trash. */
  @Test
  fun volunteerPreview_showsTheNoteWithoutTrash() {
    composeTestRule.setContent { VoiceNoteVolunteerPreview() }

    composeTestRule
        .onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(text(R.string.voice_note_organizer), substring = true)
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
  }

  /** Breaks if the player preview no longer shows the play button and the duration. */
  @Test
  fun playerPreview_showsThePlayButtonAndTheDuration() {
    composeTestRule.setContent { VoiceNotePlayerPreview() }

    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_play)).assertIsDisplayed()
    composeTestRule.onNodeWithText("0:14").assertIsDisplayed()
  }

  /** Breaks if the playing preview of the player no longer offers to stop the note. */
  @Test
  fun playerPlayingPreview_showsTheStopButton() {
    composeTestRule.setContent { VoiceNotePlayerPlayingPreview() }

    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_stop)).assertIsDisplayed()
  }

  /** Breaks if the preview of the player with the trash no longer shows the trash. */
  @Test
  fun playerWithTrashPreview_showsTheTrash() {
    composeTestRule.setContent { VoiceNotePlayerWithTrashPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }
}
