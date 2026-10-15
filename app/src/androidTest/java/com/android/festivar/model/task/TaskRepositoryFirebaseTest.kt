// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UserRepositoryFirebase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
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
class TaskRepositoryFirebaseTest {
  private lateinit var repository: TaskRepositoryFirebase
  private lateinit var task: Task

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    runBlocking {
      Firebase.auth.signInAnonymously().await()
      val userRepo = UserRepositoryFirebase(Firebase.firestore)
      userRepo.addUser(User("user-1"))
      userRepo.addUser(User("user-2"))
    }
    task = newTask()
    repository = TaskRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      if (::task.isInitialized) {
        Firebase.firestore.collection(TASK_COLLECTION_PATH).document(task.taskId).delete().await()
      }
      val userRepo = UserRepositoryFirebase(Firebase.firestore)
      runCatching { userRepo.deleteUser("user-1") }
      runCatching { userRepo.deleteUser("user-2") }
      Firebase.auth.signOut()
    }
  }

  @Test
  fun getNewUid_returnsNonEmptyUniqueIds() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    assertTrue(first.isNotEmpty())
    assertNotEquals(first, second)
  }

  @Test
  fun toFirestoreData_serializesTaskFields() {
    val data = invokeToFirestoreData(task)

    assertEquals(task.taskId, data["taskId"])
    assertEquals(task.eventId, data["eventId"])
    assertEquals(task.title, data["title"])
    assertEquals(task.description, data["description"])
    assertEquals(task.startTime.toString(), data["startTime"])
    assertEquals(task.endTime.toString(), data["endTime"])
    assertEquals(task.estimatedTime!!.toNanos(), data["estimatedTime"])
    assertEquals(task.location, data["location"])
    assertEquals(task.priority.name, data["priority"])
    assertEquals(task.maxAssign, data["maxAssign"])
    assertEquals(task.assignees.map { it.uid }, data["assignees"])
    assertEquals(task.completed, data["completed"])
  }

  @Test
  fun toFirestoreData_serializesOptionalFieldsAsNull() {
    val taskWithoutOptionalFields =
        task.copy(startTime = null, endTime = null, estimatedTime = null)

    val data = invokeToFirestoreData(taskWithoutOptionalFields)

    assertNull(data["startTime"])
    assertNull(data["endTime"])
    assertNull(data["estimatedTime"])
  }

  @Test
  fun documentToTask_deserializesStoredTask() = runBlocking {
    val data = invokeToFirestoreData(task)
    val document = Firebase.firestore.collection(TASK_COLLECTION_PATH).document(task.taskId)
    document.set(data).await()

    assertEquals(task, invokeDocumentToTask(document.get().await()))
  }

  @Test
  fun documentToTask_usesDefaultsForMissingOptionalFields() = runBlocking {
    val documentId = "task-${UUID.randomUUID()}"
    val document = Firebase.firestore.collection(TASK_COLLECTION_PATH).document(documentId)
    document
        .set(
            mapOf(
                "eventId" to "event-2",
                "title" to "Minimal task",
                "assignees" to listOf("user-2"),
            )
        )
        .await()

    try {
      assertEquals(
          Task(
              taskId = documentId,
              eventId = "event-2",
              title = "Minimal task",
              assignees = listOf(User("user-2")),
          ),
          invokeDocumentToTask(document.get().await()),
      )
    } finally {
      document.delete().await()
    }
  }

  @Test
  fun getAllTasks_returnsStoredTasks() = runBlocking {
    repository.addTask(task)

    assertEquals(listOf(task), repository.getAllTasks(task.eventId))
  }

  @Test
  fun getAllTasks_returnsEmptyListWhenNoTasksExist() = runBlocking {
    assertTrue(repository.getAllTasks(task.eventId).isEmpty())
  }

  @Test
  fun getAllTasks_filtersByEventId() = runBlocking {
    val otherTask = task.copy(taskId = "task-${UUID.randomUUID()}", eventId = "event-2")
    repository.addTask(task)
    repository.addTask(otherTask)

    try {
      assertEquals(listOf(task), repository.getAllTasks(task.eventId))
    } finally {
      Firebase.firestore
          .collection(TASK_COLLECTION_PATH)
          .document(otherTask.taskId)
          .delete()
          .await()
    }
  }

  @Test
  fun getTask_returnsTaskById() = runBlocking {
    repository.addTask(task)

    assertEquals(task, repository.getTask(task.taskId))
  }

  @Test
  fun getTask_throwsWhenTaskDoesNotExist() = runBlocking {
    assertThrows { repository.getTask(task.taskId) }
  }

  @Test
  fun addTask_storesTaskAndThrowsWhenTaskIdAlreadyExists() = runBlocking {
    repository.addTask(task)

    assertEquals(task, repository.getTask(task.taskId))

    assertThrows { repository.addTask(task) }
  }

  @Test
  fun editTask_updatesExistingTask() = runBlocking {
    repository.addTask(task)
    val updatedTask = task.copy(title = "Updated task", completed = true)

    repository.editTask(task.taskId, updatedTask)

    assertEquals(updatedTask, repository.getTask(task.taskId))
  }

  @Test
  fun editTask_throwsWhenTaskIdDiffers() = runBlocking {
    assertThrows { repository.editTask(task.taskId, task.copy(taskId = "other-task")) }
  }

  @Test
  fun editTask_throwsWhenTaskDoesNotExist() = runBlocking {
    assertThrowsOfType<NoSuchElementException> { repository.editTask(task.taskId, task) }
  }

  @Test
  fun editTask_doesNotCreateTaskWhenTaskDoesNotExist() = runBlocking {
    assertThrowsOfType<NoSuchElementException> { repository.editTask(task.taskId, task) }

    assertThrowsOfType<NoSuchElementException> { repository.getTask(task.taskId) }
  }

  @Test
  fun deleteTask_removesTask() = runBlocking {
    repository.addTask(task)

    repository.deleteTask(task.taskId)

    assertThrows { repository.getTask(task.taskId) }
  }

  @Test
  fun deleteTask_throwsWhenTaskDoesNotExist() = runBlocking {
    assertThrowsOfType<NoSuchElementException> { repository.deleteTask(task.taskId) }
  }

  @Test
  fun deleteTask_throwsWhenTaskWasAlreadyDeleted() = runBlocking {
    repository.addTask(task)
    repository.deleteTask(task.taskId)

    assertThrowsOfType<NoSuchElementException> { repository.deleteTask(task.taskId) }
  }

  @Test
  fun deleteTask_concurrentDeletesOnlySucceedOnce() = runBlocking {
    repository.addTask(task)

    val results =
        listOf(
                async { runCatching { repository.deleteTask(task.taskId) } },
                async { runCatching { repository.deleteTask(task.taskId) } },
            )
            .awaitAll()

    assertEquals(1, results.count { it.isSuccess })
    assertTrue(results.single { it.isFailure }.exceptionOrNull() is NoSuchElementException)
  }

  private fun newTask(): Task =
      Task(
          taskId = "task-${UUID.randomUUID()}",
          eventId = "event-1",
          title = "Task",
          description = "Description",
          startTime = LocalDateTime.of(2026, 7, 1, 14, 0),
          endTime = LocalDateTime.of(2026, 7, 1, 16, 0),
          estimatedTime = Duration.ofHours(1),
          location = "EPFL",
          priority = Priority.MEDIUM,
          maxAssign = 2,
          assignees = listOf(User("user-1")),
          completed = false,
      )

  private suspend fun assertThrows(block: suspend () -> Unit) {
    var thrown = false
    try {
      block()
    } catch (_: Exception) {
      thrown = true
    }
    assertTrue("Expected an exception", thrown)
  }

  private suspend inline fun <reified T : Throwable> assertThrowsOfType(
      crossinline block: suspend () -> Unit,
  ) {
    var thrown: Throwable? = null
    try {
      block()
    } catch (exception: Exception) {
      thrown = exception
    }
    assertTrue("Expected ${T::class.java.simpleName} but got $thrown", thrown is T)
  }

  @Suppress("UNCHECKED_CAST")
  private fun invokeToFirestoreData(task: Task): Map<String, Any?> {
    val method =
        TaskRepositoryFirebase::class.java.getDeclaredMethod("toFirestoreData", Task::class.java)
    method.isAccessible = true
    return method.invoke(repository, task) as Map<String, Any?>
  }

  private suspend fun invokeDocumentToTaskSuspend(document: DocumentSnapshot): Task {
    val method =
        TaskRepositoryFirebase::class.java.declaredMethods.first { it.name == "documentToTask" }
    method.isAccessible = true
    return suspendCoroutineUninterceptedOrReturn<Task> { cont ->
      method.invoke(repository, document, cont)
    }
  }

  private fun invokeDocumentToTask(document: DocumentSnapshot): Task {
    return runBlocking { invokeDocumentToTaskSuspend(document) }
  }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      FirebaseEmulatorSetup.configure()
    }
  }
}
