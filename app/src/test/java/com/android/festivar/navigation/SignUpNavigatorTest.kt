// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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

class SignUpNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = SignUpNavigator(navController)

  @Test
  fun signInClickPopsBackStack() {
    navigator.onSignInClick()

    verify { navController.popBackStack(AppScreens.Login.name, inclusive = false) }
  }

  @Test
  fun backClickPopsBackStack() {
    navigator.onBackClick()

    verify { navController.popBackStack(AppScreens.Login.name, inclusive = false) }
  }

  @Test
  fun signUpSuccessNavigatesToEventsOverview() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onSignUpSuccess()

    verify { navController.navigate(AppScreens.EventsOverview.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertEquals(navController.graph.id, navOptions.popUpToId)
    assertTrue(navOptions.isPopUpToInclusive())
  }
}
