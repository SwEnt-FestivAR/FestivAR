package com.android.festivar.navigation.event

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.event.create.CreateEventScreenTestTags as Create
import com.android.festivar.ui.event.overview.EventsOverviewScreenTestTags as Overview
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val USER_ID = "user"

/** Time between two taps of a double tap, well within the exit transition of the screen. */
private const val TAP_GAP_MILLIS = 100L

class CreateEventNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val signedInUser = mockk<FirebaseUser> { every { uid } returns USER_ID }

  private lateinit var navController: NavHostController

  /**
   * Red if a second tap on the arrow during the exit transition pops again, which empties the back
   * stack here and leaves a blank screen.
   */
  @Test
  fun twoTapsOnBackStillNavigatesToEventsOverviewScreen() {
    openTheRouteAs(signedInUser)
    composeTestRule.mainClock.autoAdvance = false

    composeTestRule.onNodeWithTag(Create.NAVIGATION_BUTTON).performClick()
    composeTestRule.mainClock.advanceTimeBy(TAP_GAP_MILLIS)
    composeTestRule.onNodeWithTag(Create.NAVIGATION_BUTTON).performClick()
    composeTestRule.mainClock.autoAdvance = true

    composeTestRule.runOnIdle {
      assertEquals(AppScreens.EventsOverview.name, navController.currentDestination?.route)
    }
    composeTestRule.onNodeWithTag(Overview.TITLE).assertIsDisplayed()
  }

  /** Opens the route as [user]; null means that nobody is signed in. */
  private fun openTheRouteAs(user: FirebaseUser?) {
    composeTestRule.setContent {
      navController = rememberNavController()
      NavHost(navController = navController, startDestination = AppScreens.EventsOverview.name) {
        eventGraph(navController, FakeAuthRepository(currentUser = user))
      }
    }
    composeTestRule.runOnUiThread { navController.navigate(AppScreens.CreateEvent.name) }
    composeTestRule.onNodeWithTag(Create.TITLE).assertIsDisplayed()
  }
}
