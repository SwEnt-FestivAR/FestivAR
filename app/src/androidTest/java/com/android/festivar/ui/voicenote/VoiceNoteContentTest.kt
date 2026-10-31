// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
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

/** The voice note section, the notices and the record bar for every state, with no ViewModel. */
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
    AppTheme {
      Column {
        VoiceNoteSection(state, callbacks)
        VoiceNoteNotices(state, callbacks)
        VoiceNoteRecordBar(state, callbacks)
      }
    }
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
  fun volunteerSeesTheNoteWithoutTrashNorRecordBar() {
    show(VoiceNoteUiState(note = note, canManage = false))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR).assertTextContains("Organizer · 09:12")
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
  }

  @Test
  fun volunteerSeesNothingWithoutNote() {
    show(VoiceNoteUiState())
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
  }

  @Test
  fun recordBarIsHiddenWhenANoteExists() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
    tag(VoiceNoteTestTags.RECORDING_BAR).assertDoesNotExist()
  }

  @Test
  fun emptyStateWithRecordBar() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText("No voice note yet").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hold to record").assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).assertIsDisplayed()
    tag(VoiceNoteTestTags.RECORDING_HINT).assertDoesNotExist()
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
  fun recordingShowsTimerAndHint() {
    show(
        VoiceNoteUiState(
            canManage = true,
            recorder = RecorderState.Recording(Duration.ofSeconds(3)),
        )
    )
    composeTestRule.onNodeWithText("Recording 0:03 · Release to send").assertIsDisplayed()
    tag(VoiceNoteTestTags.RECORDING_HINT)
        .assertTextContains("Release to send. Slide left to cancel.")
    tag(VoiceNoteTestTags.RECORDING_BAR).assertIsDisplayed()
  }

  @Test
  fun holdAndReleaseSends() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      up()
    }
    assertEquals(listOf("start", "release"), events)
  }

  @Test
  fun slideLeftThenReleaseCancels() {
    show(VoiceNoteUiState(canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      moveBy(Offset(-400f, 0f))
      up()
    }
    assertEquals(listOf("start", "cancel"), events)
  }

  @Test
  fun deniedMicrophoneShowsTheNoteAndDisablesRecording() {
    show(VoiceNoteUiState(canManage = true, micDenied = true))
    // The tag is on the card and the text is in a child node, so each is checked on its own.
    tag(VoiceNoteTestTags.MIC_DENIED).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Microphone is off for FestivAR", substring = true)
        .assertIsDisplayed()
    tag(VoiceNoteTestTags.OPEN_SETTINGS).performClick()
    tag(VoiceNoteTestTags.VOICE_NOTE_RECORD).performTouchInput {
      down(center)
      up()
    }
    assertEquals(listOf("settings"), events)
  }

  @Test
  fun sendFailedShowsRetry() {
    show(VoiceNoteUiState(canManage = true, sendFailed = true))
    tag(VoiceNoteTestTags.SEND_ERROR).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Voice note not sent. Check your connection.")
        .assertIsDisplayed()
    tag(VoiceNoteTestTags.SEND_RETRY).performClick()
    assertEquals(listOf("retry"), events)
  }

  @Test
  fun playbackFailedShowsRetryForVolunteers() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Failed(note.audioUrl)))
    tag(VoiceNoteTestTags.PLAYBACK_ERROR).assertIsDisplayed()
    tag(VoiceNoteTestTags.PLAYBACK_RETRY).performClick()
    assertEquals(listOf("retry"), events)
  }

  @Test
  fun noErrorsByDefault() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.PLAYBACK_ERROR).assertDoesNotExist()
    tag(VoiceNoteTestTags.SEND_ERROR).assertDoesNotExist()
    tag(VoiceNoteTestTags.MIC_DENIED).assertDoesNotExist()
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
