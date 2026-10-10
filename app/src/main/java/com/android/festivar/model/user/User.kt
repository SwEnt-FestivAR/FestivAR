// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import com.android.festivar.model.event.Event
import com.android.festivar.model.task.Task

/**
 * Represents a [User] in the FestivAR application.
 *
 * A [User] is immutable: changes return a new [User] instance, so the returned value must be kept.
 * Users are identified by their [uid]; names and other profile data do not affect that identity.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [uid] is not blank;
 * - [joinedEvents] contains no blank or duplicate event identifiers.
 * - [tasksAssigned] contains no blank or duplicate task identifiers.
 *
 * @property uid The user's unique identifier.
 * @property name The user's name.
 * @property surname The user's surname.
 * @property email The user's email address.
 * @property joinedEvents EventIds of events the user has joined.
 * @property tasksAssigned TaskIds of tasks assigned to the user.
 */
data class User(
    val uid: String,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val joinedEvents: List<String> = emptyList(),
    val tasksAssigned: List<String> = emptyList(),
) {

  init {
    require(uid.isNotBlank()) { "The uid cannot be empty." }
    require(joinedEvents.none(String::isBlank)) { "A joined event id cannot be empty." }
    require(joinedEvents.distinct().size == joinedEvents.size) { "A joined event is duplicated." }
    require(tasksAssigned.none(String::isBlank)) { "An assigned task id cannot be empty." }
    require(tasksAssigned.distinct().size == tasksAssigned.size) {
      "An assigned task is duplicated."
    }
  }

  /**
   * Adds an event identifier to [joinedEvents] by returning a new version of this user.
   *
   * This method only updates the current [User]. Propagating the change to the corresponding
   * [Event] object, if any, is the caller's responsibility.
   *
   * @throws IllegalArgumentException if [eventId] is already joined.
   */
  fun addJoinedEvent(eventId: String): User {
    require(eventId !in joinedEvents) { "This event is already joined by the user." }
    return copy(joinedEvents = joinedEvents + eventId)
  }

  /**
   * Adds a task identifier to [tasksAssigned] by returning a new version of this user.
   *
   * This method only updates the current [User]. Propagating the change to the corresponding [Task]
   * object, if any, is the caller's responsibility.
   *
   * @throws IllegalArgumentException if [taskId] is already assigned.
   */
  fun addAssignedTask(taskId: String): User {
    require(taskId !in tasksAssigned) { "This task is already assigned to the user." }
    return copy(tasksAssigned = tasksAssigned + taskId)
  }

  /**
   * Removes an event identifier from [joinedEvents] by returning a new version of this user.
   *
   * This method only updates the current [User]. Propagating the change to the corresponding
   * [Event] object, if any, is the caller's responsibility.
   *
   * @throws IllegalArgumentException if [eventId] has not been joined by this user.
   */
  fun removeJoinedEvent(eventId: String): User {
    require(eventId in joinedEvents) { "This event has not been joined by the user." }
    return copy(joinedEvents = joinedEvents - eventId)
  }

  /**
   * Removes a task identifier from [tasksAssigned] by returning a new version of this user.
   *
   * This method only updates the current [User]. Propagating the change to the corresponding [Task]
   * object, if any, is the caller's responsibility.
   *
   * @throws IllegalArgumentException if [taskId] is not assigned to this user.
   */
  fun removeAssignedTask(taskId: String): User {
    require(taskId in tasksAssigned) { "This task is not assigned to the user." }
    return copy(tasksAssigned = tasksAssigned - taskId)
  }
}
