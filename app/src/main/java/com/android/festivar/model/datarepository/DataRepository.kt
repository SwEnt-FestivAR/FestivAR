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








}

/**
 * Thrown when an operation spanning several repositories failed midway, leaving the stored data
 * inconsistent (for example an event lists a member whose user does not list the event).
 */
class DataInconsistencyException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)