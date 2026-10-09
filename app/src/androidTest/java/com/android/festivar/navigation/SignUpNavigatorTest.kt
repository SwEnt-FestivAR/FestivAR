// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignUpNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = SignUpNavigator(navController)

  @Test
  fun signInClickNavigatesToLogin() {
    navigator.onSignInClick()

    verify { navController.navigate(AppScreens.Login.name) }
  }

  @Test
  fun backClickPopsBackStack() {
    navigator.onBackClick()

    verify { navController.popBackStack() }
  }

  @Test
  fun signUpSuccessNavigatesToEventOverview() {
    navigator.onSignUpSuccess()

    verify {
      navController.navigate(
          AppScreens.EventOverview.name,
          any<NavOptionsBuilder.() -> Unit>(),
      )
    }
  }
}
