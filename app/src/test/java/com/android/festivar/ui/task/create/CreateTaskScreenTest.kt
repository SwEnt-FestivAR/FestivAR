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
  fun scheduleError_isShownOnlyWhenTheScheduleIsInvalid() {
    showContent(
        CreateTaskUiState(
            title = "Run power to stage",
            startDate = day,
            endDate = day.minusDays(1),
        )
    )

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SCHEDULE_ERROR)
        .assertTextEquals(scheduleErrorMessage(ScheduleError.END_BEFORE_START))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun noScheduleError_whenScheduleIsValid() {
    showContent(CreateTaskUiState(title = "Run power to stage", startDate = day, endDate = day))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SCHEDULE_ERROR).assertDoesNotExist()
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
}
