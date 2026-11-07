// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The voice note section for every state, with no ViewModel. */
@RunWith(AndroidJUnit4::class)
class VoiceNoteContentTest {
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
          onRetry = { events += "retry" },
          onOpenSettings = { events += "settings" },
      )

  private fun show(state: VoiceNoteUiState) = composeTestRule.setContent {
    AppTheme { VoiceNoteSection(state, callbacks) }
  }

  private fun tag(tag: String) = composeTestRule.onNodeWithTag(tag)

  /**
   * Breaks if the owner no longer sees the author line, the duration, the play button and the
   * trash.
   */
  @Test
  fun ownerSeesTheNoteWithTrash() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(text(R.string.voice_note_author_you, "09:12"))
    tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).assertTextContains("0:14")
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  /** Breaks if a volunteer sees the trash, or the note is not credited to the organizer. */
  @Test
  fun volunteerSeesTheNoteWithoutTrash() {
    show(VoiceNoteUiState(note = note, canManage = false))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(text(R.string.voice_note_author_organizer, "09:12"))
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
  }

  /** Breaks if a volunteer sees the empty state or a player for a task with no note. */
  @Test
  fun volunteerSeesNothingWithoutNote() {
    show(VoiceNoteUiState())
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertDoesNotExist()
  }

  /** Breaks if the owner of a task without a note no longer sees the empty state and its texts. */
  @Test
  fun ownerSeesTheEmptyStateWithoutNote() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_empty_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_empty_body)).assertIsDisplayed()
  }

  /** Breaks if the empty state stays on screen next to an existing note. */
  @Test
  fun emptyStateIsHiddenWhenANoteExists() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
  }

  /** Breaks if tapping the play button does not call onTogglePlay exactly once. */
  @Test
  fun playTogglesThroughTheCallback() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).performClick()
    assertEquals(listOf("toggle"), events)
  }

  /** Breaks if the button does not offer to stop the note while it plays. */
  @Test
  fun playButtonChangesWhilePlaying() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Playing(note.audioUrl)))
    composeTestRule
        .onNodeWithContentDescription(text(R.string.voice_note_pause))
        .assertIsDisplayed()
  }

  /** Breaks if the button does not offer to play the note when nothing plays. */
  @Test
  fun idlePlayButtonOffersToPlay() {
    show(VoiceNoteUiState(note = note))
    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_play)).assertIsDisplayed()
  }

  /** Breaks if the trash deletes right away instead of only asking to confirm. */
  @Test
  fun trashAsksToConfirm() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).performClick()
    assertEquals(listOf("delete"), events)
  }

  /** Breaks if the dialog loses its texts, or its buttons stop calling dismiss and confirm. */
  @Test
  fun deleteDialogOffersKeepAndDelete() {
    show(VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_delete_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_delete_body)).assertIsDisplayed()
    tag(VoiceNoteTestTags.KEEP_BUTTON).performClick()
    tag(VoiceNoteTestTags.CONFIRM_DELETE_BUTTON).performClick()
    assertEquals(listOf("dismiss", "confirm"), events)
  }

  /** Breaks if the delete dialog is shown without being asked for. */
  @Test
  fun noDialogByDefault() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertDoesNotExist()
  }
}
