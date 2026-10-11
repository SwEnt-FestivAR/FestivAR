// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserTest {

  private val uidMessage = "The uid cannot be empty."
  private val blankEventIdMessage = "A joined event id cannot be empty."
  private val duplicateEventMessage = "A joined event is duplicated."

  @Test
  fun init_succeeds_withOnlyRequiredFields() {
    val user = User("u1")

    assertEquals("u1", user.uid)
    assertEquals("", user.name)
    assertEquals("", user.surname)
    assertEquals("", user.email)
    assertTrue(user.joinedEvents.isEmpty())
    assertTrue(user.tasksAssigned.isEmpty())
  }

  @Test
  fun init_succeeds_withAllFields() {
    val user =
        User(
            "u1",
            name = "Sara",
            surname = "Keller",
            email = "sara@example.com",
            joinedEvents = listOf("e1", "e2"),
            tasksAssigned = listOf("t1", "t2"),
        )

    assertEquals("Sara", user.name)
    assertEquals("Keller", user.surname)
    assertEquals("sara@example.com", user.email)
    assertEquals(listOf("e1", "e2"), user.joinedEvents)
    assertEquals(listOf("t1", "t2"), user.tasksAssigned)
  }

  @Test
  fun usersWithTheSameUidButOtherDataAreNotEqual() {
    val sara =
        User(
            "u1",
            name = "Sara",
            surname = "Keller",
            email = "sara@example.com",
            joinedEvents = listOf("e1"),
            tasksAssigned = listOf("t1"),
        )

    assertNotEquals(sara, User("u1"))
    assertNotEquals(sara, sara.copy(name = "Sarah"))
    assertNotEquals(sara, sara.copy(surname = "Muller"))
    assertNotEquals(sara, sara.copy(email = "sarah@example.com"))
    assertNotEquals(sara, sara.copy(joinedEvents = listOf("e2")))
    assertNotEquals(sara, sara.copy(tasksAssigned = listOf("t2")))
  }

  @Test
  fun init_fails_whenUidIsBlank() {
    for (blank in listOf("", " ", "\t", "\n")) {
      assertInvalid(uidMessage) { User(blank) }
    }
  }

  @Test
  fun init_fails_whenJoinedEventIdIsBlank() {
    assertInvalid(blankEventIdMessage) { User("u1", joinedEvents = listOf("e1", " ")) }
  }

  @Test
  fun init_fails_whenJoinedEventIsDuplicated() {
    assertInvalid(duplicateEventMessage) { User("u1", joinedEvents = listOf("e1", "e1")) }
  }

  @Test
  fun init_fails_whenAssignedTaskIdIsBlank() {
    assertInvalid("An assigned task id cannot be empty.") {
      User("u1", tasksAssigned = listOf("t1", " "))
    }
  }

  @Test
  fun init_fails_whenAssignedTaskIsDuplicated() {
    assertInvalid("An assigned task is duplicated.") {
      User("u1", tasksAssigned = listOf("t1", "t1"))
    }
  }

  @Test
  fun copy_fails_whenTheCopyBreaksARule() {
    val user = User("u1", joinedEvents = listOf("e1"))

    assertInvalid(uidMessage) { user.copy(uid = "") }
    assertInvalid(blankEventIdMessage) { user.copy(joinedEvents = listOf("")) }
    assertInvalid(duplicateEventMessage) { user.copy(joinedEvents = listOf("e1", "e1")) }
    assertInvalid("An assigned task id cannot be empty.") { user.copy(tasksAssigned = listOf("")) }
    assertInvalid("An assigned task is duplicated.") {
      user.copy(tasksAssigned = listOf("t1", "t1"))
    }
  }

  @Test
  fun addJoinedEvent_returnsCopyAndPreservesOtherFields() {
    val user = User("u1", name = "Sara", surname = "Keller", joinedEvents = listOf("e1"))

    val result = user.addJoinedEvent("e2")

    assertEquals(user.copy(joinedEvents = listOf("e1", "e2")), result)
    assertEquals(listOf("e1"), user.joinedEvents)
  }

  @Test
  fun addJoinedEvent_fails_whenEventIsAlreadyJoined() {
    val user = User("u1", joinedEvents = listOf("e1"))

    assertInvalid("This event is already joined by the user.") { user.addJoinedEvent("e1") }
  }

  @Test
  fun addJoinedEvent_fails_whenEventIdIsBlank() {
    val user = User("u1")

    assertInvalid(blankEventIdMessage) { user.addJoinedEvent(" ") }
  }

  @Test
  fun addAssignedTask_returnsCopyAndPreservesOtherFields() {
    val user =
        User(
            "u1",
            name = "Sara",
            email = "sara@example.com",
            joinedEvents = listOf("e1"),
            tasksAssigned = listOf("t1"),
        )

    val result = user.addAssignedTask("t2")

    assertEquals(user.copy(tasksAssigned = listOf("t1", "t2")), result)
    assertEquals(listOf("t1"), user.tasksAssigned)
  }

  @Test
  fun addAssignedTask_fails_whenTaskIsAlreadyAssigned() {
    val user = User("u1", tasksAssigned = listOf("t1"))

    assertInvalid("This task is already assigned to the user.") { user.addAssignedTask("t1") }
  }

  @Test
  fun addAssignedTask_fails_whenTaskIdIsBlank() {
    val user = User("u1")

    assertInvalid("An assigned task id cannot be empty.") { user.addAssignedTask(" ") }
  }

  @Test
  fun removeJoinedEvent_returnsCopyAndPreservesOtherFields() {
    val user = User("u1", name = "Sara", joinedEvents = listOf("e1", "e2"))

    val result = user.removeJoinedEvent("e1")

    assertEquals(user.copy(joinedEvents = listOf("e2")), result)
    assertEquals(listOf("e1", "e2"), user.joinedEvents)
  }

  @Test
  fun removeJoinedEvent_fails_whenEventIsNotJoined() {
    val user = User("u1", joinedEvents = listOf("e1"))

    assertInvalid("This event has not been joined by the user.") { user.removeJoinedEvent("e2") }
  }

  @Test
  fun removeAssignedTask_returnsCopyAndPreservesOtherFields() {
    val user = User("u1", tasksAssigned = listOf("t1", "t2"))

    val result = user.removeAssignedTask("t1")

    assertEquals(user.copy(tasksAssigned = listOf("t2")), result)
    assertEquals(listOf("t1", "t2"), user.tasksAssigned)
  }

  @Test
  fun removeAssignedTask_fails_whenTaskIsNotAssigned() {
    val user = User("u1", tasksAssigned = listOf("t1"))

    assertInvalid("This task is not assigned to the user.") { user.removeAssignedTask("t2") }
  }

  private fun assertInvalid(expectedMessage: String, build: () -> User) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }
}
