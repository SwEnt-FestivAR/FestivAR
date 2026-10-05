package com.android.festivar.model.task

/** Represents a repository that manages a local list of tasks. */
class TasksRepositoryLocal : TasksRepository {
  private val tasks: MutableList<Task> = mutableListOf()
  private var counter = 0

  override fun getNewUid(): String {
    return (counter++).toString()
  }

  override suspend fun getAllTasks(): List<Task> {
    return tasks
  }

  override suspend fun getTask(taskId: String): Task {
    return tasks.find { it.taskId == taskId }
        ?: throw Exception("TasksRepositoryLocal: Task not found")
  }

  override suspend fun addTask(task: Task) {
    tasks.add(task)
  }

  override suspend fun editTask(taskId: String, newValue: Task) {
    if(newValue.taskId != taskId){
      throw IllegalArgumentException("TasksRepositoryLocal: newValue is not an edit of Task")
    }
    val index = tasks.indexOfFirst { it.taskId == taskId }
    if (index != -1) {
      tasks[index] = newValue
    } else {
      throw Exception("TasksRepositoryLocal: Task not found")
    }
  }

  override suspend fun deleteTask(taskId: String) {
    val index = tasks.indexOfFirst { it.taskId == taskId }
    if (index != -1) {
      tasks.removeAt(index)
    } else {
      throw Exception("TasksRepositoryLocal: Task not found")
    }
  }
}
