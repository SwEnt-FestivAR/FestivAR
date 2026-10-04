package com.android.festivar.model.task

import com.android.festivar.model.temporary.User
import java.time.Duration
import java.time.LocalDateTime

data class Task(
    val taskId: String,
    val eventId: String,
    val title: String,
    val description: String = "",
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val estimatedTime: Duration? = null,
    val location: String = "",
    val priority: Priority = Priority.NONE,
    val maxAssign: Int = 1,
    val assignees: List<User> = emptyList(),
    val completed: Boolean = false,
) {

  init {
      require(taskId.isNotBlank()) { "The taskId cannot be empty." }
      require(eventId.isNotBlank()) { "The eventId cannot be empty." }
      require(title.isNotBlank()) { "The title cannot be empty." }
      require(maxAssign >= 1) { "maxAssign must be at least 1." }
      require(assignees.size <= maxAssign) {
          "maxAssign is lower than the number of assigned people."
      }
      require(assignees.distinct().size == assignees.size) { "A user is assigned twice." }
      require(startTime == null || endTime == null || !endTime.isBefore(startTime)) {
          "endTime must be after startTime."
      }
      require(estimatedTime == null || !estimatedTime.isNegative) {
          "estimatedTime cannot be negative."
      }
  }

  fun complete(): Task {
      return copy(completed = true)
  }

  fun addAssignee(user: User): Task {
    check(assignees.size < maxAssign) { "The task is full." }

    require(user !in assignees) {
        "This user is already assigned to the task."
    }

    return copy(assignees = assignees + user)
  }

  fun removeAssignee(user: User): Task {
    require(user in assignees) {
        "This user is not assigned to the task."
    }
    return copy(assignees = assignees - user)
  }
}
