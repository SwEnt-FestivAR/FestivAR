// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.datarepository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.task.TASK_COLLECTION_PATH
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TaskRepositoryFirebase
import com.android.festivar.model.user.USER_COLLECTION_PATH
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import io.mockk.mockk
import java.io.IOException
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the operations of [DataRepository] on the Firestore repositories, against the emulator.
 *
 * Their rules and rollbacks are tested through [DataRepositoryLocalTest], with the same code. This
 * test checks what only Firestore can break: an event stores the ids of its members and tasks and a
 * task the ids of its assignees, resolved again on every read, a missing document must surface as a
 * [NoSuchElementException], and the undos must be able to write the old documents back.
 *
 * Every test starts from the same data as [DataRepositoryLocalTest], with unique ids because the
 * emulator is shared:
 * - `e1` has the members alice and bob, and the task `t1`, owned by alice and assigned to bob;
 * - carla exists but is in no event and has no task.
 */
@RunWith(AndroidJUnit4::class)
class DataRepositoryFirebaseTest {
  private val runId = UUID.randomUUID().toString()
  private val alice = "alice-$runId"
  private val bob = "bob-$runId"
  private val carla = "carla-$runId"
  private val e1 = "e1-$runId"
  private val e3 = "e3-$runId"
  private val t1 = "t1-$runId"
  private val t3 = "t3-$runId"

  private val start = ZonedDateTime.parse("2026-07-01T14:00:00+02:00")

  private val usersRepository = UsersRepositoryFirebase(Firebase.firestore)
  private val tasksRepository = TaskRepositoryFirebase(Firebase.firestore)
  private val eventsRepository = EventRepositoryFirebase(Firebase.firestore)
  private lateinit var dataRepository: DataRepository

  /** A task of `e1` that is not stored yet, owned by bob and assigned to alice. */
  private val newTask
    get() =
        Task(
            taskId = t3,
            eventId = e1,
            title = "Clean up",
            ownerId = bob,
            assignees = listOf(User(alice)),
        )

  @Before
  fun setUp() = runBlocking {
    Firebase.auth.signInAnonymously().await()
    usersRepository.addUser(User(alice, joinedEvents = listOf(e1), tasksAssigned = listOf(t1)))
    usersRepository.addUser(User(bob, joinedEvents = listOf(e1), tasksAssigned = listOf(t1)))
    usersRepository.addUser(User(carla))
    val task =
        Task(
            taskId = t1,
            eventId = e1,
            title = "Set up the stage",
            maxAssign = 2,
            assignees = listOf(User(bob)),
            ownerId = alice,
        )
    tasksRepository.addTask(task)
    eventsRepository.addEvent(
        Event(
            eventId = e1,
            title = "Summer festival",
            members = listOf(User(alice), User(bob)),
            tasks = listOf(task),
            startDate = start,
            endDate = start.plusHours(10),
        )
    )
    dataRepository = repositoryWith(usersRepository)
  }

  // Deleting a document that does not exist does nothing, so whatever a test left is removed.
  @After
  fun tearDown() = runBlocking {
    val db = Firebase.firestore
    listOf(e1, e3).forEach { db.collection("events").document(it).delete().await() }
    listOf(t1, t3).forEach { db.collection(TASK_COLLECTION_PATH).document(it).delete().await() }
    listOf(alice, bob, carla).forEach {
      db.collection(USER_COLLECTION_PATH).document(it).delete().await()
    }
    Firebase.auth.signOut()
  }

  // ********************************* Helpers ********************************* //

  // The repositories are built here rather than taken from the providers, which other instrumented
  // tests replace. The authentication is not used by any operation.
  private fun repositoryWith(users: UsersRepository) =
      DataRepositoryFirebase(mockk<AuthRepository>(), tasksRepository, eventsRepository, users)

  /** Everything this test stored, as ids, so it can be compared before and after an operation. */
  private suspend fun snapshot() =
      listOf(
          memberIds(e1),
          taskIds(e1),
          tasksRepository.getAllTasks(e1).map { it.taskId to it.assignees.map(User::uid) },
          usersRepository.getListUsers(listOf(alice, bob, carla)),
      )

  private suspend inline fun <reified T : Throwable> assertFailsWithoutChanges(
      crossinline block: suspend () -> Unit
  ) {
    val before = snapshot()
    val thrown = runCatching { block() }.exceptionOrNull()
    assertTrue("Expected ${T::class.java.simpleName} but got $thrown", thrown is T)
    assertEquals(before, snapshot())
  }

  private suspend fun assertUser(uid: String, joinedEvents: List<String>, tasks: List<String>) {
    val user = usersRepository.getUser(uid)
    assertEquals(joinedEvents, user.joinedEvents)
    assertEquals(tasks, user.tasksAssigned)
  }

  /** Checks the assignees of [taskId], both in the stored task and in its event's copy. */
  private suspend fun assertAssignees(taskId: String, expected: List<String>) {
    val task = tasksRepository.getTask(taskId)
    assertEquals(expected, task.assignees.map { it.uid })
    val copy = eventsRepository.getEvent(task.eventId).tasks.single { it.taskId == taskId }
    assertEquals(expected, copy.assignees.map { it.uid })
  }

  private suspend fun assertMissing(block: suspend () -> Unit) {
    assertTrue(runCatching { block() }.exceptionOrNull() is NoSuchElementException)
  }

  private suspend fun memberIds(eventId: String) =
      eventsRepository.getEvent(eventId).members.map { it.uid }

  private suspend fun taskIds(eventId: String) =
      eventsRepository.getEvent(eventId).tasks.map { it.taskId }

  // ********************************* Operations ********************************* //

  @Test
  fun addTask_storesTheTaskAndLinksItToItsEventOwnerAndAssignees() = runBlocking {
    dataRepository.addTask(newTask)

    // The assignees are read back with their updated tasks: assertAssignees compares their ids.
    assertEquals(newTask, tasksRepository.getTask(t3).copy(assignees = newTask.assignees))
    assertEquals(listOf(t1, t3), taskIds(e1))
    assertAssignees(t3, listOf(alice))
    assertUser(alice, listOf(e1), listOf(t1, t3))
    assertUser(bob, listOf(e1), listOf(t1, t3))
  }

  @Test
  fun deleteTask_removesTheTaskFromTheRepositoryItsEventOwnerAndAssignees() = runBlocking {
    dataRepository.deleteTask(t1)

    assertMissing { tasksRepository.getTask(t1) }
    assertTrue(taskIds(e1).isEmpty())
    assertUser(alice, listOf(e1), emptyList())
    assertUser(bob, listOf(e1), emptyList())
  }

  @Test
  fun createEvent_storesTheEventAndAddsItToItsMembers() = runBlocking {
    val event =
        Event(
            eventId = e3,
            title = "Closing party",
            members = listOf(User(alice), User(carla)),
            startDate = start,
            endDate = start.plusHours(3),
        )

    dataRepository.createEvent(event)

    assertEquals(listOf(alice, carla), memberIds(e3))
    assertUser(alice, listOf(e1, e3), listOf(t1))
    assertUser(carla, listOf(e3), emptyList())
  }

  @Test
  fun deleteEvent_removesTheEventItsTasksAndUnlinksItsMembers() = runBlocking {
    dataRepository.deleteEvent(e1)

    assertMissing { eventsRepository.getEvent(e1) }
    assertMissing { tasksRepository.getTask(t1) }
    assertUser(alice, emptyList(), emptyList())
    assertUser(bob, emptyList(), emptyList())
  }

  @Test
  fun joinEvent_addsTheUserToTheMembersAndTheEventToTheUser() = runBlocking {
    dataRepository.joinEvent(e1, carla)

    assertEquals(listOf(alice, bob, carla), memberIds(e1))
    assertUser(carla, listOf(e1), emptyList())
  }

  @Test
  fun quitEvent_removesTheUserFromTheMembersAndTheTasksOfTheEvent() = runBlocking {
    dataRepository.quitEvent(e1, bob)

    assertEquals(listOf(alice), memberIds(e1))
    assertAssignees(t1, emptyList())
    assertUser(bob, emptyList(), emptyList())
  }

  @Test
  fun joinTask_addsTheUserToTheAssigneesAndTheTaskToTheUser() = runBlocking {
    dataRepository.joinTask(t1, carla)

    assertAssignees(t1, listOf(bob, carla))
    assertUser(carla, emptyList(), listOf(t1))
  }

  @Test
  fun quitTask_removesTheUserFromTheAssigneesAndTheTaskFromTheUser() = runBlocking {
    dataRepository.quitTask(t1, bob)

    assertAssignees(t1, emptyList())
    assertUser(bob, listOf(e1), emptyList())
  }

  // ********************************* Failures ********************************* //

  // Each lookup reaches a different repository read: getEvent, getTask, getListUsers and getUser.
  @Test
  fun unknownIds_throwNoSuchElementExceptionAndChangeNothing() = runBlocking {
    val unknown = "unknown-$runId"

    assertFailsWithoutChanges<NoSuchElementException> { dataRepository.joinEvent(unknown, carla) }
    assertFailsWithoutChanges<NoSuchElementException> { dataRepository.joinTask(unknown, carla) }
    assertFailsWithoutChanges<NoSuchElementException> {
      dataRepository.addTask(newTask.copy(assignees = listOf(User(unknown))))
    }
    assertFailsWithoutChanges<NoSuchElementException> { dataRepository.joinEvent(e1, unknown) }
  }

  /**
   * deleteEvent deletes the event and its task, then edits alice before bob. Once bob's edit fails,
   * the undos write the deleted documents back with addTask and addEvent, and alice's old version.
   */
  @Test
  fun failingWrite_isUndoneInFirestore() = runBlocking {
    val brokenUsers =
        object : UsersRepository by usersRepository {
          override suspend fun editUser(uId: String, newValue: User) {
            if (uId == bob) throw IOException("Connection lost while editing $uId")
            usersRepository.editUser(uId, newValue)
          }
        }
    dataRepository = repositoryWith(brokenUsers)

    assertFailsWithoutChanges<IOException> { dataRepository.deleteEvent(e1) }
  }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      FirebaseEmulatorSetup.configure()
    }
  }
}
