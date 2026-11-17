// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event.role

import com.android.festivar.model.event.role.Permission.CAN_ASSIGN_TASK
import com.android.festivar.model.event.role.Permission.CAN_CREATE_ANNOUNCEMENT
import com.android.festivar.model.event.role.Permission.CAN_CREATE_TASK
import com.android.festivar.model.event.role.Permission.CAN_DELETE_ANNOUNCEMENT
import com.android.festivar.model.event.role.Permission.CAN_DELETE_OWN_ANNOUNCEMENT
import com.android.festivar.model.event.role.Permission.CAN_DELETE_OWN_TASK
import com.android.festivar.model.event.role.Permission.CAN_DELETE_TASK
import com.android.festivar.model.event.role.Permission.CAN_EDIT_ANNOUNCEMENT
import com.android.festivar.model.event.role.Permission.CAN_EDIT_EVENT
import com.android.festivar.model.event.role.Permission.CAN_EDIT_OWN_ANNOUNCEMENT
import com.android.festivar.model.event.role.Permission.CAN_EDIT_OWN_TASK
import com.android.festivar.model.event.role.Permission.CAN_EDIT_TASK
import com.android.festivar.model.event.role.Permission.CAN_KICK_MEMBER
import com.android.festivar.model.event.role.Permission.CAN_MANAGE_ROLES
import com.android.festivar.model.event.role.Permission.CAN_SEE_ANNOUNCEMENT
import com.android.festivar.model.event.role.Permission.CAN_SEE_TASK
import com.android.festivar.model.event.role.Permission.CAN_TAKE_TASK
import com.android.festivar.model.event.role.Permission.CAN_MARK_TASK_DONE
import com.android.festivar.model.event.role.Permission.CAN_SEE_MEMBERS

/**
 * The [Role] enum class represents all actual existing roles that are attributed within an
 * [com.android.festivar.model.event.Event]. Each role has its own set of [Permission] that
 * represents what a member having this [Role] is allowed to do in the
 * [com.android.festivar.model.event.Event].
 *
 * For the permissions such as having the right to kick, to manage the role, to assign, if the
 * [Role] has that [Permission], it can apply that right only to the roles that it dominates.
 * Domination here is defined as a strict domination over the other [Role].
 *
 * @param permissions The set of permissions, of accesses that the role has within an
 *   [com.android.festivar.model.event.Event].
 */
enum class Role(val permissions: Set<Permission>) {

  // A visitor of the event. They can only see the event details (to a certain extent) and the
  // announcements.
  VISITOR(VISITOR_PERMISSIONS),
  // A staff member of the event, the lowest grade of staff. They can see the tasks, create new
  // ones, and edit or delete only the tasks they own.
  STAFF(STAFF_PERMISSIONS),
  // A team leader, who coordinates the staff for specific areas or subjects within the event (the
  // team lead for the workshop at 11AM for example). They can edit, delete and assign every task
  // of the event, and publish announcements.
  TEAM_LEAD(TEAM_LEAD_PERMISSIONS),
  // A more general staff manager that manages the team leads and has more control on every task
  // and announcement.
  STAFF_MANAGER(STAFF_MANAGER_PERMISSIONS),
  // An administrator of the event, who has all rights except deleting the event and owning or
  // transferring its ownership.
  ADMIN(ADMIN_PERMISSIONS),
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
   * @return true if this can kick the [com.android.festivar.model.user.User] (needs to dominate
   *   their [Role] and have the according [Permission]), false otherwise.
   */
  fun canKick(that: Role): Boolean {
    return permissions.contains(CAN_KICK_MEMBER) && dominates(that)
  }
}

/** The permissions for the VISITOR [Role]. */
private val VISITOR_PERMISSIONS = setOf(CAN_SEE_ANNOUNCEMENT)

/** The permissions for the STAFF [Role]. */
private val STAFF_PERMISSIONS =
    VISITOR_PERMISSIONS +
        setOf(
            CAN_SEE_TASK,
            CAN_EDIT_OWN_TASK,
            CAN_DELETE_OWN_TASK,
            CAN_CREATE_TASK,
            CAN_TAKE_TASK,
            CAN_MARK_TASK_DONE,
            CAN_SEE_MEMBERS,
        )

/** The permissions for the TEAM_LEAD [Role]. */
private val TEAM_LEAD_PERMISSIONS =
    STAFF_PERMISSIONS +
        setOf(
            CAN_EDIT_TASK,
            CAN_DELETE_TASK,
            CAN_ASSIGN_TASK,
            CAN_EDIT_OWN_ANNOUNCEMENT,
            CAN_DELETE_OWN_ANNOUNCEMENT,
            CAN_CREATE_ANNOUNCEMENT,
        )

/** The permissions for the STAFF_MANAGER [Role]. */
private val STAFF_MANAGER_PERMISSIONS =
    TEAM_LEAD_PERMISSIONS +
        setOf(
            CAN_EDIT_ANNOUNCEMENT,
            CAN_DELETE_ANNOUNCEMENT,
            CAN_EDIT_EVENT,
            CAN_MANAGE_ROLES,
        )

/** The permissions for the ADMIN [Role]. */
private val ADMIN_PERMISSIONS = STAFF_MANAGER_PERMISSIONS + setOf(CAN_KICK_MEMBER)
