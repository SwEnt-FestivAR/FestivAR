// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.role.Permission
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User

// The checks of what a member of an [Event] is allowed to do on its tasks, announcements and
// members. They only answer a question and never change the [Event], built on [Event.roleOf] and
// [Event.canAct].

/**
 * Tells whether a member can edit a [Task] of the [Event]: either their [Role] has
 * [Permission.CAN_EDIT_TASK], or they own the [task] and their [Role] has
 * [Permission.CAN_EDIT_OWN_TASK].
 *
 * @param userId The [User.uid] of the member who wants to edit [task].
 * @param task The [Task] to edit.
 * @return `true` if the member can edit [task], `false` otherwise.
 * @throws IllegalArgumentException If [task] is linked to a different event, or if [userId] is
 *   not a member of the event.
 */
fun Event.canEditTask(userId: String, task: Task): Boolean {
  requireTaskOfThisEvent(task)
  return canActOn(userId, task.ownerId, Permission.CAN_EDIT_TASK, Permission.CAN_EDIT_OWN_TASK)
}

/**
 * Tells whether a member can delete a [Task] of the [Event]: either their [Role] has
 * [Permission.CAN_DELETE_TASK], or they own the [task] and their [Role] has
 * [Permission.CAN_DELETE_OWN_TASK].
 *
 * @param userId The [User.uid] of the member who wants to delete [task].
 * @param task The [Task] to delete.
 * @return `true` if the member can delete [task], `false` otherwise.
 * @throws IllegalArgumentException If [task] is linked to a different event, or if [userId] is
 *   not a member of the event.
 */
fun Event.canDeleteTask(userId: String, task: Task): Boolean {
  requireTaskOfThisEvent(task)
  return canActOn(userId, task.ownerId, Permission.CAN_DELETE_TASK, Permission.CAN_DELETE_OWN_TASK)
}

/**
 * Tells whether a member can mark a [Task] of the [Event] as done: they must be one of its
 * [Task.assignees] and their [Role] must have [Permission.CAN_MARK_TAKEN_TASK_DONE].
 *
 * @param userId The [User.uid] of the member who wants to mark [task] as done.
 * @param task The [Task] to mark as done.
 * @return `true` if the member can mark [task] as done, `false` otherwise.
 * @throws IllegalArgumentException If [task] is linked to a different event, or if [userId] is
 *   not a member of the event.
 */
fun Event.canMarkTaskDone(userId: String, task: Task): Boolean {
  requireTaskOfThisEvent(task)
  return canAct(userId, Permission.CAN_MARK_TAKEN_TASK_DONE) &&
      task.assignees.any { it.uid == userId }
}

/**
 * Tells whether a member can assign a task to another member: their [Role] must have
 * [Permission.CAN_ASSIGN_TASK] and strictly dominate the [Role] of the assignee. A member who wants
 * a task for themselves takes it instead, with [Permission.CAN_TAKE_TASK].
 *
 * @param actorId The [User.uid] of the member who assigns the task.
 * @param assigneeId The [User.uid] of the member the task is assigned to.
 * @return `true` if [actorId] can assign a task to [assigneeId], `false` otherwise.
 * @throws IllegalArgumentException If [actorId] or [assigneeId] is not a member of the event.
 */
fun Event.canAssignTask(actorId: String, assigneeId: String): Boolean {
  val actorRole = roleOf(actorId)
  return actorRole.hasPermission(Permission.CAN_ASSIGN_TASK) &&
      actorRole.dominates(roleOf(assigneeId))
}

/**
 * Tells whether a member can edit an [Announcement] of the [Event]: either their [Role] has
 * [Permission.CAN_EDIT_ANNOUNCEMENT], or they are its author and their [Role] has
 * [Permission.CAN_EDIT_OWN_ANNOUNCEMENT].
 *
 * @param userId The [User.uid] of the member who wants to edit [announcement].
 * @param announcement The [Announcement] to edit.
 * @return `true` if the member can edit [announcement], `false` otherwise.
 * @throws IllegalArgumentException If [announcement] is not in the event, or if [userId] is not a
 *   member of the event.
 */
fun Event.canEditAnnouncement(userId: String, announcement: Announcement): Boolean {
  requireAnnouncementOfThisEvent(announcement)
  return canActOn(
      userId,
      announcement.authorId,
      Permission.CAN_EDIT_ANNOUNCEMENT,
      Permission.CAN_EDIT_OWN_ANNOUNCEMENT,
  )
}

/**
 * Tells whether a member can delete an [Announcement] of the [Event]: either their [Role] has
 * [Permission.CAN_DELETE_ANNOUNCEMENT], or they are its author and their [Role] has
 * [Permission.CAN_DELETE_OWN_ANNOUNCEMENT].
 *
 * @param userId The [User.uid] of the member who wants to delete [announcement].
 * @param announcement The [Announcement] to delete.
 * @return `true` if the member can delete [announcement], `false` otherwise.
 * @throws IllegalArgumentException If [announcement] is not in the event, or if [userId] is not a
 *   member of the event.
 */
fun Event.canDeleteAnnouncement(userId: String, announcement: Announcement): Boolean {
  requireAnnouncementOfThisEvent(announcement)
  return canActOn(
      userId,
      announcement.authorId,
      Permission.CAN_DELETE_ANNOUNCEMENT,
      Permission.CAN_DELETE_OWN_ANNOUNCEMENT,
  )
}

/**
 * Tells whether a member can change the [Role] of another member, as decided by
 * [Role.canManageTo]: their [Role] must have [Permission.CAN_MANAGE_ROLES] and strictly dominate
 * both the current [Role] of [userId] and [newRole]. So nobody can change their own [Role], change
 * the [Role] of the owner, or give the [Role.OWNER].
 *
 * @param actorId The [User.uid] of the member who wants to change the [Role].
 * @param userId The [User.uid] of the member whose [Role] would change.
 * @param newRole The [Role] that [userId] would get.
 * @return `true` if [actorId] can give [newRole] to [userId], `false` otherwise.
 * @throws IllegalArgumentException If [actorId] or [userId] is not a member of the event.
 */
fun Event.canSetRole(actorId: String, userId: String, newRole: Role): Boolean {
  return roleOf(actorId).canManageTo(roleOf(userId), newRole)
}

/**
 * Tells whether a member can kick another member out of the [Event], as decided by
 * [Role.canKick]: their [Role] must have [Permission.CAN_KICK_MEMBER] and strictly dominate the
 * [Role] of [userId]. So nobody can kick themselves or the owner.
 *
 * @param actorId The [User.uid] of the member who wants to kick.
 * @param userId The [User.uid] of the member who would be kicked.
 * @return `true` if [actorId] can kick [userId], `false` otherwise.
 * @throws IllegalArgumentException If [actorId] or [userId] is not a member of the event.
 */
fun Event.canKick(actorId: String, userId: String): Boolean {
  return roleOf(actorId).canKick(roleOf(userId))
}

/**
 * Tells whether a member can act on an object owned by [ownerId]: either their [Role] has
 * [anyPermission], or they own the object and their [Role] has [ownPermission].
 */
private fun Event.canActOn(
    userId: String,
    ownerId: String,
    anyPermission: Permission,
    ownPermission: Permission,
): Boolean {
  return canAct(userId, anyPermission) || (ownerId == userId && canAct(userId, ownPermission))
}

/** Checks that [task] belongs to the [Event], through its [Task.eventId]. */
private fun Event.requireTaskOfThisEvent(task: Task) {
  require(task.eventId == eventId) { "This task is linked to a different event." }
}

/** Checks that [announcement] is one of the [Event.announcements]. */
private fun Event.requireAnnouncementOfThisEvent(announcement: Announcement) {
  require(announcementOrNull(announcement.announcementId) != null) {
    "This announcement is not in the event."
  }
}
