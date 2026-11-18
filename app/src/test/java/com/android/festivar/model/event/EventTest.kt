// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.role.Permission
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class EventTest {

  // Messages of the `init` block of Event, in the order in which the rules are checked.
  private val eventIdMessage = "The eventId cannot be empty."
  private val ownerIdMessage = "The ownerId cannot be empty."
  private val titleMessage = "The title cannot be empty."
  private val duplicateTaskMessage = "A task is duplicated."
  private val duplicateMemberMessage = "A user is registered twice."
  private val ownerNotMemberMessage = "The owner must be a member."
  private val dateMessage = "endDate must be strictly after startDate."
  private val duplicateAnnouncementMessage = "An announcement is duplicated."
  // Also the message of addAnnouncement on a full event.
  private val tooManyAnnouncementsMessage =
      "An event cannot have more than $MAX_ANNOUNCEMENTS_PER_EVENT announcements."

  // Messages of the functions of Event.
  private val alreadyMemberMessage = "This user is already registered in the event."
  private val notMemberMessage = "This user is not a member of the event."
  private val taskAlreadyInEventMessage = "This task is already in the event."
  private val taskOfAnotherEventMessage = "This task is linked to a different event."
  private val taskNotInEventMessage = "This task is not in the event."
  private val announcementAlreadyInEventMessage = "This announcement is already in the event."
  private val announcementNotInEventMessage = "This announcement is not in the event."

  // Messages of the rules on the roles and of the functions that change them.
  private val rolesMessage =
      "Every member must have exactly one role, and only members can have one."
  private val ownerRoleMessage = "The owner must have the OWNER role."
  private val onlyOwnerMessage = "Only the owner can have the OWNER role."
  private val setRoleMessage = "This member cannot give this role to this user."
  private val kickMessage = "This member cannot kick this user."
  private val transferMessage = "This member cannot transfer the ownership of the event."
  private val alreadyOwnerMessage = "This member already owns the event."

  private val start = ZonedDateTime.of(2026, 7, 1, 14, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private val alice = User("alice")
  private val bob = User("bob")
  private val outsider = User("outsider")
  private val carla = User("carla")

  /** Builds a valid task of the event "e1" with the given identifier. */
  private fun task(taskId: String): Task =
      Task(ownerId = "owner-1", taskId = taskId, eventId = "e1", title = "Task")

  /** Builds an event with valid default values, so that only the given fields matter. */
  private fun event(
      eventId: String = "e1",
      ownerId: String = "owner-1",
      title: String = "Festival",
      members: List<User> = listOf(User("owner-1")),
      tasks: List<Task> = emptyList(),
      startDate: ZonedDateTime = start,
      endDate: ZonedDateTime = start.plusDays(2),
  ): Event =
      Event(
          eventId = eventId,
          ownerId = ownerId,
          title = title,
          members = members,
          tasks = tasks,
          startDate = startDate,
          endDate = endDate,
      )

  /** Builds a valid announcement with the given identifier. */
  private fun announcement(announcementId: String, title: String = "Gates open"): Announcement =
      Announcement(
          announcementId = announcementId,
          title = title,
          authorId = "alice",
          updatedAt = start,
      )

  /** Builds [count] valid announcements, with the identifiers "a1" to "a[count]". */
  private fun announcements(count: Int): List<Announcement> =
      (1..count).map { i -> announcement("a$i") }

  /** Builds a valid event with every field set, so we can check that the functions keep them. */
  private fun eventWith(
      members: List<User> = listOf(alice),
      ownerId: String = members.firstOrNull()?.uid ?: "owner-1",
      tasks: List<Task> = emptyList(),
      announcements: List<Announcement> = emptyList(),
  ): Event =
      Event(
          eventId = "e1",
          ownerId = ownerId,
          title = "Festival",
          description = "Description",
          members = members,
          startDate = start,
          endDate = start.plusDays(2),
          location = "EPFL",
          tasks = tasks,
          announcements = announcements,
      )

  /** Checks that [block] throws an [IllegalArgumentException] with [expectedMessage]. */
  private fun assertInvalid(expectedMessage: String, block: () -> Unit) {
    val exception = assertThrows(IllegalArgumentException::class.java) { block() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------- //
  // Valid events
  // ---------------------------------------------------------------------------------------- //

  /** An event with only the required fields is valid and gets the default values. */
  @Test
  fun init_succeeds_withOnlyRequiredFields() {
    val event =
        Event(
            eventId = "e1",
            ownerId = "owner-1",
            title = "Festival",
            members = listOf(User("owner-1")),
            startDate = start,
            endDate = start.plusDays(2),
        )

    assertEquals("e1", event.eventId)
    assertEquals("Festival", event.title)
    assertEquals("", event.description)
    assertEquals(listOf(User("owner-1")), event.members)
    assertEquals(start, event.startDate)
    assertEquals(start.plusDays(2), event.endDate)
    assertEquals("", event.location)
    assertTrue(event.tasks.isEmpty())
    assertTrue(event.announcements.isEmpty())
    assertFalse(event.closed)
  }

  /** An event with every field set to a valid value is valid. */
  @Test
  fun init_succeeds_withAllFieldsValid() {
    val event = eventWith(members = listOf(alice, bob), tasks = listOf(task("t1"), task("t2")))

    assertEquals(listOf(alice, bob), event.members)
    assertEquals(listOf(task("t1"), task("t2")), event.tasks)
  }

  // ---------------------------------------------------------------------------------------- //
  // init rules
  // ---------------------------------------------------------------------------------------- //

  /** A blank eventId is refused. */
  @Test
  fun init_fails_whenEventIdIsBlank() {
    assertInvalid(eventIdMessage) { event(eventId = "") }
    assertInvalid(eventIdMessage) { event(eventId = " \t\n") }
  }

  /** Breaks if [Event] accepts a blank `ownerId`. */
  @Test
  fun init_fails_whenOwnerIdIsBlank() {
    assertInvalid(ownerIdMessage) { event(ownerId = "") }
    assertInvalid(ownerIdMessage) { event(ownerId = " \t\n") }
  }

  /** A blank title is refused. */
  @Test
  fun init_fails_whenTitleIsBlank() {
    assertInvalid(titleMessage) { event(title = "") }
    assertInvalid(titleMessage) { event(title = " \t\n") }
  }

  /** Two tasks with the same taskId are refused, even if their other fields are different. */
  @Test
  fun init_fails_whenATaskIdIsDuplicated() {
    assertInvalid(duplicateTaskMessage) {
      event(tasks = listOf(task("t1"), task("t1").copy(title = "Other")))
    }
  }

  /** The same user registered twice is refused, even as two different objects. */
  @Test
  fun init_fails_whenAUserIsRegisteredTwice() {
    assertInvalid(duplicateMemberMessage) { event(members = listOf(User("same"), User("same"))) }
  }

  /** An endDate strictly after startDate is accepted, even by a single nanosecond. */
  @Test
  fun init_succeeds_whenEndDateIsAfterStartDate() {
    event(startDate = start, endDate = start.plusDays(1))
    event(startDate = start, endDate = start.plusNanos(1))
  }

  /** An endDate at the same moment as startDate is refused. */
  @Test
  fun init_fails_whenEndDateIsAtTheSameMomentAsStartDate() {
    assertInvalid(dateMessage) { event(startDate = start, endDate = start) }
  }

  /** An endDate at the same instant in another time zone is also refused. */
  @Test
  fun init_fails_whenEndDateIsTheSameInstantInAnotherTimeZone() {
    val sameInstantInLondon = start.withZoneSameInstant(ZoneId.of("Europe/London"))

    assertInvalid(dateMessage) { event(startDate = start, endDate = sameInstantInLondon) }
  }

  /** An endDate before startDate is refused. */
  @Test
  fun init_fails_whenEndDateIsBeforeStartDate() {
    assertInvalid(dateMessage) { event(startDate = start, endDate = start.minusDays(1)) }
  }

  /** copy() goes through the init block, so it cannot create an invalid event. */
  @Test
  fun copy_fails_whenTheCopyBreaksARule() {
    val event = eventWith()

    assertInvalid(eventIdMessage) { event.copy(eventId = "") }
    assertInvalid(titleMessage) { event.copy(title = " ") }
    assertInvalid(duplicateTaskMessage) { event.copy(tasks = listOf(task("t1"), task("t1"))) }
    assertInvalid(duplicateMemberMessage) { event.copy(members = listOf(alice, alice)) }
    assertInvalid(ownerNotMemberMessage) { event.copy(ownerId = outsider.uid) }
    assertInvalid(dateMessage) { event.copy(endDate = start) }
  }

  // **************************************************************************************//
  // close, addMember, removeMember, addTask and removeTask
  // **************************************************************************************//

  /** close returns a closed copy, keeps every other field and does not change the original. */
  @Test
  fun close_returnsClosedCopy_andKeepsOtherFields() {
    val event = eventWith(members = listOf(alice), tasks = listOf(task("t1")))

    val closedEvent = event.close()

    assertTrue(closedEvent.closed)
    assertEquals(event.copy(closed = true), closedEvent)
    assertFalse(event.closed)
  }

  /**
   * Breaks if addMember does not add the user at the end with DEFAULT_ROLE, changes another field
   * or changes the original.
   */
  @Test
  fun addMember_addsUserAtTheEnd() {
    val event = eventWith(members = listOf(alice))

    val result = event.addMember(bob)

    assertEquals(
        event.copy(members = listOf(alice, bob), roles = event.roles + (bob.uid to DEFAULT_ROLE)),
        result,
    )
    assertEquals(listOf(alice), event.members)
  }

  /** addMember refuses a user that is already a member, even as another equal object. */
  @Test
  fun addMember_fails_whenUserIsAlreadyMember() {
    val event = eventWith(members = listOf(User("same")))

    assertInvalid(alreadyMemberMessage) { event.addMember(User("same")) }
  }

  /** Breaks if removeMember removes another user or their role, or changes the original. */
  @Test
  fun removeMember_removesOnlyTheGivenUser() {
    val event = eventWith(members = listOf(alice, bob), ownerId = bob.uid)

    val result = event.removeMember(alice.uid)

    assertEquals(event.copy(members = listOf(bob), roles = event.roles - alice.uid), result)
    assertEquals(listOf(alice, bob), event.members)
  }

  /** Breaks if `removeMember` lets the event owner be removed. */
  @Test
  fun removeMember_fails_whenUserIsOwner() {
    val event = eventWith(members = listOf(alice, bob), ownerId = alice.uid)

    assertInvalid("The event owner cannot be removed.") { event.removeMember(alice.uid) }
  }

  /** removeMember refuses an id that is not the one of a member. */
  @Test
  fun removeMember_fails_whenUserIsNotMember() {
    val event = eventWith(members = listOf(alice))

    assertInvalid(notMemberMessage) { event.removeMember(outsider.uid) }
  }

  // Members are identified by their uid, whatever their name or surname.

  /** A member registered twice with the same uid is refused, even with other names. */
  @Test
  fun init_fails_whenAUidIsRegisteredTwice_withOtherNames() {
    assertInvalid(duplicateMemberMessage) {
      event(members = listOf(User("u1", name = "Sara", surname = "Keller"), User("u1")))
    }
  }

  /** addMember refuses a user whose uid is already a member, even with other names. */
  @Test
  fun addMember_fails_whenAMemberHasTheSameUid() {
    val event = eventWith(members = listOf(User("u1", name = "Sara", surname = "Keller")))

    assertInvalid(alreadyMemberMessage) { event.addMember(User("u1")) }
  }

  /** Breaks if removeMember misses the member or role with the given uid because of their names. */
  @Test
  fun removeMember_removesTheMemberWithTheSameUid() {
    val sara = User("u1", name = "Sara", surname = "Keller")
    val event = eventWith(members = listOf(alice, sara, bob))

    val result = event.removeMember("u1")

    assertEquals(event.copy(members = listOf(alice, bob), roles = event.roles - "u1"), result)
    assertEquals(listOf(alice, sara, bob), event.members)
  }

  /** removeMember identifies a member by its uid only: the name of a member is refused. */
  @Test
  fun removeMember_fails_whenGivenTheNameOfAMember() {
    val event = eventWith(members = listOf(User("u1", name = "Sara", surname = "Keller")))

    assertInvalid(notMemberMessage) { event.removeMember("Sara") }
  }

  /** addTask adds the task at the end, keeps every other field and does not change the original. */
  @Test
  fun addTask_addsTaskAtTheEnd() {
    val event = eventWith(tasks = listOf(task("t1")))

    val result = event.addTask(task("t2"))

    assertEquals(event.copy(tasks = listOf(task("t1"), task("t2"))), result)
    assertEquals(listOf(task("t1")), event.tasks)
  }

  /** addTask refuses a task whose taskId is already in the event, even if its fields differ. */
  @Test
  fun addTask_fails_whenTaskIdIsAlreadyInEvent() {
    val event = eventWith(tasks = listOf(task("t1")))

    assertInvalid(taskAlreadyInEventMessage) { event.addTask(task("t1").copy(title = "Other")) }
  }

  /** addTask refuses a task whose eventId is not the event's own. */
  @Test
  fun addTask_fails_whenTaskBelongsToAnotherEvent() {
    val event = eventWith(tasks = listOf(task("t1")))

    assertInvalid(taskOfAnotherEventMessage) { event.addTask(task("t2").copy(eventId = "e2")) }
  }

  /** removeTask removes only the task with the given taskId and does not change the original. */
  @Test
  fun removeTask_removesOnlyTheGivenTask() {
    val event = eventWith(tasks = listOf(task("t1"), task("t2")))

    val result = event.removeTask("t1")

    assertEquals(event.copy(tasks = listOf(task("t2"))), result)
    assertEquals(listOf(task("t1"), task("t2")), event.tasks)
  }

  /** removeTask refuses a taskId that is not in the event. */
  @Test
  fun removeTask_fails_whenTaskIsNotInEvent() {
    val event = eventWith(tasks = listOf(task("t1")))

    assertInvalid(taskNotInEventMessage) { event.removeTask("unknown") }
  }

  // **************************************************************************************//
  // announcements
  // **************************************************************************************//

  /** Two announcements with the same id are refused, even if their other fields differ. */
  @Test
  fun init_fails_whenAnAnnouncementIdIsDuplicated() {
    assertInvalid(duplicateAnnouncementMessage) {
      eventWith(announcements = listOf(announcement("a1"), announcement("a1", title = "Other")))
    }
  }

  /** An event can hold exactly the maximum number of announcements. */
  @Test
  fun init_succeeds_withTheMaximumNumberOfAnnouncements() {
    val event = eventWith(announcements = announcements(MAX_ANNOUNCEMENTS_PER_EVENT))

    assertEquals(announcements(MAX_ANNOUNCEMENTS_PER_EVENT), event.announcements)
  }

  /** An event with one announcement more than the maximum is refused. */
  @Test
  fun init_fails_whenThereAreTooManyAnnouncements() {
    assertInvalid(tooManyAnnouncementsMessage) {
      eventWith(announcements = announcements(MAX_ANNOUNCEMENTS_PER_EVENT + 1))
    }
  }

  /** addAnnouncement can fill the last free place of an event. */
  @Test
  fun addAnnouncement_succeeds_upToTheMaximum() {
    val event = eventWith(announcements = announcements(MAX_ANNOUNCEMENTS_PER_EVENT - 1))

    val result = event.addAnnouncement(announcement("a$MAX_ANNOUNCEMENTS_PER_EVENT"))

    assertEquals(announcements(MAX_ANNOUNCEMENTS_PER_EVENT), result.announcements)
  }

  /** addAnnouncement refuses a new announcement once the event holds the maximum. */
  @Test
  fun addAnnouncement_fails_whenEventIsFull() {
    val event = eventWith(announcements = announcements(MAX_ANNOUNCEMENTS_PER_EVENT))

    assertInvalid(tooManyAnnouncementsMessage) {
      event.addAnnouncement(announcement("a${MAX_ANNOUNCEMENTS_PER_EVENT + 1}"))
    }
  }

  /** addAnnouncement adds it at the end and keeps every other field. */
  @Test
  fun addAnnouncement_addsItAtTheEnd() {
    val event = eventWith(announcements = listOf(announcement("a1")))

    val result = event.addAnnouncement(announcement("a2"))

    assertEquals(event.copy(announcements = listOf(announcement("a1"), announcement("a2"))), result)
  }

  /** addAnnouncement refuses an id that is already in the event, even if its fields differ. */
  @Test
  fun addAnnouncement_fails_whenItsIdIsAlreadyInEvent() {
    val event = eventWith(announcements = listOf(announcement("a1")))

    assertInvalid(announcementAlreadyInEventMessage) {
      event.addAnnouncement(announcement("a1", title = "Other"))
    }
  }

  /** editAnnouncement replaces the one with the same id in place. */
  @Test
  fun editAnnouncement_replacesTheOneWithTheSameId() {
    val event =
        eventWith(
            announcements = listOf(announcement("a1"), announcement("a2"), announcement("a3"))
        )
    val edited = announcement("a2", title = "Stage B is closed").togglePin()

    val result = event.editAnnouncement(edited)

    assertEquals(
        event.copy(announcements = listOf(announcement("a1"), edited, announcement("a3"))),
        result,
    )
  }

  /** editAnnouncement refuses an announcement whose id is not in the event. */
  @Test
  fun editAnnouncement_fails_whenItsIdIsNotInEvent() {
    val event = eventWith(announcements = listOf(announcement("a1")))

    assertInvalid(announcementNotInEventMessage) { event.editAnnouncement(announcement("a2")) }
  }

  /** removeAnnouncement removes only the one with the given id. */
  @Test
  fun removeAnnouncement_removesOnlyTheGivenOne() {
    val event = eventWith(announcements = listOf(announcement("a1"), announcement("a2")))

    val result = event.removeAnnouncement("a1")

    assertEquals(event.copy(announcements = listOf(announcement("a2"))), result)
  }

  /** removeAnnouncement refuses an id that is not in the event. */
  @Test
  fun removeAnnouncement_fails_whenItIsNotInEvent() {
    val event = eventWith(announcements = listOf(announcement("a1")))

    assertInvalid(announcementNotInEventMessage) { event.removeAnnouncement("unknown") }
  }

  /** announcementOrNull finds the one with the given id among several. */
  @Test
  fun announcementOrNull_returnsTheOneWithTheGivenId() {
    val event =
        eventWith(
            announcements = listOf(announcement("a1"), announcement("a2"), announcement("a3"))
        )

    assertEquals(announcement("a2"), event.announcementOrNull("a2"))
  }

  /** announcementOrNull returns null for an id that is not in the event. */
  @Test
  fun announcementOrNull_isNull_whenItIsNotInEvent() {
    val event = eventWith(announcements = listOf(announcement("a1")))

    assertNull(event.announcementOrNull("unknown"))
  }

  // **************************************************************************************//
  // isPast
  // **************************************************************************************//

  /** An event that is not closed is not past before its end, whether it has started or not. */
  @Test
  fun isPast_isFalse_beforeTheEnd() {
    val event = event(startDate = start, endDate = start.plusDays(2))

    assertFalse(event.isPast(start.minusDays(1)))
    assertFalse(event.isPast(start))
    assertFalse(event.isPast(start.plusDays(2).minusNanos(1)))
  }

  /** An event is past from its end on, including exactly at its end. */
  @Test
  fun isPast_isTrue_fromTheEnd() {
    val event = event(startDate = start, endDate = start.plusDays(2))

    assertTrue(event.isPast(start.plusDays(2)))
    assertTrue(event.isPast(start.plusDays(3)))
  }

  /** A closed event is past, even before it starts or ends. */
  @Test
  fun isPast_isTrue_whenClosed_evenBeforeTheEnd() {
    val event = event(startDate = start, endDate = start.plusDays(2)).close()

    assertTrue(event.isPast(start.minusDays(1)))
    assertTrue(event.isPast(start.plusDays(1)))
  }

  /** The end is compared as an instant, so the time zone of the given time does not matter. */
  @Test
  fun isPast_comparesInstants_acrossTimeZones() {
    val end = start.plusDays(2)
    val event = event(startDate = start, endDate = end)
    val endInLondon = end.withZoneSameInstant(ZoneId.of("Europe/London"))

    assertTrue(event.isPast(endInLondon))
    assertFalse(event.isPast(endInLondon.minusNanos(1)))
  }

  // **************************************************************************************//
  // roles
  // **************************************************************************************//

  /** An event owned by alice, with the other members of [roles] and their given [Role]s. */
  private fun eventWithRoles(vararg roles: Pair<User, Role>): Event =
      eventWith(members = listOf(alice) + roles.map { it.first })
          .copy(
              roles = mapOf(alice.uid to Role.OWNER) + roles.associate { it.first.uid to it.second }
          )

  /** Breaks if the default roles stop giving OWNER to the owner and DEFAULT_ROLE to the others. */
  @Test
  fun init_givesTheOwnerRoleToTheOwnerAndTheDefaultRoleToTheOthers_byDefault() {
    val event = eventWith(members = listOf(alice, bob))

    assertEquals(mapOf(alice.uid to Role.OWNER, bob.uid to DEFAULT_ROLE), event.roles)
    assertEquals(Role.VISITOR, DEFAULT_ROLE)
  }

  /** Breaks if an event accepts a member without a role, or a role for a non-member. */
  @Test
  fun init_fails_whenTheRolesAreNotExactlyTheMembers() {
    val event = eventWith(members = listOf(alice, bob))

    assertInvalid(rolesMessage) { event.copy(roles = mapOf(alice.uid to Role.OWNER)) }
    assertInvalid(rolesMessage) { event.copy(roles = event.roles + (outsider.uid to Role.STAFF)) }
  }

  /** Breaks if the owner can have another role than OWNER, even by changing the owner alone. */
  @Test
  fun init_fails_whenTheOwnerHasNotTheOwnerRole() {
    val event = eventWith(members = listOf(alice, bob))

    assertInvalid(ownerRoleMessage) {
      event.copy(roles = mapOf(alice.uid to Role.ADMIN, bob.uid to DEFAULT_ROLE))
    }
    assertInvalid(ownerRoleMessage) { event.copy(ownerId = bob.uid) }
  }

  /** Breaks if a second member can have the OWNER role. */
  @Test
  fun init_fails_whenAnotherMemberHasTheOwnerRole() {
    assertInvalid(onlyOwnerMessage) { eventWithRoles(bob to Role.OWNER) }
  }

  /** Breaks if roleOf returns another role than the stored one for a member, the owner included. */
  @Test
  fun roleOf_returnsTheRoleOfEachMember() {
    val event = eventWithRoles(bob to Role.STAFF, carla to Role.ADMIN)

    assertEquals(Role.OWNER, event.roleOf(alice.uid))
    assertEquals(Role.STAFF, event.roleOf(bob.uid))
    assertEquals(Role.ADMIN, event.roleOf(carla.uid))
  }

  /** Breaks if roleOf or canAct answers for a user who is not a member instead of refusing them. */
  @Test
  fun roleOfAndCanAct_fail_forANonMember() {
    val event = eventWithRoles(bob to Role.STAFF)

    assertInvalid(notMemberMessage) { event.roleOf(outsider.uid) }
    assertInvalid(notMemberMessage) { event.canAct(outsider.uid, Permission.CAN_SEE_TASK) }
  }

  /** Breaks if canAct answers something else than the permissions of the member's role. */
  @Test
  fun canAct_followsThePermissionsOfTheRole() {
    val event = eventWithRoles(bob to Role.STAFF)

    assertTrue(event.canAct(bob.uid, Permission.CAN_SEE_TASK))
    assertFalse(event.canAct(bob.uid, Permission.CAN_CREATE_TASK))
    assertTrue(event.canAct(alice.uid, Permission.CAN_DELETE_EVENT))
  }

  /** Breaks if setRole changes another role than the one of the given member, or the original. */
  @Test
  fun setRole_changesOnlyTheRoleOfTheMember() {
    val event = eventWithRoles(bob to Role.VISITOR, carla to Role.STAFF)

    val result = event.setRole(alice.uid, bob.uid, Role.TEAM_LEAD)

    assertEquals(event.copy(roles = event.roles + (bob.uid to Role.TEAM_LEAD)), result)
    assertEquals(Role.VISITOR, event.roleOf(bob.uid))
  }

  /**
   * Breaks if setRole accepts a member without the permission, a role or a member they do not
   * dominate, a change of their own role or of the owner's role, or giving the OWNER role.
   */
  @Test
  fun setRole_fails_whenTheMemberIsNotAllowed() {
    val event =
        eventWithRoles(bob to Role.STAFF_MANAGER, carla to Role.VISITOR, outsider to Role.ADMIN)

    // carla, a visitor, has no right to manage the roles.
    assertInvalid(setRoleMessage) { event.setRole(carla.uid, bob.uid, Role.VISITOR) }
    // bob does not dominate the new role, nor the current role of an admin.
    assertInvalid(setRoleMessage) { event.setRole(bob.uid, carla.uid, Role.STAFF_MANAGER) }
    assertInvalid(setRoleMessage) { event.setRole(bob.uid, outsider.uid, Role.VISITOR) }
    // Nobody changes their own role, the role of the owner or gives the OWNER role.
    assertInvalid(setRoleMessage) { event.setRole(bob.uid, bob.uid, Role.STAFF) }
    assertInvalid(setRoleMessage) { event.setRole(outsider.uid, alice.uid, Role.VISITOR) }
    assertInvalid(setRoleMessage) { event.setRole(alice.uid, carla.uid, Role.OWNER) }
  }

  /** Breaks if setRole accepts an actor or a target who is not a member of the event. */
  @Test
  fun setRole_fails_forANonMember() {
    val event = eventWithRoles(bob to Role.STAFF)

    assertInvalid(notMemberMessage) { event.setRole(outsider.uid, bob.uid, Role.VISITOR) }
    assertInvalid(notMemberMessage) { event.setRole(alice.uid, outsider.uid, Role.VISITOR) }
  }

  /** Breaks if kickMember does not remove the member and their role exactly like removeMember. */
  @Test
  fun kickMember_removesTheMemberAndTheirRole() {
    val event = eventWithRoles(bob to Role.ADMIN, carla to Role.STAFF)

    assertEquals(event.removeMember(carla.uid), event.kickMember(bob.uid, carla.uid))
    assertEquals(event.removeMember(bob.uid), event.kickMember(alice.uid, bob.uid))
  }

  /**
   * Breaks if kickMember accepts a member without the permission, or kicking a member they do not
   * strictly dominate, themselves or the owner.
   */
  @Test
  fun kickMember_fails_whenTheMemberIsNotAllowed() {
    val dave = User("dave")
    val event =
        eventWithRoles(
            bob to Role.ADMIN,
            carla to Role.STAFF_MANAGER,
            outsider to Role.ADMIN,
            dave to Role.VISITOR,
        )

    // carla dominates dave, but can only manage the roles, not kick.
    assertInvalid(kickMessage) { event.kickMember(carla.uid, dave.uid) }
    assertInvalid(kickMessage) { event.kickMember(bob.uid, outsider.uid) }
    assertInvalid(kickMessage) { event.kickMember(bob.uid, bob.uid) }
    assertInvalid(kickMessage) { event.kickMember(bob.uid, alice.uid) }
  }

  /** Breaks if transferOwnership does more than making the new owner OWNER and the old ADMIN. */
  @Test
  fun transferOwnership_swapsTheOwnerAndMakesThePreviousOneAdmin() {
    val event = eventWithRoles(bob to Role.STAFF, carla to Role.VISITOR)

    val result = event.transferOwnership(alice.uid, bob.uid)

    assertEquals(
        event.copy(
            ownerId = bob.uid,
            roles =
                mapOf(alice.uid to Role.ADMIN, bob.uid to Role.OWNER, carla.uid to Role.VISITOR),
        ),
        result,
    )
  }

  /**
   * Breaks if transferOwnership accepts anyone but the owner, a transfer to the owner themselves or
   * a new owner who is not a member.
   */
  @Test
  fun transferOwnership_fails_whenNotAllowed() {
    val event = eventWithRoles(bob to Role.ADMIN)

    assertInvalid(transferMessage) { event.transferOwnership(bob.uid, bob.uid) }
    assertInvalid(alreadyOwnerMessage) { event.transferOwnership(alice.uid, alice.uid) }
    assertInvalid(notMemberMessage) { event.transferOwnership(alice.uid, outsider.uid) }
    assertInvalid(notMemberMessage) { event.transferOwnership(outsider.uid, bob.uid) }
  }
}
