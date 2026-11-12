// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

// This enum defines the different screens in the app, used for navigation purposes.
// Screens without their real implementation yet show a placeholder in the navigation graph
enum class AppScreens {
  Login,
  ForgotPassword,
  SignUp,
  TaskOverview,
  EventsOverview,
  Profile,
  JoinEvent,
  EventDetails,
  EditEvent,
  CreateEvent,
  CreateTask,
  CreateAnnouncement,
  AnnouncementDetails,
  AnnouncementFeed,
  TaskDetails,
  EditTask,
  MyTasks;

  companion object {
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

/** Where a task's details were opened from; it decides the selected tab of the bottom bar. */
enum class TaskOrigin {
  EVENT,
  MY_TASKS,
}
