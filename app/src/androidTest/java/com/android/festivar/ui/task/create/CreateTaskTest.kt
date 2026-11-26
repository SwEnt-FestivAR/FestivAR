// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.pickDays
import com.android.festivar.ui.components.fieldErrorMessageRes
import com.android.festivar.ui.task.priorityLabelRes
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Before

/** The text of the string resource [id] in the app under test. */
internal fun string(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

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
  var addTaskGate: CompletableDeferred<Unit>? = null

  @Before
  open fun setUp() {
    repository = TasksRepositoryLocal()
    // STUB over the fake: addTask fails while addTaskFails is true, to test the save error.
    val failable =
        object : TasksRepository by repository {
          override suspend fun addTask(task: Task) {
            if (addTaskFails) throw IllegalStateException("offline")
            addTaskGate?.await()
            repository.addTask(task)
          }
        }
    // The data repository also adds the task to its event and to its owner, so both must exist.
    val owner = User("owner-1", joinedEvents = listOf(EVENT_ID))
    val start = ZonedDateTime.now().plusDays(1)
    val event =
        Event(
            eventId = EVENT_ID,
            ownerId = owner.uid,
            title = "Summer festival",
            members = listOf(owner),
            startDate = start,
            endDate = start.plusDays(2),
        )
    val dataRepository =
        DataRepositoryLocal(
            authRepository = FakeAuthRepository(),
            tasksRepository = failable,
            eventsRepository = EventsRepositoryLocal(listOf(event)),
            usersRepository = UsersRepositoryLocal(listOf(owner)),
        )
    viewModel = CreateTaskViewModel(EVENT_ID, { "owner-1" }, dataRepository)
    createdCount = 0
    backCount = 0
    addTaskFails = false
    addTaskGate = null
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

  /** Replaces the content of the field [tag]: the number fields start with a value. */
  private fun ComposeTestRule.replaceText(tag: String, text: String) =
      onNodeWithTag(tag).performScrollTo().performTextReplacement(text)

  /** Taps the plus of the people needed stepper [times] times. */
  fun ComposeTestRule.increaseCreateTaskMaxAssign(times: Int = 1) {
    repeat(times) {
      onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_INCREASE_BUTTON)
          .performScrollTo()
          .performClick()
    }
  }

  /** Taps the minus of the people needed stepper once. */
  fun ComposeTestRule.decreaseCreateTaskMaxAssign() {
    onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON)
        .performScrollTo()
        .performClick()
  }

  /** Checks the number the people needed stepper shows. */
  fun ComposeTestRule.checkCreateTaskMaxAssignIs(value: Int) {
    onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_FIELD)
        .performScrollTo()
        .assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value.toString())
        )
  }

  fun ComposeTestRule.enterCreateTaskEstimatedTime(text: String) =
      replaceText(CreateTaskScreenTestTags.ESTIMATED_TIME_FIELD, text)

  /**
   * Taps the chip that names [priority] inside the selector. The row of chips scrolls sideways, so
   * the chip may be clipped: the click action is used and not a tap at its position.
   */
  fun ComposeTestRule.pickCreateTaskPriority(priority: Priority) {
    val label =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(priorityLabelRes(priority))
    onNodeWithTag(CreateTaskScreenTestTags.PRIORITY_SELECTOR).performScrollTo()
    onNode(
            hasText(label) and
                hasAnyAncestor(hasTestTag(CreateTaskScreenTestTags.PRIORITY_SELECTOR))
        )
        .performSemanticsAction(SemanticsActions.OnClick)
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
    form.range.startDate?.let {
      pickDays(CreateTaskScreenTestTags.DATES_FIELD, it, form.range.endDate ?: it)
    }
    form.range.startTime?.let { pickCreateTaskTime(CreateTaskScreenTestTags.START_TIME_FIELD, it) }
    form.range.endTime?.let { pickCreateTaskTime(CreateTaskScreenTestTags.END_TIME_FIELD, it) }
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

  fun ComposeTestRule.checkEndDateErrorIsDisplayed(error: FieldError) =
      checkErrorIsDisplayed(CreateTaskScreenTestTags.END_DATE_ERROR, error)

  fun ComposeTestRule.checkEstimatedTimeErrorIsDisplayed() =
      checkErrorIsDisplayed(
          CreateTaskScreenTestTags.ESTIMATED_TIME_ERROR,
          FieldError.INVALID_ESTIMATED_TIME,
      )

  fun ComposeTestRule.checkNoFieldErrorIsDisplayed() {
    onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR).assertDoesNotExist()
    onNodeWithTag(CreateTaskScreenTestTags.END_DATE_ERROR).assertDoesNotExist()
    onNodeWithTag(CreateTaskScreenTestTags.ESTIMATED_TIME_ERROR).assertDoesNotExist()
  }

  companion object {
    const val EVENT_ID = "event-1"
    const val UI_WAIT_TIMEOUT = 5_000L

    /** The time a time picker proposes when its field is still empty. */
    val PICKER_DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

    /**
     * The first sample day, fixed and far ahead so that the end is never in the past. The range
     * picker scrolls to any month, so the day does not depend on the clock.
     */
    private val baseDay: LocalDate = LocalDate.of(2037, 10, 17)

    /** A form with every field filled and a valid schedule. */
    val completeForm =
        CreateTaskUiState(
            title = "Run power to stage",
            description = "Extension reel is in the blue crate by the van.",
            location = "Stage, north corner",
            range =
                DateTimeRange(
                    baseDay,
                    PICKER_DEFAULT_TIME,
                    baseDay.plusDays(1),
                    PICKER_DEFAULT_TIME,
                ),
        )

    /** A form with the only mandatory field. */
    val titleOnlyForm = CreateTaskUiState(title = "Run power to stage")
  }
}
