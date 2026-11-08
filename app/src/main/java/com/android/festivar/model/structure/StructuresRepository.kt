// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

/** Represents a repository that manages [Structure] items. */
interface StructuresRepository {

  /** Generates and returns a new unique identifier for a [Structure] item. */
  fun getNewUid(): String

  /**
   * Retrieves all [Structure] items belonging to an event from the repository.
   *
   * @param eventId The unique identifier of the [Event] whose structures to retrieve.
   * @return A list of [Structure] items belonging to the event.
   */
  suspend fun getAllStructures(eventId: String): List<Structure>

  /**
   * Retrieves a specific [Structure] item by its unique identifier.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param structureId The unique identifier of the [Structure] item to retrieve.
   * @return The [Structure] item with the specified identifier.
   * @throws NoSuchElementException if the [Structure] item is not found.
   */
  suspend fun getStructure(structureId: String): Structure

  /**
   * Adds a new [Structure] item to the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param structure The [Structure] item to add. Its [Structure.structureId] must not be used by
   *   another [Structure] item.
   * @throws IllegalArgumentException if a [Structure] item with the same identifier already exists.
   */
  suspend fun addStructure(structure: Structure)

  /**
   * Edits an existing [Structure] item in the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param structureId The unique identifier of the [Structure] item to edit.
   * @param newValue The new value for the [Structure] item. Its [Structure.structureId] must be
   *   equal to [structureId].
   * @throws IllegalArgumentException if [newValue] does not have the same identifier as
   *   [structureId].
   * @throws NoSuchElementException if the [Structure] item is not found.
   */
  suspend fun editStructure(structureId: String, newValue: Structure)

  /**
   * Deletes a [Structure] item from the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param structureId The unique identifier of the [Structure] item to delete.
   * @throws NoSuchElementException if the [Structure] item is not found.
   */
  suspend fun deleteStructure(structureId: String)
}
