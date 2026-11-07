// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
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
 * Renders the previews of the voice note player, so that a preview that no longer compiles or
 * crashes when drawn is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class VoiceNotePreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun text(id: Int, vararg args: Any) =
      ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

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
