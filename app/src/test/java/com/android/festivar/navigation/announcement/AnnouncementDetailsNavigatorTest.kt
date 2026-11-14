// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
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
}
