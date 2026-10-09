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
    assertTrue(user.eventsOwned.isEmpty())
  }

  @Test
  fun init_succeeds_withAllFields() {
    val user = User("u1", name = "Sara", surname = "Keller", eventsOwned = listOf("e1", "e2"))

    assertEquals("Sara", user.name)
    assertEquals("Keller", user.surname)
    assertEquals(listOf("e1", "e2"), user.eventsOwned)
  }

  @Test
  fun usersWithTheSameUidButOtherDataAreNotEqual() {
    val sara = User("u1", name = "Sara", surname = "Keller", eventsOwned = listOf("e1"))

    assertNotEquals(sara, User("u1"))
    assertNotEquals(sara, sara.copy(name = "Sarah"))
    assertNotEquals(sara, sara.copy(surname = "Muller"))
    assertNotEquals(sara, sara.copy(eventsOwned = listOf("e2")))
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
  fun copy_fails_whenTheCopyBreaksARule() {
    val user = User("u1", eventsOwned = listOf("e1"))

    assertInvalid(uidMessage) { user.copy(uid = "") }
    assertInvalid(blankEventIdMessage) { user.copy(eventsOwned = listOf("")) }
    assertInvalid(duplicateEventMessage) { user.copy(eventsOwned = listOf("e1", "e1")) }
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
