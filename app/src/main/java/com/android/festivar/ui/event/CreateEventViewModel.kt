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
import java.time.LocalDate
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
    val startDate: ZonedDateTime? = null,
    val endDate: ZonedDateTime? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val venue: String = "",
    val notes: String = "",
    val activeDatePicker: DateTimeField? = null,
    val errorName: Boolean = false,
    val errorDate: Boolean = false,
    val errorVenue: Boolean = false,
    val isSaving: Boolean = false,
    val createdEventId: String? = null,
    @StringRes val errorMsg: Int? = null,
)

/**
 * Holds the create event form, validates it and saves the event with the signed-in user as its
 * first member.
 *
 * @property eventsRepository where the event is written.
 * @property usersRepository where the creator's profile is read and its joined events updated; the
 *   provider's repository when null.
 * @property userId the uid of the signed-in user, blank when nobody is.
 */
class CreateEventViewModel(
    private val eventsRepository: EventsRepository,
    private val usersRepository: UsersRepository? = null,
    private val userId: String = "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(CreateEventUIState())
  val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

  /** Applies an edit of the form; the save error is stale once the form changes, so it goes. */
  private fun edit(change: (CreateEventUIState) -> CreateEventUIState) {
    _uiState.update { change(it).copy(errorMsg = null) }
  }

  fun updateName(newName: String) {
    edit { it.copy(name = newName) }
  }

  fun updateStartDate(newDate: ZonedDateTime) {
    edit { it.copy(startDate = newDate) }
  }

  fun updateEndDate(newDate: ZonedDateTime) {
    edit { it.copy(endDate = newDate) }
  }

  fun updateStartDateMillis(newDateMillis: Long) {
    _uiState.update { it.copy(startDateMillis = newDateMillis) }
  }

  fun updateEndDateMillis(newDateMillis: Long) {
    _uiState.update { it.copy(endDateMillis = newDateMillis) }
  }

  fun updateVenue(newVenue: String) {
    edit { it.copy(venue = newVenue) }
  }

  fun updateNotes(newNotes: String) {
    edit { it.copy(notes = newNotes) }
  }

  fun updateActiveDatePicker(newActiveDatePicker: DateTimeField?) {
    _uiState.update { it.copy(activeDatePicker = newActiveDatePicker) }
  }

  fun pickDate(date: LocalDate) {
    val zone = ZoneId.systemDefault()
    when (_uiState.value.activeDatePicker) {
      DateTimeField.START_DATE -> {
        val start = date.atStartOfDay(zone)
        updateStartDateMillis(start.toInstant().toEpochMilli())
        updateStartDate(start)
      }
      DateTimeField.END_DATE -> {
        val end = date.plusDays(1).atStartOfDay(zone).minusSeconds(1)
        updateEndDateMillis(end.toInstant().toEpochMilli())
        updateEndDate(end)
      }
      else -> Unit
    }
    updateActiveDatePicker(null)
  }

  fun removeErrorName() {
    _uiState.update { it.copy(errorName = false) }
  }

  fun removeErrorDate() {
    _uiState.update { it.copy(errorDate = false) }
  }

  fun removeErrorVenue() {
    _uiState.update { it.copy(errorVenue = false) }
  }

  fun setErrors() {
    _uiState.update {
      it.copy(
          errorName = _uiState.value.name.isBlank(),
          errorDate = !endDateComesAfterStartDate(),
          errorVenue = _uiState.value.venue.isBlank(),
      )
    }
  }

  fun endDateComesAfterStartDate(): Boolean {
    val state = _uiState.value
    // Event itself requires a strictly later end, so equal dates are refused here, not thrown
    return state.startDate != null &&
        state.endDate != null &&
        state.endDate.isAfter(state.startDate)
  }

  fun formIsComplete(): Boolean {
    return _uiState.value.name.isNotBlank() &&
        _uiState.value.startDate != null &&
        _uiState.value.endDate != null &&
        _uiState.value.venue.isNotBlank()
  }

  fun validEvent(): Boolean {
    return formIsComplete() && endDateComesAfterStartDate()
  }

  /**
   * Saves the form as a new event with the creator as its first member. Ignored while a save runs
   * or once one succeeded; a failure leaves the form as it is and sets
   * [CreateEventUIState.errorMsg].
   */
  fun createEvent() {
    val state = _uiState.value
    if (state.isSaving || state.createdEventId != null) return
    if (!validEvent()) {
      setErrors()
      return
    }
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
          val repo = usersRepository ?: UsersRepositoryProvider.repository
          try {
            repo.getUser(userId)
          } catch (_: Exception) {
            null
          }
        } else null

    val fallbackCreator = creator ?: if (userId.isNotBlank()) User(uid = userId) else null
    val membersList = if (fallbackCreator != null) listOf(fallbackCreator) else emptyList()

    val newEvent =
        Event(
            eventId = eventsRepository.getNewUid(),
            title = _uiState.value.name,
            description = _uiState.value.notes,
            members = membersList,
            startDate = _uiState.value.startDate!!,
            endDate = _uiState.value.endDate!!,
            location = _uiState.value.venue,
        )

    eventsRepository.addEvent(newEvent)

    if (creator != null) {
      val repo = usersRepository ?: UsersRepositoryProvider.repository
      try {
        repo.editUser(creator.uid, creator.addJoinedEvent(newEvent.eventId))
      } catch (_: Exception) {
        // If updating user joinedEvents fails, event is still created
      }
    }
    _uiState.update { it.copy(isSaving = false, createdEventId = newEvent.eventId) }
  }
}
