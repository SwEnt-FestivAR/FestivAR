// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsersRepositoryFirebaseTest {
  private lateinit var repository: UserRepositoryFirebase
  private lateinit var user: User
  private val createdUids = mutableSetOf<String>()

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    runBlocking { Firebase.auth.signInAnonymously().await() }
    user =
        User(
            uid = "user-${UUID.randomUUID()}",
            name = "Sara",
            surname = "Keller",
            email = "sara@example.com",
            joinedEvents = listOf("event-1", "event-2"),
            tasksAssigned = listOf("task-1"),
        )
    createdUids.clear()
    createdUids.add(user.uid)
    repository = UserRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      createdUids.forEach { uid ->
        Firebase.firestore.collection(USER_COLLECTION_PATH).document(uid).delete().await()
      }
      Firebase.auth.signOut()
    }
  }

  @Test
  fun getNewUid_returnsUniqueFirestoreDocumentIds() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    assertEquals(first, Firebase.firestore.collection(USER_COLLECTION_PATH).document(first).id)
    assertEquals(second, Firebase.firestore.collection(USER_COLLECTION_PATH).document(second).id)
    assertNotEquals(first, second)
  }

  @Test
  fun toFirestoreData_doesNotStoreUidAndSerializesUserFields() {
    val data = invokeToFirestoreData(user)

    assertFalse(data.containsKey("uid"))
    assertEquals(user.name, data["name"])
    assertEquals(user.surname, data["surname"])
    assertEquals(user.email, data["email"])
    assertEquals(user.joinedEvents, data["joinedEvents"])
    assertEquals(user.tasksAssigned, data["tasksAssigned"])
  }

  @Test
  fun documentToUser_usesDocumentIdAsUid() = runBlocking {
    val document = Firebase.firestore.collection(USER_COLLECTION_PATH).document(user.uid)
    document.set(invokeToFirestoreData(user)).await()

    assertEquals(user, invokeDocumentToUser(document.get().await()))
  }

  @Test
  fun documentToUser_usesDefaultsForMissingFields() = runBlocking {
    val documentId = repository.getNewUid()
    createdUids.add(documentId)
    val document = Firebase.firestore.collection(USER_COLLECTION_PATH).document(documentId)
    document.set(mapOf("name" to "Alex")).await()

    assertEquals(
        User(uid = documentId, name = "Alex"),
        invokeDocumentToUser(document.get().await()),
    )
  }

  @Test
  fun getAllUsers_returnsStoredUsers() = runBlocking {
    val anotherUser = user.copy(uid = repository.getNewUid())
    createdUids.add(anotherUser.uid)
    repository.addUser(user)
    repository.addUser(anotherUser)

    assertTrue(repository.getAllUsers().containsAll(listOf(user, anotherUser)))
  }

  @Test
  fun getUser_returnsUserByDocumentId() = runBlocking {
    repository.addUser(user)

    assertEquals(user, repository.getUser(user.uid))
  }

  @Test
  fun getUser_throwsWhenUserDoesNotExist() = runBlocking {
    assertThrows<NoSuchElementException> { repository.getUser(user.uid) }
  }

  @Test
  fun getListUsers_returnsRequestedUsersInInputOrder() = runBlocking {
    val anotherUser = user.copy(uid = repository.getNewUid())
    createdUids.add(anotherUser.uid)
    repository.addUser(user)
    repository.addUser(anotherUser)

    assertEquals(
        listOf(anotherUser, user, anotherUser),
        repository.getListUsers(listOf(anotherUser.uid, user.uid, anotherUser.uid)),
    )
  }

  @Test
  fun getListUsers_chunksRequestsOverFirestoreQueryLimit() = runBlocking {
    val users = (1..31).map { user.copy(uid = repository.getNewUid()) }
    createdUids.addAll(users.map(User::uid))
    users.forEach { repository.addUser(it) }

    assertEquals(users, repository.getListUsers(users.map(User::uid)))
  }

  @Test
  fun getListUsers_returnsEmptyListForEmptyInput() = runBlocking {
    assertTrue(repository.getListUsers(emptyList()).isEmpty())
  }

  @Test
  fun getListUsers_throwsWhenAUserDoesNotExist() = runBlocking {
    repository.addUser(user)

    assertThrows<NoSuchElementException> {
      repository.getListUsers(listOf(user.uid, "missing-${UUID.randomUUID()}"))
    }
  }

  @Test
  fun addUser_storesUserAndThrowsWhenUidAlreadyExists() = runBlocking {
    repository.addUser(user)

    assertEquals(user, repository.getUser(user.uid))
    assertThrows<IllegalArgumentException> { repository.addUser(user) }
  }

  @Test
  fun editUser_updatesExistingUser() = runBlocking {
    repository.addUser(user)
    val updatedUser = user.copy(name = "Updated Sara", tasksAssigned = listOf("task-2"))

    repository.editUser(user.uid, updatedUser)

    assertEquals(updatedUser, repository.getUser(user.uid))
  }

  @Test
  fun editUser_throwsWhenUidDiffers() = runBlocking {
    assertThrows<IllegalArgumentException> {
      repository.editUser(user.uid, user.copy(uid = "other"))
    }
  }

  @Test
  fun editUser_throwsWhenUserDoesNotExist() = runBlocking {
    assertThrows<NoSuchElementException> { repository.editUser(user.uid, user) }
  }

  @Test
  fun deleteUser_removesUser() = runBlocking {
    repository.addUser(user)

    repository.deleteUser(user.uid)

    assertThrows<NoSuchElementException> { repository.getUser(user.uid) }
  }

  @Test
  fun deleteUser_throwsWhenUserDoesNotExist() = runBlocking {
    assertThrows<NoSuchElementException> { repository.deleteUser(user.uid) }
  }

  @Test
  fun deleteUser_concurrentDeletesOnlySucceedOnce() = runBlocking {
    repository.addUser(user)

    val results =
        listOf(
                async { runCatching { repository.deleteUser(user.uid) } },
                async { runCatching { repository.deleteUser(user.uid) } },
            )
            .awaitAll()

    assertEquals(1, results.count { it.isSuccess })
    assertTrue(results.single { it.isFailure }.exceptionOrNull() is NoSuchElementException)
  }

  private suspend inline fun <reified T : Throwable> assertThrows(
      crossinline block: suspend () -> Unit,
  ) {
    var thrown: Throwable? = null
    try {
      block()
    } catch (exception: Exception) {
      thrown = exception
    }
    assertTrue("Expected ${T::class.java.simpleName}", thrown is T)
  }

  @Suppress("UNCHECKED_CAST")
  private fun invokeToFirestoreData(user: User): Map<String, Any?> {
    val method =
        UserRepositoryFirebase::class.java.getDeclaredMethod("toFirestoreData", User::class.java)
    method.isAccessible = true
    return method.invoke(repository, user) as Map<String, Any?>
  }

  private fun invokeDocumentToUser(document: DocumentSnapshot): User {
    val method =
        UserRepositoryFirebase::class
            .java
            .getDeclaredMethod(
                "documentToUser",
                DocumentSnapshot::class.java,
            )
    method.isAccessible = true
    return method.invoke(repository, document) as User
  }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      Firebase.firestore.useEmulator("10.0.2.2", 8080)
    }
  }
}
