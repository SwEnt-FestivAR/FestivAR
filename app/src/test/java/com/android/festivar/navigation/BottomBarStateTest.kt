// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import com.android.festivar.ui.components.NavigationSubScreen
import com.android.festivar.ui.components.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomBarStateTest {
  private val taskDetailsRoute = "${AppScreens.TaskDetails.name}/{taskId}?from={from}"

  // Breaks if the events overview loses its bar or selects another tab.
  @Test
  fun theEventsOverviewSelectsEvents() {
    assertEquals(
        BottomBarState(NavigationTab.EVENTS),
        bottomBarStateFor(AppScreens.EventsOverview.name, null),
    )
  }

  // Breaks if the event details stop nesting the Events tab under the details disc.
  @Test
  fun theEventDetailsNestEventsUnderTheDetails() {
    assertEquals(
        BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.EVENT_DETAILS),
        bottomBarStateFor("${AppScreens.EventDetails.name}/{eventId}", null),
    )
  }

  // Breaks if the task board of an event stops nesting the Events tab under the tasks disc.
  @Test
  fun theTaskOverviewNestsEventsUnderTheTaskBoard() {
    assertEquals(
        BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.EVENT_TASKS),
        bottomBarStateFor("${AppScreens.TaskOverview.name}/{eventId}", null),
    )
  }

  // Breaks if a task opened from My tasks falls back to the Events tab or loses the task disc.
  @Test
  fun taskDetailsFromMyTasksNestMyTasksUnderTheTask() {
    assertEquals(
        BottomBarState(NavigationTab.MY_TASKS, NavigationSubScreen.TASK_DETAILS),
        bottomBarStateFor(taskDetailsRoute, TaskOrigin.MY_TASKS.name),
    )
  }

  // Breaks if a task opened from an event leaves the Events tab or loses the task disc.
  @Test
  fun taskDetailsFromAnEventNestEventsUnderTheTask() {
    assertEquals(
        BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.TASK_DETAILS),
        bottomBarStateFor(taskDetailsRoute, TaskOrigin.EVENT.name),
    )
  }

  // Breaks if a missing origin is not treated as the default event origin.
  @Test
  fun taskDetailsWithoutAnOriginNestEventsUnderTheTask() {
    assertEquals(
        BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.TASK_DETAILS),
        bottomBarStateFor(taskDetailsRoute, null),
    )
  }

  // Breaks if the My tasks destination does not select its own tab.
  @Test
  fun myTasksSelectsMyTasks() {
    assertEquals(
        BottomBarState(NavigationTab.MY_TASKS),
        bottomBarStateFor(AppScreens.MyTasks.name, null),
    )
  }

  // Breaks if the profile does not select its own tab.
  @Test
  fun theProfileSelectsProfile() {
    assertEquals(
        BottomBarState(NavigationTab.PROFILE),
        bottomBarStateFor(AppScreens.Profile.name, null),
    )
  }

  // Breaks if the bar shows over the login.
  @Test
  fun theLoginHasNoBar() {
    assertNull(bottomBarStateFor(AppScreens.Login.name, null))
  }

  // Breaks if the bar shows over the sign up.
  @Test
  fun theSignUpHasNoBar() {
    assertNull(bottomBarStateFor(AppScreens.SignUp.name, null))
  }

  // Breaks if the bar shows over the password reset.
  @Test
  fun theForgotPasswordHasNoBar() {
    assertNull(bottomBarStateFor(AppScreens.ForgotPassword.name, null))
  }

  // Breaks if the bar shows over the join form.
  @Test
  fun theJoinEventHasNoBar() {
    assertNull(bottomBarStateFor(AppScreens.JoinEvent.name, null))
  }

  // Breaks if the bar shows over the event creation form.
  @Test
  fun theCreateEventHasNoBar() {
    assertNull(bottomBarStateFor(AppScreens.CreateEvent.name, null))
  }

  // Breaks if the bar shows over the task creation form.
  @Test
  fun theCreateTaskHasNoBar() {
    assertNull(bottomBarStateFor("${AppScreens.CreateTask.name}/{eventId}", null))
  }

  // Breaks if the bar shows over the announcement form.
  @Test
  fun theCreateAnnouncementHasNoBar() {
    assertNull(bottomBarStateFor("${AppScreens.CreateAnnouncement.name}/{eventId}", null))
  }

  // Breaks if the bar shows over the task edit form.
  @Test
  fun theEditTaskHasNoBar() {
    assertNull(bottomBarStateFor("${AppScreens.EditTask.name}/{taskId}", null))
  }

  // Breaks if an unknown route crashes or gets a bar.
  @Test
  fun anUnknownRouteHasNoBar() {
    assertNull(bottomBarStateFor("Whatever/{x}", null))
  }

  // Breaks if the moment before the first destination crashes or gets a bar.
  @Test
  fun aNullRouteHasNoBar() {
    assertNull(bottomBarStateFor(null, null))
  }
}
