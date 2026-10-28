package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
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
import com.android.festivar.ui.components.startDateError
import com.android.festivar.ui.task.create.END_OF_DAY
import com.android.festivar.ui.task.create.START_OF_DAY
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    get() = startDate?.atTime(startTime ?: START_OF_DAY)?.atZone(zone)

  val endDateTime: ZonedDateTime?
    get() = endDate?.atTime(endTime ?: END_OF_DAY)?.atZone(zone)

  val nameError: FieldError?
    get() = if (nameEdited && name.isBlank()) FieldError.EMPTY_EVENT_NAME else null

  val startDateError: FieldError?
    get() = startDateError(startDate, startTime)

  val endDateError: FieldError?
    get() = endDateError(DateTimeRange(startDate, startTime, endDate, endTime))

  val isValid: Boolean
    get() =
        name.isNotBlank() &&
            startDateTime != null &&
            endDateTime != null &&
            nameError == null &&
            startDateError == null &&
            endDateError == null

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
) : ViewModel() {
  private val _uiState = MutableStateFlow(CreateEventUIState())
  val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

  /** Applies an edit of the form; the save error is stale once the form changes, so it goes. */
  private fun edit(change: (CreateEventUIState) -> CreateEventUIState) = _uiState.update {
    change(it).copy(errorMsg = null)
  }

  fun updateName(newName: String) = edit { it.copy(name = newName, nameEdited = true) }

  fun updateStartDate(newDate: LocalDate) = edit { it.copy(startDate = newDate) }

  fun updateEndDate(newDate: LocalDate) = edit { it.copy(endDate = newDate) }

  /** Sets the time the task starts. */
  fun updateStartTime(time: LocalTime) = edit { it.copy(startTime = time) }

  /** Sets the time the task ends. */
  fun updateEndTime(time: LocalTime) = edit { it.copy(endTime = time) }

  fun updateVenue(newVenue: String) = edit { it.copy(venue = newVenue) }

  fun updateNotes(newNotes: String) = edit { it.copy(notes = newNotes) }

  fun showPicker(newOpenPicker: DateTimeField?) = edit { it.copy(openPicker = newOpenPicker) }

  /** Sets [date] in the field whose picker is open, then closes the picker. */
  fun pickDate(date: LocalDate) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_DATE -> updateStartDate(date)
      DateTimeField.END_DATE -> updateEndDate(date)
      else -> Unit
    }
    showPicker(null)
  }

  /** Sets [time] in the field whose picker is open, then closes the picker. */
  fun pickTime(time: LocalTime) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_TIME -> updateStartTime(time)
      DateTimeField.END_TIME -> updateEndTime(time)
      else -> Unit
    }
    showPicker(null)
  }

  /**
   * Saves the form as a new event with the creator as its first member. Ignored while a save runs
   * or once one succeeded; a failure leaves the form as it is and sets
   * [CreateEventUIState.errorMsg].
   */
  fun createEvent() {
    val state = _uiState.value
    if (!_uiState.value.isValid || state.isSaving || state.createdEventId != null) return
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        save()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isSaving = false, errorMsg = R.string.create_event_error_save_failed)
        }
      }
    }
  }

  private suspend fun save() {
    val creator =
        if (userId.isNotBlank()) {
          try {
            usersRepository.getUser(userId)
          } catch (_: Exception) {
            null
          }
        } else null

    val fallbackCreator = creator ?: if (userId.isNotBlank()) User(uid = userId) else null
    val membersList = if (fallbackCreator != null) listOf(fallbackCreator) else emptyList()

    val newEvent = _uiState.value.toEvent(eventsRepository.getNewUid(), membersList)

    eventsRepository.addEvent(newEvent)

    if (creator != null) {
      try {
        usersRepository.editUser(creator.uid, creator.addJoinedEvent(newEvent.eventId))
      } catch (_: Exception) {
        // If updating user joinedEvents fails, event is still created
      }
    }
    _uiState.update { it.copy(isSaving = false, createdEventId = newEvent.eventId) }
  }
}
