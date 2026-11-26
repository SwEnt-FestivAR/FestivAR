// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.navigation.AppScreens
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementDetailsNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = AnnouncementDetailsNavigator(navController)

  @Test
  fun backClickPopsBackStack() {
    navigator.onBack()

    verify { navController.popBackStack() }
  }

  @Test
  fun detailsRouteIncludesEventAndAnnouncementIds() {
    assertEquals(
        "AnnouncementDetails/event-123/announcement-456",
        AppScreens.announcementDetails("event-123", "announcement-456"),
    )
  }

  /**
   * Breaks if edit opens another route than the edit of this announcement of this event (for
   * example with the two ids swapped), or if the navigation drops launchSingleTop, so that a double
   * tap opens the edit twice.
   */
  @Test
  fun editOpensTheEditOfTheAnnouncementSingleTop() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onEdit("event-123", "announcement-456")

    verify {
      navController.navigate("EditAnnouncement/event-123/announcement-456", capture(options))
    }
    assertTrue(navOptions(options.captured).shouldLaunchSingleTop())
  }
}
