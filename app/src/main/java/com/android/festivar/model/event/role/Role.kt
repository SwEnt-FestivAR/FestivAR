package com.android.festivar.model.event.role

import com.android.festivar.model.event.role.Permission.*

/**
 * The [Role] enum class represents all actual existing roles that are attributed within an
 * [com.android.festivar.model.event.Event]. Each role has its own set of [Permission] that
 * represents what that member having this [Role] is allowed to do in the
 * [com.android.festivar.model.event.Event].
 *
 * For the permissions such as having the right to kick, to manage the role, to assign, if the
 * [Role] has that [Permission], he can apply that right only to the roles that it dominates.
 * Domination here is defined as a strict domination over the other [Role].
 *
 * @param permissions The set of permissions, of accesses that the role has within an
 *   [com.android.festivar.model.event.Event].
 */
enum class Role(val permissions: Set<Permission>) {

  // A visitor of the event, he can only see the event details (to a certain extent) and can see
  // the announcements.
  VISITOR(
      setOf(
          CAN_SEE_ANNOUNCEMENT,
      )
  ),
  // A staff member of the event, the lowest grade of a staff of the event. He can be managed by
  // anyone
  // and only interacts in the task he is assigned to and
  STAFF(
      setOf(
          CAN_SEE_TASK,
          CAN_EDIT_OWN_TASK,
          CAN_DELETE_OWN_TASK,
          CAN_CREATE_TASK,
          CAN_SEE_ANNOUNCEMENT,
      )
  ),
  // A team leader, he manages his own subteam of staff within the event for specific areas or
  // subjects within the event (the team lead for the workshop at 11AM for example).
  TEAM_LEAD(
      setOf(
          CAN_SEE_TASK,
          CAN_EDIT_OWN_TASK,
          CAN_DELETE_OWN_TASK,
          CAN_EDIT_TASK,
          CAN_DELETE_TASK,
          CAN_CREATE_TASK,
          CAN_ASSIGN_TASK,
          CAN_SEE_ANNOUNCEMENT,
          CAN_EDIT_OWN_ANNOUNCEMENT,
          CAN_DELETE_OWN_ANNOUNCEMENT,
          CAN_CREATE_ANNOUNCEMENT,
      )
  ),
  // A more general staff manager that manages the team leads and has more control on every task
  // and announcement.
  STAFF_MANAGER(
      setOf(
          CAN_SEE_TASK,
          CAN_EDIT_OWN_TASK,
          CAN_DELETE_OWN_TASK,
          CAN_EDIT_TASK,
          CAN_DELETE_TASK,
          CAN_CREATE_TASK,
          CAN_ASSIGN_TASK,
          CAN_SEE_ANNOUNCEMENT,
          CAN_EDIT_OWN_ANNOUNCEMENT,
          CAN_DELETE_OWN_ANNOUNCEMENT,
          CAN_EDIT_ANNOUNCEMENT,
          CAN_DELETE_ANNOUNCEMENT,
          CAN_CREATE_ANNOUNCEMENT,
          CAN_EDIT_EVENT,
          CAN_MANAGE_ROLES,
      )
  ),
  // An administrator of the event that has all rights at the exception of deleting the event and
  // having or transferring its ownership.
  ADMIN(
      setOf(
          CAN_SEE_TASK,
          CAN_EDIT_OWN_TASK,
          CAN_DELETE_OWN_TASK,
          CAN_EDIT_TASK,
          CAN_DELETE_TASK,
          CAN_CREATE_TASK,
          CAN_ASSIGN_TASK,
          CAN_SEE_ANNOUNCEMENT,
          CAN_EDIT_OWN_ANNOUNCEMENT,
          CAN_DELETE_OWN_ANNOUNCEMENT,
          CAN_EDIT_ANNOUNCEMENT,
          CAN_DELETE_ANNOUNCEMENT,
          CAN_CREATE_ANNOUNCEMENT,
          CAN_EDIT_EVENT,
          CAN_MANAGE_ROLES,
          CAN_KICK_MEMBER,
      )
  ),
  // The unique owner of the event that has all rights on it.
  OWNER(Permission.entries.toSet());

  /**
   * Returns whether this has the searched [permission] or not.
   *
   * @param permission The permission we try to find out if the [Role] contains.
   * @return true if this has [permission], false otherwise.
   */
  fun hasPermission(permission: Permission): Boolean {
    return permissions.contains(permission)
  }

  /**
   * Returns whether this dominates strictly [that].
   *
   * @param that The [Role] we try to check if this dominates or not.
   * @return true if this dominates [that], false otherwise.
   */
  fun dominates(that: Role): Boolean {
    return permissions.containsAll(that.permissions) && permissions.size > that.permissions.size
  }

  /**
   * Returns whether this and [that] are comparable in term of dominance.
   *
   * @param that The [Role] we try to check if it is comparable to this.
   * @return true if the roles are comparable (one dominates the other), false otherwise.
   */
  fun comparable(that: Role): Boolean {
    return this.dominates(that) || that.dominates(this)
  }

  /**
   * Returns whether this can manage the [Role] (upgrade it or downgrade it) of a different
   * [com.android.festivar.model.user.User].
   *
   * @param from The initial [Role] of the [com.android.festivar.model.user.User].
   * @param to The final [Role] of the [com.android.festivar.model.user.User] which this tries to
   *   change the [com.android.festivar.model.user.User] to.
   * @return true if this can manage the [Role] from [from] to [to] (needs to dominate the two roles
   *   and have the according [Permission]), false otherwise.
   */
  fun canManageTo(from: Role, to: Role): Boolean {
    return permissions.contains(CAN_MANAGE_ROLES) && dominates(from) && dominates(to)
  }

  /**
   * Returns whether this can kick a member out of the [com.android.festivar.model.event.Event].
   *
   * @param that The [Role] of the [com.android.festivar.model.user.User] this tries to kick.
   * @return true if this can kick the [com.android.festivar.model.user.User] (needs to dominate his
   *   [Role] and have the according [Permission]), false otherwise.
   */
  fun canKick(that: Role): Boolean {
    return permissions.contains(CAN_KICK_MEMBER) && dominates(that)
  }
}
