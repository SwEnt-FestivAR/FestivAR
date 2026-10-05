package com.android.festivar.model.task

import com.android.festivar.model.temporary.User
import java.time.Duration
import java.time.LocalDateTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskTest {

  // Messages of the `init` block of Task, in the order in which the rules are checked.
  private val taskIdMessage = "The taskId cannot be empty."
  private val eventIdMessage = "The eventId cannot be empty."
  private val titleMessage = "The title cannot be empty."
  private val maxAssignMessage = "maxAssign must be at least 1."
  private val tooManyAssigneesMessage = "maxAssign is lower than the number of assigned people."
  private val duplicateMessage = "A user is assigned twice."
  private val timeMessage = "endTime must not be before startTime."
  private val estimatedTimeMessage = "estimatedTime cannot be negative."

  private val start = LocalDateTime.of(2026, 7, 1, 14, 0)

  /** Strings that are blank, so refused for taskId, eventId and title. */
  private val blankStrings = listOf("", " ", "   ", "\t", "\n", " \t\n ")

  /** Builds a list of [n] different users. */
  private fun users(n: Int): List<User> = (0 until n).map { User("user$it") }

  /** Checks that [build] throws an [IllegalArgumentException] with [expectedMessage]. */
  private fun assertInvalid(expectedMessage: String, build: () -> Task) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------- //
  // Valid tasks
  // ---------------------------------------------------------------------------------------- //

  /** A task with only the required fields is valid and gets the default values. */
  @Test
  fun init_succeeds_withOnlyRequiredFields() {
    val task = Task(taskId = "t1", eventId = "e1", title = "Task")

    assertEquals("t1", task.taskId)
    assertEquals("e1", task.eventId)
    assertEquals("Task", task.title)
    assertEquals("", task.description)
    assertNull(task.startTime)
    assertNull(task.endTime)
    assertNull(task.estimatedTime)
    assertEquals("", task.location)
    assertEquals(Priority.NONE, task.priority)
    assertEquals(1, task.maxAssign)
    assertTrue(task.assignees.isEmpty())
    assertFalse(task.completed)
  }

  /** A task with every field set to a valid value is valid, for every priority. */
  @Test
  fun init_succeeds_withAllFieldsValid_forEveryPriority() {
    for (priority in Priority.entries) {
      val task =
          Task(
              taskId = "t1",
              eventId = "e1",
              title = "Task",
              description = "Description",
              startTime = start,
              endTime = start.plusHours(2),
              estimatedTime = Duration.ofHours(1),
              location = "EPFL",
              priority = priority,
              maxAssign = 3,
              assignees = users(3),
          )
      assertEquals(priority, task.priority)
      assertEquals(users(3), task.assignees)
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // taskId, eventId and title
  // ---------------------------------------------------------------------------------------- //

  /** A blank taskId is refused. */
  @Test
  fun init_fails_whenTaskIdIsBlank() {
    for (blank in blankStrings) {
      assertInvalid(taskIdMessage) { Task(taskId = blank, eventId = "e1", title = "Task") }
    }
  }

  /** A blank eventId is refused. */
  @Test
  fun init_fails_whenEventIdIsBlank() {
    for (blank in blankStrings) {
      assertInvalid(eventIdMessage) { Task(taskId = "t1", eventId = blank, title = "Task") }
    }
  }

  /** A blank title is refused. */
  @Test
  fun init_fails_whenTitleIsBlank() {
    for (blank in blankStrings) {
      assertInvalid(titleMessage) { Task(taskId = "t1", eventId = "e1", title = blank) }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // maxAssign and assignees
  // ---------------------------------------------------------------------------------------- //

  /** maxAssign lower than 1 is refused. */
  @Test
  fun init_fails_whenMaxAssignIsLowerThanOne() {
    for (maxAssign in (-100..0) + Int.MIN_VALUE) {
      assertInvalid(maxAssignMessage) {
        Task(taskId = "t1", eventId = "e1", title = "Task", maxAssign = maxAssign)
      }
    }
  }

  /** maxAssign of at least 1 is accepted. */
  @Test
  fun init_succeeds_whenMaxAssignIsAtLeastOne() {
    for (maxAssign in (1..100) + Int.MAX_VALUE) {
      val task = Task(taskId = "t1", eventId = "e1", title = "Task", maxAssign = maxAssign)
      assertEquals(maxAssign, task.maxAssign)
    }
  }

  /** More assignees than maxAssign is refused. */
  @Test
  fun init_fails_whenMoreAssigneesThanMaxAssign() {
    for (maxAssign in 1..10) {
      for (size in maxAssign + 1..maxAssign + 5) {
        assertInvalid(tooManyAssigneesMessage) {
          Task(
              taskId = "t1",
              eventId = "e1",
              title = "Task",
              maxAssign = maxAssign,
              assignees = users(size),
          )
        }
      }
    }
  }

  /** A user assigned twice is refused, wherever the duplicate is in the list. */
  @Test
  fun init_fails_whenAUserIsAssignedTwice() {
    for (size in 1..10) {
      val distinctUsers = users(size)
      for (duplicated in distinctUsers) {
        for (position in 0..size) {
          val assignees = distinctUsers.toMutableList().apply { add(position, duplicated) }
          assertInvalid(duplicateMessage) {
            Task(
                taskId = "t1",
                eventId = "e1",
                title = "Task",
                maxAssign = assignees.size,
                assignees = assignees,
            )
          }
        }
      }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // startTime and endTime
  // ---------------------------------------------------------------------------------------- //

  /** startTime and endTime can each be null, whatever the other one is. */
  @Test
  fun init_succeeds_whenStartTimeOrEndTimeIsNull() {
    val times = listOf(null, start, start.minusYears(1), start.plusYears(1))
    for (startTime in times) {
      for (endTime in times) {
        if (startTime != null && endTime != null) continue
        val task =
            Task(
                taskId = "t1",
                eventId = "e1",
                title = "Task",
                startTime = startTime,
                endTime = endTime,
            )
        assertEquals(startTime, task.startTime)
        assertEquals(endTime, task.endTime)
      }
    }
  }

  /** An endTime equal to or after startTime is accepted. */
  @Test
  fun init_succeeds_whenEndTimeIsNotBeforeStartTime() {
    for (minutes in 0L..600L) {
      val task =
          Task(
              taskId = "t1",
              eventId = "e1",
              title = "Task",
              startTime = start,
              endTime = start.plusMinutes(minutes),
          )
      assertEquals(start.plusMinutes(minutes), task.endTime)
    }
  }

  /** An endTime before startTime is refused, even by a single nanosecond. */
  @Test
  fun init_fails_whenEndTimeIsBeforeStartTime() {
    for (minutes in 1L..600L) {
      assertInvalid(timeMessage) {
        Task(
            taskId = "t1",
            eventId = "e1",
            title = "Task",
            startTime = start,
            endTime = start.minusMinutes(minutes),
        )
      }
    }

    assertInvalid(timeMessage) {
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Task",
          startTime = start,
          endTime = start.minusNanos(1),
      )
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // estimatedTime
  // ---------------------------------------------------------------------------------------- //

  /** A null, zero or positive estimatedTime is accepted. */
  @Test
  fun init_succeeds_whenEstimatedTimeIsNullOrNotNegative() {
    val durations =
        listOf(null, Duration.ZERO, Duration.ofNanos(1)) + (1L..600L).map { Duration.ofMinutes(it) }
    for (duration in durations) {
      val task = Task(taskId = "t1", eventId = "e1", title = "Task", estimatedTime = duration)
      assertEquals(duration, task.estimatedTime)
    }
  }

  /** A negative estimatedTime is refused. */
  @Test
  fun init_fails_whenEstimatedTimeIsNegative() {
    val durations = listOf(Duration.ofNanos(-1)) + (1L..600L).map { Duration.ofMinutes(-it) }
    for (duration in durations) {
      assertInvalid(estimatedTimeMessage) {
        Task(taskId = "t1", eventId = "e1", title = "Task", estimatedTime = duration)
      }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // copy goes through init
  // ---------------------------------------------------------------------------------------- //

  /** copy() goes through the init block, so it cannot create an invalid task. */
  @Test
  fun copy_fails_whenTheCopyBreaksARule() {
    val task =
        Task(
            taskId = "t1",
            eventId = "e1",
            title = "Task",
            startTime = start,
            endTime = start.plusHours(1),
            maxAssign = 2,
            assignees = users(2),
        )

    assertInvalid(taskIdMessage) { task.copy(taskId = "") }
    assertInvalid(eventIdMessage) { task.copy(eventId = "") }
    assertInvalid(titleMessage) { task.copy(title = " ") }
    assertInvalid(maxAssignMessage) { task.copy(maxAssign = 0) }
    assertInvalid(tooManyAssigneesMessage) { task.copy(maxAssign = 1) }
    assertInvalid(duplicateMessage) { task.copy(assignees = List(2) { User("same") }) }
    assertInvalid(timeMessage) { task.copy(endTime = start.minusHours(1)) }
    assertInvalid(estimatedTimeMessage) { task.copy(estimatedTime = Duration.ofHours(-1)) }
  }

  // **************************************************************************************//
  // complete, addAssignee and removeAssignee
  // **************************************************************************************//

  private val taskFullMessage = "The task is full."
  private val alreadyAssignedMessage = "This user is already assigned to the task."
  private val notAssignedMessage = "This user is not assigned to the task."

  /** A user that is never part of the lists built by [users]. */
  private val outsider = User("outsider")

  /** Builds a valid task with every field set, so we can check that the functions keep them. */
  private fun taskWith(
      maxAssign: Int,
      assignees: List<User> = emptyList(),
  ): Task =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Task",
          description = "Description",
          startTime = start,
          endTime = start.plusHours(2),
          estimatedTime = Duration.ofHours(1),
          location = "EPFL",
          priority = Priority.HIGH,
          maxAssign = maxAssign,
          assignees = assignees,
      )

  /** Checks that [block] throws an exception of class [type] with [expectedMessage]. */
  private fun <T : Throwable> assertThrowsWithMessage(
      type: Class<T>,
      expectedMessage: String,
      block: () -> Unit,
  ) {
    val exception = assertThrows(type) { block() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------- //
  // complete
  // ---------------------------------------------------------------------------------------- //

  /**
   * complete returns a completed copy, keeps every other field and does not change the original.
   */
  @Test
  fun complete_returnsCompletedCopy_andKeepsOtherFields() {
    for (maxAssign in 1..10) {
      for (size in 0..maxAssign) {
        val task = taskWith(maxAssign = maxAssign, assignees = users(size))

        val completedTask = task.complete()

        assertTrue(completedTask.completed)
        assertEquals(task.copy(completed = true), completedTask)
        assertFalse(task.completed)
      }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // addAssignee
  // ---------------------------------------------------------------------------------------- //

  /**
   * addAssignee adds the users one by one at the end of the list until the task is full, keeps
   * every other field and never changes the previous version of the task.
   */
  @Test
  fun addAssignee_addsUsersOneByOne_untilTaskIsFull() {
    for (maxAssign in 1..10) {
      var task = taskWith(maxAssign = maxAssign)

      for ((i, user) in users(maxAssign).withIndex()) {
        val previous = task
        task = task.addAssignee(user)

        assertEquals(users(i + 1), task.assignees)
        assertEquals(previous.copy(assignees = users(i + 1)), task)
        assertEquals(users(i), previous.assignees)
      }
      assertEquals(maxAssign, task.assignees.size)
    }
  }

  /** addAssignee throws an IllegalStateException when the task is full. */
  @Test
  fun addAssignee_fails_whenTaskIsFull() {
    for (maxAssign in 1..10) {
      val task = taskWith(maxAssign = maxAssign, assignees = users(maxAssign))

      assertThrowsWithMessage(IllegalStateException::class.java, taskFullMessage) {
        task.addAssignee(outsider)
      }
      assertEquals(users(maxAssign), task.assignees) // The task is not modified.
    }
  }

  /** addAssignee throws an IllegalArgumentException when the user is already assigned. */
  @Test
  fun addAssignee_fails_whenUserIsAlreadyAssigned() {
    for (maxAssign in 2..10) {
      for (size in 1 until maxAssign) { // Never full, so only the "already assigned" rule fails.
        val task = taskWith(maxAssign = maxAssign, assignees = users(size))

        for (user in task.assignees) {
          assertThrowsWithMessage(IllegalArgumentException::class.java, alreadyAssignedMessage) {
            task.addAssignee(user)
          }
        }
        assertEquals(users(size), task.assignees) // The task is not modified.
      }
    }
  }

  /** A user equal to an assigned one (same uid, other object) is also refused. */
  @Test
  fun addAssignee_fails_whenAnEqualUserIsAlreadyAssigned() {
    val task = taskWith(maxAssign = 3, assignees = listOf(User("same")))

    assertThrowsWithMessage(IllegalArgumentException::class.java, alreadyAssignedMessage) {
      task.addAssignee(User("same"))
    }
  }

  /**
   * When the task is full and the user is already assigned, the "full" check comes first, so an
   * IllegalStateException is thrown.
   */
  @Test
  fun addAssignee_fails_withTaskFull_whenFullAndUserAlreadyAssigned() {
    for (maxAssign in 1..10) {
      val task = taskWith(maxAssign = maxAssign, assignees = users(maxAssign))

      for (user in task.assignees) {
        assertThrowsWithMessage(IllegalStateException::class.java, taskFullMessage) {
          task.addAssignee(user)
        }
      }
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // removeAssignee
  // ---------------------------------------------------------------------------------------- //

  /**
   * removeAssignee removes only the given user, keeps the order of the others and every other
   * field, and does not change the original task. Tested for every user of lists of 1 to 10 users.
   */
  @Test
  fun removeAssignee_removesOnlyTheGivenUser() {
    for (size in 1..10) {
      val task = taskWith(maxAssign = size, assignees = users(size))

      for (user in task.assignees) {
        val result = task.removeAssignee(user)

        assertEquals(users(size) - user, result.assignees) // Others kept, in the same order.
        assertFalse(result.assignees.contains(user))
        assertEquals(task.copy(assignees = users(size) - user), result) // Only assignees changed.
        assertEquals(users(size), task.assignees) // The original task is not modified.
      }
    }
  }

  /** removeAssignee throws an IllegalArgumentException when the user is not assigned. */
  @Test
  fun removeAssignee_fails_whenUserIsNotAssigned() {
    for (size in 0..10) {
      val task = taskWith(maxAssign = maxOf(size, 1), assignees = users(size))

      for (user in listOf(outsider, User("user$size"), User("user-1"))) {
        assertThrowsWithMessage(IllegalArgumentException::class.java, notAssignedMessage) {
          task.removeAssignee(user)
        }
      }
      assertEquals(users(size), task.assignees) // The task is not modified.
    }
  }
}
