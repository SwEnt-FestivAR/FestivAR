// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assertDoesNotExist
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the Create task screen on a device or emulator, through the tags annotated in Figma. */
@RunWith(AndroidJUnit4::class)
class CreateTaskScreenInstrumentedTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val repository = TasksRepositoryLocal()
  private val viewModel = CreateTaskViewModel(eventId = "event-1", tasksRepository = repository)
  private var created = 0
  private var backs = 0

  private fun showScreen() {
    composeTestRule.setContent {
      AppTheme {
        CreateTaskScreen(
            viewModel = viewModel,
            onBack = { backs++ },
            onTaskCreated = { created++ },
        )
      }
    }
  }

  private fun type(tag: String, text: String) {
    composeTestRule.onNodeWithTag(tag).performScrollTo().performTextInput(text)
  }

  /** Opens the picker behind [fieldTag], selects day [dayOfMonth] of the shown month, confirms. */
  private fun pickDay(fieldTag: String, dayOfMonth: Int) {
    composeTestRule.onNodeWithTag(fieldTag).performScrollTo().performClick()
    composeTestRule.onNode(hasText(dayOfMonth.toString()) and hasClickAction()).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  /** Opens the picker behind [fieldTag] and confirms the time it proposes (09:00 when empty). */
  private fun confirmProposedTime(fieldTag: String) {
    composeTestRule.onNodeWithTag(fieldTag).performScrollTo().performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  private fun savedTasks() = runBlocking { repository.getAllTasks() }

  @Test
  fun everyTaggedElementIsDisplayed() {
    showScreen()

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
  fun createButton_isDisabledUntilATitleIsTyped() {
    showScreen()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    type(CreateTaskScreenTestTags.TITLE_FIELD, "Run power to stage")

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun typedTextIsShownInEachField() {
    showScreen()

    type(CreateTaskScreenTestTags.TITLE_FIELD, "Run power to stage")
    type(CreateTaskScreenTestTags.DESCRIPTION_FIELD, "Extension reel is in the blue crate.")
    type(CreateTaskScreenTestTags.LOCATION_FIELD, "Stage, north corner")

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .assertTextContains("Run power to stage")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Extension reel is in the blue crate.")
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.LOCATION_FIELD)
        .assertTextContains("Stage, north corner")
  }

  @Test
  fun backButton_leavesTheScreenWithoutSaving() {
    showScreen()
    type(CreateTaskScreenTestTags.TITLE_FIELD, "Run power to stage")

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()

    assertEquals(1, backs)
    assertEquals(0, created)
    assertEquals(emptyList<Any>(), savedTasks())
  }

  @Test
  fun endBeforeStart_showsTheScheduleErrorAndBlocksCreate() {
    showScreen()
    type(CreateTaskScreenTestTags.TITLE_FIELD, "Run power to stage")

    pickDay(CreateTaskScreenTestTags.START_DATE_FIELD, dayOfMonth = 2)
    pickDay(CreateTaskScreenTestTags.END_DATE_FIELD, dayOfMonth = 1)

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SCHEDULE_ERROR)
        .assertTextEquals(scheduleErrorMessage(ScheduleError.END_BEFORE_START))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun cancellingATimePicker_closesItAndLeavesTheFieldEmpty() {
    showScreen()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD)
        .assertTextEquals("Start time")
  }

  @Test
  fun fillingEveryFieldAndPressingCreate_savesTheCompleteTask() {
    showScreen()

    type(CreateTaskScreenTestTags.TITLE_FIELD, "Run power to stage")
    type(
        CreateTaskScreenTestTags.DESCRIPTION_FIELD,
        "Extension reel is in the blue crate by the van.",
    )
    type(CreateTaskScreenTestTags.LOCATION_FIELD, "Stage, north corner")
    pickDay(CreateTaskScreenTestTags.START_DATE_FIELD, dayOfMonth = 1)
    confirmProposedTime(CreateTaskScreenTestTags.START_TIME_FIELD)
    pickDay(CreateTaskScreenTestTags.END_DATE_FIELD, dayOfMonth = 2)
    confirmProposedTime(CreateTaskScreenTestTags.END_TIME_FIELD)

    val firstOfMonth = LocalDate.now().withDayOfMonth(1)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_DATE_FIELD)
        .assertTextContains(formatDate(firstOfMonth))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_DATE_FIELD)
        .assertTextContains(formatDate(firstOfMonth.plusDays(1)))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.START_TIME_FIELD)
        .assertTextContains(formatTime(LocalTime.of(9, 0)))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.END_TIME_FIELD)
        .assertTextContains(formatTime(LocalTime.of(9, 0)))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val saved = savedTasks().single()
    assertEquals("event-1", saved.eventId)
    assertEquals("Run power to stage", saved.title)
    assertEquals("Extension reel is in the blue crate by the van.", saved.description)
    assertEquals("Stage, north corner", saved.location)
    assertEquals(firstOfMonth.atTime(9, 0), saved.startTime)
    assertEquals(firstOfMonth.plusDays(1).atTime(9, 0), saved.endTime)
    assertEquals(1, created)
    assertEquals(0, backs)
  }
}
