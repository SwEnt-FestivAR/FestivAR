// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.ui.components.FieldError
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
    fieldTags.forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }
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
  fun goingBackLeavesTheScreenWithoutSaving() {
    composeTestRule.enterCreateTaskDetails(completeForm)
    composeTestRule.clickOnBack()
    assertEquals(1, backCount)
    assertEquals(0, createdCount)
    assertTrue(savedTasks().isEmpty())
  }
}
