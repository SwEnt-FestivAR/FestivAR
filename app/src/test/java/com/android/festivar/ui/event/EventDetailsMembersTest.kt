// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

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
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.UserListTestTags
import com.android.festivar.ui.event.details.EventDetailsScreen
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.details.EventDetailsViewModel
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

  /** The organizer is listed last on purpose: the sheet must still put them first. */
  private val event = MembersFixture.eventOf(lea, sara, organizer)

  @SuppressLint("ViewModelConstructorInComposable")
  private fun setContent(event: Event) {
    composeTestRule.setContent {
      AppTheme {
        EventDetailsScreen(
            viewModel =
                EventDetailsViewModel(
                    DataRepositoryLocal(
                        authRepository = FakeAuthRepository(),
                        eventsRepository = EventsRepositoryLocal(listOf(event)),
                    ),
                    event.eventId,
                ),
            onBack = {},
            onTasksButtonClicked = {},
            onAnnouncementsClicked = {},
            onEventDeleted = {},
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

  /** Breaks if the list has several groups, or leaves out or doubles a member. */
  @Test
  fun theListIsOneSingleListWithEveryMemberOnce() {
    setContent(event)

    openMembers()

    composeTestRule.onAllNodes(isMemberRow).assertCountEquals(event.members.size)
    event.members.forEach {
      composeTestRule.onNodeWithTag(UserListTestTags.row(it.uid)).assertIsDisplayed()
    }
    composeTestRule.onAllNodes(isCategoryHeader).assertCountEquals(0)
  }

  /** Breaks if the organizer is not pinned above the volunteers, which keep their order. */
  @Test
  fun theOrganizerIsPinnedAtTheTopThenTheVolunteers() {
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
   * Breaks if a row shows anything but the avatar and the name, plus the Organizer tag on the
   * organizer only, e.g. an availability or a role under a name.
   */
  @Test
  fun eachRowShowsOnlyTheAvatarTheNameAndTheOrganizerTag() {
    setContent(event)

    openMembers()

    assertEquals(listOf("OR", "Olivia Roy", "Organizer"), textsInRow(organizer))
    assertEquals(listOf("LM", "Léa Martin"), textsInRow(lea))
    assertEquals(listOf("SK", "Sara Keller"), textsInRow(sara))
  }

  /** The texts shown in the row of [user], in the order they are drawn. */
  private fun textsInRow(user: User): List<String> =
      composeTestRule
          .onAllNodes(
              hasAnyAncestor(hasTestTag(UserListTestTags.row(user.uid))) and
                  SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
              useUnmergedTree = true,
          )
          .fetchSemanticsNodes()
          .flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }
}
