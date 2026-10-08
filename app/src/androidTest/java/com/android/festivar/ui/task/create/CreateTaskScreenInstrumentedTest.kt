// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the Create task screen on a device or emulator, through the tags annotated in Figma. */
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

  // --- Display -------------------------------------------------------------------------------

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
        .assertTextContains("Create task")
    fieldTags.forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }
    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  @Test
  fun createButtonIsDisabledWhenTheFormIsEmpty() {
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun everyFieldIsReachableByScrolling() {
    fieldTags.forEach { composeTestRule.onNodeWithTag(it).performScrollTo().assertIsDisplayed() }
  }

  // --- Entering values -----------------------------------------------------------------------

  @Test
  fun canEnterTitle() {
    val text = "title"
    composeTestRule.enterCreateTaskTitle(text)
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD).assertTextContains(text)
    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  @Test
  fun canEnterDescription() {
    val text = "description"
    composeTestRule.enterCreateTaskDescription(text)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD)
        .assertTextContains(text)
    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  @Test
  fun canEnterLocation() {
    val text = "location"
    composeTestRule.enterCreateTaskLocation(text)
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.LOCATION_FIELD).assertTextContains(text)
    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  @Test
  fun descriptionHasSufficientHeight() {
    val description =
        "This is a very long description that should span multiple lines \n" +
            "in the input field to test whether the height of the description input is sufficient. \n" +
            "Adding even more text to be sure that it is really long and spans multiple lines. \n" +
            "Hopefully this is enough!"
    composeTestRule.enterCreateTaskDescription(description)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD)
        .assertTextContains(description, substring = false)
  }

  @Test
  fun canPickADateAndATime() {
    val date = completeForm.startDate!!
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.START_DATE_FIELD, date)
    composeTestRule.pickCreateTaskTime(
        CreateTaskScreenTestTags.START_TIME_FIELD,
        PICKER_DEFAULT_TIME,
    )

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_DATE_FIELD)
        .assertTextContains(formatDate(date))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD)
        .assertTextContains(formatTime(PICKER_DEFAULT_TIME))
    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  @Test
  fun cancellingAPickerClosesItAndLeavesTheFieldUntouched() {
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD)
        .assertTextEquals("Start time")
  }

  // --- Invalid forms -------------------------------------------------------------------------

  @Test
  fun savingWithABlankTitleShouldDoNothing() = checkNoTaskWereAdded {
    composeTestRule.enterCreateTaskDetails(completeForm.copy(title = " ")) // Title is mandatory
    composeTestRule.clickOnCreate()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
    assertEquals(0, createdCount)
  }

  @Test
  fun savingWithTheEndBeforeTheStartShouldDoNothing() = checkNoTaskWereAdded {
    composeTestRule.enterCreateTaskDetails(
        completeForm.copy(
            startDate = completeForm.endDate, // one day after the end
            endDate = completeForm.startDate,
        )
    )
    composeTestRule.clickOnCreate()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
    assertEquals(0, createdCount)
  }

  @Test
  fun savingATimeWithoutItsDateShouldDoNothing() = checkNoTaskWereAdded {
    composeTestRule.enterCreateTaskDetails(
        titleOnlyForm.copy(startTime = PICKER_DEFAULT_TIME) // no start date
    )
    composeTestRule.clickOnCreate()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
    assertEquals(0, createdCount)
  }

  @Test
  fun enteringTheEndBeforeTheStartShowsErrorMessage() {
    val day: LocalDate = completeForm.startDate!!
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.START_DATE_FIELD, day.plusDays(1))
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.END_DATE_FIELD, day)
    composeTestRule.checkEndDateErrorIsDisplayed(FieldError.END_BEFORE_START)
  }

  @Test
  fun enteringATimeWithoutItsDateShowsErrorMessage() {
    composeTestRule.pickCreateTaskTime(CreateTaskScreenTestTags.END_TIME_FIELD, PICKER_DEFAULT_TIME)
    composeTestRule.checkEndDateErrorIsDisplayed(FieldError.MISSING_DATE)
  }

  @Test
  fun enteringABlankTitleShowsErrorMessage() {
    composeTestRule.enterCreateTaskTitle(" ") // Title is mandatory
    composeTestRule.checkTitleErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun theTitleErrorDisappearsOnceATitleIsEntered() {
    composeTestRule.enterCreateTaskTitle(" ")
    composeTestRule.checkTitleErrorIsDisplayed()

    composeTestRule.enterCreateTaskTitle("Run power to stage")

    composeTestRule.checkNoFieldErrorIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun enteringAStartTimeWithoutItsDateShowsErrorMessageUnderTheStartDate() {
    composeTestRule.pickCreateTaskTime(
        CreateTaskScreenTestTags.START_TIME_FIELD,
        PICKER_DEFAULT_TIME,
    )
    composeTestRule.checkStartDateErrorIsDisplayed(FieldError.MISSING_DATE)
  }

  @Test
  fun theDateErrorDisappearsOnceTheScheduleIsFixed() {
    val day: LocalDate = completeForm.startDate!!
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.START_DATE_FIELD, day.plusDays(1))
    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.END_DATE_FIELD, day)
    composeTestRule.checkEndDateErrorIsDisplayed(FieldError.END_BEFORE_START)

    composeTestRule.pickCreateTaskDate(CreateTaskScreenTestTags.END_DATE_FIELD, day.plusDays(2))

    composeTestRule.checkNoFieldErrorIsDisplayed()
  }

  // --- Valid forms ---------------------------------------------------------------------------

  @Test
  fun createButtonIsEnabledOnceATitleIsEntered() {
    composeTestRule.enterCreateTaskDetails(titleOnlyForm)
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun savingATitleOnlyFormAddsAnUnscheduledTask() {
    composeTestRule.enterCreateTaskDetails(titleOnlyForm)
    composeTestRule.clickOnCreate(waitForRedirection = true)

    val saved = savedTasks().single()
    assertEquals(titleOnlyForm.title, saved.title)
    assertEquals(null, saved.startTime)
    assertEquals(null, saved.endTime)
    assertEquals(EVENT_ID, saved.eventId)
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

  // --- Navigation ----------------------------------------------------------------------------

  @Test
  fun goingBackLeavesTheScreenWithoutSaving() = checkNoTaskWereAdded {
    composeTestRule.enterCreateTaskDetails(completeForm)
    composeTestRule.clickOnBack()
    assertEquals(1, backCount)
    assertEquals(0, createdCount)
  }
}
