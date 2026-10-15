// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

  @Test
  fun selectAvatarNavigatesToProfile() {
    navigator.onSelectAvatar()

    verify { navController.navigate(AppScreens.Profile.name) }
  }

  @Test
  fun signedOutNavigatesToLoginAndClearsBackStack() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onSignedOut()

    verify { navController.navigate(AppScreens.Login.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertEquals(navController.graph.id, navOptions.popUpToId)
    assertTrue(navOptions.isPopUpToInclusive())
  }
}
