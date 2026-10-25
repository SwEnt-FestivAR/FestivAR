package com.android.festivar.model.audio

interface AudioRepository {

  /** Generates and returns a new unique identifier for an [Audio] item. */
  fun getNewUid(): String

  /**
   * Adds a new [Audio] item to the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param audio The [Audio] item to add item.
   * @throws IllegalArgumentException if an [Audio] item with the same identifier already exists.
   */
  suspend fun addAudio(audio: Audio)

  /**
   * Deletes an [Audio] item from the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param fileName The unique identifier of the [Audio] item to delete.
   * @throws NoSuchElementException if the [Audio] item is not found.
   */
  suspend fun deleteAudio(fileName: String)
}
