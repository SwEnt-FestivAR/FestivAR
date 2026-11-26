// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.event.create.CreateEventScreenTestTags
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.edit.EditEventScreenTestTags
import com.android.festivar.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
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
            authRepository =
                FakeAuthRepository(
                    currentUser = mockk<FirebaseUser> { every { uid } returns "owner-1" }
                ),
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
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route ==
          "${AppScreens.EventDetails.name}/{eventId}"
    }

    assertEquals(eventId, navController.currentBackStackEntry?.arguments?.getString("eventId"))
  }

  /** Breaks if a member whose role cannot edit the event is not sent back to the event details. */
  @Test
  fun editEventRoute_memberWithoutEditRightReturnsToDetails() {
    val eventId = "event-123"
    val staffId = "staff-1"
    val event =
        Event(
                eventId = eventId,
                ownerId = "owner-1",
                title = "Festival",
                startDate = ZonedDateTime.now().plusDays(1),
                endDate = ZonedDateTime.now().plusDays(2),
            )
            .addMember(User(uid = staffId))
            .let { it.setRole(it.ownerId, staffId, Role.STAFF) }
    val dataRepository =
        DataRepositoryLocal(
            authRepository =
                FakeAuthRepository(
                    currentUser = mockk<FirebaseUser> { every { uid } returns staffId }
                ),
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
      navController.currentBackStackEntry?.destination?.route ==
          "${AppScreens.EventDetails.name}/{eventId}"
    }

    assertEquals(eventId, navController.currentBackStackEntry?.arguments?.getString("eventId"))
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SCREEN).assertDoesNotExist()
  }

  /** Breaks if deleting the event from its details does not delete it and go back to the list. */
  @Test
  fun eventDetailsRoute_deletingReturnsToTheEventsOverview() {
    val eventId = "event-123"
    val owner = User(uid = "owner-1", joinedEvents = listOf(eventId))
    val event =
        Event(
            eventId = eventId,
            ownerId = owner.uid,
            title = "Festival",
            members = listOf(owner),
            startDate = ZonedDateTime.now().plusDays(1),
            endDate = ZonedDateTime.now().plusDays(2),
        )
    val eventsRepository = EventsRepositoryLocal(listOf(event))
    val dataRepository =
        DataRepositoryLocal(
            authRepository =
                FakeAuthRepository(
                    currentUser =
                        mockk<FirebaseUser> {
                          every { uid } returns owner.uid
                          every { isAnonymous } returns false
                        }
                ),
            eventsRepository = eventsRepository,
            usersRepository = UsersRepositoryLocal(listOf(owner)),
        )
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      AppTheme {
        navController = rememberNavController()
        NavHost(navController, startDestination = AppScreens.EventsOverview.name) {
          eventGraph(navController, dataRepository)
        }
      }
    }
    composeTestRule.runOnIdle { navController.navigate(AppScreens.eventDetails(eventId)) }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EventDetailsScreenTestTags.MORE_ACTIONS_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.DELETE_MENU_ITEM).performClick()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.DELETE_CONFIRM_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route == AppScreens.EventsOverview.name
    }

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { eventsRepository.getEvent(eventId) }
    }
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
            authRepository = FakeAuthRepository(currentUser = firebaseUser),
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

  /** Breaks if the create event route lets an anonymous account stay on the form. */
  @Test
  fun createEventRoute_anonymousAccountReturnsToOverview() {
    val navController = openCreateEventRoute(isAnonymous = true)

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route == AppScreens.EventsOverview.name
    }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).assertDoesNotExist()
  }

  /** Breaks if the create event route sends a signed-up account back to the overview. */
  @Test
  fun createEventRoute_signedUpAccountStaysOnForm() {
    val navController = openCreateEventRoute(isAnonymous = false)

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(CreateEventScreenTestTags.NAME_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    assertEquals(
        AppScreens.CreateEvent.name,
        navController.currentBackStackEntry?.destination?.route,
    )
  }

  /**
   * Starts on the events overview, signed in with an account that is anonymous or not, and opens
   * the create event route on top of it, as the "Create" button does.
   */
  private fun openCreateEventRoute(isAnonymous: Boolean): NavHostController {
    val uid = "user-1"
    val firebaseUser =
        mockk<FirebaseUser> {
          every { this@mockk.uid } returns uid
          every { this@mockk.isAnonymous } returns isAnonymous
        }
    val dataRepository =
        DataRepositoryLocal(
            authRepository = FakeAuthRepository(currentUser = firebaseUser),
            usersRepository = UsersRepositoryLocal(listOf(User(uid = uid))),
        )
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      AppTheme {
        navController = rememberNavController()
        NavHost(navController, startDestination = AppScreens.EventsOverview.name) {
          eventGraph(navController, dataRepository)
        }
      }
    }
    composeTestRule.runOnIdle { navController.navigate(AppScreens.CreateEvent.name) }
    return navController
  }
}
