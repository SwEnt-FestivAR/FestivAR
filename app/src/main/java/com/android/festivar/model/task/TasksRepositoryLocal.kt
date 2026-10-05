package com.android.festivar.model.task

/** Represents a repository that manages a local list of tasks. */
class TasksRepositoryLocal : TasksRepository {
  private val todos: MutableList<Task> = mutableListOf()
  private var counter = 0

  override fun getNewUid(): String {
    return (counter++).toString()
  }

  override suspend fun getAllTasks(): List<Task> {
    return todos
  }

  override suspend fun getTask(taskId: String): Task {
    return todos.find { it.taskId == taskId }
        ?: throw Exception("TasksRepositoryLocal: Task not found")
  }

  override suspend fun addTask(task: Task) {
    todos.add(task)
  }

  override suspend fun editTask(taskId: String, newValue: Task) {
    val index = todos.indexOfFirst { it.taskId == taskId }
    if (index != -1) {
      todos[index] = newValue
    } else {
      throw Exception("TasksRepositoryLocal: Task not found")
    }
  }

  override suspend fun deleteTask(taskId: String) {
    val index = todos.indexOfFirst { it.taskId == taskId }
    if (index != -1) {
      todos.removeAt(index)
    } else {
      throw Exception("TasksRepositoryLocal: Task not found")
    }
  }
}
