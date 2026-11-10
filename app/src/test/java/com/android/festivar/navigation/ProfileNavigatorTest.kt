// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import android.content.Intent
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.navigation.profile.ProfileNavigator
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = ProfileNavigator(navController)

  @Test
  fun backClickGoesBackToThePreviousScreen() {
    navigator.onBack()

    verify { navController.popBackStack() }
  }

  @Test
  fun signedOutNavigatesToLoginAndClearsTheBackStack() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onSignedOut()

    verify { navController.navigate(AppScreens.Login.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertEquals(navController.graph.id, navOptions.popUpToId)
    assertTrue(navOptions.isPopUpToInclusive())
  }

  @Test
  fun settingsClickLaunchesSettingsActivity() {
    var launchedIntent: Intent? = null
    val settingsNavigator = ProfileNavigator(navController) { launchedIntent = it }

    settingsNavigator.onSettingsClick()

    assertNotNull(launchedIntent)
  }
}
