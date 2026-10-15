// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
package com.android.festivar.navigation

// This enum defines the different screens in the app, used for navigation purposes.
// Put only fully implemented screens here
enum class AppScreens {
  Login,
  SignUp,
  TaskOverview,
  EventOverview,
  EventDetails,
  CreateEvent,
  CreateTask,
  TaskDetails;

  companion object {
    fun taskDetails(taskId: String) = "${TaskDetails.name}/$taskId"

    fun taskOverview(eventId: String) = "${TaskOverview.name}/$eventId"
  }
}
