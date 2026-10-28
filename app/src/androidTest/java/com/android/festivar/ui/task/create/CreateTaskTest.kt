// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.fieldErrorMessageRes
import com.android.festivar.ui.components.hasDayOfMonth
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Before

/**
 * Superclass of the Create task screen tests: a fresh repository and ViewModel before each test,
 * and the actions and checks the tests share, so that a test reads as what the user does. The
 * subclass owns the compose rule and calls [showCreateTaskScreen] from its own `@Before`.
 */
abstract class CreateTaskTest {
  // FAKE: a real, working repository that keeps the tasks in memory instead of Firebase. The tests
  // read the saved tasks back from it. There is no mock: nothing checks that a method was called.
  lateinit var repository: TasksRepositoryLocal
  lateinit var viewModel: CreateTaskViewModel

  /** How many times the screen reported that the task was created, or that the user went back. */
  var createdCount = 0
  var backCount = 0

  /** While true, saving a task fails as if the device were offline. */
  var addTaskFails = false

  @Before
  open fun setUp() {
    repository = TasksRepositoryLocal()
    // STUB over the fake: addTask fails while addTaskFails is true, to test the save error.
    val failable =
        object : TasksRepository by repository {
          override suspend fun addTask(task: Task) {
            if (addTaskFails) throw IllegalStateException("offline")
            repository.addTask(task)
          }
        }
    viewModel = CreateTaskViewModel(EVENT_ID, "owner-1", failable)
    createdCount = 0
    backCount = 0
    addTaskFails = false
  }

  fun savedTasks(): List<Task> = runBlocking { repository.getAllTasks(EVENT_ID) }

  fun ComposeContentTestRule.showCreateTaskScreen() {
    setContent {
      AppTheme {
        CreateTaskScreen(
            viewModel = viewModel,
            onBack = { backCount++ },
            onTaskCreated = { createdCount++ },
        )
      }
    }
  }

  private fun ComposeTestRule.enterText(tag: String, text: String) =
      onNodeWithTag(tag).performScrollTo().performTextInput(text)

  fun ComposeTestRule.enterCreateTaskTitle(title: String) =
      enterText(CreateTaskScreenTestTags.TITLE_FIELD, title)

  /**
   * Picks [date] in the date picker opened by the field [fieldTag]. The picker opens on the current
   * month, so [date] must be in it.
   */
  fun ComposeTestRule.pickCreateTaskDate(fieldTag: String, date: LocalDate) {
    val today = LocalDate.now()
    require(date.year == today.year && date.month == today.month) {
      "The date picker opens on the current month, so $date cannot be picked."
    }
    onNodeWithTag(fieldTag).performScrollTo().performClick()
    onNode(hasDayOfMonth(date.dayOfMonth)).performClick()
    onNodeWithText("OK").performClick()
  }

  /** Picks [time] in the time picker opened by [fieldTag]. Only the proposed 09:00 is supported. */
  fun ComposeTestRule.pickCreateTaskTime(fieldTag: String, time: LocalTime) {
    require(time == PICKER_DEFAULT_TIME) {
      "Only the proposed time $PICKER_DEFAULT_TIME is pickable."
    }
    onNodeWithTag(fieldTag).performScrollTo().performClick()
    onNodeWithText("OK").performClick()
  }

  /** Fills every field that [form] has a value for. */
  fun ComposeTestRule.enterCreateTaskDetails(form: CreateTaskUiState) {
    if (form.title.isNotEmpty()) enterCreateTaskTitle(form.title)
    if (form.description.isNotEmpty()) {
      enterText(CreateTaskScreenTestTags.DESCRIPTION_FIELD, form.description)
    }
    if (form.location.isNotEmpty()) {
      enterText(CreateTaskScreenTestTags.LOCATION_FIELD, form.location)
    }
    form.startDate?.let { pickCreateTaskDate(CreateTaskScreenTestTags.START_DATE_FIELD, it) }
    form.startTime?.let { pickCreateTaskTime(CreateTaskScreenTestTags.START_TIME_FIELD, it) }
    form.endDate?.let { pickCreateTaskDate(CreateTaskScreenTestTags.END_DATE_FIELD, it) }
    form.endTime?.let { pickCreateTaskTime(CreateTaskScreenTestTags.END_TIME_FIELD, it) }
  }

  fun ComposeTestRule.clickOnCreate(waitForRedirection: Boolean = false) {
    onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsDisplayed().performClick()
    waitUntil(UI_WAIT_TIMEOUT) { !waitForRedirection || createdCount > 0 }
  }

  fun ComposeTestRule.clickOnBack() {
    onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).assertIsDisplayed().performClick()
  }

  private fun ComposeTestRule.checkErrorIsDisplayed(errorTag: String, error: FieldError) {
    val message =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(fieldErrorMessageRes(error))
    onNodeWithTag(errorTag).performScrollTo().assertIsDisplayed().assertTextEquals(message)
  }

  fun ComposeTestRule.checkTitleErrorIsDisplayed(error: FieldError = FieldError.EMPTY_TITLE) =
      checkErrorIsDisplayed(CreateTaskScreenTestTags.TITLE_ERROR, error)

  fun ComposeTestRule.checkStartDateErrorIsDisplayed(error: FieldError) =
      checkErrorIsDisplayed(CreateTaskScreenTestTags.START_DATE_ERROR, error)

  fun ComposeTestRule.checkEndDateErrorIsDisplayed(error: FieldError) =
      checkErrorIsDisplayed(CreateTaskScreenTestTags.END_DATE_ERROR, error)

  fun ComposeTestRule.checkNoFieldErrorIsDisplayed() {
    onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR).assertDoesNotExist()
    onNodeWithTag(CreateTaskScreenTestTags.START_DATE_ERROR).assertDoesNotExist()
    onNodeWithTag(CreateTaskScreenTestTags.END_DATE_ERROR).assertDoesNotExist()
  }

  companion object {
    const val EVENT_ID = "event-1"
    const val UI_WAIT_TIMEOUT = 5_000L

    /** The time a time picker proposes when its field is still empty. */
    val PICKER_DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

    /** The date picker only offers the current month, so the sample dates are taken from it. */
    private val firstOfMonth: LocalDate = LocalDate.now().withDayOfMonth(1)

    /** A form with every field filled and a valid schedule. */
    val completeForm =
        CreateTaskUiState(
            title = "Run power to stage",
            description = "Extension reel is in the blue crate by the van.",
            location = "Stage, north corner",
            startDate = firstOfMonth,
            startTime = PICKER_DEFAULT_TIME,
            endDate = firstOfMonth.plusDays(1),
            endTime = PICKER_DEFAULT_TIME,
        )

    /** A form with the only mandatory field. */
    val titleOnlyForm = CreateTaskUiState(title = "Run power to stage")
  }
}
