package com.android.festivar.model.task

/** Represents a repository that manages a local list of tasks. */
class TasksRepositoryLocal : TasksRepository {
  private val tasks: MutableList<Task> = mutableListOf()
  private var counter = 0

  override fun getNewUid(): String {
    return (counter++).toString()
  }

  override suspend fun getAllTasks(): List<Task> {
    return tasks.toList()
  }

  override suspend fun getTask(taskId: String): Task {
    return tasks.find { it.taskId == taskId }
        ?: throw NoSuchElementException("TasksRepositoryLocal: Task $taskId not found")
  }

  override suspend fun addTask(task: Task) {
    require(tasks.none { it.taskId == task.taskId }) {
      "TasksRepositoryLocal: a Task with taskId ${task.taskId} already exists"
    }
    tasks.add(task)
  }

  override suspend fun editTask(taskId: String, newValue: Task) {
    require(newValue.taskId == taskId) {
      "TasksRepositoryLocal: newValue.taskId (${newValue.taskId}) must be equal to taskId ($taskId)"
    }

    val index = tasks.indexOfFirst { it.taskId == taskId }

    if (index != -1) {
      tasks[index] = newValue
    } else {
      throw NoSuchElementException("TasksRepositoryLocal: Task $taskId not found")
    }
  }

  override suspend fun deleteTask(taskId: String) {
    val index = tasks.indexOfFirst { it.taskId == taskId }
    if (index != -1) {
      tasks.removeAt(index)
    } else {
      throw NoSuchElementException("TasksRepositoryLocal: Task $taskId not found")
    }
  }
}
