// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import io.mockk.mockk
import io.mockk.verify
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
    navigator.onSignedIn()

    verify {
      navController.navigate(
          AppScreens.EventOverview.name,
          any<NavOptionsBuilder.() -> Unit>(),
      )
    }
  }
}
