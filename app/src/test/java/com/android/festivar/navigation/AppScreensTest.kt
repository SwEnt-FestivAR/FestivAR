// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppScreensTest {
  // Breaks if a route without arguments is no longer matched to its screen.
  @Test
  fun aRouteWithoutArgumentsIsMatchedToItsScreen() {
    assertEquals(AppScreens.CreateEvent, AppScreens.fromRoute("CreateEvent"))
  }

  // Breaks if a route pattern with an argument is no longer matched to its screen.
  @Test
  fun aRouteWithAnArgumentIsMatchedToItsScreen() {
    assertEquals(AppScreens.CreateTask, AppScreens.fromRoute("CreateTask/{eventId}"))
  }

  // Breaks if the optional query of a route pattern stops being ignored when matching.
  @Test
  fun aRouteWithAQueryIsMatchedToItsScreen() {
    assertEquals(
        AppScreens.TaskDetails,
        AppScreens.fromRoute("TaskDetails/{taskId}?from={from}"),
    )
  }

  // Breaks if a query right after the screen name, without a slash, stops being ignored.
  @Test
  fun aRouteWithAQueryAndNoSlashIsMatchedToItsScreen() {
    assertEquals(AppScreens.MyTasks, AppScreens.fromRoute("MyTasks?from=x"))
  }

  // Breaks if an unknown or missing route is matched to a screen instead of null.
  @Test
  fun anUnknownOrMissingRouteMatchesNoScreen() {
    assertNull(AppScreens.fromRoute("Whatever/{x}"))
    assertNull(AppScreens.fromRoute(null))
  }

  // Breaks if a route is matched by prefix, so "TaskDetailsX" is taken for the task details.
  @Test
  fun aNameThatOnlyStartsLikeAScreenMatchesNoScreen() {
    assertNull(AppScreens.fromRoute("TaskDetailsX/{taskId}"))
  }

  // Breaks if a form is declared as another presentation, so it is treated as a regular screen.
  @Test
  fun everyFormIsDeclaredAsAForm() {
    val forms =
        setOf(
            AppScreens.JoinEvent,
            AppScreens.EditEvent,
            AppScreens.CreateEvent,
            AppScreens.CreateTask,
            AppScreens.CreateAnnouncement,
            AppScreens.EditTask,
        )
    assertEquals(
        forms,
        AppScreens.entries.filter { it.presentation == ScreenPresentation.Form }.toSet(),
    )
  }

  // Breaks if an authentication screen is declared as another presentation, so it is treated as a
  // regular screen.
  @Test
  fun everyAuthenticationScreenIsDeclaredAsAuth() {
    val auth = setOf(AppScreens.Login, AppScreens.ForgotPassword, AppScreens.SignUp)
    assertEquals(
        auth,
        AppScreens.entries.filter { it.presentation == ScreenPresentation.Auth }.toSet(),
    )
  }

  // Breaks if a task opened from an event stops carrying its origin, so the bar loses the badge.
  @Test
  fun taskDetailsDefaultsToTheEventOrigin() {
    assertEquals("TaskDetails/t1?from=EVENT", AppScreens.taskDetails("t1"))
  }

  // Breaks if the My tasks origin is dropped from the route, so the bar selects Events instead.
  @Test
  fun taskDetailsFromMyTasksCarriesTheMyTasksOrigin() {
    assertEquals(
        "TaskDetails/t1?from=MY_TASKS",
        AppScreens.taskDetails("t1", TaskOrigin.MY_TASKS),
    )
  }
}
