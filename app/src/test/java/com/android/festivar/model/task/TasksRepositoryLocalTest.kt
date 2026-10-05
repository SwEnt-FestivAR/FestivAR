package com.android.festivar.model.task

import java.time.Duration
import java.time.LocalDateTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class TasksRepositoryLocalTest {
  private lateinit var tasksRepositoryLocal: TasksRepositoryLocal

  private val task =
      Task(
          taskId = "1",
          eventId = "event1",
          title = "Task",
          description = "Do something",
          startTime = LocalDateTime.of(2026, 7, 1, 14, 0),
          endTime = LocalDateTime.of(2026, 7, 1, 16, 0),
          estimatedTime = Duration.ofHours(1),
          location = "EPFL",
          priority = Priority.MEDIUM,
          maxAssign = 4,
      )

  @Before
  fun setUp() {
    tasksRepositoryLocal = TasksRepositoryLocal()
  }

  /**
   * This test verifies that getNewUid generates a non-empty identifier, and that a second call to
   * getNewUid generates a different identifier.
   */
  @Test
  fun correctlyGeneratesNewUID() {
    val taskId = tasksRepositoryLocal.getNewUid()
    assertTrue(taskId.isNotEmpty())

    val anotherUid = tasksRepositoryLocal.getNewUid()
    assertTrue(taskId != anotherUid)
    assertTrue(anotherUid.isNotEmpty())
  }

  /**
   * This test verifies that addTask successfully adds a Task item to the local repository. It also
   * tests that getAllTasks and getTask successfully retrieve the tasks.
   */
  @Test
  fun addTask_succeeds() = runTest {
    tasksRepositoryLocal.addTask(task)

    // Verify that the Task was added
    val tasks = tasksRepositoryLocal.getAllTasks()
    assertTrue(tasks.contains(task)) // Ensure the task is present
    assertEquals(1, tasks.size) // Ensure only one task is present

    val retrievedTask = tasksRepositoryLocal.getTask(task.taskId)
    assertEquals(task, retrievedTask)
  }

  /**
   * This test verifies that editTask successfully updates an existing Task item in the local
   * repository. It also checks that the old Task item is no longer present and the updated item is
   * present with the correct updated values.
   */
  @Test
  fun editTask_succeeds() = runTest {
    tasksRepositoryLocal.addTask(task)

    val updatedTask = task.copy(title = "Updated Task")

    tasksRepositoryLocal.editTask(task.taskId, updatedTask)

    // Verify that the Task was updated
    val tasks = tasksRepositoryLocal.getAllTasks()
    assertTrue(tasks.contains(updatedTask)) // Ensure the updated task is present
    assertTrue(!tasks.contains(task)) // Ensure the old task is not present
    assertEquals(1, tasks.size) // Ensure only one task is present
  }

  /**
   * This test verifies that editTask throws an exception when trying to update a Task item that
   * does not exist in the local repository.
   */
  @Test
  fun editTask_failsWhenTaskNotFound() {
    assertThrows(NoSuchElementException::class.java) {
      runTest { tasksRepositoryLocal.editTask(task.taskId, task) }
    }
  }

  /**
   * This test verifies that editTask throws an IllegalArgumentException when newValue does not have
   * the same identifier as taskId, and that no task is overwritten.
   */
  @Test
  fun editTask_fails_whenNewValueHasDifferentTaskId() = runTest {
    val task2 = task.copy(taskId = "2", title = "Second Task")
    tasksRepositoryLocal.addTask(task)
    tasksRepositoryLocal.addTask(task2)

    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { tasksRepositoryLocal.editTask(task.taskId, task2.copy(title = "Overwrite")) }
    }

    assertEquals(listOf(task, task2), tasksRepositoryLocal.getAllTasks())
  }

  /**
   * This test verifies that deleteTask successfully removes a Task item from the local repository,
   * and that getTask then throws an exception for it.
   */
  @Test
  fun deleteTaskById_callsOnSuccess() = runTest {
    tasksRepositoryLocal.addTask(task)

    tasksRepositoryLocal.deleteTask(task.taskId)

    // Verify that the Task was deleted
    val tasks = tasksRepositoryLocal.getAllTasks()
    assertTrue(!tasks.contains(task)) // Ensure the task is not present
    assertEquals(0, tasks.size) // Ensure no tasks are present

    assertThrows(NoSuchElementException::class.java) {
      runBlocking { tasksRepositoryLocal.getTask(task.taskId) }
    }
  }

  /** This test verifies that deleteTask only removes the Task item with the given identifier. */
  @Test
  fun deleteTaskById_deletesTheCorrectTask() = runTest {
    val task2 = task.copy(taskId = "2", title = "Second Task")
    tasksRepositoryLocal.addTask(task)
    tasksRepositoryLocal.addTask(task2)

    tasksRepositoryLocal.deleteTask(task.taskId)

    // Verify that the correct Task was deleted
    val tasks = tasksRepositoryLocal.getAllTasks()
    assertTrue(!tasks.contains(task)) // Ensure the first task is not present
    assertTrue(tasks.contains(task2)) // Ensure the second task is still present
    assertTrue(tasks.size == 1) // Ensure that it has not created or duplicated an element
  }

  /**
   * This test verifies that deleteTask throws an exception when trying to delete a Task item that
   * does not exist in the local repository.
   */
  @Test
  fun deleteTaskById_callsOnFailure_whenTaskNotFound() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { tasksRepositoryLocal.deleteTask("non-existent-id") }
    }
  }

  /**
   * This test verifies that getAllTasks returns an empty list if no further Task add were done in
   * the repository.
   */
  @Test
  fun getAllTasks_returnEmptyList_whenCalledAtBeginning() = runTest {
    val tasks = tasksRepositoryLocal.getAllTasks()
    assertTrue(tasks.isEmpty())
  }

  /**
   * This test verifies that getTask throws an exception when trying to get a Task from an empty
   * repository.
   */
  @Test
  fun getTaskById_callsOnFailure_whenRepositoryEmpty() {
    assertThrows(NoSuchElementException::class.java) { runBlocking { tasksRepositoryLocal.getTask("") } }
  }

  /** This test verifies that getTask returns the exact same Task that was added previously. */
  @Test
  fun getTaskById_callsOnSuccess_sameObjectGet() = runTest {
    tasksRepositoryLocal.addTask(task)
    assertEquals(task, tasksRepositoryLocal.getTask(task.taskId))
  }

  // **************************************************************************************//

  /** Number of tasks used by the tests below that fill the repository with many tasks. */
  private val manyTasksCount = 50

  /** Builds the i-th test task: a copy of [task] with a unique identifier and title. */
  private fun taskNumber(i: Int): Task = task.copy(taskId = "task$i", title = "Task $i")

  /** Adds [manyTasksCount] different tasks to the repository and returns them, in order. */
  private suspend fun addManyTasks(): List<Task> {
    val tasks = (0 until manyTasksCount).map { taskNumber(it) }
    for (t in tasks) {
      tasksRepositoryLocal.addTask(t)
    }
    return tasks
  }

  /**
   * Fails the test if [block] does not throw a [NoSuchElementException]. Any other exception is not
   * caught, so it also fails the test. Usable inside `runTest`.
   */
  private suspend fun assertThrowsSuspend(block: suspend () -> Unit) {
    try {
      block()
    } catch (e: NoSuchElementException) {
      return
    }
    fail("A NoSuchElementException was expected but none was thrown.")
  }

  /**
   * This test verifies that many successive calls to getNewUid all return different, non-empty
   * identifiers.
   */
  @Test
  fun getNewUid_generatesManyUniqueIds() {
    val ids = mutableSetOf<String>()
    for (i in 0 until 1000) {
      val id = tasksRepositoryLocal.getNewUid()
      assertTrue(id.isNotEmpty())
      assertTrue("The identifier $id was generated twice.", ids.add(id))
    }
    assertEquals(1000, ids.size)
  }

  /** This test verifies that tasks created with identifiers from getNewUid can all be retrieved. */
  @Test
  fun getNewUid_identifiersCanBeUsedToAddAndRetrieveTasks() = runTest {
    val tasks =
        (0 until manyTasksCount).map { task.copy(taskId = tasksRepositoryLocal.getNewUid()) }
    for (t in tasks) {
      tasksRepositoryLocal.addTask(t)
    }
    for (t in tasks) {
      assertEquals(t, tasksRepositoryLocal.getTask(t.taskId))
    }
  }

  /**
   * This test verifies that after each addTask, the new task is present and the size grows by one.
   */
  @Test
  fun addTask_manyTasks_sizeGrowsAndAllTasksPresent() = runTest {
    val added = mutableListOf<Task>()
    for (i in 0 until manyTasksCount) {
      val t = taskNumber(i)
      tasksRepositoryLocal.addTask(t)
      added.add(t)

      val tasks = tasksRepositoryLocal.getAllTasks()
      assertEquals(i + 1, tasks.size)
      for (previous in added) {
        assertTrue(tasks.contains(previous))
      }
    }
  }

  /**
   * This test verifies that getAllTasks returns all the added tasks, in the order they were added.
   */
  @Test
  fun getAllTasks_manyTasks_returnsAllTasksInOrder() = runTest {
    val tasks = addManyTasks()
    assertEquals(tasks, tasksRepositoryLocal.getAllTasks())
  }

  /**
   * This test verifies that getTask returns the right task for every identifier in a full
   * repository.
   */
  @Test
  fun getTask_manyTasks_returnsEachCorrectTask() = runTest {
    val tasks = addManyTasks()
    for (t in tasks) {
      assertEquals(t, tasksRepositoryLocal.getTask(t.taskId))
    }
  }

  /**
   * This test verifies that getTask throws for many unknown identifiers, even when the repository
   * is full.
   */
  @Test
  fun getTask_manyTasks_failsForUnknownIds() = runTest {
    addManyTasks()
    for (i in manyTasksCount until manyTasksCount * 2) {
      assertThrowsSuspend { tasksRepositoryLocal.getTask("task$i") }
    }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("unknown") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("pikachu") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("glbstkf") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("pokemon") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("rayquaza") }
    assertThrowsSuspend {
      tasksRepositoryLocal.getTask("IL ETAIT UN PETIT NAVIREUH IL ETAIT UN PETIT NAVIIIIIRE")
    }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("Swent EPFL") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("task1\n") }
    assertThrowsSuspend { tasksRepositoryLocal.getTask("task1\r\n") }
  }

  /**
   * This test verifies that editTask throws for unknown identifiers and leaves the repository
   * unchanged.
   */
  @Test
  fun editTask_manyTasks_failsForUnknownIdsAndChangesNothing() = runTest {
    val tasks = addManyTasks()
    for (i in manyTasksCount until manyTasksCount * 2) {
      assertThrowsSuspend { tasksRepositoryLocal.editTask("task$i", taskNumber(i)) }
    }
    assertEquals(tasks, tasksRepositoryLocal.getAllTasks())
  }

  /** This test verifies that deleting every task one by one removes exactly one task each time. */
  @Test
  fun deleteTask_manyTasks_deletesOneByOne() = runTest {
    val remaining = addManyTasks().toMutableList()

    while (remaining.isNotEmpty()) {
      val deleted = remaining.removeAt(0)
      tasksRepositoryLocal.deleteTask(deleted.taskId)

      val tasks = tasksRepositoryLocal.getAllTasks().toList()
      assertEquals(remaining.size, tasks.size)
      assertFalse(tasks.contains(deleted))
      assertEquals(remaining, tasks)
      assertThrowsSuspend { tasksRepositoryLocal.getTask(deleted.taskId) }
    }
    assertTrue(tasksRepositoryLocal.getAllTasks().isEmpty())
  }

  /**
   * This test verifies that deleteTask throws for unknown or already deleted identifiers and
   * changes nothing.
   */
  @Test
  fun deleteTask_manyTasks_failsForUnknownOrAlreadyDeletedIds() = runTest {
    val tasks = addManyTasks()
    for (i in manyTasksCount until manyTasksCount * 2) {
      assertThrowsSuspend { tasksRepositoryLocal.deleteTask("task$i") }
    }
    assertEquals(tasks, tasksRepositoryLocal.getAllTasks())

    tasksRepositoryLocal.deleteTask(tasks[0].taskId)
    assertThrowsSuspend { tasksRepositoryLocal.deleteTask(tasks[0].taskId) }
  }
}
