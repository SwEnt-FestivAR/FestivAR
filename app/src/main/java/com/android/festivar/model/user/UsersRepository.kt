package com.android.festivar.model.user

import com.android.festivar.model.event.Event
import com.android.festivar.model.task.Task

/** Represents a repository that manages [User] items. */
interface UsersRepository {

  /** Generates and returns a new unique identifier for a [User] item. */
  fun getNewUid(): String

  /**
   * Retrieves all [User] item in the database.
   *
   * @return A list of all [User] items.
   */
  suspend fun getAllUsers(): List<User>

  /**
   * Retrieves a specific [User] item by its unique identifier.
   *
   * @param uId The unique identifier of the [User] item to retrieve.
   * @return The [User] item with the specified identifier.
   * @throws NoSuchElementException if the [User] item is not found.
   */
  suspend fun getUser(uId: String): User

  /**
   * Retrieves all [User] items belonging to an [Event] from the repository.
   *
   * @param eventId The unique identifier of the [Event] whose users to retrieve.
   * @return A list of [User] items belonging to the [Event].
   */
  suspend fun getEventUsers(eventId: String): List<User>

  /**
   * Retrieves all [User] items belonging to a [Task] from the repository.
   *
   * @param taskId The unique identifier of the [Task] whose users to retrieve.
   * @return A list of [User] items belonging to the [Task].
   */
  suspend fun getTaskUsers(taskId: String): List<User>

  /**
   * Adds a new [User] item to the repository.
   *
   * @param user The [User] item to add. Its [User.uid] must not be used by another [User] item.
   * @throws IllegalArgumentException if a [User] item with the same identifier already exists.
   */
  suspend fun addUser(user: User)

  /**
   * Edits an existing [User] item in the repository.
   *
   * @param uId The unique identifier of the [User] item to edit.
   * @param newValue The new value for the [User] item. Its [User.uid] must be equal to [uId].
   * @throws IllegalArgumentException if [newValue] does not have the same identifier as [uId].
   * @throws NoSuchElementException if the [User] item is not found.
   */
  suspend fun editUser(uId: String, newValue: User)

  /**
   * Deletes a [User] item from the repository.
   *
   * @param uId The unique identifier of the [User] item to delete.
   * @throws NoSuchElementException if the [User] item is not found.
   */
  suspend fun deleteUser(uId: String)
}
