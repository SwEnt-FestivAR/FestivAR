package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.endDateError
import com.android.festivar.ui.event.EventFormViewModel
import com.android.festivar.ui.event.computeEndDateTime
import com.android.festivar.ui.event.computeStartDateTime
import com.android.festivar.ui.event.isEventFormValid
import com.android.festivar.ui.event.validateEventName
import com.android.festivar.ui.event.validateStartDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val venue: String = "",
    val notes: String = "",
    val openPicker: DateTimeField? = null,
    val isSaving: Boolean = false,
    val createdEventId: String? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
    @StringRes val errorMsg: Int? = null,
) {
  val startDateTime: ZonedDateTime?
    get() = computeStartDateTime(startDate, startTime, zone)

  val endDateTime: ZonedDateTime?
    get() = computeEndDateTime(endDate, endTime, zone)

  val nameError: FieldError?
    get() = validateEventName(name, nameEdited)

  val startDateError: FieldError?
    get() = validateStartDate(startDate, startTime)

  val endDateError: FieldError?
    get() = endDateError(DateTimeRange(startDate, startTime, endDate, endTime), zone)

  val isValid: Boolean
    get() =
        isEventFormValid(
            true,
            name,
            startDateTime,
            endDateTime,
            nameError,
            startDateError,
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
 */
class CreateEventViewModel(
    private val dataRepository: DataRepository,
    private val userId: String = "",
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
) : EventFormViewModel<CreateEventUIState>(CreateEventUIState()) {
  val uiState: StateFlow<CreateEventUIState> = formState
  private var pendingEventId: String? = null

  override fun clearError(state: CreateEventUIState) = state.copy(errorMsg = null)

  override fun updateNameState(state: CreateEventUIState, newName: String) =
      state.copy(name = newName, nameEdited = true)

  override fun updateStartDateState(state: CreateEventUIState, newDate: LocalDate) =
      state.copy(startDate = newDate)

  override fun updateEndDateState(state: CreateEventUIState, newDate: LocalDate) =
      state.copy(endDate = newDate)

  override fun updateStartTimeState(state: CreateEventUIState, time: LocalTime) =
      state.copy(startTime = time)

  override fun updateEndTimeState(state: CreateEventUIState, time: LocalTime) =
      state.copy(endTime = time)

  override fun updateVenueState(state: CreateEventUIState, newVenue: String) =
      state.copy(venue = newVenue)

  override fun updateNotesState(state: CreateEventUIState, newNotes: String) =
      state.copy(notes = newNotes)

  override fun updatePickerState(state: CreateEventUIState, picker: DateTimeField?) =
      state.copy(openPicker = picker)

  override fun currentPicker(state: CreateEventUIState) = state.openPicker

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
    updateState { it.copy(isSaving = true, errorMsg = null, openPicker = null) }
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
    val creator = dataRepository.usersRepository.getUser(userId)

    val newEvent = form.toEvent(eventId, creator)
    dataRepository.createEvent(newEvent)
  }

  private suspend fun onSaveFailed(eventId: String, error: Exception) {
    Log.w(TAG, "Creating the event failed, checking whether it was stored", error)
    val alreadySaved =
        try {
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
}
