package com.android.festivar.model.task

/** Represents a repository that manages [Task] items. */
interface TasksRepository {

  /** Generates and returns a new unique identifier for a [Task] item. */
  fun getNewUid(): String

  /**
   * Retrieves all [Task] items belonging to an event from the repository.
   *
   * @param eventId The unique identifier of the [Event] whose tasks to retrieve.
   * @return A list of [Task] items belonging to the event.
   */
  suspend fun getAllTasks(eventId: String): List<Task>

  /**
   * Retrieves a specific [Task] item by its unique identifier.
   *
   * @param taskId The unique identifier of the [Task] item to retrieve.
   * @return The [Task] item with the specified identifier.
   * @throws NoSuchElementException if the [Task] item is not found.
   */
  suspend fun getTask(taskId: String): Task

  /**
   * Adds a new [Task] item to the repository.
   *
   * @param task The [Task] item to add. Its [Task.taskId] must not be used by another [Task] item.
   * @throws IllegalArgumentException if a [Task] item with the same identifier already exists.
   */
  suspend fun addTask(task: Task)

  /**
   * Edits an existing [Task] item in the repository.
   *
   * @param taskId The unique identifier of the [Task] item to edit.
   * @param newValue The new value for the [Task] item. Its [Task.taskId] must be equal to [taskId].
   * @throws IllegalArgumentException if [newValue] does not have the same identifier as [taskId].
   * @throws NoSuchElementException if the [Task] item is not found.
   */
  suspend fun editTask(taskId: String, newValue: Task)

  /**
   * Deletes a [Task] item from the repository.
   *
   * @param taskId The unique identifier of the [Task] item to delete.
   * @throws NoSuchElementException if the [Task] item is not found.
   */
  suspend fun deleteTask(taskId: String)
}
