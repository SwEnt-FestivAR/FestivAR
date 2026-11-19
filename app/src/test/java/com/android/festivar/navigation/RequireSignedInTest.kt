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
import kotlinx.coroutines.runBlocking
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

  private lateinit var authRepository: FakeAuthRepository

  private fun open(user: FirebaseUser?) {
    authRepository = FakeAuthRepository(currentUser = user)
    val dataRepository = DataRepositoryLocal(authRepository = authRepository)
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

  /** Breaks if a sign-out while the route is open is not noticed, leaving the route on screen. */
  @Test
  fun aSignOutWhileTheRouteIsOpenSendsTheUserToTheLogin() {
    open(mockk<FirebaseUser> { every { uid } returns "user-1" })

    composeTestRule.runOnUiThread { authRepository.setCurrentUser(null) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(contentTag).assertDoesNotExist()
    composeTestRule.onNodeWithTag(loginTag).assertExists()
    assertEquals(AppScreens.Login.name, navController.currentDestination?.route)
  }

  /** Breaks if a sign-out redirect leaves the route reachable with back. */
  @Test
  fun aSignOutWhileTheRouteIsOpenClearsTheBackStack() {
    open(mockk<FirebaseUser> { every { uid } returns "user-1" })

    composeTestRule.runOnUiThread { authRepository.setCurrentUser(null) }
    composeTestRule.waitForIdle()

    assertNull(navController.previousBackStackEntry)
  }

  /** Breaks if the route keeps the user it opened with after another user signs in. */
  @Test
  fun anotherUserSigningInWhileTheRouteIsOpenIsHandedToIt() {
    open(mockk<FirebaseUser> { every { uid } returns "user-1" })

    composeTestRule.runOnUiThread {
      authRepository.setCurrentUser(mockk<FirebaseUser> { every { uid } returns "user-2" })
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(contentTag).assertTextEquals("user-2")
  }

  /** Breaks if a sign-out through the auth repository does not send the user to the login. */
  @Test
  fun aSuccessfulSignOutThroughTheRepositorySendsTheUserToTheLogin() {
    open(mockk<FirebaseUser> { every { uid } returns "user-1" })

    runBlocking { authRepository.signOut() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(contentTag).assertDoesNotExist()
    assertEquals(AppScreens.Login.name, navController.currentDestination?.route)
  }

  /** Breaks if a failed sign-out is taken as a sign-out and removes the route. */
  @Test
  fun aFailedSignOutThroughTheRepositoryKeepsTheRoute() {
    open(mockk<FirebaseUser> { every { uid } returns "user-1" })
    authRepository.signOutResult = Result.failure(IllegalStateException("offline"))

    runBlocking { authRepository.signOut() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(contentTag).assertTextEquals("user-1")
  }
}
