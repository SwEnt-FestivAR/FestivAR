package com.android.festivar.model.images

import androidx.core.net.toUri
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.net.URLConnection
import java.net.URLDecoder
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Represents an image file ready for upload. */
data class StoredImage(val file: File) {
  val fileName: String
    get() = file.name

  val uri
    get() = file.toUri()
}

/** Firebase implementation of [ImageStorage] managing image uploads, deletions, and rendering. */
class ImageStorageFirebase(private val storageRef: StorageReference) : ImageStorage {
  private val imageReference = storageRef.child("image")

  /** Generates a unique identifier string using UUID. */
  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  /**
   * Uploads a [storedImage] to the specified [subFolder] in Firebase Storage and returns its
   * download URL.
   */
  override suspend fun upload(
      storedImage: StoredImage,
      subFolder: String,
  ): String {
    try {
      val extension = storedImage.file.extension.let { if (it.isNotBlank()) ".$it" else ".png" }
      val fileName = "${getNewUid()}$extension"
      val ref = imageReference.child(subFolder).child(fileName)
      val guessedMIME = URLConnection.guessContentTypeFromName(storedImage.fileName)
      val metadata = StorageMetadata.Builder().setContentType(guessedMIME).build()
      ref.putFile(storedImage.uri, metadata).await()
      val downloadUrl = ref.downloadUrl.await().toString()
      return downloadUrl.ifBlank {
        throw IOException("Image upload did not return a valid download URL.")
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (e is IOException) throw e
      throw IOException("Failed to upload image file.", e)
    }
  }

  override suspend fun delete(imageUrl: String) {
    if (imageUrl.isBlank()) return
    try {
      val ref =
          try {
            storageRef.storage.getReferenceFromUrl(imageUrl)
          } catch (_: IllegalArgumentException) {
            // getReferenceFromUrl rejects emulator URLs, so we read the object path ourselves.
            val pathPart = imageUrl.substringAfter("/o/").substringBefore("?")
            val decodedPath = withContext(IO) { URLDecoder.decode(pathPart, "UTF-8") }
            require(decodedPath.startsWith("image/")) { "Not an image URL: $imageUrl" }
            storageRef.storage.reference.child(decodedPath)
          }
      ref.delete().await()
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
        // Deleting a non-existent file is not an issue.
        return
      }
      throw IOException("Failed to delete image from storage.", e)
    }
  }

  /**
   * To show an image, you should use the Glide library, like this : GlideImage( model = imageUrl,
   * contentDescription = contentDescription, ) This comment can be removed when you do, it's just
   * an explanation
   */
}
