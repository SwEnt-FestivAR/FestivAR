// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import io.mockk.mockk
import java.io.IOException
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the operations of [DataRepository] through [DataRepositoryLocal].
 *
 * Every test starts from the same consistent data:
 * - `e1` has the members alice and bob, and the task `t1`, owned by alice and assigned to bob;
 * - `e2` has the member bob, and the task `t2`, owned by bob, assigned to bob and full;
 * - carla exists but is in no event and has no task.
 */
class DataRepositoryLocalTest {
  private lateinit var tasksRepository: TasksRepositoryLocal
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var usersRepository: UsersRepositoryLocal
  private lateinit var dataRepository: DataRepository

  private val start = ZonedDateTime.parse("2026-07-01T14:00:00+02:00")

  private val alice = User("alice", joinedEvents = listOf("e1"), tasksAssigned = listOf("t1"))
  private val bob =
      User("bob", joinedEvents = listOf("e1", "e2"), tasksAssigned = listOf("t1", "t2"))
  private val carla = User("carla")

  private val t1 =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Set up the stage",
          maxAssign = 2,
          assignees = listOf(bob),
          ownerId = "alice",
      )
  private val t2 =
      Task(
          taskId = "t2",
          eventId = "e2",
          title = "Sell tickets",
          assignees = listOf(bob),
          ownerId = "bob",
      )

  private val e1 =
      Event(
          eventId = "e1",
          ownerId = alice.uid,
          title = "Summer festival",
          members = listOf(alice, bob),
          tasks = listOf(t1),
          startDate = start,
          endDate = start.plusHours(10),
      )
  private val e2 =
      Event(
          eventId = "e2",
          ownerId = bob.uid,
          title = "Jazz night",
          members = listOf(bob),
          tasks = listOf(t2),
          startDate = start,
          endDate = start.plusHours(4),
      )

  /** A task of `e1` that is not stored yet, owned by bob and assigned to alice. */
  private val t3 =
      Task(
          taskId = "t3",
          eventId = "e1",
          title = "Clean up",
          assignees = listOf(alice),
          ownerId = "bob",
      )

  /** An event that is not stored yet, with the members alice and carla. */
  private val e3 =
      Event(
          eventId = "e3",
          ownerId = alice.uid,
          title = "Closing party",
          members = listOf(alice, carla),
          startDate = start,
          endDate = start.plusHours(3),
      )

  @Before
  fun setUp() {
    tasksRepository = TasksRepositoryLocal()
    eventsRepository = EventsRepositoryLocal(listOf(e1, e2))
    usersRepository = UsersRepositoryLocal(listOf(alice, bob, carla))
    runBlocking {
      tasksRepository.addTask(t1)
      tasksRepository.addTask(t2)
    }
    dataRepository = repositoryWith(usersRepository)
  }

  // ********************************* Helpers ********************************* //

  /** Everything stored, compared as sets because an undo may store an item back at the end. */
  private data class State(val events: Set<Event>, val tasks: Set<Task>, val users: Set<User>)

  private suspend fun snapshot() =
      State(
          eventsRepository.getAllEvents().toSet(),
          listOf("e1", "e2", "e3").flatMap { tasksRepository.getAllTasks(it) }.toSet(),
          usersRepository.getAllUsers().toSet(),
      )

  private fun repositoryWith(
      users: UsersRepository = usersRepository,
      tasks: TasksRepository = tasksRepository,
  ) = DataRepositoryLocal(mockk<AuthRepository>(), tasks, eventsRepository, users)

  /** A [TasksRepository] that behaves like [delegate], except that every edit fails. */
  private class BrokenTasksRepository(private val delegate: TasksRepository) :
      TasksRepository by delegate {
    override suspend fun editTask(taskId: String, newValue: Task) {
      throw IOException("Connection lost while editing $taskId")
    }
  }

  /**
   * A [UsersRepository] that behaves like [delegate], except that editing [failingUid] fails like a
   * lost connection. If [staysBroken], every later edit fails too, including the undos.
   */
  private class BrokenUsersRepository(
      private val delegate: UsersRepository,
      private val failingUid: String,
      private val staysBroken: Boolean,
  ) : UsersRepository by delegate {
    private var broken = false

    override suspend fun editUser(uId: String, newValue: User) {
      if (uId == failingUid) broken = true
      if (uId == failingUid || (staysBroken && broken)) {
        throw IOException("Connection lost while editing $uId")
      }
      delegate.editUser(uId, newValue)
    }
  }

  /**
   * Checks that [block] throws exactly [expected] (not a subclass, such as a
   * [DataInconsistencyException] for an [IllegalStateException]) and leaves every repository
   * unchanged.
   */
  private suspend fun <T : Throwable> assertFailsWithoutChanges(
      expected: Class<T>,
      block: suspend () -> Unit,
  ): T {
    val before = snapshot()
    val exception = assertThrows(expected) { runBlocking { block() } }
    assertEquals(expected, exception.javaClass)
    assertEquals(before, snapshot())
    return exception
  }

  /**
   * Runs [operation] while the edit of [failingUid] fails, and checks that the original exception
   * is rethrown and that every write already done is undone.
   */
  private suspend fun assertRollsBack(
      failingUid: String,
      operation: suspend DataRepository.() -> Unit,
  ) {
    dataRepository = repositoryWith(BrokenUsersRepository(usersRepository, failingUid, false))
    assertFailsWithoutChanges(IOException::class.java) { dataRepository.operation() }
  }

  /**
   * Runs [operation] while every task edit fails, a write done after the event's edit, and checks
   * that the original exception is rethrown and that the event's edit is undone.
   */
  private suspend fun assertRollsBackWhenTaskEditFails(
      operation: suspend DataRepository.() -> Unit
  ) {
    dataRepository = repositoryWith(tasks = BrokenTasksRepository(tasksRepository))
    assertFailsWithoutChanges(IOException::class.java) { dataRepository.operation() }
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

  private suspend fun memberIds(eventId: String) =
      eventsRepository.getEvent(eventId).members.map { it.uid }

  private suspend fun taskIds(eventId: String) =
      eventsRepository.getEvent(eventId).tasks.map { it.taskId }

  // ********************************* Constructor ********************************* //

  @Test
  fun defaultRepositories_startEmpty() = runTest {
    val repository = DataRepositoryLocal(mockk<AuthRepository>())

    assertTrue(repository.eventsRepository.getAllEvents().isEmpty())
    assertTrue(repository.tasksRepository.getAllTasks("e1").isEmpty())
    assertTrue(repository.usersRepository.getAllUsers().isEmpty())
  }

  // ********************************* addTask ********************************* //

  @Test
  fun addTask_storesTheTaskAndLinksItToItsEventOwnerAndAssignees() = runTest {
    dataRepository.addTask(t3)

    assertEquals(t3, tasksRepository.getTask("t3"))
    assertEquals(listOf("t1", "t3"), taskIds("e1"))
    assertAssignees("t3", listOf("alice"))
    assertUser("alice", listOf("e1"), listOf("t1", "t3"))
    assertUser("bob", listOf("e1", "e2"), listOf("t1", "t2", "t3"))
    assertUser("carla", emptyList(), emptyList())
    assertEquals(listOf("t2"), taskIds("e2"))
  }

  @Test
  fun addTask_withTheOwnerAlsoAssigned_linksItOnce() = runTest {
    dataRepository.addTask(t3.copy(assignees = listOf(bob)))

    assertUser("bob", listOf("e1", "e2"), listOf("t1", "t2", "t3"))
    assertUser("alice", listOf("e1"), listOf("t1"))
  }

  @Test
  fun addTask_alreadyInTheEventListedOrStored_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.addTask(t1.copy(title = "Duplicate"))
    }
    // bob, the owner, already lists t2.
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.addTask(t2.copy(eventId = "e1"))
    }
    // Nobody lists t2 and e1 does not contain it, but the tasks repository does: the first write
    // fails, with nothing to undo.
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.addTask(t2.copy(eventId = "e1", ownerId = "alice", assignees = listOf(alice)))
    }
  }

  @Test
  fun addTask_unknownEventOrUser_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.addTask(t3.copy(eventId = "unknown"))
    }
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.addTask(t3.copy(ownerId = "unknown"))
    }
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.addTask(t3.copy(assignees = listOf(User("unknown"))))
    }
  }

  @Test
  fun addTask_failingWrite_isUndone() = runTest {
    // bob, the owner, is edited before alice: the last write fails, then the first user's one.
    assertRollsBack("alice") { addTask(t3) }
    assertRollsBack("bob") { addTask(t3) }
  }

  // ********************************* deleteTask ********************************* //

  @Test
  fun deleteTask_removesTheTaskFromTheRepositoryItsEventOwnerAndAssignees() = runTest {
    dataRepository.deleteTask("t1")

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { tasksRepository.getTask("t1") }
    }
    assertTrue(taskIds("e1").isEmpty())
    assertUser("alice", listOf("e1"), emptyList())
    assertUser("bob", listOf("e1", "e2"), listOf("t2"))
    assertEquals(t2, tasksRepository.getTask("t2"))
  }

  @Test
  fun deleteTask_ownerNoLongerListingIt_deletesItAnyway() = runTest {
    // alice, the owner of t1, loses it from her tasks when she quits e1.
    dataRepository.quitEvent("e1", "alice")

    dataRepository.deleteTask("t1")

    assertTrue(taskIds("e1").isEmpty())
    assertUser("alice", emptyList(), emptyList())
    assertUser("bob", listOf("e1", "e2"), listOf("t2"))
  }

  @Test
  fun deleteTask_unknownTask_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.deleteTask("unknown")
    }
  }

  @Test
  fun deleteTask_failingWrite_isUndone() = runTest {
    // The assignees are edited before the owner: the owner's write fails last.
    assertRollsBack("bob") { deleteTask("t1") }
    assertRollsBack("alice") { deleteTask("t1") }
  }

  // ********************************* createEvent ********************************* //

  @Test
  fun createEvent_storesTheEventAndAddsItToItsMembers() = runTest {
    dataRepository.createEvent(e3)

    assertEquals(e3, eventsRepository.getEvent("e3"))
    assertUser("alice", listOf("e1", "e3"), listOf("t1"))
    assertUser("carla", listOf("e3"), emptyList())
    assertUser("bob", listOf("e1", "e2"), listOf("t1", "t2"))
  }

  @Test
  fun createEvent_withTasks_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.createEvent(e3.copy(tasks = listOf(t3.copy(eventId = "e3"))))
    }
  }

  @Test
  fun createEvent_existingId_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.createEvent(e3.copy(eventId = "e1", members = listOf(carla)))
    }
  }

  @Test
  fun createEvent_unknownMember_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.createEvent(e3.copy(members = listOf(alice, User("unknown"))))
    }
  }

  @Test
  fun createEvent_failingWrite_isUndone() = runTest { assertRollsBack("carla") { createEvent(e3) } }

  // ********************************* deleteEvent ********************************* //

  @Test
  fun deleteEvent_removesTheEventItsTasksAndUnlinksItsMembers() = runTest {
    dataRepository.deleteEvent("e1")

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { eventsRepository.getEvent("e1") }
    }
    assertTrue(tasksRepository.getAllTasks("e1").isEmpty())
    assertUser("alice", emptyList(), emptyList())
    assertUser("bob", listOf("e2"), listOf("t2"))
    assertEquals(e2, eventsRepository.getEvent("e2"))
    assertEquals(t2, tasksRepository.getTask("t2"))
  }

  @Test
  fun deleteEvent_withAnAssigneeNotMember_deletesEveryTaskButTheAssigneeKeepsIt() = runTest {
    dataRepository.addTask(t3.copy(assignees = listOf(carla)))

    dataRepository.deleteEvent("e1")

    assertTrue(tasksRepository.getAllTasks("e1").isEmpty())
    assertUser("alice", emptyList(), emptyList())
    assertUser("bob", listOf("e2"), listOf("t2"))
    assertUser("carla", emptyList(), listOf("t3"))
  }

  @Test
  fun deleteEvent_unknownEvent_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.deleteEvent("unknown")
    }
  }

  @Test
  fun deleteEvent_failingWrite_isUndone() = runTest { assertRollsBack("bob") { deleteEvent("e1") } }

  // ********************************* joinEvent ********************************* //

  @Test
  fun joinEvent_addsTheUserToTheMembersAndTheEventToTheUser() = runTest {
    dataRepository.joinEvent("e1", "carla")

    assertEquals(listOf("alice", "bob", "carla"), memberIds("e1"))
    assertUser("carla", listOf("e1"), emptyList())
  }

  @Test
  fun joinEvent_alreadyMember_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.joinEvent("e1", "bob")
    }
  }

  @Test
  fun joinEvent_unknownEventOrUser_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.joinEvent("unknown", "carla")
    }
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.joinEvent("e1", "unknown")
    }
  }

  @Test
  fun joinEvent_failingWrite_isUndone() = runTest {
    assertRollsBack("carla") { joinEvent("e1", "carla") }
  }

  // ********************************* quitEvent ********************************* //

  @Test
  fun quitEvent_removesTheUserFromTheMembersAndTheTasksOfThatEventOnly() = runTest {
    dataRepository.quitEvent("e1", "bob")

    assertEquals(listOf("alice"), memberIds("e1"))
    assertAssignees("t1", emptyList())
    assertUser("bob", listOf("e2"), listOf("t2"))
    assertEquals(listOf("bob"), memberIds("e2"))
    assertAssignees("t2", listOf("bob"))
  }

  @Test
  fun quitEvent_notAssignedToItsTasks_leavesTheTasksUnchanged() = runTest {
    dataRepository.joinEvent("e1", "carla")

    dataRepository.quitEvent("e1", "carla")

    assertEquals(listOf("alice", "bob"), memberIds("e1"))
    assertEquals(t1, tasksRepository.getTask("t1"))
    assertAssignees("t1", listOf("bob"))
    assertUser("carla", emptyList(), emptyList())
  }

  @Test
  fun quitEvent_byATaskOwner_keepsTheOwnerButRemovesTheTaskFromTheirTasks() = runTest {
    dataRepository.quitEvent("e1", "alice")

    assertEquals(listOf("bob"), memberIds("e1"))
    assertEquals(listOf("t1"), taskIds("e1"))
    assertEquals("alice", tasksRepository.getTask("t1").ownerId)
    assertUser("alice", emptyList(), emptyList())
  }

  @Test
  fun quitEvent_notMember_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.quitEvent("e1", "carla")
    }
  }

  @Test
  fun quitEvent_unknownEventOrUser_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.quitEvent("unknown", "bob")
    }
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.quitEvent("e1", "unknown")
    }
  }

  @Test
  fun quitEvent_failingWrite_isUndone() = runTest {
    assertRollsBack("bob") { quitEvent("e1", "bob") }
    assertRollsBackWhenTaskEditFails { quitEvent("e1", "bob") }
  }

  // ********************************* joinTask ********************************* //

  @Test
  fun joinTask_addsTheUserToTheAssigneesAndTheTaskToTheUser() = runTest {
    dataRepository.joinTask("t1", "carla")

    assertAssignees("t1", listOf("bob", "carla"))
    assertUser("carla", emptyList(), listOf("t1"))
  }

  @Test
  fun joinTask_byItsOwner_keepsTheTaskOnceInTheOwnerTasks() = runTest {
    dataRepository.joinTask("t1", "alice")

    assertAssignees("t1", listOf("bob", "alice"))
    assertUser("alice", listOf("e1"), listOf("t1"))
  }

  @Test
  fun joinTask_byItsOwnerAfterQuittingIt_addsTheTaskBackToTheOwner() = runTest {
    dataRepository.joinTask("t1", "alice")
    dataRepository.quitTask("t1", "alice")
    assertUser("alice", listOf("e1"), emptyList())

    dataRepository.joinTask("t1", "alice")

    assertAssignees("t1", listOf("bob", "alice"))
    assertUser("alice", listOf("e1"), listOf("t1"))
  }

  @Test
  fun joinTask_keepsThePositionOfTheTaskInItsEvent() = runTest {
    dataRepository.addTask(t3)

    dataRepository.joinTask("t1", "carla")

    assertEquals(listOf("t1", "t3"), taskIds("e1"))
  }

  @Test
  fun joinTask_fullTask_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalStateException::class.java) {
      dataRepository.joinTask("t2", "carla")
    }
  }

  @Test
  fun joinTask_alreadyAssigned_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.joinTask("t1", "bob")
    }
  }

  @Test
  fun joinTask_unknownTaskOrUser_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.joinTask("unknown", "carla")
    }
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.joinTask("t1", "unknown")
    }
  }

  @Test
  fun joinTask_failingWrite_isUndone() = runTest {
    assertRollsBack("carla") { joinTask("t1", "carla") }
    assertRollsBackWhenTaskEditFails { joinTask("t1", "carla") }
  }

  // ********************************* quitTask ********************************* //

  @Test
  fun quitTask_removesTheUserFromTheAssigneesAndTheTaskFromTheUser() = runTest {
    dataRepository.quitTask("t1", "bob")

    assertAssignees("t1", emptyList())
    assertUser("bob", listOf("e1", "e2"), listOf("t2"))
  }

  @Test
  fun quitTask_keepsThePositionOfTheTaskInItsEvent() = runTest {
    dataRepository.addTask(t3)

    dataRepository.quitTask("t1", "bob")

    assertEquals(listOf("t1", "t3"), taskIds("e1"))
  }

  @Test
  fun quitTask_notAssigned_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(IllegalArgumentException::class.java) {
      dataRepository.quitTask("t1", "carla")
    }
  }

  @Test
  fun quitTask_unknownTaskOrUser_throwsAndChangesNothing() = runTest {
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.quitTask("unknown", "bob")
    }
    assertFailsWithoutChanges(NoSuchElementException::class.java) {
      dataRepository.quitTask("t1", "unknown")
    }
  }

  @Test
  fun quitTask_failingWrite_isUndone() = runTest {
    assertRollsBack("bob") { quitTask("t1", "bob") }
    assertRollsBackWhenTaskEditFails { quitTask("t1", "bob") }
  }

  // ********************************* Rollback ********************************* //

  /**
   * In addTask, bob is edited before alice. Once alice's edit fails, the connection stays lost, so
   * undoing bob's edit fails too: the task and the event must still be undone.
   */
  @Test
  fun failingUndo_throwsDataInconsistencyExceptionAfterRunningTheOtherUndos() = runTest {
    dataRepository = repositoryWith(BrokenUsersRepository(usersRepository, "alice", true))

    val thrown = runCatching { dataRepository.addTask(t3) }.exceptionOrNull()

    assertTrue(thrown is DataInconsistencyException)
    // When assertions are enabled, as in tests, the coroutines stack trace recovery can rethrow a
    // copy of the exception thrown in withContext, whose cause is the original one.
    val exception = thrown!!.cause as? DataInconsistencyException ?: thrown
    assertTrue(exception.cause is IOException)
    assertEquals(1, exception.suppressed.size)
    assertTrue(exception.suppressed.single() is IOException)
    assertTrue(tasksRepository.getAllTasks("e1").none { it.taskId == "t3" })
    assertEquals(e1, eventsRepository.getEvent("e1"))
  }
}
