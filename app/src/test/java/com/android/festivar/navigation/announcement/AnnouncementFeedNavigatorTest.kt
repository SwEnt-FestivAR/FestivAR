// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AnnouncementFeedNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val backStackEntry = mockk<NavBackStackEntry>()
  private val navigator = AnnouncementFeedNavigator(navController, backStackEntry)

  @Test
  fun backClickPopsBackStackWhenAtCurrentEntry() {
    every { navController.currentBackStackEntry } returns backStackEntry

    navigator.onBack()

    verify { navController.popBackStack() }
  }

  @Test
  fun backClickDoesNotPopBackStackWhenNotAtCurrentEntry() {
    every { navController.currentBackStackEntry } returns mockk()

    navigator.onBack()

    verify(exactly = 0) { navController.popBackStack() }
  }

  @Test
  fun addAnnouncementNavigatesToCreateAnnouncement() {
    navigator.onAddAnnouncement("e1")

    verify { navController.navigate(AppScreens.createAnnouncement("e1")) }
  }

  @Test
  fun selectAnnouncementNavigatesToAnnouncementDetails() {
    navigator.onSelectAnnouncement("e1", "a1")

    verify { navController.navigate(AppScreens.announcementDetails("e1", "a1")) }
  }
}
