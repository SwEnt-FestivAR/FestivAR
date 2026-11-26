// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class EditAnnouncementNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val backStackEntry = mockk<NavBackStackEntry>()
  private val navigator = EditAnnouncementNavigator(navController, backStackEntry)

  /** Breaks if back or a saved edit does not pop the edit back to the details. */
  @Test
  fun backAndSavePopTheEditWhenItIsOnTop() {
    every { navController.currentBackStackEntry } returns backStackEntry

    navigator.onBack()
    navigator.onSaved()

    verify(exactly = 2) { navController.popBackStack() }
  }

  /**
   * Breaks if a tap during the exit transition, when the edit is no longer on top, pops the details
   * too.
   */
  @Test
  fun backAndSaveDoNothingOnceTheEditIsNoLongerOnTop() {
    every { navController.currentBackStackEntry } returns mockk()

    navigator.onBack()
    navigator.onSaved()

    verify(exactly = 0) { navController.popBackStack() }
  }
}
