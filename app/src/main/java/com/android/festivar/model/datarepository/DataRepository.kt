// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Coordinates the operations that span several repositories, so that [Event]s, [Task]s and [User]s
 * stay consistent with each other (for example an event member always lists the event in
 * [User.joinedEvents]).
 *
 * Every operation works in two phases:
 * 1. it reads the current data and computes every new version, so a rule broken by the model (for
 *    example a full task) fails before anything is written;
 * 2. it writes the new versions one by one, remembering how to undo each write. If a write fails,
 *    the writes already done are undone in reverse order, then the original exception is rethrown.
 *
 * The undo is not a transaction: it writes back the versions read in phase 1, so a change made by
 * someone else in the meantime can be overwritten. If an undo fails too, the data stays
 * inconsistent and a [DataInconsistencyException] is thrown.
 *
 * @property authRepository The repository handling the authentication.
 * @property tasksRepository The repository storing the [Task]s.
 * @property eventsRepository The repository storing the [Event]s.
 * @property usersRepository The repository storing the [User]s.
 */
interface DataRepository {

  val authRepository: AuthRepository
  val tasksRepository: TasksRepository
  val eventsRepository: EventsRepository
  val usersRepository: UsersRepository

  /**
   * Runs every undo, even if some fail, without being canceled by the calling coroutine.
   *
   * @param undoProcess The undos to run, in the order they must run (the last write first).
   * @param cause The exception that made the operation fail.
   * @throws DataInconsistencyException If any undo failed. Its cause is [cause] and the undo
   *   failures are added as suppressed exceptions.
   */
  private suspend fun rollback(undoProcess: List<suspend () -> Unit>, cause: Exception) =
      withContext(NonCancellable) {
        val failures = undoProcess.mapNotNull { undo -> runCatching { undo() }.exceptionOrNull() }
        if (failures.isNotEmpty()) {
          throw DataInconsistencyException("Operation failed and could not be fully undone", cause)
              .apply { failures.forEach(::addSuppressed) }
        }
      }

  /**
   * Stores a new [Task], adds it to its [Event] and to the [User.tasksAssigned] of its owner and of
   * each of its assignees.
   *
   * This function does not check that the owner and the assignees are members of the [Event]: the
   * caller is responsible for it.
   *
   * @param task The new [Task]. Its [Task.eventId] must be an existing [Event].
   * @throws NoSuchElementException If the event, the owner or an assignee does not exist.
   * @throws IllegalArgumentException If the task already exists or is already in the event, or if
   *   its owner or one of its assignees already lists it in [User.tasksAssigned].
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun addTask(task: Task) {
    // Get the necessary objects.
    val event = eventsRepository.getEvent(task.eventId)
    val userIds = (listOf(task.ownerId) + task.assignees.map { it.uid }).distinct()
    val users = usersRepository.getListUsers(userIds)

    // Compute the new versions that have to be stored.
    val newEvent = event.addTask(task)
    val newUsers = users.map { it.addAssignedTask(task.taskId) }

    // Store the changes.
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {
      tasksRepository.addTask(task)
      undoProcess.addFirst { tasksRepository.deleteTask(task.taskId) }

      eventsRepository.editEvent(event.eventId, newEvent)
      undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

      editUsers(users, newUsers, undoProcess)
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Deletes a [Task], removes it from its [Event] and from the [User.tasksAssigned] of its owner
   * and of each of its assignees. If the owner no longer lists the task (for example after
   * [quitTask] or [quitEvent]), their [User.tasksAssigned] is left unchanged.
   *
   * @param taskId The identifier of the [Task] to delete.
   * @throws NoSuchElementException If the task, its event, its owner or an assignee does not exist.
   * @throws IllegalArgumentException If the event does not contain the task, or if an assignee who
   *   is not the owner does not list it in [User.tasksAssigned].
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun deleteTask(taskId: String) {
    // Get the necessary objects.
    val task = tasksRepository.getTask(taskId)
    val event = eventsRepository.getEvent(task.eventId)
    val userIds = task.assignees.map { it.uid }.distinct().filter { it != task.ownerId }
    val users = usersRepository.getListUsers(userIds)
    val ownerUserId = task.ownerId
    val ownerUser = usersRepository.getUser(ownerUserId)

    // Compute the new versions that have to be stored.
    val newEvent = event.removeTask(taskId)
    val newUsers = users.map { it.removeAssignedTask(taskId) }
    val newOwnerUser =
        if (ownerUser.tasksAssigned.contains(taskId)) {
          ownerUser.removeAssignedTask(taskId)
        } else {
          ownerUser
        }

    // Store the changes.
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {

      tasksRepository.deleteTask(taskId)
      undoProcess.addFirst { tasksRepository.addTask(task) }

      eventsRepository.editEvent(event.eventId, newEvent)
      undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

      editUsers(users, newUsers, undoProcess)

      usersRepository.editUser(ownerUserId, newOwnerUser)
      undoProcess.addFirst { usersRepository.editUser(ownerUserId, ownerUser) }
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Stores a new [Event] and adds it to the [User.joinedEvents] of each of its members.
   *
   * @param event The new [Event]. It must not contain any task yet: tasks are added afterwards with
   *   [addTask].
   * @throws IllegalArgumentException If the event already contains tasks, if it already exists, or
   *   if a member has already joined it.
   * @throws NoSuchElementException If a member does not exist.
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun createEvent(event: Event) {
    require(event.tasks.isEmpty())

    // Get the necessary objects.
    val userIds = event.members.map { it.uid }
    val users = usersRepository.getListUsers(userIds)

    // Compute the new versions that have to be stored.
    val newUsers = users.map { it.addJoinedEvent(event.eventId) }

    // Store the changes.
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {
      eventsRepository.addEvent(event)
      undoProcess.addFirst { eventsRepository.deleteEvent(event.eventId) }

      editUsers(users, newUsers, undoProcess)
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Deletes an [Event] and all its [Task]s, then removes the event from the [User.joinedEvents] of
   * each member and its tasks from their [User.tasksAssigned].
   *
   * Only the members are updated: a user assigned to one of the tasks without being a member of the
   * event keeps the deleted task in [User.tasksAssigned].
   *
   * @param eventId The identifier of the [Event] to delete.
   * @throws NoSuchElementException If the event, one of its tasks or a member does not exist.
   * @throws IllegalArgumentException If a member does not list the event in [User.joinedEvents].
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun deleteEvent(eventId: String) {

    // Get the necessary objects.
    val event = eventsRepository.getEvent(eventId)
    val tasks = tasksRepository.getAllTasks(eventId)
    val userIds = event.members.map { it.uid }
    val users = usersRepository.getListUsers(userIds)

    // Compute the new versions that have to be stored.
    val newUsers = users.map {
      var newIt = it
      for (task in tasks) {
        if (newIt.tasksAssigned.contains(task.taskId)) {
          newIt = newIt.removeAssignedTask(task.taskId)
        }
      }
      newIt = newIt.removeJoinedEvent(eventId)
      newIt
    }

    // Store the changes.
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {

      eventsRepository.deleteEvent(eventId)
      undoProcess.addFirst { eventsRepository.addEvent(event) }

      for (task in tasks) {
        tasksRepository.deleteTask(task.taskId)
        undoProcess.addFirst { tasksRepository.addTask(task) }
      }

      editUsers(users, newUsers, undoProcess)
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Adds a [User] to the members of an [Event] and the event to the user's [User.joinedEvents].
   *
   * @param eventId The identifier of the [Event] to join.
   * @param userId The [User.uid] of the user joining it.
   * @throws NoSuchElementException If the event or the user does not exist.
   * @throws IllegalArgumentException If the user is already a member of the event.
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun joinEvent(eventId: String, userId: String) {

    // Get the necessary objects.
    val event = eventsRepository.getEvent(eventId)
    val user = usersRepository.getUser(userId)

    // Compute the new versions that have to be stored.
    val newUser = user.addJoinedEvent(eventId)
    val newEvent = event.addMember(newUser)

    // Store the changes.
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {

      eventsRepository.editEvent(eventId, newEvent)
      undoProcess.addFirst { eventsRepository.editEvent(eventId, event) }

      usersRepository.editUser(userId, newUser)
      undoProcess.addFirst { usersRepository.editUser(userId, user) }
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Removes a [User] from the members of an [Event] and from the assignees of each of its [Task]s,
   * then removes the event from the user's [User.joinedEvents] and its tasks from
   * [User.tasksAssigned].
   *
   * The user stays the owner of the tasks they created: [Task.ownerId] is not changed, but these
   * tasks are still removed from their [User.tasksAssigned].
   *
   * The [Event.tasks] of the stored event are replaced by the tasks stored in [tasksRepository].
   *
   * @param eventId The identifier of the [Event] to quit.
   * @param userId The [User.uid] of the user quitting it.
   * @throws NoSuchElementException If the event or the user does not exist.
   * @throws IllegalArgumentException If the user is not a member of the event.
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun quitEvent(eventId: String, userId: String) {

    // Get the necessary objects.
    val event = eventsRepository.getEvent(eventId)
    val user = usersRepository.getUser(userId)
    val tasks = tasksRepository.getAllTasks(eventId)

    // Compute the new versions that have to be stored.
    val newTasks = tasks.map {
      if (it.assignees.map { it2 -> it2.uid }.contains(userId)) {
        it.removeAssignee(userId)
      } else {
        it
      }
    }
    val newEvent = event.copy(tasks = newTasks).removeMember(userId)
    var newUser = user.removeJoinedEvent(eventId)
    for (task in tasks) {
      if (newUser.tasksAssigned.contains(task.taskId)) {
        newUser = newUser.removeAssignedTask(task.taskId)
      }
    }

    // Store the changes.
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {

      eventsRepository.editEvent(eventId, newEvent)
      undoProcess.addFirst { eventsRepository.editEvent(eventId, event) }

      for ((old, new) in tasks.zip(newTasks)) {
        if (old != new) {
          tasksRepository.editTask(new.taskId, new)
          undoProcess.addFirst { tasksRepository.editTask(old.taskId, old) }
        }
      }

      usersRepository.editUser(userId, newUser)
      undoProcess.addFirst { usersRepository.editUser(userId, user) }
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Adds a [User] to the assignees of a [Task], in the task and in its [Event], and the task to the
   * user's [User.tasksAssigned]. If the user already lists the task (for example its owner), their
   * [User.tasksAssigned] is left unchanged.
   *
   * This function does not check that the user is a member of the [Event] the [Task] belongs to:
   * the caller is responsible for it.
   *
   * @param taskId The identifier of the [Task] to join.
   * @param userId The [User.uid] of the user joining it.
   * @throws NoSuchElementException If the task, its event or the user does not exist.
   * @throws IllegalStateException If the task is already full.
   * @throws IllegalArgumentException If the user is already assigned to the task.
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun joinTask(taskId: String, userId: String) {

    // Get the necessary objects.
    val task = tasksRepository.getTask(taskId)
    val user = usersRepository.getUser(userId)
    val event = eventsRepository.getEvent(task.eventId)

    // Compute the new versions that have to be stored.
    val newTask = task.addAssignee(user)
    val newUser =
        if (!user.tasksAssigned.contains(task.taskId)) {
          user.addAssignedTask(taskId)
        } else {
          user
        }

    val newEvent = event.copy(tasks = event.tasks.map { if (it.taskId == taskId) newTask else it })

    // Store the changes.
    changeAssignment(event, newEvent, task, newTask, user, newUser)
  }

  /**
   * Removes a [User] from the assignees of a [Task], in the task and in its [Event], and the task
   * from the user's [User.tasksAssigned], even if the user is its owner.
   *
   * @param taskId The identifier of the [Task] to quit.
   * @param userId The [User.uid] of the user quitting it.
   * @throws NoSuchElementException If the task, its event or the user does not exist.
   * @throws IllegalArgumentException If the user is not assigned to the task or does not list it in
   *   [User.tasksAssigned].
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  suspend fun quitTask(taskId: String, userId: String) {

    // Get the necessary objects.
    val task = tasksRepository.getTask(taskId)
    val user = usersRepository.getUser(userId)
    val event = eventsRepository.getEvent(task.eventId)

    // Compute the new versions that have to be stored.
    val newTask = task.removeAssignee(userId)
    val newUser = user.removeAssignedTask(taskId)
    val newEvent = event.copy(tasks = event.tasks.map { if (it.taskId == taskId) newTask else it })

    // Store the changes.
    changeAssignment(event, newEvent, task, newTask, user, newUser)
  }

  /**
   * Stores the new versions of an [Event], one of its [Task]s and a [User] after the user joined or
   * quit the task. If a write fails, the writes already done are undone, then the original
   * exception is rethrown.
   *
   * @param event The current version of the event, read before any write.
   * @param newEvent The new version of the event, with [newTask] in place of [task].
   * @param task The current version of the task, read before any write.
   * @param newTask The new version of the task.
   * @param user The current version of the user, read before any write.
   * @param newUser The new version of the user.
   * @throws DataInconsistencyException If a write failed and could not be fully undone.
   */
  private suspend fun changeAssignment(
      event: Event,
      newEvent: Event,
      task: Task,
      newTask: Task,
      user: User,
      newUser: User,
  ) {
    val undoProcess = ArrayDeque<suspend () -> Unit>()
    try {
      eventsRepository.editEvent(event.eventId, newEvent)
      undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

      tasksRepository.editTask(task.taskId, newTask)
      undoProcess.addFirst { tasksRepository.editTask(task.taskId, task) }

      usersRepository.editUser(user.uid, newUser)
      undoProcess.addFirst { usersRepository.editUser(user.uid, user) }
    } catch (e: Exception) {
      rollback(undoProcess, e)
      throw e
    }
  }

  /**
   * Stores the new version of each [User] and remembers how to undo each write, in the same way as
   * the other writes of an operation.
   *
   * @param users The current versions of the users, read before any write.
   * @param newUsers The new versions to store, in the same order as [users].
   * @param undoProcess The undos of the operation, to which the undo of each user write is added.
   */
  private suspend fun editUsers(
      users: List<User>,
      newUsers: List<User>,
      undoProcess: ArrayDeque<suspend () -> Unit>,
  ) {
    for ((old, new) in users.zip(newUsers)) {
      usersRepository.editUser(new.uid, new)
      undoProcess.addFirst { usersRepository.editUser(old.uid, old) }
    }
  }
}

/**
 * Thrown when an operation spanning several repositories failed midway, leaving the stored data
 * inconsistent (for example an event lists a member whose user does not list the event).
 */
class DataInconsistencyException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
