// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

/**
 * Represents a [User] in the FestivAR application.
 *
 * A [User] is immutable: changes return a new [User] instance, so the returned value must be kept.
 * Users are identified by their [uid]; names and other profile data do not affect that identity.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [uid] is not blank;
 * - [eventsOwned] contains no blank or duplicate event identifiers.
 * - [tasksAssigned] contains no blank or duplicate task identifiers.
 *
 * @property uid The user's unique identifier.
 * @property name The user's name.
 * @property surname The user's surname.
 * @property email The user's email address.
 * @property eventsOwned EventIds of events created/owned by the user.
 * @property tasksAssigned TaskIds of tasks assigned to the user.
 */
data class User(
    val uid: String,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val eventsOwned: List<String> = emptyList(),
    val tasksAssigned: List<String> = emptyList(),
) {

  init {
    require(uid.isNotBlank()) { "The uid cannot be empty." }
    require(eventsOwned.none(String::isBlank)) { "An owned event id cannot be empty." }
    require(eventsOwned.distinct().size == eventsOwned.size) { "An owned event is duplicated." }
    require(tasksAssigned.none(String::isBlank)) { "An assigned task id cannot be empty." }
    require(tasksAssigned.distinct().size == tasksAssigned.size) {
      "An assigned task is duplicated."
    }
  }

  /**
   * Adds an event identifier to [eventsOwned] by returning a new version of this user.
   *
   * @throws IllegalArgumentException if [eventId] is already owned.
   */
  fun addOwnedEvent(eventId: String): User {
    require(eventId !in eventsOwned) { "This event is already owned by the user." }
    return copy(eventsOwned = eventsOwned + eventId)
  }

  /**
   * Adds a task identifier to [tasksAssigned] by returning a new version of this user.
   *
   * @throws IllegalArgumentException if [taskId] is already assigned.
   */
  fun addAssignedTask(taskId: String): User {
    require(taskId !in tasksAssigned) { "This task is already assigned to the user." }
    return copy(tasksAssigned = tasksAssigned + taskId)
  }

  /**
   * Removes an event identifier from [eventsOwned] by returning a new version of this user.
   *
   * @throws IllegalArgumentException if [eventId] is not owned by this user.
   */
  fun removeOwnedEvent(eventId: String): User {
    require(eventId in eventsOwned) { "This event is not owned by the user." }
    return copy(eventsOwned = eventsOwned - eventId)
  }
}
