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
import com.android.festivar.navigation.announcement.announcementGraph
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreenTestTags as Tags
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

private const val EVENT_ID = "e1"
private const val USER_ID = "user-1"
private const val PREVIOUS_SCREEN = "previous screen"

/** Time between two taps of a double tap, well within the exit transition of the screen. */
private const val TAP_GAP_MILLIS = 100L

/**
 * Opens the Create announcement route from another screen, as the app does, so that leaving it has
 * somewhere to go back to.
 */
class AnnouncementNavGraphTest {
  @get:Rule val composeTestRule = createComposeRule()

  // FAKE: a real repository that keeps the event in memory, read back by the test.
  private val repository =
      EventsRepositoryLocal(
          listOf(
              Event(
                  eventId = EVENT_ID,
                  ownerId = "owner",
                  title = "Balelec",
                  startDate = ZonedDateTime.now().plusDays(1),
                  endDate = ZonedDateTime.now().plusDays(2),
              )
          )
      )
  private val signedInUser = mockk<FirebaseUser> { every { uid } returns USER_ID }
  private lateinit var navController: NavHostController

  /** Opens the route as [user]; null means that nobody is signed in. */
  private fun openTheRouteAs(user: FirebaseUser?) {
    composeTestRule.setContent {
      navController = rememberNavController()
      NavHost(navController = navController, startDestination = PREVIOUS_SCREEN) {
        composable(PREVIOUS_SCREEN) { Text(PREVIOUS_SCREEN) }
        announcementGraph(
            navController,
            DataRepositoryLocal(
                FakeAuthRepository(currentUser = user),
                eventsRepository = repository,
            ),
        )
      }
    }
    composeTestRule.runOnUiThread {
      navController.navigate(AppScreens.createAnnouncement(EVENT_ID))
    }
    composeTestRule.onNodeWithTag(Tags.TITLE).assertIsDisplayed()
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
}
