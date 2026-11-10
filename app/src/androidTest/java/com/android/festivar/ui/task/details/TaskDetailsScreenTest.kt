// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.task.details

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.theme.AppTheme
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring only: the screen shows the ViewModel's state and calls back. The rules are unit tested.
 * Also composes the previews once, so one that crashes is caught by the tests. What each state
 * shows is tested in TaskDetailsContentTest.
 */
@RunWith(AndroidJUnit4::class)
class TaskDetailsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val sami = User("u1", "Sami", "Kabbaj")
  private val task = Task("t1", "e1", "Run power to stage", maxAssign = 1, ownerId = "owner-1")

  private lateinit var repository: TasksRepositoryLocal
  private lateinit var dataRepository: DataRepositoryLocal

  // Taking the task goes through dataRepository.joinTask, which also reads its event and the user.
  @Before
  fun setUp() {
    val start = ZonedDateTime.parse("2026-07-01T14:00:00+02:00")
    val event =
        Event(
            eventId = task.eventId,
            title = "Summer festival",
            tasks = listOf(task),
            startDate = start,
            endDate = start.plusHours(10),
        )
    repository = TasksRepositoryLocal()
    runBlocking { repository.addTask(task) }
    dataRepository =
        DataRepositoryLocal(
            authRepository = mockk(),
            tasksRepository = repository,
            eventsRepository = EventsRepositoryLocal(listOf(event)),
            usersRepository = UsersRepositoryLocal(listOf(sami, User("owner-1"))),
        )
  }

  private fun exists(tag: String) =
      composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

  private fun showScreen(
      user: User = sami,
      onBack: () -> Unit = {},
      onEdit: (String) -> Unit = {},
      onTaskDeleted: () -> Unit = {},
  ) {
    val viewModel = TaskDetailsViewModel(task.taskId, user, dataRepository)
    composeTestRule.setContent {
      AppTheme {
        TaskDetailsScreen(
            viewModel = viewModel,
            onBack = onBack,
            onEdit = onEdit,
            onTaskDeleted = onTaskDeleted,
        )
      }
    }
    composeTestRule.waitUntil(5_000) { exists(TaskDetailsScreenTestTags.TITLE) }
  }

  // Breaks if the owner gets no edit button, or the tap does not pass the task id.
  @Test
  fun theOwnerSeesTheEditButtonAndATapCallsOnEdit() {
    val edited = mutableListOf<String>()
    showScreen(user = User("owner-1"), onEdit = { edited += it })

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.EDIT_MENU_ITEM).performClick()

    assertEquals(listOf(task.taskId), edited)
  }

  // Breaks if a volunteer who does not own the task is offered the edit.
  @Test
  fun aVolunteerSeesNoOwnerMenu() {
    showScreen()

    assertFalse(exists(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON))
  }

  @Test
  fun successfulDeletionCallsOnTaskDeleted() {
    var deletionCount = 0
    showScreen(user = User("owner-1"), onTaskDeleted = { deletionCount++ })

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_MENU_ITEM).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_CONFIRM_BUTTON).performClick()

    composeTestRule.waitUntil(5_000) { deletionCount == 1 }
    assertEquals(1, deletionCount)
  }

  private fun assertBadge(label: String) =
      composeTestRule
          .onNodeWithTag(TaskDetailsScreenTestTags.STATUS_BADGE)
          .onChild()
          .assertTextContains(label)

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

  @Test
  fun previewsCompose() {
    composeTestRule.setContent {
      Column {
        TaskDetailsOpenPreview()
        TaskDetailsTakenPreview()
        TaskDetailsYoursPreview()
        TaskDetailsDonePreview()
        TaskDetailsLoadErrorPreview()
        TaskDetailsSaveErrorPreview()
      }
    }
    // Every preview but the load error shows a task, so has a status badge.
    composeTestRule.onAllNodesWithTag(TaskDetailsScreenTestTags.STATUS_BADGE).assertCountEquals(5)
  }
}
