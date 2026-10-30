// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.ui.components.FieldError
import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the Create task screen on a device or emulator, through the tags annotated in Figma. The
 * validation rules and the saving logic are tested in `CreateTaskViewModelTest`; these tests check
 * that the screen shows them and forwards the user's input.
 */
@RunWith(AndroidJUnit4::class)
class CreateTaskScreenInstrumentedTest : CreateTaskTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private val fieldTags =
      listOf(
          CreateTaskScreenTestTags.TITLE_FIELD,
          CreateTaskScreenTestTags.DESCRIPTION_FIELD,
          CreateTaskScreenTestTags.LOCATION_FIELD,
          CreateTaskScreenTestTags.START_DATE_FIELD,
          CreateTaskScreenTestTags.START_TIME_FIELD,
          CreateTaskScreenTestTags.END_DATE_FIELD,
          CreateTaskScreenTestTags.END_TIME_FIELD,
          CreateTaskScreenTestTags.PRIORITY_SELECTOR,
          CreateTaskScreenTestTags.MAX_ASSIGN_FIELD,
          CreateTaskScreenTestTags.ESTIMATED_TIME_FIELD,
      )

  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.showCreateTaskScreen()
  }

  @Test
  fun displayAllComponents() {
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Create task")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON)
        .assertIsDisplayed()
        .assertIsNotEnabled() // The form is empty.
    fieldTags.forEach { composeTestRule.onNodeWithTag(it).performScrollTo().assertIsDisplayed() }
    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  @Test
  fun aBlankTitleShowsAnErrorUntilATitleIsEntered() {
    composeTestRule.enterCreateTaskTitle(" ") // Title is mandatory
    composeTestRule.checkTitleErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    composeTestRule.enterCreateTaskTitle("Run power to stage")

    composeTestRule.checkNoFieldErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun aTimeWithoutItsDateShowsAnErrorUnderThatDate() {
    composeTestRule.pickCreateTaskTime(
        CreateTaskScreenTestTags.START_TIME_FIELD,
        PICKER_DEFAULT_TIME,
    )
    composeTestRule.checkStartDateErrorIsDisplayed(FieldError.MISSING_DATE)

    composeTestRule.pickCreateTaskTime(CreateTaskScreenTestTags.END_TIME_FIELD, PICKER_DEFAULT_TIME)
    composeTestRule.checkEndDateErrorIsDisplayed(FieldError.MISSING_DATE)
  }

  @Test
  fun anEndBeforeTheStartShowsAnErrorUntilTheScheduleIsFixed() {
    val day: LocalDate = completeForm.startDate!!
    composeTestRule.enterCreateTaskTitle("Run power to stage")
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.START_DATE_FIELD, day.plusDays(1))
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.END_DATE_FIELD, day)
    composeTestRule.checkEndDateErrorIsDisplayed(FieldError.END_BEFORE_START)
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.END_DATE_FIELD, day.plusDays(2))

    composeTestRule.checkNoFieldErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun thePeopleNeededStepperStartsAtOneWithTheMinusDisabled() {
    composeTestRule.checkCreateTaskMaxAssignIs(1)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON)
        .assertIsNotEnabled()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_INCREASE_BUTTON)
        .assertIsEnabled()
  }

  @Test
  fun thePlusAndMinusChangeThePeopleNeededByOneDownToOne() {
    composeTestRule.enterCreateTaskTitle("Run power to stage")

    composeTestRule.increaseCreateTaskMaxAssign(times = 2)
    composeTestRule.checkCreateTaskMaxAssignIs(3)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON)
        .assertIsEnabled()

    composeTestRule.decreaseCreateTaskMaxAssign()
    composeTestRule.checkCreateTaskMaxAssignIs(2)
    composeTestRule.decreaseCreateTaskMaxAssign()
    composeTestRule.checkCreateTaskMaxAssignIs(1)

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON)
        .assertIsNotEnabled()
    composeTestRule.checkNoFieldErrorIsDisplayed() // The stepper can never be invalid.
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun anEstimatedTimeOfZeroShowsAnErrorUntilItIsFixedOrCleared() {
    composeTestRule.enterCreateTaskTitle("Run power to stage")
    composeTestRule.enterCreateTaskEstimatedTime("0")
    composeTestRule.checkEstimatedTimeErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    composeTestRule.enterCreateTaskEstimatedTime("") // Optional: empty is fine.

    composeTestRule.checkNoFieldErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun aMinusSignTypedInTheEstimatedTimeIsNotKept() {
    composeTestRule.enterCreateTaskEstimatedTime("-")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.ESTIMATED_TIME_FIELD)
        .assert(!hasText("-", substring = true))

    composeTestRule.enterCreateTaskEstimatedTime("-30")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.ESTIMATED_TIME_FIELD)
        .assert(hasText("30", substring = true) and !hasText("-", substring = true))
  }

  @Test
  fun savingThePriorityThePeopleAndTheEstimatedTimeAddsThemToTheTask() {
    composeTestRule.enterCreateTaskTitle("Run power to stage")
    composeTestRule.pickCreateTaskPriority(Priority.URGENT)
    composeTestRule.increaseCreateTaskMaxAssign(times = 2)
    composeTestRule.enterCreateTaskEstimatedTime("90")
    composeTestRule.clickOnCreate(waitForRedirection = true)

    val saved = savedTasks().single()
    assertEquals(Priority.URGENT, saved.priority)
    assertEquals(3, saved.maxAssign)
    assertEquals(Duration.ofMinutes(90), saved.estimatedTime)
  }

  @Test
  fun savingACompleteFormAddsTheTaskAndRedirects() {
    composeTestRule.enterCreateTaskDetails(completeForm)
    composeTestRule.clickOnCreate(waitForRedirection = true)

    val saved = savedTasks().single()
    assertEquals(EVENT_ID, saved.eventId)
    assertEquals(completeForm.title, saved.title)
    assertEquals(completeForm.description, saved.description)
    assertEquals(completeForm.location, saved.location)
    assertEquals(completeForm.startDateTime, saved.startTime)
    assertEquals(completeForm.endDateTime, saved.endTime)
    assertEquals(1, createdCount)
    assertEquals(0, backCount)
  }

  @Test
  fun aFailedSaveShowsAnErrorAndCanBeRetried() {
    addTaskFails = true
    composeTestRule.enterCreateTaskDetails(titleOnlyForm)
    composeTestRule.clickOnCreate()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { viewModel.uiState.value.errorMsg != null }
    val message =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.create_task_error_save_failed)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SAVE_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(message)
    assertTrue(savedTasks().isEmpty())
    assertEquals(0, createdCount)

    addTaskFails = false
    composeTestRule.clickOnCreate(waitForRedirection = true)

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SAVE_ERROR).assertDoesNotExist()
    assertEquals(titleOnlyForm.title, savedTasks().single().title)
    assertEquals(1, createdCount)
  }

  @Test
  fun goingBackFromAnUntouchedFormLeavesWithoutAsking() {
    composeTestRule.clickOnBack()
    assertEquals(1, backCount)
  }

  @Test
  fun goingBackFromAnEditedFormAsksBeforeLeaving() {
    composeTestRule.enterCreateTaskDetails(completeForm)
    composeTestRule.clickOnBack()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText(string(R.string.create_task_discard_title)).assertIsDisplayed()
    assertEquals(0, backCount)

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertDoesNotExist()
    assertEquals(0, backCount)
  }

  @Test
  fun systemBackFromAnUntouchedFormLeavesWithoutAsking() {
    Espresso.pressBack()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertDoesNotExist()
    assertEquals(1, backCount)
  }

  @Test
  fun systemBackFromAnEditedFormAsksBeforeLeaving() {
    composeTestRule.enterCreateTaskDetails(completeForm)

    Espresso.pressBack()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
    assertEquals(0, backCount)
  }

  @Test
  fun systemBackDismissesTheOpenDiscardDialogAndStaysOnTheForm() {
    composeTestRule.enterCreateTaskDetails(completeForm)
    Espresso.pressBack()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()

    Espresso.pressBack() // Closes the dialog, like "Keep editing".
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE).assertIsDisplayed()
    assertEquals(0, backCount)
  }

  @Test
  fun goingBackOnceTheTaskIsCreatedLeavesWithoutAsking() {
    composeTestRule.enterCreateTaskDetails(completeForm)
    composeTestRule.clickOnCreate(waitForRedirection = true)

    composeTestRule.clickOnBack()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertDoesNotExist()
    assertEquals(1, backCount)
  }

  @Test
  fun discardingAnEditedFormLeavesTheScreenWithoutSaving() {
    composeTestRule.enterCreateTaskDetails(completeForm)
    composeTestRule.clickOnBack()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_BUTTON).performClick()
    assertEquals(1, backCount)
    assertEquals(0, createdCount)
    assertTrue(savedTasks().isEmpty())
  }
}
