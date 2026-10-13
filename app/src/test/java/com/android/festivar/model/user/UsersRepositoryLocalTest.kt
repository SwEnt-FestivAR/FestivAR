// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsersRepositoryLocalTest {
  private lateinit var repository: UsersRepositoryLocal

  private val sara = User("u1", name = "Sara")
  private val alex = User("u2", name = "Alex")

  @Before
  fun setUp() {
    repository = UsersRepositoryLocal()
  }

  @Test
  fun getNewUid_generatesDifferentNonEmptyIdentifiers() {
    val uid = repository.getNewUid()
    val otherUid = repository.getNewUid()
    assertTrue(uid.isNotEmpty())
    assertTrue(otherUid.isNotEmpty())
    assertNotEquals(uid, otherUid)
  }

  @Test fun startsEmptyByDefault() = runTest { assertTrue(repository.getAllUsers().isEmpty()) }

  @Test
  fun startsWithInitialUsers() = runTest {
    repository = UsersRepositoryLocal(listOf(sara, alex))
    assertEquals(listOf(sara, alex), repository.getAllUsers())
  }

  @Test
  fun initialUsersWithDuplicatedUidThrows() {
    assertThrows(IllegalArgumentException::class.java) {
      UsersRepositoryLocal(listOf(sara, alex.copy(uid = sara.uid)))
    }
  }

  @Test
  fun addUser_thenGetUserAndGetAllUsersReturnIt() = runTest {
    repository.addUser(sara)
    assertEquals(sara, repository.getUser(sara.uid))
    assertEquals(listOf(sara), repository.getAllUsers())
  }

  @Test
  fun addUser_withExistingUidThrows() = runTest {
    repository.addUser(sara)
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.addUser(sara.copy(name = "Other")) }
    }
    assertEquals(listOf(sara), repository.getAllUsers())
  }

  @Test
  fun getAllUsers_returnsACopy() = runTest {
    repository.addUser(sara)
    val users = repository.getAllUsers()
    repository.addUser(alex)
    assertEquals(listOf(sara), users)
  }

  @Test
  fun getUser_findsUserAmongOthers() = runTest {
    repository = UsersRepositoryLocal(listOf(sara, alex))
    assertEquals(alex, repository.getUser(alex.uid))
  }

  @Test
  fun getUser_unknownUidThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getUser("unknown") }
    }
  }

  @Test
  fun getListUsers_returnsRequestedUsersInOrder() = runTest {
    repository = UsersRepositoryLocal(listOf(sara, alex))
    assertEquals(listOf(alex, sara), repository.getListUsers(listOf(alex.uid, sara.uid)))
  }

  @Test
  fun getListUsers_unknownUidThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getListUsers(listOf(sara.uid, "unknown")) }
    }
  }

  @Test
  fun editUser_replacesUser() = runTest {
    repository.addUser(sara)
    val edited = sara.copy(name = "Sara Keller")
    repository.editUser(sara.uid, edited)
    assertEquals(edited, repository.getUser(sara.uid))
    assertEquals(1, repository.getAllUsers().size)
  }

  @Test
  fun editUser_withDifferentUidThrows() = runTest {
    repository.addUser(sara)
    repository.addUser(alex)
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.editUser(sara.uid, alex.copy(name = "Changed")) }
    }
    assertEquals(listOf(sara, alex), repository.getAllUsers())
  }

  @Test
  fun editUser_unknownUidThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.editUser(sara.uid, sara) }
    }
  }

  @Test
  fun deleteUser_removesUser() = runTest {
    repository = UsersRepositoryLocal(listOf(sara, alex))
    repository.deleteUser(sara.uid)
    assertEquals(listOf(alex), repository.getAllUsers())
  }

  @Test
  fun deleteUser_unknownUidThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteUser("unknown") }
    }
  }
}
