// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.voicenote.VoiceNoteRecordTestTags
import com.android.festivar.ui.voicenote.VoiceNoteTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the task details previews so that preview regressions are caught by instrumented tests
 * and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class TaskDetailsPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun openPreview_showsTaskAndTakeAction() {
    composeTestRule.setContent { TaskDetailsOpenPreview() }

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun takenPreview_showsTaskWithoutActions() {
    composeTestRule.setContent { TaskDetailsTakenPreview() }

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun yoursPreview_showsGiveBackAndMarkDoneActions() {
    composeTestRule.setContent { TaskDetailsYoursPreview() }

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun donePreview_showsDoneTaskWithoutActions() {
    composeTestRule.setContent { TaskDetailsDonePreview() }

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.STATUS_BADGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun voiceNoteOwnerPreview_showsEmptyStateAndRecordBar() {
    composeTestRule.setContent { TaskDetailsVoiceNoteOwnerPreview() }

    composeTestRule
        .onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_EMPTY)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertIsDisplayed()
  }

  @Test
  fun voiceNoteVolunteerPreview_showsNoteWithoutRecordBar() {
    composeTestRule.setContent { TaskDetailsVoiceNoteVolunteerPreview() }

    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD).assertDoesNotExist()
  }

  @Test
  fun loadErrorPreview_showsRetryError() {
    composeTestRule.setContent { TaskDetailsLoadErrorPreview() }

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_ACTION).assertIsDisplayed()
  }

  @Test
  fun saveErrorPreview_showsTaskAndDismissError() {
    composeTestRule.setContent { TaskDetailsSaveErrorPreview() }

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_ACTION).assertIsDisplayed()
  }
}
