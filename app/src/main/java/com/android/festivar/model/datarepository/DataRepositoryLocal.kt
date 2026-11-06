// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal

/**
 * A [DataRepository] that keeps its data in memory, meant for tests and previews.
 *
 * By default it uses the local repositories ([TasksRepositoryLocal], [EventsRepositoryLocal] and
 * [UsersRepositoryLocal]), all starting empty. Any of them can be replaced, for example by a
 * repository that fails on purpose to test the rollback.
 *
 * @param authRepository The repository handling the authentication.
 * @param tasksRepository The repository storing the tasks.
 * @param eventsRepository The repository storing the events.
 * @param usersRepository The repository storing the users.
 */
class DataRepositoryLocal(
    override val authRepository: AuthRepository = FakeAuthRepository(),
    override val tasksRepository: TasksRepository = TasksRepositoryLocal(),
    override val eventsRepository: EventsRepository = EventsRepositoryLocal(),
    override val usersRepository: UsersRepository = UsersRepositoryLocal(),
) : DataRepository
