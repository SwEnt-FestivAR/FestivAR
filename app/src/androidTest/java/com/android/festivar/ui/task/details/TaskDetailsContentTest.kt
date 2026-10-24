// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The stateless details screen: what it renders for a given state, with no ViewModel. */
@RunWith(AndroidJUnit4::class)
class TaskDetailsContentTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val sami = User("u1", "Sami", "Kabbaj")
  private val yasser = User("u2", "Yasser", "Ameur")

  /** Free for the second place, and held by [sami], so it is "yours" for him. */
  private val task =
      Task(
          "t1",
          "e1",
          "Run power to stage",
          description = "Use the blue crate.",
          startTime = ZonedDateTime.of(2026, 10, 17, 9, 30, 0, 0, ZoneId.of("Europe/Zurich")),
          endTime = ZonedDateTime.of(2026, 10, 17, 9, 50, 0, 0, ZoneId.of("Europe/Zurich")),
          location = "Stage, north corner",
          maxAssign = 2,
          assignees = listOf(sami),
      )

  /** The state seen by [sami]; the status is derived from [task] as the ViewModel does. */
  private fun state(
      task: Task? = this.task,
      isLoading: Boolean = false,
      isUpdating: Boolean = false,
      errorMsg: Int? = null,
  ) = TaskDetailsUiState(sami.uid, task, isLoading, isUpdating, errorMsg)

  private fun show(
      state: TaskDetailsUiState,
      onTake: () -> Unit = {},
      onGiveBack: () -> Unit = {},
      onMarkDone: () -> Unit = {},
      onRetry: () -> Unit = {},
      onDismissError: () -> Unit = {},
      onBack: () -> Unit = {},
  ) = composeTestRule.setContent {
    TaskDetailsContent(state, onBack, onTake, onGiveBack, onMarkDone, onRetry, onDismissError)
  }

  @Test
  fun showsTheTask() {
    show(state())
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertTextContains(task.title)
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.STATUS_BADGE)
        .onChild()
        .assertTextContains("Yours")
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.WHEN)
        .assertTextContains("Sat 17 Oct, 09:30 to 09:50")
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.PLACE)
        .assertTextContains("Location: Stage, north corner")
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DESCRIPTION).assertIsDisplayed()
  }

  @Test
  fun doneTaskShowsTheDoneBadge() {
    show(state(task.complete()))
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.STATUS_BADGE)
        .onChild()
        .assertTextContains("Done")
  }

  @Test
  fun openTaskOffersTake() {
    var taken = 0
    show(state(task.removeAssignee(sami)), onTake = { taken++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).performClick()
    assertEquals(1, taken)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun yourTaskOffersGiveBackAndMarkDone() {
    var givenBack = 0
    var done = 0
    show(state(), onGiveBack = { givenBack++ }, onMarkDone = { done++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).performClick()
    assertEquals(1, givenBack)
    assertEquals(1, done)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun actionsAreDisabledWhileSaving() {
    show(state(isUpdating = true))
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun takenTaskHasNoActions() =
      assertNoActions(Task("t2", "e1", "Full", maxAssign = 1, assignees = listOf(yasser)))

  @Test fun doneTaskHasNoActions() = assertNoActions(task.complete())

  private fun assertNoActions(task: Task) {
    show(state(task))
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun loadingShowsASpinnerAndNoTask() {
    show(state(task = null, isLoading = true))
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertDoesNotExist()
  }

  @Test
  fun loadErrorOffersRetry() {
    var retries = 0
    show(
        state(task = null, errorMsg = R.string.task_details_load_failed),
        onRetry = { retries++ },
    )
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_ACTION).performClick()
    assertEquals(1, retries)
  }

  @Test
  fun saveErrorOffersDismiss() {
    var dismissed = 0
    show(state(errorMsg = R.string.task_details_save_failed), onDismissError = { dismissed++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ERROR_ACTION).performClick()
    assertEquals(1, dismissed)
  }

  @Test
  fun backIsForwarded() {
    var backs = 0
    show(state(), onBack = { backs++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.BACK_BUTTON).performClick()
    assertEquals(1, backs)
  }
}
