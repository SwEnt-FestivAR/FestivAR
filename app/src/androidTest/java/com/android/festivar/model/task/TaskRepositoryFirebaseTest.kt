// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.task

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

    assertTrue(first.isNotEmpty())
    assertNotEquals(first, second)
  }

  @Test
  fun getAllTasks_returnsStoredTasks() = runBlocking {
    repository.addTask(task)

    assertEquals(listOf(task), repository.getAllTasks())
  }

  @Test
  fun getAllTasks_returnsEmptyListWhenNoTasksExist() = runBlocking {
    assertTrue(repository.getAllTasks().isEmpty())
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
  fun addTask_storesTask() = runBlocking {
    repository.addTask(task)

    assertEquals(task, repository.getTask(task.taskId))
  }

  @Test
  fun addTask_throwsWhenTaskIdAlreadyExists() = runBlocking {
    repository.addTask(task)

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
          assignees = listOf(com.android.festivar.model.temporary.User("user-1")),
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

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      Firebase.firestore.useEmulator("10.0.2.2", 8080)
    }
  }
}
