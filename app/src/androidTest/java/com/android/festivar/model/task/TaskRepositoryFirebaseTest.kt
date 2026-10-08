package com.android.festivar.model.task

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.temporary.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskRepositoryFirebaseTest {
  private lateinit var repository: TaskRepositoryFirebase
  private lateinit var task: Task

  @Before
  fun setUp() {
    Firebase.auth.useEmulator("10.0.2.2", 9099)
    runBlocking { Firebase.auth.signInAnonymously().await() }
    task = newTask()
    repository = TaskRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      if (::task.isInitialized) {
        Firebase.firestore.collection(TASK_COLLECTION_PATH).document(task.taskId).delete().await()
      }
      Firebase.auth.signOut()
    }
  }

  @Test
  fun getNewUid_returnsNonEmptyUniqueIds() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    Assert.assertTrue(first.isNotEmpty())
    Assert.assertNotEquals(first, second)
  }

  @Test
  fun toFirestoreData_serializesTaskFields() {
    val data = invokeToFirestoreData(task)

    Assert.assertEquals(task.taskId, data["taskId"])
    Assert.assertEquals(task.eventId, data["eventId"])
    Assert.assertEquals(task.title, data["title"])
    Assert.assertEquals(task.description, data["description"])
    Assert.assertEquals(task.startTime.toString(), data["startTime"])
    Assert.assertEquals(task.endTime.toString(), data["endTime"])
    Assert.assertEquals(task.estimatedTime!!.toNanos(), data["estimatedTime"])
    Assert.assertEquals(task.location, data["location"])
    Assert.assertEquals(task.priority.name, data["priority"])
    Assert.assertEquals(task.maxAssign, data["maxAssign"])
    Assert.assertEquals(task.assignees.map { it.uid }, data["assignees"])
    Assert.assertEquals(task.completed, data["completed"])
  }

  @Test
  fun toFirestoreData_serializesOptionalFieldsAsNull() {
    val taskWithoutOptionalFields =
        task.copy(startTime = null, endTime = null, estimatedTime = null)

    val data = invokeToFirestoreData(taskWithoutOptionalFields)

    Assert.assertNull(data["startTime"])
    Assert.assertNull(data["endTime"])
    Assert.assertNull(data["estimatedTime"])
  }

  @Test
  fun documentToTask_deserializesStoredTask() = runBlocking {
    val data = invokeToFirestoreData(task)
    val document = Firebase.firestore.collection(TASK_COLLECTION_PATH).document(task.taskId)
    document.set(data).await()

    Assert.assertEquals(task, invokeDocumentToTask(document.get().await()))
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
                "assignees" to listOf(mapOf("uid" to "user-2")),
            )
        )
        .await()

    try {
      Assert.assertEquals(
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

    Assert.assertEquals(listOf(task), repository.getAllTasks(task.eventId))
  }

  @Test
  fun getAllTasks_returnsEmptyListWhenNoTasksExist() = runBlocking {
    Assert.assertTrue(repository.getAllTasks(task.eventId).isEmpty())
  }

  @Test
  fun getAllTasks_filtersByEventId() = runBlocking {
    val otherTask = task.copy(taskId = "task-${UUID.randomUUID()}", eventId = "event-2")
    repository.addTask(task)
    repository.addTask(otherTask)

    try {
      Assert.assertEquals(listOf(task), repository.getAllTasks(task.eventId))
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

    Assert.assertEquals(task, repository.getTask(task.taskId))
  }

  @Test
  fun getTask_throwsWhenTaskDoesNotExist() = runBlocking {
    assertThrows { repository.getTask(task.taskId) }
  }

  @Test
  fun addTask_storesTaskAndThrowsWhenTaskIdAlreadyExists() = runBlocking {
    repository.addTask(task)

    Assert.assertEquals(task, repository.getTask(task.taskId))

    assertThrows { repository.addTask(task) }
  }

  @Test
  fun editTask_updatesExistingTask() = runBlocking {
    repository.addTask(task)
    val updatedTask = task.copy(title = "Updated task", completed = true)

    repository.editTask(task.taskId, updatedTask)

    Assert.assertEquals(updatedTask, repository.getTask(task.taskId))
  }

  @Test
  fun editTask_throwsWhenTaskIdDiffers() = runBlocking {
    assertThrows { repository.editTask(task.taskId, task.copy(taskId = "other-task")) }
  }

  @Test
  fun editTask_throwsWhenTaskDoesNotExist() = runBlocking {
    assertThrows { repository.editTask(task.taskId, task) }
  }

  @Test
  fun deleteTask_removesTask() = runBlocking {
    repository.addTask(task)

    repository.deleteTask(task.taskId)

    assertThrows { repository.getTask(task.taskId) }
  }

  @Test
  fun deleteTask_throwsWhenTaskDoesNotExist() = runBlocking {
    assertThrows { repository.deleteTask(task.taskId) }
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
    Assert.assertTrue("Expected an exception", thrown)
  }

  @Suppress("UNCHECKED_CAST")
  private fun invokeToFirestoreData(task: Task): Map<String, Any?> {
    val method =
        TaskRepositoryFirebase::class.java.getDeclaredMethod("toFirestoreData", Task::class.java)
    method.isAccessible = true
    return method.invoke(repository, task) as Map<String, Any?>
  }

  private fun invokeDocumentToTask(document: DocumentSnapshot): Task {
    val method =
        TaskRepositoryFirebase::class
            .java
            .getDeclaredMethod(
                "documentToTask",
                DocumentSnapshot::class.java,
            )
    method.isAccessible = true
    return method.invoke(repository, document) as Task
  }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      Firebase.firestore.useEmulator("10.0.2.2", 8080)
    }
  }
}