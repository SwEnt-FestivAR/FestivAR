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
import com.android.festivar.model.event.role.Role.ADMIN
import com.android.festivar.model.event.role.Role.OWNER
import com.android.festivar.model.event.role.Role.STAFF
import com.android.festivar.model.event.role.Role.STAFF_MANAGER
import com.android.festivar.model.event.role.Role.TEAM_LEAD
import com.android.festivar.model.event.role.Role.VISITOR
import org.junit.Assert.assertEquals
import org.junit.Test

class RoleTest {

  /** Every role, from the lowest to the highest. Each one must strictly dominate the previous. */
  private val rolesFromLowest = listOf(VISITOR, STAFF, TEAM_LEAD, STAFF_MANAGER, ADMIN, OWNER)

  /** The rank of [role] in [rolesFromLowest], written independently of [Role.dominates]. */
  private fun rank(role: Role): Int = rolesFromLowest.indexOf(role)

  // ---------------------------------------------------------------------------------------- //
  // Permissions of each role
  // ---------------------------------------------------------------------------------------- //

  /** The expected permissions of every role, written here as the specification of the roles. */
  private val expectedPermissions: Map<Role, Set<Permission>> = run {
    val visitor = setOf(CAN_SEE_ANNOUNCEMENT)
    val staff =
        visitor + setOf(CAN_SEE_TASK, CAN_EDIT_OWN_TASK, CAN_DELETE_OWN_TASK, CAN_CREATE_TASK)
    val teamLead =
        staff +
            setOf(
                CAN_EDIT_TASK,
                CAN_DELETE_TASK,
                CAN_ASSIGN_TASK,
                CAN_EDIT_OWN_ANNOUNCEMENT,
                CAN_DELETE_OWN_ANNOUNCEMENT,
                CAN_CREATE_ANNOUNCEMENT,
            )
    val staffManager =
        teamLead +
            setOf(CAN_EDIT_ANNOUNCEMENT, CAN_DELETE_ANNOUNCEMENT, CAN_EDIT_EVENT, CAN_MANAGE_ROLES)
    val admin = staffManager + CAN_KICK_MEMBER
    mapOf(
        VISITOR to visitor,
        STAFF to staff,
        TEAM_LEAD to teamLead,
        STAFF_MANAGER to staffManager,
        ADMIN to admin,
        OWNER to Permission.entries.toSet(),
    )
  }

  /** Red if a permission is added to or removed from a role, or if a role is added. */
  @Test
  fun everyRoleHasExactlyItsExpectedPermissions() {
    assertEquals(Role.entries.toSet(), expectedPermissions.keys)
    for (role in Role.entries) {
      val expected = expectedPermissions.getValue(role)

      assertEquals("Permissions of $role", expected, role.permissions)
      for (permission in Permission.entries) {
        assertEquals(
            "$role has $permission",
            permission in expected,
            role.hasPermission(permission),
        )
      }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // dominates and comparable
  // ---------------------------------------------------------------------------------------- //

  /**
   * Red if the roles stop forming a strict chain: a role dominates exactly the roles below it,
   * never itself nor a role above it.
   */
  @Test
  fun aRoleDominatesExactlyTheRolesBelowIt() {
    assertEquals(Role.entries.toSet(), rolesFromLowest.toSet())
    for (role in Role.entries) {
      for (that in Role.entries) {
        assertEquals("$role dominates $that", rank(role) > rank(that), role.dominates(that))
      }
    }
  }

  /** Every two different roles are comparable, but a role is not comparable with itself. */
  @Test
  fun everyTwoDifferentRolesAreComparable() {
    for (role in Role.entries) {
      for (that in Role.entries) {
        assertEquals("$role comparable to $that", role != that, role.comparable(that))
      }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // canManageTo and canKick
  // ---------------------------------------------------------------------------------------- //

  /**
   * A role manages a member from one role to another only if it has CAN_MANAGE_ROLES and is
   * strictly above both roles, checked for every combination of roles.
   */
  @Test
  fun canManageToNeedsThePermissionAndToBeAboveBothRoles() {
    val managers = setOf(STAFF_MANAGER, ADMIN, OWNER)
    for (manager in Role.entries) {
      for (from in Role.entries) {
        for (to in Role.entries) {
          val expected =
              manager in managers && rank(manager) > rank(from) && rank(manager) > rank(to)

          assertEquals("$manager manages $from to $to", expected, manager.canManageTo(from, to))
        }
      }
    }
  }

  /**
   * A role kicks a member only if it has CAN_KICK_MEMBER and is strictly above the member's role,
   * checked for every pair of roles.
   */
  @Test
  fun canKickNeedsThePermissionAndToBeAboveTheRole() {
    val kickers = setOf(ADMIN, OWNER)
    for (kicker in Role.entries) {
      for (that in Role.entries) {
        val expected = kicker in kickers && rank(kicker) > rank(that)

        assertEquals("$kicker kicks $that", expected, kicker.canKick(that))
      }
    }
  }
}
