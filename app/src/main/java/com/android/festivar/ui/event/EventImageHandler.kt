package com.android.festivar.ui.event

// Co-authored-by: Gemini <noreply@google.com>

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import com.android.festivar.R
import com.android.festivar.model.images.ImageStorage
import com.android.festivar.model.images.StoredImage
import com.android.festivar.ui.event.create.CreateEventUIState
import com.android.festivar.ui.event.edit.EditEventUIState
import com.android.festivar.ui.profile.isProfilePictureTooLarge
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_TAG = "EventImageHandler"

/**
 * Handles photo picker selection, temporary file management, uploading to [ImageStorage], and image
 * deletion flow for event forms.
 */
class EventImageHandler(
    private val imageStorage: ImageStorage,
    private val ioDispatcher: CoroutineDispatcher,
) {

  /** Opens the delete image confirmation dialog if an image is present. */
  fun <S> requestDeleteImage(
      currentState: S,
      hasImage: Boolean,
      updateState: ((S) -> S) -> Unit,
  ) {
    if (!hasImage) return
    updateState { setShowDeleteImageConfirmation(it, true) }
  }

  /** Closes the delete image confirmation dialog without deleting the image. */
  fun <S> cancelDeleteImage(updateState: ((S) -> S) -> Unit) {
    updateState { setShowDeleteImageConfirmation(it, false) }
  }

  /** Confirms deleting the image from storage and removes it from the form state. */
  fun <S> confirmDeleteImage(
      imageUrl: String?,
      coroutineScope: CoroutineScope,
      updateState: ((S) -> S) -> Unit,
      setImageUrlNull: (S) -> S,
  ) {
    updateState { setImageUrlNull(it) }
    if (!imageUrl.isNullOrEmpty()) {
      coroutineScope.launch(ioDispatcher) {
        withContext(NonCancellable) {
          try {
            imageStorage.delete(imageUrl)
          } catch (e: Exception) {
            Log.e(LOG_TAG, "Error deleting image", e)
          }
        }
      }
    }
  }

  /** Deletes a temporary image file from disk. */
  fun deleteTemporaryImage(file: File) {
    if (file.exists() && !file.delete()) {
      Log.w(LOG_TAG, "Could not delete temporary image ${file.path}")
    }
  }

  /** Reads selected image URI from content resolver, checks file size, and uploads to storage. */
  @OptIn(ExperimentalStdlibApi::class)
  fun <S> updateImage(
      uri: Uri,
      contentResolver: ContentResolver,
      cacheDir: File,
      isSaving: Boolean,
      currentImageUrl: String?,
      coroutineScope: CoroutineScope,
      @StringRes saveErrorResId: Int,
      updateState: ((S) -> S) -> Unit,
      setSavingState: (S, Boolean, Int?) -> S,
      setImageUrlState: (S, String?, Boolean, Int?) -> S,
  ) {
    if (isSaving) return
    updateState { setSavingState(it, true, null) }
    coroutineScope.launch {
      var temporaryFile: File? = null
      try {
        val extension =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
                ?: "jpg"
        val storedImage: StoredImage? =
            withContext(ioDispatcher) {
              val file = File.createTempFile("temp-event-image-", ".$extension", cacheDir)
              try {
                val input =
                    contentResolver.openInputStream(uri) ?: error("Unable to read selected image.")
                input.use { FileOutputStream(file).use { output -> input.copyTo(output) } }
                if (isProfilePictureTooLarge(file.length())) {
                  deleteTemporaryImage(file)
                  null
                } else {
                  StoredImage(file)
                }
              } catch (e: Exception) {
                deleteTemporaryImage(file)
                throw e
              }
            }
        if (storedImage == null) {
          updateState { setSavingState(it, false, R.string.profile_picture_too_large) }
        } else {
          temporaryFile = storedImage.file
          saveImage(
              storedImage = storedImage,
              oldImageUrl = currentImageUrl,
              saveErrorResId = saveErrorResId,
              updateState = updateState,
              setImageUrlState = setImageUrlState,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Unable to read selected image", e)
        updateState { setSavingState(it, false, saveErrorResId) }
      } finally {
        temporaryFile?.let { file ->
          withContext(NonCancellable + ioDispatcher) { deleteTemporaryImage(file) }
        }
      }
    }
  }

  private suspend fun <S> saveImage(
      storedImage: StoredImage,
      oldImageUrl: String?,
      @StringRes saveErrorResId: Int,
      updateState: ((S) -> S) -> Unit,
      setImageUrlState: (S, String?, Boolean, Int?) -> S,
  ) {
    var newImageUrl: String? = null
    try {
      newImageUrl = imageStorage.upload(storedImage, "events")
      updateState { setImageUrlState(it, newImageUrl, false, null) }
      var oldImageDeletionFailed = false
      if (!oldImageUrl.isNullOrBlank() && oldImageUrl != newImageUrl) {
        try {
          imageStorage.delete(oldImageUrl)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error deleting old event image", e)
          oldImageDeletionFailed = true
        }
      }
      if (oldImageDeletionFailed) {
        updateState { setImageUrlState(it, newImageUrl, false, saveErrorResId) }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(LOG_TAG, "Error saving event image", e)
      newImageUrl?.let { uploadedUrl ->
        try {
          imageStorage.delete(uploadedUrl)
        } catch (cleanupError: Exception) {
          Log.e(LOG_TAG, "Error cleaning up event image", cleanupError)
        }
      }
      updateState { setImageUrlState(it, null, false, saveErrorResId) }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S> setShowDeleteImageConfirmation(state: S, show: Boolean): S {
    return when (state) {
      is CreateEventUIState -> state.copy(showDeleteImageConfirmation = show) as S
      is EditEventUIState -> state.copy(showDeleteImageConfirmation = show) as S
      else -> state
    }
  }
}
