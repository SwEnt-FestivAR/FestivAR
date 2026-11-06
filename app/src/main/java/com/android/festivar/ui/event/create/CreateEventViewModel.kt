package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryProvider
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.endDateError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

  fun toEvent(eventId: String, membersList: List<User>): Event {
    require(isValid) { "The form is not valid." }
    return Event(
        eventId = eventId,
        title = name,
        description = notes,
        members = membersList,
        startDate = startDateTime!!,
        endDate = endDateTime!!,
        location = venue,
    )
  }
}

private const val TAG = "CreateEventViewModel"

/**
 * Holds the create event form, validates it and saves the event with the signed-in user as its
 * first member.
 *
 * @property eventsRepository where the event is written.
 * @property usersRepository where the creator's profile is read and its joined events updated.
 * @property userId the uid of the signed-in user, blank when nobody is.
 */
class CreateEventViewModel(
    private val eventsRepository: EventsRepository,
    private val usersRepository: UsersRepository = UsersRepositoryProvider.repository,
    private val userId: String = "",
) : EventFormViewModel<CreateEventUIState>(CreateEventUIState()) {
  val uiState: StateFlow<CreateEventUIState> = formState

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
    updateState { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        save()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, e.message ?: "Saving the event failed", e)
        updateState {
          it.copy(isSaving = false, errorMsg = R.string.create_event_error_save_failed)
        }
      }
    }
  }

  private suspend fun save() {
    check(userId.isNotBlank()) { "User not signed in" }
    val creator = usersRepository.getUser(userId)

    val newEvent = currentState.toEvent(eventsRepository.getNewUid(), listOf(creator))
    eventsRepository.addEvent(newEvent)
    try {
      usersRepository.editUser(creator.uid, creator.addJoinedEvent(newEvent.eventId))
    } catch (e: Exception) {
      // Do not leave an event that the creator's profile does not know about
      withContext(NonCancellable) { runCatching { eventsRepository.deleteEvent(newEvent.eventId) } }
      throw e
    }
    updateState { it.copy(isSaving = false, createdEventId = newEvent.eventId) }
  }
}
