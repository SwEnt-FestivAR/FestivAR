//Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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

interface DataRepository {

    val authRepository: AuthRepository
    val tasksRepository: TasksRepository
    val eventsRepository: EventsRepository
    val usersRepository: UsersRepository



    /** Runs every undo, even if some fail; throws [DataInconsistencyException] if any did. */
    private suspend fun rollback(undoProcess: List<suspend () -> Unit>, cause: Exception) =
        withContext(NonCancellable) {
            val failures = undoProcess.mapNotNull { undo -> runCatching { undo() }.exceptionOrNull() }
            if (failures.isNotEmpty()) {
                throw DataInconsistencyException("Operation failed and could not be fully undone", cause)
                    .apply { failures.forEach(::addSuppressed) }
            }
        }


    suspend fun addTask(task: Task) {
        val event = eventsRepository.getEvent(task.eventId)
        val userIds = (listOf(task.ownerId) + task.assignees.map { it.uid }).distinct()
        val users = usersRepository.getListUsers(userIds)

        val newEvent = event.addTask(task)
        val newUsers = users.map { it.addAssignedTask(task.taskId) }

        val undoProcess = ArrayDeque<suspend () -> Unit>()

        try {
            tasksRepository.addTask(task)
            undoProcess.addFirst { tasksRepository.deleteTask(task.taskId) }

            eventsRepository.editEvent(event.eventId, newEvent)
            undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

            for ((old, new) in users.zip(newUsers)) {
                usersRepository.editUser(new.uid, new)
                undoProcess.addFirst { usersRepository.editUser(old.uid, old) }
            }

        } catch (e: Exception) {
            rollback(undoProcess, e)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String){
        val task = tasksRepository.getTask(taskId)
        val event = eventsRepository.getEvent(task.eventId)
        val userIds = (listOf(task.ownerId) + task.assignees.map { it.uid }).distinct()
        val users = usersRepository.getListUsers(userIds)

        val newEvent = event.removeTask(taskId)
        val newUsers = users.map { it.removeAssignedTask(taskId) }

        val undoProcess = ArrayDeque<suspend () -> Unit>()

        try {
            tasksRepository.deleteTask(taskId)
            undoProcess.addFirst { tasksRepository.addTask(task) }

            eventsRepository.editEvent(event.eventId, newEvent)
            undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

            for ((old, new) in users.zip(newUsers)) {
                usersRepository.editUser(new.uid, new)
                undoProcess.addFirst { usersRepository.editUser(old.uid, old) }
            }
        } catch (e: Exception) {
            rollback(undoProcess, e)
            throw e
        }
    }

    suspend fun createEvent(event: Event){
        require(event.tasks.isEmpty())
        val userIds =  event.members.map { it.uid }
        val users = usersRepository.getListUsers(userIds)
        val newUsers = users.map { it.addJoinedEvent(event.eventId) }


        val undoProcess = ArrayDeque<suspend () -> Unit>()
        try {
            eventsRepository.addEvent(event)
            undoProcess.addFirst { eventsRepository.deleteEvent(event.eventId) }

            for ((old, new) in users.zip(newUsers)) {
                usersRepository.editUser(new.uid, new)
                undoProcess.addFirst { usersRepository.editUser(old.uid, old) }
            }

        } catch (e: Exception) {
            rollback(undoProcess, e)
            throw e
        }
    }

    suspend fun deleteEvent(eventId: String){
        val event = eventsRepository.getEvent(eventId)
        val tasks = tasksRepository.getAllTasks(eventId)
        val userIds = event.members.map{ it.uid }
        val users = usersRepository.getListUsers(userIds)

        val newUsers = users.map {
            var newIt = it
            for(task in tasks){
                if(newIt.tasksAssigned.contains(task.taskId)){
                    newIt = newIt.removeAssignedTask(task.taskId)
                }
            }
            newIt = newIt.removeJoinedEvent(eventId)
            newIt
        }

        val undoProcess = ArrayDeque<suspend () -> Unit>()
        try {
            eventsRepository.deleteEvent(eventId)
            undoProcess.addFirst { eventsRepository.addEvent(event) }

            for (task in tasks){
                tasksRepository.deleteTask(task.taskId)
                undoProcess.addFirst { tasksRepository.addTask(task) }
            }

            for ((old, new) in users.zip(newUsers)) {
                usersRepository.editUser(new.uid, new)
                undoProcess.addFirst { usersRepository.editUser(old.uid, old) }
            }

        } catch (e: Exception) {
            rollback(undoProcess, e)
            throw e
        }
    }

    suspend fun joinEvent(eventId: String, userId: String){
        val event = eventsRepository.getEvent(eventId)
        val user = usersRepository.getUser(userId)

        val newUser = user.addJoinedEvent(eventId)
        val newEvent = event.addMember(newUser)

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

    suspend fun quitEvent(eventId: String, userId: String){
        val event = eventsRepository.getEvent(eventId)
        val user = usersRepository.getUser(userId)
        val tasks = tasksRepository.getAllTasks(eventId)

        val newTasks = tasks.map {
            if(it.assignees.map { it2 -> it2.uid }.contains(userId)){
                it.removeAssignee(user)
            } else{
                it
            }
        }
        val newEvent = event.copy(tasks = newTasks).removeMember(user)
        var newUser = user.removeJoinedEvent(eventId)

        for(task in tasks){
            if(newUser.tasksAssigned.contains(task.taskId)){
                newUser = newUser.removeAssignedTask(task.taskId)
            }
        }

        val undoProcess = ArrayDeque<suspend () -> Unit>()
        try {
            eventsRepository.editEvent(eventId, newEvent)
            undoProcess.addFirst { eventsRepository.editEvent(eventId, event) }

            for ((old, new) in tasks.zip(newTasks)) {
                if(old != new){
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

    suspend fun joinTask(taskId: String, userId: String){
        val task = tasksRepository.getTask(taskId)
        val user = usersRepository.getUser(userId)
        val event = eventsRepository.getEvent(task.eventId)

        val newTask = task.addAssignee(user)
        val newUser = if(newTask.ownerId != user.uid){ user.addAssignedTask(taskId) } else { user }
        val newEvent = event.removeTask(taskId).addTask(newTask)

        val undoProcess = ArrayDeque<suspend () -> Unit>()
        try {

            eventsRepository.editEvent(event.eventId, newEvent)
            undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

            tasksRepository.editTask(taskId, newTask)
            undoProcess.addFirst { tasksRepository.editTask(taskId, task) }
            usersRepository.editUser(userId, newUser)
            undoProcess.addFirst { usersRepository.editUser(userId, user) }

        } catch (e: Exception) {
            rollback(undoProcess, e)
            throw e
        }

    }

    suspend fun quitTask(taskId: String, userId: String){
        val task = tasksRepository.getTask(taskId)
        val user = usersRepository.getUser(userId)
        val event = eventsRepository.getEvent(task.eventId)

        val newTask = task.removeAssignee(user)
        val newUser = user.removeAssignedTask(taskId)
        val newEvent = event.removeTask(taskId).addTask(newTask)


        val undoProcess = ArrayDeque<suspend () -> Unit>()
        try {

            eventsRepository.editEvent(event.eventId, newEvent)
            undoProcess.addFirst { eventsRepository.editEvent(event.eventId, event) }

            tasksRepository.editTask(taskId, newTask)
            undoProcess.addFirst { tasksRepository.editTask(taskId, task) }

            usersRepository.editUser(userId, newUser)
            undoProcess.addFirst { usersRepository.editUser(userId, user) }

        } catch (e: Exception) {
            rollback(undoProcess, e)
            throw e
        }
    }









}

/**
 * Thrown when an operation spanning several repositories failed midway, leaving the stored data
 * inconsistent (for example an event lists a member whose user does not list the event).
 */
class DataInconsistencyException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)