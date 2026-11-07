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
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
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

  /** The date and the time of the note of the previews (17/10/2026 at 09:12) as they show it. */
  private val recordedAt =
      formatRecordedAt(ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC), LocalDate.now())

  /** Breaks if the owner preview no longer shows the note with its author line and the trash. */
  @Test
  fun ownerPreview_showsTheNoteWithTrash() {
    composeTestRule.setContent { VoiceNoteOwnerPreview() }

    composeTestRule
        .onNodeWithText(text(R.string.voice_note_author_you, recordedAt))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  /** Breaks if the playing preview no longer offers to stop the note. */
  @Test
  fun ownerPlayingPreview_showsTheStopButton() {
    composeTestRule.setContent { VoiceNoteOwnerPlayingPreview() }

    composeTestRule
        .onNodeWithContentDescription(text(R.string.voice_note_pause))
        .assertIsDisplayed()
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

  /** Breaks if the volunteer preview no longer shows the note without the trash. */
  @Test
  fun volunteerPreview_showsTheNoteWithoutTrash() {
    composeTestRule.setContent { VoiceNoteVolunteerPreview() }

    composeTestRule
        .onNodeWithText(
            text(R.string.voice_note_author_other, text(R.string.voice_note_organizer), recordedAt)
        )
        .assertIsDisplayed()
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

    composeTestRule
        .onNodeWithContentDescription(text(R.string.voice_note_pause))
        .assertIsDisplayed()
  }

  /** Breaks if the preview of the player with the trash no longer shows the trash. */
  @Test
  fun playerWithTrashPreview_showsTheTrash() {
    composeTestRule.setContent { VoiceNotePlayerWithTrashPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }
}
