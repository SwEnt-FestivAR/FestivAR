// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.event

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.event.edit.EditEventScreenTestTags
import com.android.festivar.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventNavGraphTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun editEventRoute_loadsEventFromArgumentAndBackReturnsToDetails() {
    val eventId = "event-123"
    val event =
        Event(
            eventId = eventId,
            ownerId = "owner-1",
            title = "Festival from route",
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(2),
        )
    val dataRepository =
        DataRepositoryLocal(
            authRepository = mockk<AuthRepository> { every { currentUser } returns null },
            eventsRepository = EventsRepositoryLocal(listOf(event)),
        )
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      AppTheme {
        navController = rememberNavController()
        NavHost(navController, startDestination = AppScreens.eventDetails(eventId)) {
          eventGraph(navController, dataRepository)
        }
      }
    }
    composeTestRule.runOnIdle { navController.navigate(AppScreens.editEvent(eventId)) }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.NAME_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    assertEquals(
        eventId,
        navController.currentBackStackEntry?.arguments?.getString("eventId"),
    )
    val nameField = composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD)
    assertEquals(
        event.title,
        nameField.fetchSemanticsNode().config[SemanticsProperties.EditableText].text,
    )
    nameField.performTextClearance()
    nameField.performTextInput("Unsaved festival")
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.SAVE_ERROR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    nameField.performTextClearance()
    nameField.performTextInput(event.title)
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route ==
          "${AppScreens.EventDetails.name}/{eventId}"
    }

    assertEquals(eventId, navController.currentBackStackEntry?.arguments?.getString("eventId"))
  }

  @Test
  fun editEventRoute_rejectsBlankEventId() {
    assertThrows(IllegalArgumentException::class.java) { requireEventId(" ") }
  }

  @Test
  fun editEventRoute_savingReturnsToDetails() {
    val eventId = "event-123"
    val event =
        Event(
            eventId = eventId,
            ownerId = "owner-1",
            title = "Festival",
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(2),
        )
    val firebaseUser = mockk<FirebaseUser> { every { uid } returns event.ownerId }
    val dataRepository =
        DataRepositoryLocal(
            authRepository = mockk<AuthRepository> { every { currentUser } returns firebaseUser },
            eventsRepository = EventsRepositoryLocal(listOf(event)),
        )
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      AppTheme {
        navController = rememberNavController()
        NavHost(navController, startDestination = AppScreens.eventDetails(eventId)) {
          eventGraph(navController, dataRepository)
        }
      }
    }
    composeTestRule.runOnIdle { navController.navigate(AppScreens.editEvent(eventId)) }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.NAME_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.NAME_FIELD)
        .performTextInput("Updated festival")
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route ==
          "${AppScreens.EventDetails.name}/{eventId}"
    }

    assertEquals(eventId, navController.currentBackStackEntry?.arguments?.getString("eventId"))
  }
}
