// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

import com.android.festivar.model.user.User
import java.time.Duration
import java.time.LocalDateTime

/**
 * Represents a [Task] in the FestivAR application. A [Task] is something, in the context of an
 * [Event], that has to be done. To allow people to properly realize what it asks for, it has
 * several informational fields that may or may not be used by the creator of the [Task].
 *
 * A [Task] is immutable: its fields can never be changed. Every "modification" ([copy],
 * [addAssignee], [removeAssignee], [complete]) returns a new [Task] instead, so the result must be
 * kept as: `task = task.complete()`. And for any other change of one of the [Task] variables, it
 * has to be done through a copy of this [Task]. This implementation is the consequence of working
 * with Compose that needs, to refresh the View automatically, to have an object that is reassigned.
 * Only changing a field of an object would not automatically refresh the View.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [title] is not blank;
 * - [taskId] is not blank;
 * - [eventId] is not blank;
 * - [maxAssign] is at least 1;
 * - [maxAssign] is never lower than the number of [assignees];
 * - a [User] is assigned at most once, identified by its [User.uid] (no duplicates);
 * - [endTime] is not before [startTime];
 * - [estimatedTime] is not negative.
 *
 * @property taskId Unique identifier of the [Task].
 * @property eventId Identifier of the [Event] the [Task] belongs to.
 * @property title Name of the [Task], shown as the screen title.
 * @property description Free-text description of what has to be done.
 * @property startTime Beginning time of the [Task], or `null` if not set.
 * @property endTime Ending time of the [Task], or `null` if not set.
 * @property estimatedTime Estimation of the time needed to complete the [Task] (e.g. 1h), or `null`
 *   if not set.
 * @property location Where the [Task] takes place.
 * @property priority How urgent the [Task] is.
 * @property maxAssign Maximum number of people who can join or be assigned to the [Task].
 * @property assignees People who joined or were assigned the [Task].
 * @property completed Whether the [Task] has been completed.
 */
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
    require(assignees.distinctBy { it.uid }.size == assignees.size) { "A user is assigned twice." }
    require(startTime == null || endTime == null || !endTime.isBefore(startTime)) {
      "endTime must not be before startTime."
    }
    require(estimatedTime == null || !estimatedTime.isNegative) {
      "estimatedTime cannot be negative."
    }
  }

  /**
   * Meant to inform that the [Task] is completed by returning a completed version copy of `this`.
   *
   * @return The [Task] but completed.
   */
  fun complete(): Task {
    return copy(completed = true)
  }

  /**
   * Adds a new assigned [User] to the [Task] by returning a version of `this` with [user] added to
   * the [assignees] list.
   *
   * @param user The [User] that is going to be assigned to `this`, to be added to [assignees] list.
   * @return The [Task] but with [user] added to it.
   * @throws IllegalStateException If the [Task] is already full.
   * @throws IllegalArgumentException If [user] is already assigned, identified by its [User.uid].
   */
  fun addAssignee(user: User): Task {
    check(assignees.size < maxAssign) { "The task is full." }

    require(assignees.none { it.uid == user.uid }) { "This user is already assigned to the task." }

    return copy(assignees = assignees + user)
  }

  /**
   * Removes an assigned [User] from the [Task] by returning a version of `this` with [user] removed
   * from the [assignees] list. The assignee is identified by its [User.uid], so it is removed even
   * if its name or surname differ from the ones of [user].
   *
   * @param user The [User] that is going to be removed from `this`, from [assignees] list.
   * @return The [Task] but with [user] removed from it.
   * @throws IllegalArgumentException If [user] is not assigned, identified by its [User.uid].
   */
  fun removeAssignee(user: User): Task {
    require(assignees.any { it.uid == user.uid }) { "This user is not assigned to the task." }

    return copy(assignees = assignees.filterNot { it.uid == user.uid })
  }
}
