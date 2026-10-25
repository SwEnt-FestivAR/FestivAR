package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository

interface DataRepository {

    val authRepository: AuthRepository
    val tasksRepository: TasksRepository
    val eventsRepository: EventsRepository
    val usersRepository: UsersRepository

    suspend fun addTask(task: Task){
        val eventId = task.eventId
        val uId = task.ownerId
        val assignees = task.assignees

        tasksRepository.addTask(task)

        val event = eventsRepository.getEvent(eventId)
        event.addTask(task)
        eventsRepository.editEvent(eventId, event)

        if(uId != null){
            val user = usersRepository.getUser(uId)
            user.addAssignedTask(task.taskId)
            usersRepository.editUser(uId, user)
        }

        for(assignee in assignees){
            assignee.addAssignedTask(task.taskId)
            usersRepository.editUser(assignee.uid, assignee)
        }
    }

    suspend fun deleteTask(task: Task){
        val eventId = task.eventId
        val uId = task.ownerId
        val assignees = task.assignees

        tasksRepository.deleteTask(task.taskId)

        val event = eventsRepository.getEvent(eventId)
        event.removeTask(task.taskId)
        eventsRepository.editEvent(eventId, event)

        if(uId != null){
            val user = usersRepository.getUser(uId)
            user.removeAssignedTask(task.taskId)
            usersRepository.editUser(uId, user)
        }

        for (assignee in assignees){
            assignee.removeAssignedTask(task.taskId)
            usersRepository.editUser(assignee.uid, assignee)
        }
    }

    suspend fun createEvent(event: Event){
        require(event.tasks.isEmpty())
        val members = event.members

        eventsRepository.addEvent(event)

        for (member in members){
            member.addJoinedEvent(event.eventId)
            usersRepository.editUser(member.uid, member)
        }
    }

    suspend fun deleteEvent(event: Event){
        val tasks = event.tasks
        val members = event.members

        eventsRepository.deleteEvent(event.eventId)

        for (task in tasks){
            tasksRepository.deleteTask(task.taskId)

            val uId = task.ownerId
            val assignees = task.assignees

            if(uId != null){
                val user = usersRepository.getUser(uId)
                user.removeAssignedTask(task.taskId)
                usersRepository.editUser(uId, user)
            }

            for (assignee in assignees){
                assignee.removeAssignedTask(task.taskId)
                usersRepository.editUser(assignee.uid, assignee)
            }
        }

        for (member in members){
            member.removeJoinedEvent(event.eventId)
            usersRepository.editUser(member.uid, member)
        }
    }

    suspend fun joinEvent(eventId: String, user: User){
        val event = eventsRepository.getEvent(eventId)

        event.addMember(user)
        eventsRepository.editEvent(eventId, event)

        user.addJoinedEvent(eventId)
        usersRepository.editUser(user.uid, user)
    }

    suspend fun quitEvent(eventId: String, user: User){
        val event = eventsRepository.getEvent(eventId)

        event.removeMember(user)
        for(task in event.tasks.filter { task -> task.ownerId == user.uid || task.assignees.any { it -> it.uid == user.uid } }){
            if(task.ownerId == user.uid){
                val task2 = task.copy(ownerId = null)
                tasksRepository.editTask(task2.taskId, task2)
                user.removeAssignedTask(task2.taskId)
            }

            if(task.assignees.any { it -> it.uid == user.uid }){
                val task2 = task.removeAssignee(user)
                tasksRepository.editTask(task2.taskId, task2)
                user.removeAssignedTask(task2.taskId)
            }
        }

        eventsRepository.editEvent(eventId, event)

        user.removeJoinedEvent(eventId)
        usersRepository.editUser(user.uid, user)
    }

    suspend fun joinTask(taskId: String, user: User){
        val task = tasksRepository.getTask(taskId)

        task.addAssignee(user)
        tasksRepository.editTask(taskId, task)

        user.addAssignedTask(taskId)
        usersRepository.editUser(user.uid, user)
    }

    /*suspend fun quitTask(taskId: String, user: User){
        val task = tasksRepository.getTask(taskId)

        if(task.ownerId == user.uid){
            task =
        }

        task.removeAssignee(user)
        tasksRepository.editTask(taskId, task)

        user.removeAssignedTask(taskId)
        usersRepository.editUser(user.uid, user)
    }*/






}