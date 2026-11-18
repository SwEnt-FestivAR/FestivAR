// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequireSignedInTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val routeThatNeedsAUser = "needsAUser"
  private val contentTag = "content"
  private val loginTag = "login"
  private lateinit var navController: NavHostController

  private fun open(user: FirebaseUser?) {
    val dataRepository =
        DataRepositoryLocal(authRepository = FakeAuthRepository(currentUser = user))
    composeTestRule.setContent {
      navController = rememberNavController()
      NavHost(navController, startDestination = routeThatNeedsAUser) {
        composable(routeThatNeedsAUser) {
          RequireSignedIn(navController, dataRepository) { signedIn ->
            Text(signedIn.uid, Modifier.testTag(contentTag))
          }
        }
        composable(AppScreens.Login.name) { Text("login", Modifier.testTag(loginTag)) }
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if a signed-in user is not handed to the route, or the route is not drawn. */
  @Test
  fun aSignedInUserReachesTheRouteAndIsHandedToIt() {
    open(mockk<FirebaseUser> { every { uid } returns "user-1" })

    composeTestRule.onNodeWithTag(contentTag).assertTextEquals("user-1")
    assertEquals(routeThatNeedsAUser, navController.currentDestination?.route)
  }

  /** Breaks if nobody signed in no longer ends on the login screen. */
  @Test
  fun aSignedOutUserIsSentToTheLogin() {
    open(null)

    composeTestRule.onNodeWithTag(loginTag).assertExists()
    assertEquals(AppScreens.Login.name, navController.currentDestination?.route)
  }

  /** Breaks if the route that needs a user is drawn, even for one frame, with nobody signed in. */
  @Test
  fun aSignedOutUserNeverSeesTheRoute() {
    open(null)

    composeTestRule.onNodeWithTag(contentTag).assertDoesNotExist()
  }

  /** Breaks if back from the login can return to the screen that needs a user. */
  @Test
  fun theLoginRedirectClearsTheBackStack() {
    open(null)

    assertNull(navController.previousBackStackEntry)
  }
}
