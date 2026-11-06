// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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

  /** Breaks if the owner preview no longer shows the note with its author line and the trash. */
  @Test
  fun ownerPreview_showsTheNoteWithTrash() {
    composeTestRule.setContent { VoiceNoteOwnerPreview() }

    composeTestRule.onNodeWithText("You · 09:12").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  /** Breaks if the playing preview no longer offers to stop the note. */
  @Test
  fun ownerPlayingPreview_showsTheStopButton() {
    composeTestRule.setContent { VoiceNoteOwnerPlayingPreview() }

    composeTestRule.onNodeWithContentDescription("Stop the voice note").assertIsDisplayed()
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

    composeTestRule.onNodeWithText("Delete this voice note?").assertIsDisplayed()
  }

  /** Breaks if the volunteer preview no longer shows the note without the trash. */
  @Test
  fun volunteerPreview_showsTheNoteWithoutTrash() {
    composeTestRule.setContent { VoiceNoteVolunteerPreview() }

    composeTestRule.onNodeWithText("Organizer · 09:12").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
  }

  /** Breaks if the player preview no longer shows the play button and the duration. */
  @Test
  fun playerPreview_showsThePlayButtonAndTheDuration() {
    composeTestRule.setContent { VoiceNotePlayerPreview() }

    composeTestRule.onNodeWithContentDescription("Play the voice note").assertIsDisplayed()
    composeTestRule.onNodeWithText("0:14").assertIsDisplayed()
  }

  /** Breaks if the playing preview of the player no longer offers to stop the note. */
  @Test
  fun playerPlayingPreview_showsTheStopButton() {
    composeTestRule.setContent { VoiceNotePlayerPlayingPreview() }

    composeTestRule.onNodeWithContentDescription("Stop the voice note").assertIsDisplayed()
  }
}
