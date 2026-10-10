// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserTest {

  private val uidMessage = "The uid cannot be empty."
  private val blankEventIdMessage = "An owned event id cannot be empty."
  private val duplicateEventMessage = "An owned event is duplicated."

  @Test
  fun init_succeeds_withOnlyRequiredFields() {
    val user = User("u1")

    assertEquals("u1", user.uid)
    assertEquals("", user.name)
    assertEquals("", user.surname)
    assertEquals("", user.email)
    assertTrue(user.eventsOwned.isEmpty())
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
            eventsOwned = listOf("e1", "e2"),
            tasksAssigned = listOf("t1", "t2"),
        )

    assertEquals("Sara", user.name)
    assertEquals("Keller", user.surname)
    assertEquals("sara@example.com", user.email)
    assertEquals(listOf("e1", "e2"), user.eventsOwned)
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
            eventsOwned = listOf("e1"),
            tasksAssigned = listOf("t1"),
        )

    assertNotEquals(sara, User("u1"))
    assertNotEquals(sara, sara.copy(name = "Sarah"))
    assertNotEquals(sara, sara.copy(surname = "Muller"))
    assertNotEquals(sara, sara.copy(email = "sarah@example.com"))
    assertNotEquals(sara, sara.copy(eventsOwned = listOf("e2")))
    assertNotEquals(sara, sara.copy(tasksAssigned = listOf("t2")))
  }

  @Test
  fun init_fails_whenUidIsBlank() {
    for (blank in listOf("", " ", "\t", "\n")) {
      assertInvalid(uidMessage) { User(blank) }
    }
  }

  @Test
  fun init_fails_whenOwnedEventIdIsBlank() {
    assertInvalid(blankEventIdMessage) { User("u1", eventsOwned = listOf("e1", " ")) }
  }

  @Test
  fun init_fails_whenOwnedEventIsDuplicated() {
    assertInvalid(duplicateEventMessage) { User("u1", eventsOwned = listOf("e1", "e1")) }
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
    val user = User("u1", eventsOwned = listOf("e1"))

    assertInvalid(uidMessage) { user.copy(uid = "") }
    assertInvalid(blankEventIdMessage) { user.copy(eventsOwned = listOf("")) }
    assertInvalid(duplicateEventMessage) { user.copy(eventsOwned = listOf("e1", "e1")) }
    assertInvalid("An assigned task id cannot be empty.") { user.copy(tasksAssigned = listOf("")) }
    assertInvalid("An assigned task is duplicated.") {
      user.copy(tasksAssigned = listOf("t1", "t1"))
    }
  }

  @Test
  fun addOwnedEvent_returnsCopyAndPreservesOtherFields() {
    val user = User("u1", name = "Sara", surname = "Keller", eventsOwned = listOf("e1"))

    val result = user.addOwnedEvent("e2")

    assertEquals(user.copy(eventsOwned = listOf("e1", "e2")), result)
    assertEquals(listOf("e1"), user.eventsOwned)
  }

  @Test
  fun addOwnedEvent_fails_whenEventIsAlreadyOwned() {
    val user = User("u1", eventsOwned = listOf("e1"))

    assertInvalid("This event is already owned by the user.") { user.addOwnedEvent("e1") }
  }

  @Test
  fun addAssignedTask_returnsCopyAndPreservesOtherFields() {
    val user =
        User(
            "u1",
            name = "Sara",
            email = "sara@example.com",
            eventsOwned = listOf("e1"),
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
  fun removeOwnedEvent_returnsCopyAndPreservesOtherFields() {
    val user = User("u1", name = "Sara", eventsOwned = listOf("e1", "e2"))

    val result = user.removeOwnedEvent("e1")

    assertEquals(user.copy(eventsOwned = listOf("e2")), result)
    assertEquals(listOf("e1", "e2"), user.eventsOwned)
  }

  @Test
  fun removeOwnedEvent_fails_whenEventIsNotOwned() {
    val user = User("u1", eventsOwned = listOf("e1"))

    assertInvalid("This event is not owned by the user.") { user.removeOwnedEvent("e2") }
  }

  private fun assertInvalid(expectedMessage: String, build: () -> User) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }
}
