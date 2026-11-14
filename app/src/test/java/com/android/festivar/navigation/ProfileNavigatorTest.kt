// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import android.content.Intent
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.MySettingsActivity
import com.android.festivar.navigation.profile.ProfileNavigator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
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

  @Test
  fun settingsClickStartsSettingsActivityFromNavigatorContext() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    every { navController.context } returns activity

    navigator.onSettingsClick()

    assertEquals(
        MySettingsActivity::class.java.name,
        shadowOf(activity).nextStartedActivity.component?.className,
    )
  }
}
