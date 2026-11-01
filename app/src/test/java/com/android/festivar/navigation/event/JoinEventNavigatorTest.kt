// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.navigation.AppScreens
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class JoinEventNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = JoinEventNavigator(navController)

  @Test
  fun backClickPopsBackStack() {
    navigator.onBack()

    verify { navController.popBackStack() }
  }

  @Test
  fun openEventNavigatesToEventDetailsAndDropsTheJoinScreen() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onOpenEvent("e1")

    verify { navController.navigate(AppScreens.eventDetails("e1"), capture(options)) }
    val navOptions = navOptions(options.captured)
    Assert.assertEquals(AppScreens.JoinEvent.name, navOptions.popUpToRoute)
    Assert.assertTrue(navOptions.isPopUpToInclusive())
  }

  @Test
  fun signedOutNavigatesToLoginAndClearsBackStack() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onSignedOut()

    verify { navController.navigate(AppScreens.Login.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    Assert.assertEquals(navController.graph.id, navOptions.popUpToId)
    Assert.assertTrue(navOptions.isPopUpToInclusive())
  }
}
