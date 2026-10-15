// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring only: the screen shows the ViewModel's state and calls back. The rules are unit tested.
 * Also draws the previews, so one that no longer compiles or crashes is caught by the tests.
 */
@RunWith(AndroidJUnit4::class)
class TaskDetailsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val sami = User("u1", "Sami", "Kabbaj")
  private val task = Task("t1", "e1", "Run power to stage", maxAssign = 1)

  private lateinit var original: TasksRepository
  private lateinit var repository: TasksRepositoryLocal

  @Before
  fun setUp() {
    original = TasksRepositoryProvider.repository
    repository = TasksRepositoryLocal()
    runBlocking { repository.addTask(task) }
    TasksRepositoryProvider.repository = repository
  }

  @After
  fun tearDown() {
    TasksRepositoryProvider.repository = original
  }

  private fun exists(tag: String) =
      composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

  private fun showScreen(onBack: () -> Unit = {}) {
    composeTestRule.setContent {
      AppTheme { TaskDetailsScreen(task.taskId, sami, onBack = onBack) }
    }
    composeTestRule.waitUntil(5_000) { exists(TaskDetailsScreenTestTags.TITLE) }
  }

  private fun assertBadge(label: String) =
      composeTestRule
          .onNodeWithTag(TaskDetailsScreenTestTags.STATUS_BADGE)
          .onChild()
          .assertTextContains(label)

  private fun assertPreview(label: String, preview: @Composable () -> Unit) {
    composeTestRule.setContent(preview)
    assertBadge(label)
  }

  @Test
  fun showsTheLoadedTask() {
    showScreen()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertTextContains(task.title)
    assertBadge("Open")
  }

  @Test
  fun takingTheTaskSavesItAndShowsItAsYours() {
    showScreen()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).performClick()
    composeTestRule.waitUntil(5_000) { exists(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON) }
    assertBadge("Yours")
    val saved = runBlocking { repository.getTask(task.taskId) }
    assertEquals(listOf(sami.uid), saved.assignees.map { it.uid })
  }

  @Test
  fun backIsForwarded() {
    var backs = 0
    showScreen(onBack = { backs++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.BACK_BUTTON).performClick()
    assertEquals(1, backs)
  }

  @Test fun openPreviewDraws() = assertPreview("Open") { TaskDetailsOpenPreview() }

  @Test fun takenPreviewDraws() = assertPreview("Taken") { TaskDetailsTakenPreview() }

  @Test fun yoursPreviewDraws() = assertPreview("Yours") { TaskDetailsYoursPreview() }

  @Test fun donePreviewDraws() = assertPreview("Done") { TaskDetailsDonePreview() }

  @Test
  fun loadErrorPreviewOffersRetry() {
    composeTestRule.setContent { TaskDetailsLoadErrorPreview() }
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.ERROR_ACTION)
        .assertTextContains("Retry")
  }

  @Test
  fun saveErrorPreviewOffersDismiss() {
    composeTestRule.setContent { TaskDetailsSaveErrorPreview() }
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.ERROR_ACTION)
        .assertTextContains("Dismiss")
  }
}
