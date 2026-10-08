// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateTaskScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val day = LocalDate.of(2026, 10, 17)

  /** Shows the stateless content with [initial] and the given [actions]. */
  private fun showContent(
      initial: CreateTaskUiState = CreateTaskUiState(),
      actions: CreateTaskActions = CreateTaskActions(),
  ) {
    composeTestRule.setContent {
      AppTheme { CreateTaskContent(state = initial, actions = actions) }
    }
  }

  @Test
  fun displaysEveryTaggedElement() {
    showContent()

    listOf(
            CreateTaskScreenTestTags.BACK_BUTTON,
            CreateTaskScreenTestTags.TITLE,
            CreateTaskScreenTestTags.TITLE_FIELD,
            CreateTaskScreenTestTags.DESCRIPTION_FIELD,
            CreateTaskScreenTestTags.LOCATION_FIELD,
            CreateTaskScreenTestTags.START_DATE_FIELD,
            CreateTaskScreenTestTags.START_TIME_FIELD,
            CreateTaskScreenTestTags.END_DATE_FIELD,
            CreateTaskScreenTestTags.END_TIME_FIELD,
            CreateTaskScreenTestTags.CREATE_BUTTON,
        )
        .forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE).assertTextEquals("Create task")
  }

  @Test
  fun showsTheValuesOfTheState() {
    showContent(
        CreateTaskUiState(
            title = "Run power to stage",
            description = "Extension reel is in the blue crate by the van.",
            location = "Stage, north corner",
            startDate = day,
            startTime = LocalTime.of(9, 30),
            endDate = day,
            endTime = LocalTime.of(9, 50),
        )
    )

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .assertTextContains("Run power to stage")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Extension reel is in the blue crate by the van.")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.LOCATION_FIELD)
        .assertTextContains("Stage, north corner")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_DATE_FIELD)
        .assertTextContains("Sat 17 Oct")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD)
        .assertTextContains("09:30")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_DATE_FIELD)
        .assertTextContains("Sat 17 Oct")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_TIME_FIELD)
        .assertTextContains("09:50")
  }

  @Test
  fun createButton_isDisabledUntilTheFormIsValid() {
    showContent(CreateTaskUiState())
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createButton_isEnabledForAValidForm() {
    showContent(CreateTaskUiState(title = "Run power to stage"))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun typing_reportsEachTextFieldToItsOwnAction() {
    var title = ""
    var description = ""
    var location = ""
    showContent(
        actions =
            CreateTaskActions(
                onTitleChange = { title = it },
                onDescriptionChange = { description = it },
                onLocationChange = { location = it },
            )
    )

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD).performTextInput("A")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD).performTextInput("B")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.LOCATION_FIELD).performTextInput("C")

    assertEquals("A", title)
    assertEquals("B", description)
    assertEquals("C", location)
  }

  @Test
  fun backAndCreate_callTheirActions() {
    var backs = 0
    var creates = 0
    showContent(
        initial = CreateTaskUiState(title = "Run power to stage"),
        actions = CreateTaskActions(onBack = { backs++ }, onCreate = { creates++ }),
    )

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()

    assertEquals(1, backs)
    assertEquals(1, creates)
  }

  @Test
  fun endBeforeStart_isReportedUnderTheEndDateOnly() {
    showContent(
        CreateTaskUiState(
            title = "Run power to stage",
            startDate = day,
            endDate = day.minusDays(1),
        )
    )

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_DATE_ERROR)
        .assertTextEquals(fieldErrorMessage(FieldError.END_BEFORE_START))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_DATE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun timeWithoutDate_isReportedUnderTheMatchingDate() {
    showContent(
        CreateTaskUiState(title = "Run power to stage", startTime = LocalTime.of(9, 30)),
    )

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_DATE_ERROR)
        .assertTextEquals(fieldErrorMessage(FieldError.MISSING_DATE))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.END_DATE_ERROR).assertDoesNotExist()
  }

  @Test
  fun editedBlankTitle_showsItsErrorUnderTheTitle() {
    showContent(CreateTaskUiState(title = "", titleEdited = true))

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR)
        .assertTextEquals(fieldErrorMessage(FieldError.EMPTY_TITLE))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun untouchedForm_showsNoFieldError() {
    showContent(CreateTaskUiState())

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_DATE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.END_DATE_ERROR).assertDoesNotExist()
  }

  @Test
  fun validSchedule_showsNoFieldError() {
    showContent(CreateTaskUiState(title = "Run power to stage", startDate = day, endDate = day))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_DATE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.END_DATE_ERROR).assertDoesNotExist()
  }

  @Test
  fun clearingTheTitle_showsItsErrorEndToEnd() {
    val viewModel = CreateTaskViewModel("event-1", TasksRepositoryLocal())
    composeTestRule.setContent {
      AppTheme { CreateTaskScreen(viewModel = viewModel, onBack = {}, onTaskCreated = {}) }
    }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR).assertDoesNotExist()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD).performTextInput(" ")

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR)
        .assertTextEquals(fieldErrorMessage(FieldError.EMPTY_TITLE))
  }

  @Test
  fun saveError_isShown() {
    showContent(CreateTaskUiState(title = "Run power to stage", errorMsg = "offline"))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SAVE_ERROR).assertTextEquals("offline")
  }

  @Test
  fun tappingATimeField_opensThePickerAndConfirmingReportsTheTime() {
    var picked: LocalTime? = null
    showContent(actions = CreateTaskActions(onStartTimeChange = { picked = it }))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    // With no time yet, the picker opens on its 09:00 default.
    assertEquals(LocalTime.of(9, 0), picked)
    composeTestRule.onNodeWithText("OK").assertDoesNotExist()
  }

  @Test
  fun cancellingAPicker_closesItWithoutReportingAValue() {
    var reported = false
    showContent(actions = CreateTaskActions(onEndTimeChange = { reported = true }))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.END_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    assertFalse(reported)
    composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
  }

  @Test
  fun tappingADateField_opensTheDatePicker() {
    showContent()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_DATE_FIELD).performClick()

    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun endToEnd_fillingTheFormAndPressingCreateSavesTheTask() {
    val repository = TasksRepositoryLocal()
    val viewModel = CreateTaskViewModel("event-1", repository)
    var created = 0
    var backs = 0
    composeTestRule.setContent {
      AppTheme {
        CreateTaskScreen(
            viewModel = viewModel,
            onBack = { backs++ },
            onTaskCreated = { created++ },
        )
      }
    }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextInput("Run power to stage")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.LOCATION_FIELD)
        .performTextInput("Stage, north corner")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val saved = runBlocking { repository.getAllTasks() }.single()
    assertEquals("Run power to stage", saved.title)
    assertEquals("Stage, north corner", saved.location)
    assertEquals("event-1", saved.eventId)
    assertEquals(1, created)
    assertEquals(0, backs)
  }

  /** Opens the picker behind [fieldTag], selects day [dayOfMonth] of the shown month, confirms. */
  private fun pickDay(fieldTag: String, dayOfMonth: Int) {
    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNode(hasText(dayOfMonth.toString()) and hasClickAction()).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  /** Opens the picker behind [fieldTag] and confirms the time it proposes (09:00 when empty). */
  private fun confirmProposedTime(fieldTag: String) {
    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  @Test
  fun endToEnd_everyFieldFilled_savesTheCompleteTask() {
    val repository = TasksRepositoryLocal()
    val viewModel = CreateTaskViewModel("event-1", repository)
    var created = 0
    composeTestRule.setContent {
      AppTheme {
        CreateTaskScreen(viewModel = viewModel, onBack = {}, onTaskCreated = { created++ })
      }
    }

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextInput("Run power to stage")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD)
        .performTextInput("Extension reel is in the blue crate by the van.")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.LOCATION_FIELD)
        .performTextInput("Stage, north corner")
    pickDay(CreateTaskScreenTestTags.START_DATE_FIELD, dayOfMonth = 1)
    confirmProposedTime(CreateTaskScreenTestTags.START_TIME_FIELD)
    pickDay(CreateTaskScreenTestTags.END_DATE_FIELD, dayOfMonth = 2)
    confirmProposedTime(CreateTaskScreenTestTags.END_TIME_FIELD)

    // The fields show what was picked, and the button is ready.
    val firstOfMonth = LocalDate.now().withDayOfMonth(1)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_DATE_FIELD)
        .assertTextContains(formatDate(firstOfMonth))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_DATE_FIELD)
        .assertTextContains(formatDate(firstOfMonth.plusDays(1)))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD)
        .assertTextContains("09:00")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_TIME_FIELD)
        .assertTextContains("09:00")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val saved = runBlocking { repository.getAllTasks() }.single()
    assertEquals("event-1", saved.eventId)
    assertEquals("Run power to stage", saved.title)
    assertEquals("Extension reel is in the blue crate by the van.", saved.description)
    assertEquals("Stage, north corner", saved.location)
    assertEquals(firstOfMonth.atTime(9, 0), saved.startTime)
    assertEquals(firstOfMonth.plusDays(1).atTime(9, 0), saved.endTime)
    assertEquals(1, created)
  }
}
