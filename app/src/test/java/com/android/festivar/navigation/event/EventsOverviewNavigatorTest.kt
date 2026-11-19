// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class EventsOverviewNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = EventsOverviewNavigator(navController)

  @Test
  fun selectEventNavigatesToEventDetails() {
    navigator.onSelectEvent(mockk { every { eventId } returns "e1" })

    verify { navController.navigate(AppScreens.eventDetails("e1")) }
  }

  @Test
  fun addEventNavigatesToCreateEvent() {
    navigator.onAddEvent()

    verify { navController.navigate(AppScreens.CreateEvent.name) }
  }

  @Test
  fun joinEventNavigatesToJoinEvent() {
    navigator.onJoinEvent()

    verify { navController.navigate(AppScreens.JoinEvent.name) }
  }
}
