// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.navigation.announcement.announcementGraph
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreenTestTags as Tags
import com.android.festivar.ui.announcement.feed.AnnouncementFeedScreenTestTags as FeedTags
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val EVENT_ID = "e1"
private const val USER_ID = "user-1"
private const val PREVIOUS_SCREEN = "previous screen"

/** Time between two taps of a double tap, well within the exit transition of the screen. */
private const val TAP_GAP_MILLIS = 100L

/**
 * Opens the Create announcement and the announcement feed routes from another screen, as the app
 * does, so that leaving them has somewhere to go back to.
 */
class AnnouncementNavGraphTest {
  @get:Rule val composeTestRule = createComposeRule()

  // FAKE: a real repository that keeps the event in memory, read back by the test.
  private val repository =
      EventsRepositoryLocal(
          listOf(
              Event(
                  eventId = EVENT_ID,
                  title = "Balelec",
                  startDate = ZonedDateTime.now().plusDays(1),
                  endDate = ZonedDateTime.now().plusDays(2),
              )
          )
      )
  private val signedInUser = mockk<FirebaseUser> { every { uid } returns USER_ID }
  private lateinit var navController: NavHostController

  @Before
  fun setUp() {
    EventsRepositoryProvider.repository = repository
  }

  @After
  fun tearDown() {
    EventsRepositoryProvider.reset()
  }

  /** Shows the announcement graph behind a previous screen, as [user]; null means signed out. */
  private fun showTheGraphAs(user: FirebaseUser?) {
    composeTestRule.setContent {
      navController = rememberNavController()
      NavHost(navController = navController, startDestination = PREVIOUS_SCREEN) {
        composable(PREVIOUS_SCREEN) { Text(PREVIOUS_SCREEN) }
        announcementGraph(
            navController,
            DataRepositoryLocal(FakeAuthRepository(currentUser = user)),
        )
      }
    }
  }

  /** Opens the route as [user]; null means that nobody is signed in. */
  private fun openTheRouteAs(user: FirebaseUser?) {
    showTheGraphAs(user)
    composeTestRule.runOnUiThread {
      navController.navigate(AppScreens.createAnnouncement(EVENT_ID))
    }
    composeTestRule.onNodeWithTag(Tags.TITLE).assertIsDisplayed()
  }

  /** Opens the feed of the event, from the previous screen. */
  private fun openTheFeed() {
    showTheGraphAs(signedInUser)
    composeTestRule.runOnUiThread { navController.navigate(AppScreens.announcementFeed(EVENT_ID)) }
    composeTestRule.onNodeWithTag(FeedTags.TITLE).assertIsDisplayed()
  }

  private fun fillTheForm() {
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).performTextInput("Stage B is closed")
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_FIELD).performTextInput("Strong wind.")
  }

  /** Fails unless the route was popped: a navigate() to the previous screen would keep an entry. */
  private fun assertThePreviousScreenIsAloneOnTheBackStack() {
    composeTestRule.runOnIdle {
      assertEquals(PREVIOUS_SCREEN, navController.currentDestination?.route)
      assertNull(navController.previousBackStackEntry)
    }
    composeTestRule.onNodeWithText(PREVIOUS_SCREEN).assertIsDisplayed()
  }

  // Breaks if the event id of the route or the signed-in user does not reach the view model, or if
  // publishing does not pop the route, for example by navigating to the previous screen instead.
  @Test
  fun publishingSavesInTheEventOfTheRouteAsTheUserAndGoesBack() {
    openTheRouteAs(signedInUser)
    fillTheForm()
    composeTestRule.onNodeWithTag(Tags.PUBLISH_BUTTON).performClick()

    assertThePreviousScreenIsAloneOnTheBackStack()
    composeTestRule.onNodeWithTag(Tags.TITLE).assertDoesNotExist()
    val saved = runBlocking { repository.getEvent(EVENT_ID).announcements.single() }
    assertEquals(USER_ID, saved.authorId)
  }

  // Breaks if the route stops treating a missing user as a blank author: the announcement would
  // then be saved without an author, or the screen would leave as if it had been published.
  @Test
  fun publishingWhileSignedOutShowsTheErrorAndStaysWithoutSaving() {
    openTheRouteAs(null)
    fillTheForm()
    composeTestRule.onNodeWithTag(Tags.PUBLISH_BUTTON).performClick()

    composeTestRule.onNodeWithTag(Tags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.TITLE).assertIsDisplayed()
    val announcements = runBlocking { repository.getEvent(EVENT_ID).announcements }
    assertEquals(0, announcements.size)
  }

  // Breaks if the back arrow of the screen is not connected to the back stack, or if it does not
  // pop the route, for example by navigating to the previous screen instead.
  @Test
  fun backGoesToThePreviousScreenWithoutSaving() {
    openTheRouteAs(signedInUser)
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()

    assertThePreviousScreenIsAloneOnTheBackStack()
    val announcements = runBlocking { repository.getEvent(EVENT_ID).announcements }
    assertEquals(0, announcements.size)
  }

  // Breaks if a second tap on the arrow during the exit transition pops again, which empties the
  // back stack here and leaves a blank screen.
  @Test
  fun twoTapsOnBackBeforeTheTransitionEndsLeaveOnlyOnce() {
    openTheRouteAs(signedInUser)
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    composeTestRule.mainClock.advanceTimeBy(TAP_GAP_MILLIS)
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    composeTestRule.mainClock.autoAdvance = true

    assertThePreviousScreenIsAloneOnTheBackStack()
  }

  // Breaks if a tap on the arrow just after publishing pops again: publishing reaches the same pop
  // through onPublished, so guarding only the arrow would leave this case open.
  @Test
  fun aTapOnBackJustAfterPublishingLeavesOnlyOnce() {
    openTheRouteAs(signedInUser)
    fillTheForm()

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.onNodeWithTag(Tags.PUBLISH_BUTTON).performClick()
    composeTestRule.mainClock.advanceTimeUntil {
      navController.currentDestination?.route == PREVIOUS_SCREEN
    }
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    composeTestRule.mainClock.autoAdvance = true

    assertThePreviousScreenIsAloneOnTheBackStack()
    val announcements = runBlocking { repository.getEvent(EVENT_ID).announcements }
    assertEquals(1, announcements.size)
  }

  // Breaks if the event id of the feed route does not reach its view model, which would then show
  // the announcements of no event, or of another one.
  @Test
  fun feedShowsTheAnnouncementsOfTheEventOfTheRoute() {
    val announcement =
        Announcement(
            announcementId = "a1",
            title = "Stage B is closed",
            authorId = USER_ID,
            updatedAt = ZonedDateTime.now(),
        )
    runBlocking { repository.addAnnouncement(EVENT_ID, announcement) }

    openTheFeed()

    composeTestRule.onNodeWithTag(FeedTags.announcementRow(announcement)).assertIsDisplayed()
  }

  // Breaks if "Add announcement" does not open the Create announcement route of the same event,
  // e.g. by replacing the feed instead of opening on top of it.
  @Test
  fun addOpensCreateAnnouncementForTheEventOnTopOfTheFeed() {
    openTheFeed()

    composeTestRule.onNodeWithTag(FeedTags.ADD_ANNOUNCEMENT_FAB).performClick()

    composeTestRule.onNodeWithTag(Tags.TITLE).assertIsDisplayed()
    composeTestRule.runOnIdle {
      assertEquals(EVENT_ID, navController.currentBackStackEntry?.arguments?.getString("eventId"))
      assertEquals(
          "${AppScreens.AnnouncementFeed.name}/{eventId}",
          navController.previousBackStackEntry?.destination?.route,
      )
    }
  }

  // Breaks if the feed's back arrow does not pop the feed, or pops again on a second tap during the
  // exit transition, which empties the back stack here and leaves a blank screen.
  @Test
  fun twoTapsOnTheFeedsBackLeaveTheFeedOnlyOnce() {
    openTheFeed()
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.onNodeWithTag(FeedTags.BACK_BUTTON).performClick()
    composeTestRule.mainClock.advanceTimeBy(TAP_GAP_MILLIS)
    composeTestRule.onNodeWithTag(FeedTags.BACK_BUTTON).performClick()
    composeTestRule.mainClock.autoAdvance = true

    assertThePreviousScreenIsAloneOnTheBackStack()
    composeTestRule.onNodeWithTag(FeedTags.TITLE).assertDoesNotExist()
  }
}
