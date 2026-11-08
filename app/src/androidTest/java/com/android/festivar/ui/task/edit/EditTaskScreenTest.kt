// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.edit

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.structure.StructuresRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.ui.task.create.CreateTaskActions
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.create.string
import com.android.festivar.ui.theme.AppTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring of the Edit task screen: it shows the ViewModel's state and forwards the user's input. The
 * rules (what can be saved, what counts as a change) are unit tested in `EditTaskViewModelTest`.
 */
@RunWith(AndroidJUnit4::class)
class EditTaskScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val task =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          maxAssign = 3,
          assignees = listOf(User("u1"), User("u2")),
          ownerId = "owner-1",
      )

  // FAKE: the in-memory repository. STUB over it: getTask waits for loadGate while it is not null,
  // so the spinner can be seen before the task arrives, and editTask fails while editFails.
  private val local = TasksRepositoryLocal()
  private var loadGate: CompletableDeferred<Unit>? = null
  private var editFails = false
  private val repository =
      object : TasksRepository by local {
        override suspend fun getTask(taskId: String): Task {
          loadGate?.await()
          return local.getTask(taskId)
        }

        override suspend fun editTask(taskId: String, newValue: Task) {
          if (editFails) throw IllegalStateException("offline")
          local.editTask(taskId, newValue)
        }
      }

  private var savedCount = 0
  private var backCount = 0

  private fun seed() = runBlocking { local.addTask(task) }

  private fun stored(): Task = runBlocking { local.getTask(task.taskId) }

  private fun exists(tag: String) =
      composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

  private fun showScreen(userId: String = "owner-1") {
    val viewModel =
        EditTaskViewModel(
            task.taskId,
            { userId },
            repository,
            StructuresRepositoryLocal(),
            EventsRepositoryLocal(),
        )
    composeTestRule.setContent {
      AppTheme {
        EditTaskScreen(
            viewModel = viewModel,
            onBack = { backCount++ },
            onSaved = { savedCount++ },
        )
      }
    }
  }

  private fun showLoadedScreen(userId: String = "owner-1") {
    seed()
    showScreen(userId)
    composeTestRule.waitUntil(5_000) { exists(CreateTaskScreenTestTags.TITLE_FIELD) }
  }

  private fun editTitle(title: String) =
      composeTestRule
          .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
          .performScrollTo()
          .performTextReplacement(title)

  // Breaks if the spinner is not shown while loading, or the form is not filled from the task.
  @Test
  fun theScreenShowsTheSpinnerThenTheFilledForm() {
    seed()
    loadGate = CompletableDeferred()
    showScreen()

    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOADING).assertIsDisplayed()

    loadGate?.complete(Unit)
    composeTestRule.waitUntil(5_000) { exists(CreateTaskScreenTestTags.TITLE_FIELD) }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD).assert(hasText(task.title))
    assertFalse(exists(EditTaskScreenTestTags.LOADING))
  }

  // Breaks if a state that has not been loaded yet shows the load error instead of the spinner.
  @Test
  fun beforeTheLoadRunsTheScreenShowsTheSpinnerNotTheLoadError() {
    val notLoaded =
        EditTaskViewModel(
                task.taskId,
                { "owner-1" },
                repository,
                StructuresRepositoryLocal(),
                EventsRepositoryLocal(),
            )
            .uiState
            .value
    composeTestRule.setContent {
      AppTheme {
        EditTaskContent(
            state = notLoaded,
            actions = CreateTaskActions(),
            onRetry = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOADING).assertIsDisplayed()
    assertFalse(exists(EditTaskScreenTestTags.LOAD_ERROR))
  }

  // Breaks if a failed load shows no error, or the retry does not load the task again.
  @Test
  fun aMissingTaskShowsTheLoadErrorAndRetryReloads() {
    showScreen()
    composeTestRule.waitUntil(5_000) { exists(EditTaskScreenTestTags.LOAD_ERROR) }
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOAD_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOAD_RETRY).assertIsDisplayed()

    seed()
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOAD_RETRY).performClick()

    composeTestRule.waitUntil(5_000) { exists(CreateTaskScreenTestTags.TITLE_FIELD) }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD).assert(hasText(task.title))
  }

  // Breaks if the screen does not pass maxAssignError or canSave to the form.
  @Test
  fun peopleNeededBelowTheAssigneesShowsTheErrorAndDisablesSave() {
    showLoadedScreen()
    repeat(2) {
      composeTestRule
          .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON)
          .performScrollTo()
          .performClick()
    }

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_ERROR)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  // Breaks if the button does not call save, or onSaved is not called exactly once.
  @Test
  fun savingCallsOnSavedOnce() {
    showLoadedScreen()
    editTitle("Run power to the stage")

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()

    composeTestRule.waitUntil(5_000) { savedCount > 0 }
    composeTestRule.waitForIdle()
    assertEquals(1, savedCount)
    assertEquals("Run power to the stage", stored().title)
  }

  // Breaks if back leaves an edited form without asking, or "Keep editing" leaves anyway.
  @Test
  fun backWithChangesAsksBeforeLeaving() {
    showLoadedScreen()
    editTitle("Run power to the stage")

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertFalse(exists(CreateTaskScreenTestTags.DISCARD_DIALOG))
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.SCREEN).assertIsDisplayed()
    assertEquals(0, backCount)
  }

  // Breaks if a user who does not own the task gets the form, or no notice saying why not.
  @Test
  fun aNonOwnerSeesWhyInsteadOfTheForm() {
    seed()
    showScreen(userId = "volunteer-1")
    composeTestRule.waitUntil(5_000) { exists(EditTaskScreenTestTags.NOT_OWNER) }

    composeTestRule
        .onNodeWithTag(EditTaskScreenTestTags.NOT_OWNER)
        .assertTextEquals(string(R.string.edit_task_not_owner))
    assertFalse(exists(CreateTaskScreenTestTags.TITLE_FIELD))
    assertFalse(exists(CreateTaskScreenTestTags.CREATE_BUTTON))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
  }

  // Breaks if the screen drops the save error from the form, or a failed save clears the edit.
  @Test
  fun aFailedSaveShowsTheSaveErrorAndKeepsTheForm() {
    editFails = true
    showLoadedScreen()
    editTitle("Run power to the stage")

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()

    composeTestRule.waitUntil(5_000) { exists(CreateTaskScreenTestTags.SAVE_ERROR) }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .assert(hasText("Run power to the stage"))
    assertEquals(0, savedCount)
  }

  // Breaks if "Discard" in the dialog does not leave the screen, or leaves it twice.
  @Test
  fun discardingLeavesTheScreen() {
    showLoadedScreen()
    editTitle("Run power to the stage")

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_BUTTON).performClick()

    composeTestRule.waitForIdle()
    assertEquals(1, backCount)
  }
}
