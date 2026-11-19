// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.festivar.R
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.LONG_TEXT_MAX_LENGTH
import com.android.festivar.ui.components.checkTheOnlyCounterIsUnder
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * The shared parts of the announcement forms, drawn with test-only tags and texts, so that a part
 * that falls back to the Create announcement ones fails.
 */
class AnnouncementFormUiTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val tags =
      AnnouncementFormTags(
          backButton = "testBack",
          title = "testTitle",
          titleField = "testTitleField",
          titleError = "testTitleError",
          descriptionField = "testDescriptionField",
          descriptionError = "testDescriptionError",
          priorityLabel = "testPriorityLabel",
          saveError = "testSaveError",
          submitButton = "testSubmit",
          savingIndicator = "testSavingIndicator",
          discardDialog = "testDiscardDialog",
          keepEditingButton = "testKeepEditing",
          discardButton = "testDiscard",
          priorityChip = { "testPriority_${it.name}" },
      )
  private val labels =
      AnnouncementFormLabels(
          title = R.string.edit_task,
          submit = R.string.edit_task_save,
          submitting = R.string.signup_signing_up,
      )

  private var backCount = 0
  // Plain values, not Compose state: the back handling must read them when back is pressed.
  private var saving = false
  private var needsConfirmation = false

  // Compose state: the content redraws when a test changes them.
  private var form by mutableStateOf(AnnouncementFormState())
  private var isSaving by mutableStateOf(false)
  private var isEditable by mutableStateOf(true)
  private var canSubmit by mutableStateOf(false)
  private var errorMsg by mutableStateOf<Int?>(null)

  private fun string(@StringRes id: Int) = composeTestRule.activity.getString(id)

  private fun node(tag: String) = composeTestRule.onNodeWithTag(tag)

  /** Everything the user can edit: both text fields and every priority chip. */
  private fun inputs() =
      (listOf(tags.titleField, tags.descriptionField) +
              AnnouncementPriority.entries.map(tags.priorityChip))
          .map(::node)

  private fun setForm(actions: AnnouncementFormActions = AnnouncementFormActions()) {
    composeTestRule.setContent {
      AppTheme {
        AnnouncementFormContent(
            form = form,
            isSaving = isSaving,
            isEditable = isEditable,
            canSubmit = canSubmit,
            errorMsg = errorMsg,
            actions = actions,
            labels = labels,
            tags = tags,
        )
      }
    }
  }

  /** The back handling around the top bar, whose button calls `requestBack`. */
  private fun setBackHandling() {
    composeTestRule.setContent {
      AppTheme {
        AnnouncementFormBackHandling(
            isSaving = { saving },
            needsConfirmation = { needsConfirmation },
            onBack = { backCount++ },
            dialogTitle = R.string.edit_task_discard_title,
            tags = tags,
            dialogMessage = R.string.edit_task_error_save_failed,
        ) { requestBack ->
          AnnouncementFormTopBar(labels.title, requestBack, tags)
        }
      }
    }
  }

  private fun pressSystemBack() {
    composeTestRule.runOnUiThread {
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if the top bar drops its title or tags, mislabels its back button, or ignores a tap. */
  @Test
  fun theTopBarShowsItsTitleAndItsBackCallsOnBack() {
    composeTestRule.setContent {
      AppTheme { AnnouncementFormTopBar(labels.title, { backCount++ }, tags) }
    }

    node(tags.title).assertTextEquals(string(R.string.edit_task))
    node(tags.backButton)
        .assertContentDescriptionEquals(string(R.string.announcement_form_back))
        .performClick()
    assertEquals(1, backCount)
  }

  /** Breaks if either back asks, or does not leave, when there is nothing to lose. */
  @Test
  fun withNothingToLoseBothBacksLeaveAtOnce() {
    setBackHandling()

    node(tags.backButton).performClick()
    pressSystemBack()

    assertEquals(2, backCount)
    node(tags.discardDialog).assertDoesNotExist()
  }

  /**
   * Breaks if a back leaves an edited form without asking, if the dialog shows another title or
   * message than the given ones, if "Keep editing" leaves, or if "Discard" does not leave once.
   */
  @Test
  fun withSomethingToLoseBothBacksAskAndOnlyDiscardLeaves() {
    needsConfirmation = true
    setBackHandling()

    node(tags.backButton).performClick()
    node(tags.discardDialog).assertIsDisplayed()
    composeTestRule.onNodeWithText(string(R.string.edit_task_discard_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(string(R.string.edit_task_error_save_failed)).assertIsDisplayed()
    node(tags.keepEditingButton).performClick()
    node(tags.discardDialog).assertDoesNotExist()
    assertEquals(0, backCount)

    pressSystemBack()
    node(tags.discardButton).performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, backCount)
    node(tags.discardDialog).assertDoesNotExist()
  }

  /**
   * Breaks if either back leaves or asks during a save, with or without something to lose, or if
   * the back handler is turned off so that system back closes the activity.
   */
  @Test
  fun duringASaveBothBacksDoNothing() {
    saving = true
    needsConfirmation = true
    setBackHandling()

    node(tags.backButton).performClick()
    pressSystemBack()
    needsConfirmation = false
    node(tags.backButton).performClick()
    pressSystemBack()

    assertEquals(0, backCount)
    node(tags.discardDialog).assertDoesNotExist()
    assertFalse(composeTestRule.activity.isFinishing)
  }

  /**
   * Breaks if a flag is read at composition: a back pressed right after a change, before the next
   * recomposition, would then act on the old value.
   */
  @Test
  fun theFlagsAreReadWhenBackIsPressed() {
    setBackHandling()

    needsConfirmation = true
    node(tags.backButton).performClick()
    node(tags.discardDialog).assertIsDisplayed()
    node(tags.keepEditingButton).performClick()
    composeTestRule.waitForIdle()

    saving = true
    pressSystemBack()
    node(tags.discardDialog).assertDoesNotExist()
    assertEquals(0, backCount)
  }

  /**
   * Breaks if the content ignores the given labels or tags, if the counter leaves the description,
   * if an input reaches the wrong callback, or if `canSubmit` does not enable the button.
   */
  @Test
  fun showsTheGivenTextsAndTagsAndReportsEveryInput() {
    val reported = mutableListOf<Any>()
    canSubmit = true
    // The typed text is drawn back, as a screen does, or the field would report a reset to "".
    setForm(
        AnnouncementFormActions(
            onBack = { reported += "back" },
            onTitleChange = {
              form = form.copy(title = it)
              reported += "title:$it"
            },
            onContentChange = {
              form = form.copy(content = it)
              reported += "content:$it"
            },
            onPriorityChange = { reported += it },
            onSubmit = { reported += "submit" },
        )
    )

    node(tags.title).assertTextEquals(string(R.string.edit_task))
    composeTestRule.checkTheOnlyCounterIsUnder(tags.descriptionField, "0/$LONG_TEXT_MAX_LENGTH")
    node(tags.titleField).performTextInput("T")
    node(tags.descriptionField).performTextInput("C")
    node(tags.priorityChip(AnnouncementPriority.URGENT)).performClick()
    node(tags.backButton).performClick()
    node(tags.submitButton)
        .assertTextEquals(string(R.string.edit_task_save))
        .assertIsEnabled()
        .performClick()

    assertEquals(
        listOf("title:T", "content:C", AnnouncementPriority.URGENT, "back", "submit"),
        reported,
    )
  }

  /**
   * Breaks if the button follows the form's validity instead of `canSubmit`, or if a save does not
   * show the spinner with the given description in place of the button text.
   */
  @Test
  fun theButtonFollowsCanSubmitAndShowsTheSpinnerDuringASave() {
    form = AnnouncementFormState(title = "Stage B", content = "Wind")
    setForm()

    node(tags.submitButton).assertIsNotEnabled()
    isSaving = true
    composeTestRule
        .onNodeWithTag(tags.savingIndicator, useUnmergedTree = true)
        .assertContentDescriptionEquals(string(R.string.signup_signing_up))
    composeTestRule.onNodeWithText(string(R.string.edit_task_save)).assertDoesNotExist()
  }

  /**
   * Breaks if a field or a chip stays editable on a locked form, or if the save error is hidden.
   */
  @Test
  fun aLockedFormDisablesEveryInputAndShowsTheSaveError() {
    isEditable = false
    errorMsg = R.string.edit_task_error_save_failed
    setForm()

    inputs().forEach { it.assertIsNotEnabled() }
    node(tags.saveError).assertTextEquals(string(R.string.edit_task_error_save_failed))
  }

  /** Breaks if a field error is shown under the other field, or under both. */
  @Test
  fun eachFieldErrorAppearsUnderItsOwnField() {
    form = AnnouncementFormState(title = "", content = "Wind", titleEdited = true)
    setForm()

    node(tags.titleError).assertTextEquals(string(R.string.field_error_empty_announcement_title))
    node(tags.descriptionError).assertDoesNotExist()

    form = AnnouncementFormState(title = "Stage B", content = "", contentEdited = true)
    node(tags.descriptionError)
        .assertTextEquals(string(R.string.field_error_empty_announcement_content))
    node(tags.titleError).assertDoesNotExist()
  }
}
