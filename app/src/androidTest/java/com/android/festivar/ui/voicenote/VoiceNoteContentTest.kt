// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

  @Test
  fun ownerSeesTheNoteWithTrash() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR).assertTextContains("You · 09:12")
    tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).assertTextContains("0:14")
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  @Test
  fun volunteerSeesTheNoteWithoutTrash() {
    show(VoiceNoteUiState(note = note, canManage = false))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR).assertTextContains("Organizer · 09:12")
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
  }

  @Test
  fun volunteerSeesNothingWithoutNote() {
    show(VoiceNoteUiState())
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertDoesNotExist()
  }

  @Test
  fun ownerSeesTheEmptyStateWithoutNote() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText("No voice note yet").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Record a short explanation volunteers can listen to before they start.")
        .assertIsDisplayed()
  }

  @Test
  fun emptyStateIsHiddenWhenANoteExists() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
  }

  @Test
  fun playTogglesThroughTheCallback() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).performClick()
    assertEquals(listOf("toggle"), events)
  }

  @Test
  fun playButtonChangesWhilePlaying() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Playing(note.audioUrl)))
    composeTestRule.onNodeWithContentDescription("Stop the voice note").assertIsDisplayed()
  }

  @Test
  fun idlePlayButtonOffersToPlay() {
    show(VoiceNoteUiState(note = note))
    composeTestRule.onNodeWithContentDescription("Play the voice note").assertIsDisplayed()
  }

  @Test
  fun trashAsksToConfirm() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).performClick()
    assertEquals(listOf("delete"), events)
  }

  @Test
  fun deleteDialogOffersKeepAndDelete() {
    show(VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete this voice note?").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("The volunteers on this task will no longer be able to hear it.")
        .assertIsDisplayed()
    tag(VoiceNoteTestTags.KEEP_BUTTON).performClick()
    tag(VoiceNoteTestTags.CONFIRM_DELETE_BUTTON).performClick()
    assertEquals(listOf("dismiss", "confirm"), events)
  }

  @Test
  fun noDialogByDefault() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertDoesNotExist()
  }
}
