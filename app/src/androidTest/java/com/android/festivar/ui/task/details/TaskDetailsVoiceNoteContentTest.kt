// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.VocalNote
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks
import com.android.festivar.ui.voicenote.VoiceNoteTestTags
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The voice note section as placed on the task details screen, with no ViewModel. */
@RunWith(AndroidJUnit4::class)
class TaskDetailsVoiceNoteContentTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val note =
      VocalNote(
          "n1",
          "https://x/n1.m4a",
          Duration.ofSeconds(14),
          LocalDateTime.of(2026, 10, 17, 9, 12),
      )

  private val task =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          description = "Extension reel is in the blue crate by the van.",
          ownerId = "owner",
      )

  private val events = mutableListOf<String>()

  private fun show(voiceNote: VoiceNoteUiState) {
    val state = TaskDetailsUiState(currentUserId = "someone", task = task, isLoading = false)
    val callbacks =
        VoiceNoteCallbacks(
            onTogglePlay = { events += "toggle" },
            onRetry = { events += "retry" },
        )
    composeTestRule.setContent {
      AppTheme {
        TaskDetailsContent(state = state, voiceNote = voiceNote, voiceNoteCallbacks = callbacks)
      }
    }
  }

  @Test
  fun voiceNote_isShownBelowTheDescription() {
    show(VoiceNoteUiState(note = note))

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    val descriptionBottom =
        composeTestRule
            .onNodeWithTag(TaskDetailsScreenTestTags.DESCRIPTION)
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
    val playTop =
        composeTestRule
            .onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_PLAY)
            .fetchSemanticsNode()
            .boundsInRoot
            .top
    assertTrue(playTop >= descriptionBottom)
  }

  @Test
  fun owner_withoutANote_seesTheRecordBarAboveTheActions() {
    show(VoiceNoteUiState(canManage = true))

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.RECORDING_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun volunteer_withoutANote_seesNoVoiceNoteAtAll() {
    show(VoiceNoteUiState())

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.RECORDING_BAR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertDoesNotExist()
  }

  @Test
  fun owner_withANote_hasNoRecordBarButCanDelete() {
    show(VoiceNoteUiState(note = note, canManage = true))

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.RECORDING_BAR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  @Test
  fun volunteer_withANote_canPlayButNotDelete() {
    show(VoiceNoteUiState(note = note))

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_PLAY).performClick()

    assertEquals(listOf("toggle"), events)
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
  }

  @Test
  fun sendFailure_showsANoticeWithRetry() {
    show(VoiceNoteUiState(canManage = true, sendFailed = true))

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.SEND_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.SEND_RETRY).performClick()

    assertEquals(listOf("retry"), events)
  }
}
