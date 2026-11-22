// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.images

import java.util.UUID

interface ImageStorage {
  /** Generates and returns a new unique identifier for an Image item. */
  fun getNewUid(): String

  /**
   * Uploads [StoredImage] into a ```subFolder``` in the remote storage and returns its URL.
   *
   * @param storedImage The image to upload
   * @param subFolder The folder in which upload (ex: all profile pictures in .../pfp/)
   * @throws IOException if the storage cannot be reached or refuses the file.
   */
  suspend fun upload(storedImage: StoredImage, subFolder: String): String

  /**
   * Deletes the Image file at ```ImageUrl```.
   *
   * Deleting a file that does not exist will not throw an error.
   *
   * @throws IOException if the storage cannot be reached or refuses the deletion.
   */
  suspend fun delete(imageUrl: String)
}

/**
 * In-memory image storage used by local repositories and previews. This does basically nothing,
 * only use for tests
 */
class ImageStorageLocal : ImageStorage {
  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun upload(storedImage: StoredImage, subFolder: String): String {
    return storedImage.uri.toString()
  }

  override suspend fun delete(imageUrl: String) = Unit
}
