// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import android.annotation.SuppressLint
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.MemberDialogTestTags
import com.android.festivar.ui.components.OptionPickerDialogTestTags
import com.android.festivar.ui.components.UserListTestTags
import com.android.festivar.ui.event.MembersFixture
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDetailsMembersTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val organizer = MembersFixture.organizer
  private val lea = MembersFixture.lea
  private val sara = MembersFixture.sara

  /** The owner is listed last on purpose: the sheet must still put them first. */
  private val event = MembersFixture.eventOf(lea, sara, organizer)

  /** Shows [event], stored in [eventsRepository], to [currentUserId]: the owner by default. */
  @SuppressLint("ViewModelConstructorInComposable")
  private fun setContent(
      event: Event,
      currentUserId: String = organizer.uid,
      eventsRepository: EventsRepository = EventsRepositoryLocal(listOf(event)),
  ) {
    composeTestRule.setContent {
      AppTheme {
        EventDetailsScreen(
            viewModel =
                EventDetailsViewModel(
                    MembersFixture.dataRepositoryWith(event, eventsRepository),
                    event.eventId,
                    currentUserId,
                ),
            onBack = {},
            onTasksButtonClicked = {},
            onAnnouncementsClicked = {},
            onEditClicked = {},
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  private fun openMembers() {
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.MEMBERS_CARD).performClick()
    composeTestRule.waitForIdle()
  }

  private fun hasTagStartingWith(prefix: String) =
      SemanticsMatcher("has a tag starting with $prefix") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
      }

  private val isMemberRow = hasTagStartingWith(UserListTestTags.row(""))
  private val isCategoryHeader = hasTagStartingWith(UserListTestTags.header(""))

  /** Breaks if the Members entry is missing or shows another count than the number of members. */
  @Test
  fun theMembersCardShowsTheNumberOfMembers() {
    setContent(event)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.MEMBERS_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithText("Members").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 members").assertIsDisplayed()
  }

  /** Breaks if a lone organizer is counted with the plural form. */
  @Test
  fun theMembersCardUsesTheSingularForOneMember() {
    setContent(MembersFixture.eventOf(organizer))

    composeTestRule.onNodeWithText("1 member").assertIsDisplayed()
  }

  /** Breaks if the details cannot be scrolled, which hides the Members card under a long text. */
  @Test
  fun theMembersCardCanBeScrolledToUnderALongDescription() {
    setContent(event.copy(description = "A long description of the event. ".repeat(60)))

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.MEMBERS_CARD)
        .performScrollTo()
        .assertIsDisplayed()
  }

  /** Breaks if the list is shown before the Members entry is tapped. */
  @Test
  fun theMembersListIsClosedUntilTheCardIsTapped() {
    setContent(event)

    composeTestRule.onAllNodesWithTag(EventDetailsScreenTestTags.MEMBERS_SHEET).assertCountEquals(0)
  }

  /** Breaks if the list does not open with the event as context, titled "Members". */
  @Test
  fun tappingTheCardOpensTheListForThisEvent() {
    setContent(event)

    openMembers()

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.MEMBERS_SHEET).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserListTestTags.TITLE).assertTextEquals("Members")
    composeTestRule.onNodeWithText("3 members · Fête de l'Asso 2026").assertIsDisplayed()
  }

  /** Breaks if going back does not close the list and return to the event details. */
  @Test
  fun goingBackClosesTheList() {
    setContent(event)
    openMembers()

    Espresso.pressBack()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithTag(EventDetailsScreenTestTags.MEMBERS_SHEET).assertCountEquals(0)
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.MEMBERS_CARD).assertIsDisplayed()
  }

  /** Breaks if the list is not grouped by role, or leaves out or doubles a member. */
  @Test
  fun theListGroupsEveryMemberOnceUnderTheirRole() {
    setContent(event)

    openMembers()

    composeTestRule.onAllNodes(isMemberRow).assertCountEquals(event.members.size)
    event.members.forEach {
      composeTestRule.onNodeWithTag(UserListTestTags.row(it.uid)).assertIsDisplayed()
    }
    // The owner, and the two other members who have the default role.
    composeTestRule.onAllNodes(isCategoryHeader).assertCountEquals(2)
  }

  /** Breaks if the owner is not pinned above the other members, which keep their order. */
  @Test
  fun theOwnerIsPinnedAtTheTopThenTheOtherMembers() {
    setContent(event)

    openMembers()

    val tops =
        listOf(organizer, lea, sara).map {
          composeTestRule.onNodeWithTag(UserListTestTags.row(it.uid)).getUnclippedBoundsInRoot().top
        }
    assertTrue(
        "Rows are not strictly in the order organizer, Léa, Sara: $tops",
        tops.zipWithNext().all { (above, below) -> above < below },
    )
  }

  /**
   * Breaks if a row shows anything but the avatar, the name, the number of tasks of the member and
   * the badge of their role.
   */
  @Test
  fun eachRowShowsTheAvatarTheNameTheTaskCountAndTheRole() {
    setContent(event)

    openMembers()

    assertEquals(listOf("OR", "Olivia Roy", "0 tasks", "Owner"), textsInRow(organizer))
    assertEquals(listOf("LM", "Léa Martin", "0 tasks", "Visitor"), textsInRow(lea))
    assertEquals(listOf("SK", "Sara Keller", "0 tasks", "Visitor"), textsInRow(sara))
  }

  // ******************************* Roles ******************************* //

  private val rolesEvent =
      MembersFixture.eventWithRoles(lea to Role.STAFF_MANAGER, sara to Role.VISITOR)

  /** Breaks if a visitor sees the tasks, the members or the menu of the event. */
  @Test
  fun aVisitorSeesNeitherTheTasksNorTheMembersNorTheMenu() {
    setContent(rolesEvent, currentUserId = sara.uid)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME).assertIsDisplayed()
    listOf(
            EventDetailsScreenTestTags.OPEN_TASKS_STAT,
            EventDetailsScreenTestTags.SEE_TASKS_BUTTON,
            EventDetailsScreenTestTags.MEMBERS_CARD,
            EventDetailsScreenTestTags.MORE_ACTIONS_BUTTON,
        )
        .forEach { composeTestRule.onAllNodesWithTag(it).assertCountEquals(0) }
  }

  /**
   * Breaks if the dialog of a member does not show them, their role and tasks, then the actions.
   */
  @Test
  fun tappingAMemberShowsTheirRoleTheirTasksAndTheActionsOnThem() {
    setContent(rolesEvent)

    openMemberDialogOf(sara)

    assertEquals(
        listOf("SK", "Sara Keller", "Visitor", "0 tasks", "Change role", "Kick"),
        textsIn(MemberDialogTestTags.DIALOG),
    )
  }

  /** Breaks if a staff manager is offered to kick, or any action on the owner, above them. */
  @Test
  fun aStaffManagerIsOnlyOfferedWhatTheirRoleAllows() {
    setContent(rolesEvent, currentUserId = lea.uid)

    openMemberDialogOf(sara)
    assertEquals(listOf("Change role"), actionsShown())
    composeTestRule.onNodeWithTag(MemberDialogTestTags.CLOSE_BUTTON).performClick()
    tap(UserListTestTags.row(organizer.uid))

    composeTestRule.onNodeWithTag(MemberDialogTestTags.DIALOG).assertIsDisplayed()
    assertEquals(emptyList<String>(), actionsShown())
  }

  /**
   * Breaks if a role is saved before it is confirmed, cancelling does not go back to the roles, or
   * the confirmed role is not shown with every dialog closed.
   */
  @Test
  fun aNewRoleIsSavedOnlyOnceConfirmedAndCancellingGoesBack() {
    setContent(rolesEvent)
    openMemberDialogOf(sara)

    tap(MemberDialogTestTags.action("Change role"))
    assertEquals(
        listOf("Change the role of Sara", "Admin", "Staff manager", "Team lead", "Staff", "Visitor")
            .plus("Current"),
        textsIn(OptionPickerDialogTestTags.DIALOG),
    )
    composeTestRule.onNodeWithText("Team lead").performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithText("Team lead").performClick()
    assertEquals("Visitor", textsInRow(sara).last())
    tap(EventDetailsScreenTestTags.CHANGE_ROLE_CONFIRM)

    assertEquals("Team lead", textsInRow(sara).last())
    listOf(EventDetailsScreenTestTags.CHANGE_ROLE_CONFIRM, MemberDialogTestTags.DIALOG).forEach {
      composeTestRule.onAllNodesWithTag(it).assertCountEquals(0)
    }
  }

  /**
   * Breaks if a member is kicked before it is confirmed, cancelling does not go back to their
   * dialog, or they stay in the list once confirmed.
   */
  @Test
  fun aMemberIsKickedOnlyOnceConfirmedAndCancellingGoesBack() {
    setContent(rolesEvent)
    openMemberDialogOf(sara)

    tap(MemberDialogTestTags.action("Kick"))
    composeTestRule.onNodeWithText("Cancel").performClick()
    tap(MemberDialogTestTags.action("Kick"))
    composeTestRule.onNodeWithText("Kick Sara Keller?").assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserListTestTags.row(sara.uid)).assertExists()
    tap(EventDetailsScreenTestTags.KICK_CONFIRM)

    composeTestRule.onAllNodesWithTag(UserListTestTags.row(sara.uid)).assertCountEquals(0)
    composeTestRule.onAllNodesWithTag(MemberDialogTestTags.DIALOG).assertCountEquals(0)
  }

  /** Breaks if a failed kick is not reported, or its error cannot be dismissed. */
  @Test
  fun aFailedKickShowsAnErrorUntilItIsDismissed() {
    setContent(rolesEvent, eventsRepository = MembersFixture.failingToSave(rolesEvent))
    openMemberDialogOf(sara)

    tap(MemberDialogTestTags.action("Kick"))
    tap(EventDetailsScreenTestTags.KICK_CONFIRM)
    composeTestRule.onNodeWithText("Could not kick this member. Please try again.").assertExists()
    composeTestRule.onNodeWithTag(UserListTestTags.row(sara.uid)).assertExists()
    composeTestRule.onNodeWithText("Dismiss").performClick()

    composeTestRule.onAllNodesWithTag(EventDetailsScreenTestTags.ACTION_ERROR).assertCountEquals(0)
  }

  private fun openMemberDialogOf(user: User) {
    openMembers()
    tap(UserListTestTags.row(user.uid))
  }

  private fun tap(tag: String) {
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.waitForIdle()
  }

  /** The labels of the buttons of the open member dialog. */
  private fun actionsShown() =
      listOf("Change role", "Kick").filter {
        composeTestRule
            .onAllNodesWithTag(MemberDialogTestTags.action(it))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

  private fun textsInRow(user: User) = textsIn(UserListTestTags.row(user.uid))

  /** The texts shown in the node tagged [tag], in the order they are drawn. */
  private fun textsIn(tag: String): List<String> =
      composeTestRule
          .onAllNodes(
              hasAnyAncestor(hasTestTag(tag)) and
                  SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
              useUnmergedTree = true,
          )
          .fetchSemanticsNodes()
          .flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }
}
