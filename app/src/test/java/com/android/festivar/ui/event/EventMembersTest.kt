// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.event.role.Role
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.event.details.membersByRole
import com.android.festivar.ui.event.details.tasksOf
import org.junit.Assert.assertEquals
import org.junit.Test

class EventMembersTest {

  private val organizer = MembersFixture.organizer
  private val lea = MembersFixture.lea
  private val sara = MembersFixture.sara

  private fun eventOf(vararg members: User) = MembersFixture.eventOf(*members)

  /** Breaks if the owner is not pinned at the top when they joined after the other members. */
  @Test
  fun theOwnerComesFirstEvenWhenTheyAreListedLast() {
    assertEquals(listOf(organizer, lea, sara), eventOf(lea, sara, organizer).membersByRole())
  }

  /**
   * Breaks if members with the same role are reordered instead of keeping the order they joined.
   */
  @Test
  fun membersWithTheSameRoleKeepTheirOrder() {
    assertEquals(listOf(organizer, sara, lea), eventOf(sara, organizer, lea).membersByRole())
  }

  /** Breaks if an event with only its owner loses them or lists them twice. */
  @Test
  fun anEventWithOnlyItsOwnerListsThemOnce() {
    assertEquals(listOf(organizer), eventOf(organizer).membersByRole())
  }

  /** Breaks if the members are not listed from the highest role to the lowest. */
  @Test
  fun theMembersAreListedFromTheHighestRoleToTheLowest() {
    val event =
        eventOf(lea, sara, organizer)
            .setRole(organizer.uid, lea.uid, Role.STAFF)
            .setRole(organizer.uid, sara.uid, Role.ADMIN)

    assertEquals(listOf(organizer, sara, lea), event.membersByRole())
  }

  /** Breaks if a member's task count includes tasks they are not assigned to. */
  @Test
  fun tasksOfCountsOnlyTheTasksTheMemberIsAssignedTo() {
    val base = eventOf(organizer, lea)
    val task = { id: String, assignees: List<User> ->
      Task(
          taskId = id,
          eventId = base.eventId,
          title = id,
          ownerId = organizer.uid,
          assignees = assignees,
          maxAssign = 2,
      )
    }
    val event =
        base.copy(
            tasks =
                listOf(
                    task("t1", listOf(lea)),
                    task("t2", listOf(lea, organizer)),
                    task("t3", emptyList()),
                )
        )

    assertEquals(2, event.tasksOf(lea.uid))
    assertEquals(1, event.tasksOf(organizer.uid))
    assertEquals(0, event.tasksOf(sara.uid))
  }
}
