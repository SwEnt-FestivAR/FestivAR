package com.android.festivar.model.event

import com.android.festivar.model.task.Task
import com.android.festivar.model.temporary.User
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class EventTest {

  // Messages of the `init` block of Event, in the order in which the rules are checked.
  private val eventIdMessage = "The eventId cannot be empty."
  private val titleMessage = "The title cannot be empty."
  private val duplicateTaskMessage = "A task is duplicated."
  private val duplicateMemberMessage = "A user is registered twice."
  private val dateMessage = "endDate must not be before startDate."

  // Messages of the functions of Event.
  private val alreadyMemberMessage = "This user is already registered in the event."
  private val notMemberMessage = "This user is not a member of the event."
  private val taskAlreadyInEventMessage = "This task is already in the event."
  private val taskNotInEventMessage = "This task is not in the event."

  private val start = LocalDate.of(2026, 7, 1)

  private val alice = User("alice")
  private val bob = User("bob")
  private val outsider = User("outsider")

  /** Builds a valid task of the event "e1" with the given identifier. */
  private fun task(taskId: String): Task = Task(taskId = taskId, eventId = "e1", title = "Task")

  /** Builds a valid event with every field set, so we can check that the functions keep them. */
  private fun eventWith(members: List<User> = emptyList(), tasks: List<Task> = emptyList()): Event =
      Event(
          eventId = "e1",
          title = "Festival",
          description = "Description",
          members = members,
          startDate = start,
          endDate = start.plusDays(2),
          location = "EPFL",
          tasks = tasks,
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
    val event = Event(eventId = "e1", title = "Festival")

    assertEquals("e1", event.eventId)
    assertEquals("Festival", event.title)
    assertEquals("", event.description)
    assertTrue(event.members.isEmpty())
    assertNull(event.startDate)
    assertNull(event.endDate)
    assertEquals("", event.location)
    assertTrue(event.tasks.isEmpty())
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
    assertInvalid(eventIdMessage) { Event(eventId = "", title = "Festival") }
    assertInvalid(eventIdMessage) { Event(eventId = " \t\n", title = "Festival") }
  }

  /** A blank title is refused. */
  @Test
  fun init_fails_whenTitleIsBlank() {
    assertInvalid(titleMessage) { Event(eventId = "e1", title = "") }
    assertInvalid(titleMessage) { Event(eventId = "e1", title = " \t\n") }
  }

  /** Two tasks with the same taskId are refused, even if their other fields are different. */
  @Test
  fun init_fails_whenATaskIdIsDuplicated() {
    assertInvalid(duplicateTaskMessage) {
      Event(eventId = "e1", title = "Festival", tasks = listOf(task("t1"), task("t1").copy(title = "Other")))
    }
  }

  /** The same user registered twice is refused, even as two different objects. */
  @Test
  fun init_fails_whenAUserIsRegisteredTwice() {
    assertInvalid(duplicateMemberMessage) {
      Event(eventId = "e1", title = "Festival", members = listOf(User("same"), User("same")))
    }
  }

  /** startDate and endDate can each be null. */
  @Test
  fun init_succeeds_whenStartDateOrEndDateIsNull() {
    Event(eventId = "e1", title = "Festival", startDate = start, endDate = null)
    Event(eventId = "e1", title = "Festival", startDate = null, endDate = start)
    Event(eventId = "e1", title = "Festival", startDate = null, endDate = null)
  }

  /** An endDate equal to (one-day event) or after startDate is accepted. */
  @Test
  fun init_succeeds_whenEndDateIsNotBeforeStartDate() {
    Event(eventId = "e1", title = "Festival", startDate = start, endDate = start)
    Event(eventId = "e1", title = "Festival", startDate = start, endDate = start.plusDays(1))
  }

  /** An endDate before startDate is refused. */
  @Test
  fun init_fails_whenEndDateIsBeforeStartDate() {
    assertInvalid(dateMessage) {
      Event(eventId = "e1", title = "Festival", startDate = start, endDate = start.minusDays(1))
    }
  }

  /** copy() goes through the init block, so it cannot create an invalid event. */
  @Test
  fun copy_fails_whenTheCopyBreaksARule() {
    val event = eventWith()

    assertInvalid(eventIdMessage) { event.copy(eventId = "") }
    assertInvalid(titleMessage) { event.copy(title = " ") }
    assertInvalid(duplicateTaskMessage) { event.copy(tasks = listOf(task("t1"), task("t1"))) }
    assertInvalid(duplicateMemberMessage) { event.copy(members = listOf(alice, alice)) }
    assertInvalid(dateMessage) { event.copy(endDate = start.minusDays(1)) }
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

  /** addMember adds the user at the end, keeps every other field and does not change the original. */
  @Test
  fun addMember_addsUserAtTheEnd() {
    val event = eventWith(members = listOf(alice))

    val result = event.addMember(bob)

    assertEquals(event.copy(members = listOf(alice, bob)), result)
    assertEquals(listOf(alice), event.members)
  }

  /** addMember refuses a user that is already a member, even as another equal object. */
  @Test
  fun addMember_fails_whenUserIsAlreadyMember() {
    val event = eventWith(members = listOf(User("same")))

    assertInvalid(alreadyMemberMessage) { event.addMember(User("same")) }
  }

  /** removeMember removes only the given user and does not change the original. */
  @Test
  fun removeMember_removesOnlyTheGivenUser() {
    val event = eventWith(members = listOf(alice, bob))

    val result = event.removeMember(alice)

    assertEquals(event.copy(members = listOf(bob)), result)
    assertEquals(listOf(alice, bob), event.members)
  }

  /** removeMember refuses a user that is not a member. */
  @Test
  fun removeMember_fails_whenUserIsNotMember() {
    val event = eventWith(members = listOf(alice))

    assertInvalid(notMemberMessage) { event.removeMember(outsider) }
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
}
