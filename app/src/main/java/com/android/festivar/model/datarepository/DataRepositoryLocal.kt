package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal

class DataRepositoryLocal(
    override val authRepository: AuthRepository,
    override val tasksRepository: TasksRepository = TasksRepositoryLocal(),
    override val eventsRepository: EventsRepository = EventsRepositoryLocal(),
    override val usersRepository: UsersRepository = UsersRepositoryLocal(),
) : DataRepository