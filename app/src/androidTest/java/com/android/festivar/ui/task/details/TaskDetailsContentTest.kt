// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.task.details

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.PeopleCardTestTags
import com.android.festivar.ui.components.UserListTestTags
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
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
          ownerId = "owner-1",
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
      onEdit: (String) -> Unit = {},
      onDeleteTask: () -> Unit = {},
  ) = composeTestRule.setContent {
    TaskDetailsContent(
        state,
        onBack,
        onTake,
        onGiveBack,
        onMarkDone,
        onRetry,
        onDismissError,
        onEdit,
        onDeleteTask,
    )
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
    show(state(task.removeAssignee(sami.uid)), onTake = { taken++ })
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
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_CONFIRM_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).performClick()
    assertEquals(1, givenBack)
    assertEquals(1, done)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).assertDoesNotExist()
  }

  // Breaks if tapping Give it back changes the task before the user has confirmed.
  @Test
  fun giveBackOpensTheConfirmationWithoutGivingBack() {
    var givenBack = 0
    show(state(), onGiveBack = { givenBack++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG).assertIsDisplayed()
    assertEquals(0, givenBack)
  }

  // Breaks if confirming does not give the task back exactly once, or leaves the dialog open.
  @Test
  fun confirmingGivesTheTaskBackAndClosesTheDialog() {
    var givenBack = 0
    show(state(), onGiveBack = { givenBack++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_CONFIRM_BUTTON).performClick()
    assertEquals(1, givenBack)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG).assertDoesNotExist()
  }

  // Breaks if the dialog flag goes back to remember, so recreating the screen drops the dialog.
  @Test
  fun confirmationSurvivesRecreation() {
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent { TaskDetailsContent(state(), {}, {}, {}, {}, {}, {}) }
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
    restorationTester.emulateSavedInstanceStateRestore()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG).assertIsDisplayed()
  }

  // Breaks if Keep it gives the task back anyway, or leaves the dialog open.
  @Test
  fun keepingItClosesTheDialogAndChangesNothing() {
    var givenBack = 0
    show(state(), onGiveBack = { givenBack++ })
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_KEEP_BUTTON).performClick()
    assertEquals(0, givenBack)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG).assertDoesNotExist()
  }

  // Breaks if the dialog does not say which task is given back.
  @Test
  fun theDialogNamesTheTask() {
    show(state())
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
    composeTestRule
        .onNode(
            hasTestTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG)
                .and(hasAnyDescendant(hasText(task.title, substring = true))),
            useUnmergedTree = true,
        )
        .assertExists()
  }

  @Test
  fun actionsAreDisabledWhileSaving() {
    show(state(isUpdating = true))
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun takenTaskHasNoActions() =
      assertNoActions(
          Task("t2", "e1", "Full", maxAssign = 1, assignees = listOf(yasser), ownerId = "owner-1")
      )

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

  private val tookItAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneId.of("Europe/Zurich"))

  private val tookItLater = tookItAt.plusMinutes(28)

  /** [sami] took the task at [tookItAt], [yasser] at [tookItLater]. */
  private val sharedTask =
      task
          .copy(assignees = emptyList(), takenAt = emptyMap())
          .addAssignee(sami, tookItAt)
          .addAssignee(yasser, tookItLater)

  private fun tookItLine(at: ZonedDateTime) =
      "Took it ${formatDate(at.toLocalDate())}, ${formatTime(at.toLocalTime())}"

  private fun rowWithText(user: User, text: String) =
      composeTestRule.onNode(
          hasTestTag(UserListTestTags.row(user.uid)).and(hasAnyDescendant(hasText(text))),
          useUnmergedTree = true,
      )

  private fun openThePeopleSheet(task: Task) {
    show(state(task))
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SECTION).performClick()
    composeTestRule.waitForIdle()
  }

  private fun row(user: User) =
      composeTestRule.onNodeWithTag(UserListTestTags.row(user.uid), useUnmergedTree = true)

  // Breaks if the section shows for a task nobody took.
  @Test
  fun noAssigneesShowsNoOnThisTaskSection() {
    show(state(task.removeAssignee(sami.uid)))
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SECTION)
        .assertDoesNotExist()
  }

  // Breaks if one person is counted as "1 people".
  @Test
  fun onePersonShowsTheSingularCount() {
    show(state())
    composeTestRule
        .onNode(
            hasTestTag(PeopleCardTestTags.COUNT).and(hasText("1 person")),
            useUnmergedTree = true,
        )
        .performScrollTo()
        .assertIsDisplayed()
  }

  // Breaks if the count does not follow the number of assignees.
  @Test
  fun twoPeopleShowThePluralCount() {
    show(state(sharedTask))
    composeTestRule
        .onNode(
            hasTestTag(PeopleCardTestTags.COUNT).and(hasText("2 people")),
            useUnmergedTree = true,
        )
        .performScrollTo()
        .assertIsDisplayed()
  }

  // Breaks if more than two people hide the third behind a "+N" that does not count them.
  @Test
  fun moreThanTwoPeopleShowThePlusCount() {
    val third = User("u3", "Marie", "Dupont")
    show(state(sharedTask.copy(maxAssign = 3).addAssignee(third, tookItAt)))
    composeTestRule.onNodeWithText("+1").assertIsDisplayed()
  }

  // Breaks if the tap does not open the sheet, or the sheet leaves someone out.
  @Test
  fun tappingTheSectionOpensTheSheetWithEveryAssignee() {
    openThePeopleSheet(sharedTask)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SHEET).assertExists()
    row(sami).assertIsDisplayed()
    row(yasser).assertIsDisplayed()
  }

  // Breaks if the sheet flag goes back to remember, so recreating the screen closes the sheet.
  @Test
  fun thePeopleSheetSurvivesRecreation() {
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent { TaskDetailsContent(state(sharedTask), {}, {}, {}, {}, {}, {}) }
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SECTION).performClick()
    composeTestRule.waitForIdle()
    restorationTester.emulateSavedInstanceStateRestore()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SHEET).assertExists()
  }

  // Breaks if the time shown is not the one stored for that person, or loses its zone.
  @Test
  fun theSheetShowsWhenEachPersonTookTheTask() {
    openThePeopleSheet(sharedTask)
    rowWithText(sami, tookItLine(tookItAt)).assertExists()
    rowWithText(yasser, tookItLine(tookItLater)).assertExists()
  }

  // Breaks if an older assignee, stored without a time, gets an invented one.
  @Test
  fun aPersonWithoutATimeHasNoTookItLine() {
    openThePeopleSheet(sharedTask.copy(takenAt = mapOf(sami.uid to tookItAt)))
    composeTestRule
        .onNode(
            hasTestTag(UserListTestTags.row(yasser.uid))
                .and(hasAnyDescendant(hasText("Took it", substring = true))),
            useUnmergedTree = true,
        )
        .assertDoesNotExist()
    composeTestRule
        .onNode(
            hasTestTag(UserListTestTags.row(sami.uid))
                .and(hasAnyDescendant(hasText("Took it", substring = true))),
            useUnmergedTree = true,
        )
        .assertExists()
  }

  // Breaks if a finished task still tags its people as only taken.
  @Test
  fun aDoneTaskTagsPeopleDone() {
    openThePeopleSheet(sharedTask.complete())
    rowWithText(sami, "Done").assertExists()
    rowWithText(yasser, "Done").assertExists()
  }

  // Breaks if a task in progress tags its people as done.
  @Test
  fun aTakenTaskTagsPeopleTaken() {
    openThePeopleSheet(sharedTask)
    rowWithText(sami, "Taken").assertExists()
    rowWithText(yasser, "Taken").assertExists()
  }

  // Breaks if the sheet lets anyone remove a person: its rows must do nothing when tapped.
  @Test
  fun nobodyCanRemoveAPerson() {
    openThePeopleSheet(sharedTask)
    row(sami).assertHasNoClickAction()
    row(yasser).assertHasNoClickAction()
    composeTestRule.onAllNodesWithText("Remove", useUnmergedTree = true).assertCountEquals(0)
  }

  @Test
  fun ownerMenuEditsTaskAndCanConfirmDeletion() {
    val edited = mutableListOf<String>()
    var deletes = 0
    show(
        state(task.copy(ownerId = sami.uid)),
        onEdit = { edited += it },
        onDeleteTask = { deletes++ },
    )

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.EDIT_MENU_ITEM).performClick()
    assertEquals(listOf(task.taskId), edited)

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_MENU_ITEM).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_CONFIRM_BUTTON).performClick()

    assertEquals(1, deletes)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_DIALOG).assertDoesNotExist()
  }

  @Test
  fun deleteConfirmationCanBeDismissed() {
    var deletes = 0
    show(state(task.copy(ownerId = sami.uid)), onDeleteTask = { deletes++ })

    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_MENU_ITEM).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_DISMISS_BUTTON).performClick()

    assertEquals(0, deletes)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.DELETE_DIALOG).assertDoesNotExist()
  }
}
