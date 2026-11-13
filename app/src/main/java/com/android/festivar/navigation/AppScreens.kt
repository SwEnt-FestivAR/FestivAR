// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

// This enum defines the different screens in the app, used for navigation purposes.
// Screens without their real implementation yet show a placeholder in the navigation graph
// Each screen declares how it is presented, here and nowhere else; Screen is the default.
enum class AppScreens(val presentation: ScreenPresentation = ScreenPresentation.Screen) {
  Login(ScreenPresentation.Auth),
  ForgotPassword(ScreenPresentation.Auth),
  SignUp(ScreenPresentation.Auth),
  TaskOverview,
  EventsOverview,
  Profile,
  JoinEvent(ScreenPresentation.Form),
  EventDetails,
  EditEvent(ScreenPresentation.Form),
  CreateEvent(ScreenPresentation.Form),
  CreateTask(ScreenPresentation.Form),
  CreateAnnouncement(ScreenPresentation.Form),
  AnnouncementDetails,
  AnnouncementFeed,
  TaskDetails,
  EditTask(ScreenPresentation.Form),
  MyTasks;

  companion object {
    /**
     * The screen whose route pattern is [route], as `NavDestination.route` gives it (for example
     * "TaskDetails/{taskId}?from={from}"); null for no route or an unknown one.
     */
    fun fromRoute(route: String?): AppScreens? {
      val name = route?.substringBefore('/')?.substringBefore('?')
      return entries.firstOrNull { it.name == name }
    }

    fun eventDetails(eventId: String) = "${EventDetails.name}/$eventId"

    fun editEvent(eventId: String) = "${EditEvent.name}/$eventId"

    fun taskDetails(taskId: String, from: TaskOrigin = TaskOrigin.EVENT) =
        "${TaskDetails.name}/$taskId?from=${from.name}"

    fun taskOverview(eventId: String) = "${TaskOverview.name}/$eventId"

    fun createTask(eventId: String) = "${CreateTask.name}/$eventId"

    fun editTask(taskId: String) = "${EditTask.name}/$taskId"

    fun createAnnouncement(eventId: String) = "${CreateAnnouncement.name}/$eventId"

    fun announcementDetails(eventId: String, announcementId: String) =
        "${AnnouncementDetails.name}/$eventId/$announcementId"

    fun announcementFeed(eventId: String) = "${AnnouncementFeed.name}/$eventId"
  }
}

/** How a screen is presented, declared once per screen in [AppScreens]. */
enum class ScreenPresentation {
  /** A regular screen; whether it shows the bottom bar is decided in `bottomBarStateFor`. */
  Screen,

  /** A form: creating or editing something, or joining an event. */
  Form,

  /** Login, sign up and password reset. */
  Auth,
}

/** Where a task's details were opened from; it decides the selected tab of the bottom bar. */
enum class TaskOrigin {
  EVENT,
  MY_TASKS,
}
