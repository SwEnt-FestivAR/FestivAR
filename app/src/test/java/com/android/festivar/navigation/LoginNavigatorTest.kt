// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = LoginNavigator(navController)

  @Test
  fun signUpClickNavigatesToSignUp() {
    navigator.onSignUpClick()

    verify { navController.navigate(AppScreens.SignUp.name) }
  }

  @Test
  fun signedInNavigatesToEventOverview() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onSignedIn()

    verify { navController.navigate(AppScreens.EventOverview.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertEquals(navController.graph.id, navOptions.popUpToId)
    assertTrue(navOptions.isPopUpToInclusive())
  }
}
