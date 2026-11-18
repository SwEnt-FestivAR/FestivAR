// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the checks of EventPermissionHandler.kt on one event, owned by [owner], with one member for
 * each other [Role].
 */
class EventPermissionHandlerTest {

  private val notMemberMessage = "This user is not a member of the event."
  private val taskOfAnotherEventMessage = "This task is linked to a different event."
  private val announcementNotInEventMessage = "This announcement is not in the event."

  private val start = ZonedDateTime.of(2026, 7, 1, 14, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private val owner = User("owner")
  private val admin = User("admin")
  private val manager = User("manager")
  private val lead = User("lead")
  private val staff = User("staff")
  private val visitor = User("visitor")
  private val outsider = User("outsider")

  /** A task of the event, owned by [ownerId] and assigned to [assignees]. */
  private fun task(ownerId: String, assignees: List<User> = emptyList()): Task =
      Task(taskId = "t1", eventId = "e1", title = "Task", ownerId = ownerId, assignees = assignees)

  /** An announcement written by [authorId], stored in [event]. */
  private fun announcement(authorId: String): Announcement =
      Announcement(
          announcementId = "a-$authorId",
          title = "News",
          authorId = authorId,
          updatedAt = start,
      )

  private val event =
      Event(
          eventId = "e1",
          ownerId = owner.uid,
          title = "Festival",
          members = listOf(owner, admin, manager, lead, staff, visitor),
          roles =
              mapOf(
                  owner.uid to Role.OWNER,
                  admin.uid to Role.ADMIN,
                  manager.uid to Role.STAFF_MANAGER,
                  lead.uid to Role.TEAM_LEAD,
                  staff.uid to Role.STAFF,
                  visitor.uid to Role.VISITOR,
              ),
          startDate = start,
          endDate = start.plusDays(1),
          announcements = listOf(announcement(lead.uid), announcement(staff.uid)),
      )

  /** Checks that [block] throws an [IllegalArgumentException] with [expectedMessage]. */
  private fun assertInvalid(expectedMessage: String, block: () -> Unit) {
    val exception = assertThrows(IllegalArgumentException::class.java) { block() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------- //
  // Tasks
  // ---------------------------------------------------------------------------------------- //

  /**
   * Editing and deleting a task follows the permissions on every task: a team lead can, a staff
   * member cannot, even on a task they own. No current role has the `_OWN_` task permissions
   * without the ones on every task, so owning a task never adds a right on its own.
   */
  @Test
  fun canEditAndDeleteTask_followThePermissionsOnEveryTask() {
    val othersTask = task(ownerId = owner.uid)
    val staffTask = task(ownerId = staff.uid)

    assertTrue(event.canEditTask(lead.uid, othersTask))
    assertTrue(event.canDeleteTask(lead.uid, othersTask))
    assertFalse(event.canEditTask(staff.uid, staffTask))
    assertFalse(event.canDeleteTask(staff.uid, staffTask))
    assertFalse(event.canEditTask(visitor.uid, othersTask))
  }

  /** A member can mark a task as done only when assigned to it and allowed by their role. */
  @Test
  fun canMarkTaskDone_needsToBeAssignedAndTheRolePermission() {
    assertTrue(event.canMarkTaskDone(staff.uid, task(owner.uid, assignees = listOf(staff))))
    assertFalse(event.canMarkTaskDone(staff.uid, task(owner.uid, assignees = listOf(lead))))
    assertFalse(event.canMarkTaskDone(visitor.uid, task(owner.uid, assignees = listOf(visitor))))
  }

  /** Every task check refuses a task of another event and a user who is not a member. */
  @Test
  fun taskChecks_fail_forATaskOfAnotherEventOrANonMember() {
    val otherEventTask = task(ownerId = lead.uid).copy(eventId = "e2")
    val checks: List<(String, Task) -> Boolean> =
        listOf(event::canEditTask, event::canDeleteTask, event::canMarkTaskDone)

    for (check in checks) {
      assertInvalid(taskOfAnotherEventMessage) { check(lead.uid, otherEventTask) }
      assertInvalid(notMemberMessage) { check(outsider.uid, task(ownerId = lead.uid)) }
    }
  }

  /**
   * A member assigns a task only with the permission and to a member strictly below them: not to
   * themselves, an equal or a higher role.
   */
  @Test
  fun canAssignTask_needsThePermissionAndToDominateTheAssignee() {
    assertTrue(event.canAssignTask(lead.uid, staff.uid))
    assertTrue(event.canAssignTask(owner.uid, admin.uid))
    assertFalse(event.canAssignTask(lead.uid, lead.uid))
    assertFalse(event.canAssignTask(lead.uid, manager.uid))
    assertFalse(event.canAssignTask(staff.uid, visitor.uid))
    assertInvalid(notMemberMessage) { event.canAssignTask(lead.uid, outsider.uid) }
  }

  // ---------------------------------------------------------------------------------------- //
  // Announcements
  // ---------------------------------------------------------------------------------------- //

  /**
   * A team lead edits and deletes only the announcements they wrote, a staff manager every one, and
   * a staff member none, even their own.
   */
  @Test
  fun canEditAndDeleteAnnouncement_followTheOwnAndTheAnyPermissions() {
    val leadNews = announcement(lead.uid)
    val staffNews = announcement(staff.uid)

    assertTrue(event.canEditAnnouncement(lead.uid, leadNews))
    assertTrue(event.canDeleteAnnouncement(lead.uid, leadNews))
    assertFalse(event.canEditAnnouncement(lead.uid, staffNews))
    assertFalse(event.canDeleteAnnouncement(lead.uid, staffNews))
    assertTrue(event.canEditAnnouncement(manager.uid, staffNews))
    assertTrue(event.canDeleteAnnouncement(manager.uid, leadNews))
    assertFalse(event.canEditAnnouncement(staff.uid, staffNews))
  }

  /** Every announcement check refuses one that is not in the event and a non-member. */
  @Test
  fun announcementChecks_fail_forAnAnnouncementNotInTheEventOrANonMember() {
    val checks: List<(String, Announcement) -> Boolean> =
        listOf(event::canEditAnnouncement, event::canDeleteAnnouncement)

    for (check in checks) {
      assertInvalid(announcementNotInEventMessage) { check(lead.uid, announcement("someone")) }
      assertInvalid(notMemberMessage) { check(outsider.uid, announcement(lead.uid)) }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // Members
  // ---------------------------------------------------------------------------------------- //

  /**
   * A role can be changed only with the permission, by a member strictly above both the current and
   * the new role.
   */
  @Test
  fun canSetRole_needsThePermissionAndToDominateBothRoles() {
    assertTrue(event.canSetRole(manager.uid, visitor.uid, Role.TEAM_LEAD))
    assertFalse(event.canSetRole(manager.uid, visitor.uid, Role.STAFF_MANAGER))
    assertFalse(event.canSetRole(manager.uid, admin.uid, Role.VISITOR))
    assertFalse(event.canSetRole(lead.uid, visitor.uid, Role.STAFF))
    assertInvalid(notMemberMessage) { event.canSetRole(manager.uid, outsider.uid, Role.STAFF) }
  }

  /** A member can be kicked only with the permission, by a member strictly above them. */
  @Test
  fun canKick_needsThePermissionAndToDominateTheMember() {
    assertTrue(event.canKick(admin.uid, manager.uid))
    assertTrue(event.canKick(owner.uid, admin.uid))
    assertFalse(event.canKick(admin.uid, admin.uid))
    assertFalse(event.canKick(admin.uid, owner.uid))
    assertFalse(event.canKick(manager.uid, visitor.uid))
    assertInvalid(notMemberMessage) { event.canKick(outsider.uid, visitor.uid) }
  }
}
