package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.images.StoredImage
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.endDateError
import com.android.festivar.ui.event.EventFormViewModel
import com.android.festivar.ui.event.computeEndDateTime
import com.android.festivar.ui.event.computeStartDateTime
import com.android.festivar.ui.event.isEventFormValid
import com.android.festivar.ui.event.validateEventName
import com.android.festivar.ui.profile.isProfilePictureTooLarge
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val LOG_TAG = "CreateEventViewModel"

/**
 * UI state of the create event form.
 *
 * @property isSaving true while the event is being written, so the button is disabled and a second
 *   tap is ignored.
 * @property createdEventId the id of the event once it is saved; the screen then leaves.
 * @property errorMsg the resource of the message shown under the button when the save failed.
 */
data class CreateEventUIState(
    val name: String = "",
    val nameEdited: Boolean = false,
    val range: DateTimeRange = DateTimeRange(),
    val venue: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val createdEventId: String? = null,
    val showDiscardConfirmation: Boolean = false,
    val isDiscarded: Boolean = false,
    val showDeleteImageConfirmation: Boolean = false,
    val canCreateEvent: Boolean = true,
    val zone: ZoneId = ZoneId.systemDefault(),
    @StringRes val errorMsg: Int? = null,
    val imageUrl: String? = null,
) {
  val hasImage: Boolean
    get() = !imageUrl.isNullOrEmpty()

  val startDateTime: ZonedDateTime?
    get() = computeStartDateTime(range.startDate, range.startTime, zone)

  val endDateTime: ZonedDateTime?
    get() = computeEndDateTime(range.endDate, range.endTime, zone)

  val nameError: FieldError?
    get() = validateEventName(name, nameEdited)

  /** The error shown under the times, or `null`. See [endDateError]. */
  val endDateError: FieldError?
    get() = endDateError(range, zone)

  val isValid: Boolean
    get() =
        isEventFormValid(
            true,
            name,
            startDateTime,
            endDateTime,
            nameError,
            endDateError,
        )

  /**
   * Whether the "Create event" button can be pressed. It is false while a save is running and once
   * the event is created, so that a tap just after the save cannot create a second event.
   */
  val canCreate: Boolean
    get() = isValid && !isSaving && createdEventId == null

  fun toEvent(eventId: String, owner: User): Event {
    require(isValid) { "The form is not valid." }
    return Event(
        eventId = eventId,
        ownerId = owner.uid,
        title = name,
        description = notes,
        members = listOf(owner),
        startDate = startDateTime!!,
        endDate = endDateTime!!,
        location = venue,
        imageUrl = imageUrl ?: "",
    )
  }
}

private const val TAG = "CreateEventViewModel"
private const val SAVE_TIMEOUT_MS = 15_000L

/**
 * Holds the create event form, validates it and saves the event with the signed-in user as its
 * first member.
 *
 * @property dataRepository The general repository handling the interactions/synchronization/updates
 *   between the different repositories. It coordinates the creation of the event and the update of
 *   its members.
 * @property userId the uid of the signed-in user, blank when nobody is.
 * @property isAnonymous Tells whether the signed-in user has an anonymous account, which cannot
 *   create an [Event]. Read when the ViewModel is created and again before saving.
 */
class CreateEventViewModel(
    private val dataRepository: DataRepository,
    private val userId: String = "",
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
    private val ioDispatcher: CoroutineDispatcher = IO,
    private val isAnonymous: () -> Boolean = { false },
) : EventFormViewModel<CreateEventUIState>(CreateEventUIState(canCreateEvent = !isAnonymous())) {
  val uiState: StateFlow<CreateEventUIState> = formState
  private var pendingEventId: String? = null

  private val imageStorage = dataRepository.imageStorage

  /** Whether leaving the form would discard any entered event details. */
  val needsDiscardConfirmation: Boolean
    get() =
        currentState.createdEventId == null && !currentState.isSaving && hasChanges(currentState)

  override fun clearError(state: CreateEventUIState) = state.copy(errorMsg = null)

  override fun updateNameState(state: CreateEventUIState, newName: String) =
      state.copy(name = newName, nameEdited = true)

  override fun updateRangeState(state: CreateEventUIState, range: DateTimeRange) =
      state.copy(range = range)

  override fun updateVenueState(state: CreateEventUIState, newVenue: String) =
      state.copy(venue = newVenue)

  override fun updateNotesState(state: CreateEventUIState, newNotes: String) =
      state.copy(notes = newNotes)

  /** Handles system or top-bar back requests, asking before losing entered details. */
  fun requestDiscard() {
    updateState { state ->
      when {
        state.isSaving || state.createdEventId != null || state.isDiscarded -> state
        hasChanges(state) -> state.copy(showDiscardConfirmation = true)
        else -> state.copy(isDiscarded = true)
      }
    }
  }

  /** Closes the discard dialog and keeps the entered details. */
  fun cancelDiscard() {
    updateState { it.copy(showDiscardConfirmation = false) }
  }

  /** Confirms that the user wants to leave and discard the entered details. */
  fun confirmDiscard() {
    val urlToDelete = uiState.value.imageUrl
    if (currentState.createdEventId == null && !urlToDelete.isNullOrBlank()) {
      viewModelScope.launch(ioDispatcher) {
        try {
          imageStorage.delete(urlToDelete)
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error deleting image on discard", e)
        }
      }
    }

    updateState {
      if (it.isSaving || it.createdEventId != null) it.copy(showDiscardConfirmation = false)
      else it.copy(showDiscardConfirmation = false, isDiscarded = true)
    }
  }

  /** This deletes the image from firebase if the Viewmodel is destroyed unexpectedly */
  override fun onCleared() {
    val state = currentState
    val imageUrlToDelete = state.imageUrl
    // 3. Fallback cleanup if the ViewModel is destroyed before event creation
    if (state.createdEventId == null && !imageUrlToDelete.isNullOrBlank()) {
      CoroutineScope(ioDispatcher + NonCancellable).launch {
        try {
          imageStorage.delete(imageUrlToDelete)
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error deleting image on ViewModel cleared", e)
        }
      }
    }
  }

  private fun hasChanges(state: CreateEventUIState): Boolean =
      state.name.isNotEmpty() ||
          state.range != DateTimeRange() ||
          state.venue.isNotEmpty() ||
          state.notes.isNotEmpty() ||
          state.hasImage

  /**
   * Saves the form as a new event with the creator as its first member. Ignored while a save runs
   * or once one succeeded; a failure leaves the form as it is and sets
   * [CreateEventUIState.errorMsg].
   */
  fun createEvent() {
    if (!currentState.canCreate) return
    val form = currentState
    val eventId = pendingEventId ?: dataRepository.eventsRepository.getNewUid()
    pendingEventId = eventId
    updateState { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        withTimeout(saveTimeoutMs) { save(form, eventId) }
        updateState { it.copy(isSaving = false, createdEventId = eventId) }
      } catch (e: TimeoutCancellationException) {
        onSaveFailed(eventId, e)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        onSaveFailed(eventId, e)
      }
    }
  }

  private suspend fun save(form: CreateEventUIState, eventId: String) {
    check(userId.isNotBlank()) { "User not signed in" }
    check(!isAnonymous()) { "An anonymous account user can't create an event." }
    val creator = dataRepository.usersRepository.getUser(userId)

    val newEvent = form.toEvent(eventId, creator)
    dataRepository.createEvent(newEvent)
  }

  private suspend fun onSaveFailed(eventId: String, error: Exception) {
    Log.w(TAG, "Creating the event failed, checking whether it was stored", error)
    val alreadySaved =
        try {
          // Note: Firebase throws an error when the event doesn't exist, so this is fine,
          // but if a mock or stub sends null instead of throwing, alreadySaved would be true
          // even though the value is null (keep in mind if testing that behavior).
          withTimeout(saveTimeoutMs) { dataRepository.eventsRepository.getEvent(eventId) }
          true
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "The event could not be checked in time, so it may not be stored")
          false
        } catch (e: CancellationException) {
          throw e
        } catch (checkError: Exception) {
          Log.e(TAG, "The event could not be checked, so it may not be stored", checkError)
          false
        }
    updateState {
      if (alreadySaved) it.copy(isSaving = false, createdEventId = eventId)
      else it.copy(isSaving = false, errorMsg = R.string.create_event_error_save_failed)
    }
  }

  private fun deleteTemporaryImage(file: File) {
    if (file.exists() && !file.delete()) {
      Log.w(LOG_TAG, "Could not delete temporary image ${file.path}")
    }
  }

  internal fun deleteTemporaryImageForTesting(file: File) = deleteTemporaryImage(file)

  /** Opens the delete image confirmation dialog if an image is present. */
  fun requestDeleteImage() {
    if (!uiState.value.hasImage) return
    updateState { it.copy(showDeleteImageConfirmation = true) }
  }

  internal fun setImageUrlForTesting(url: String?) {
    updateState { it.copy(imageUrl = url) }
  }

  /** Closes the delete image confirmation dialog without deleting the image. */
  fun cancelDeleteImage() {
    updateState { it.copy(showDeleteImageConfirmation = false) }
  }

  /** Confirms deleting the image from storage and removes it from the form state. */
  fun confirmDeleteImage() {
    val urlToDelete = uiState.value.imageUrl
    updateState { it.copy(imageUrl = null, showDeleteImageConfirmation = false) }
    if (!urlToDelete.isNullOrEmpty()) {
      CoroutineScope(ioDispatcher + NonCancellable).launch {
        try {
          imageStorage.delete(urlToDelete)
        } catch (e: Exception) {
          Log.e(LOG_TAG, "Error deleting image", e)
        }
      }
    }
  }

  private suspend fun saveImage(storedImage: StoredImage) {
    val oldImageUrl = uiState.value.imageUrl
    var newImageUrl: String? = null
    try {
      newImageUrl = imageStorage.upload(storedImage, "events")
      updateState {
        it.copy(
            imageUrl = newImageUrl,
            isSaving = false,
        )
      }
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
        updateState {
          it.copy(
              errorMsg = R.string.create_event_error_save_failed,
          )
        }
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
      updateState { it.copy(isSaving = false, errorMsg = R.string.create_event_error_save_failed) }
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  fun updateImage(uri: Uri, contentResolver: ContentResolver, cacheDir: File) {
    if (currentState.isSaving) return
    updateState { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
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
          updateState { it.copy(isSaving = false, errorMsg = R.string.profile_picture_too_large) }
        } else {
          temporaryFile = storedImage.file
          saveImage(storedImage)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Unable to read selected image", e)
        updateState {
          it.copy(isSaving = false, errorMsg = R.string.create_event_error_save_failed)
        }
      } finally {
        temporaryFile?.let { file ->
          withContext(NonCancellable + ioDispatcher) { deleteTemporaryImage(file) }
        }
      }
    }
  }
}
