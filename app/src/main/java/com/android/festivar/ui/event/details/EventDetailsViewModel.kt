package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.canKick
import com.android.festivar.model.event.canSetRole
import com.android.festivar.model.event.role.Permission
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "EventDetailsViewModel"

/**
 * What the current user can do on one member of the event, shown in the window that opens when they
 * tap this member in the members list.
 *
 * @property assignableRoles the roles the current user can give to this member, from the highest to
 *   the lowest. It includes the member's current role when the current user could give it, so the
 *   window can mark it as current. Empty if the current user cannot change this member's role.
 * @property canKick whether the window offers to kick this member out of the event.
 */
data class MemberPermissions(
    val assignableRoles: List<Role> = emptyList(),
    val canKick: Boolean = false,
) {
  /** Whether the window offers to change the role of this member. */
  val canChangeRole: Boolean
    get() = assignableRoles.isNotEmpty()
}

/**
 * What the current user can do on the Event Details screen, decided by their role in the event.
 * Everything is `false` or empty by default, so nothing is offered while the event loads or after
 * an error.
 *
 * @property canEditEvent whether the screen offers to edit (and close) the event.
 * @property canDeleteEvent whether the screen offers to delete the event.
 * @property canSeeMembers whether the screen shows the members of the event.
 * @property canSeeTasks whether the screen shows the number of open tasks and the "See tasks"
 *   button.
 * @property canManageRoles whether the role of the current user lets them change roles at all. What
 *   they can do on each member is in [members].
 * @property members what the current user can do on each member, by [User.uid]. A member who is
 *   missing gets no action: [MemberPermissions] by default.
 */
data class EventDetailsPermissions(
    val canEditEvent: Boolean = false,
    val canDeleteEvent: Boolean = false,
    val canSeeMembers: Boolean = false,
    val canSeeTasks: Boolean = false,
    val canManageRoles: Boolean = false,
    val members: Map<String, MemberPermissions> = emptyMap(),
) {
  /** What the current user can do on the member [userId]: no action if they are unknown. */
  fun onMember(userId: String): MemberPermissions = members[userId] ?: MemberPermissions()
}

/**
 * UI state of the Event Details screen.
 *
 * @property event the loaded event, or null until loading succeeds.
 * @property permissions what the current user can do in the loaded event; nothing until it loads.
 * @property isUpdating true while a change of role or a kick is being saved, so a second tap does
 *   nothing.
 * @property errorMsg the string resource of the latest failure, or null. Without an [event], the
 *   loading failed; with one, a change could not be saved and the event shown is unchanged.
 */
data class EventDetailsUIState(
    val event: Event? = null,
    val permissions: EventDetailsPermissions = EventDetailsPermissions(),
    val isUpdating: Boolean = false,
    @StringRes val errorMsg: Int? = null,
)

/**
 * Loads and exposes the event shown by the Event Details screen.
 *
 * Loading starts when the screen calls [fetchEvent]. A failed reload clears the currently displayed
 * event and exposes [EventDetailsUIState.errorMsg] so the screen can show an error.
 *
 * @param dataRepository repository giving access to the events repository used to retrieve the
 *   event.
 * @param eventId identifier of the event to load.
 * @param currentUserId identifier of the signed-in user, whose role in the event decides the
 *   [EventDetailsUIState.permissions].
 */
class EventDetailsViewModel(
    private val dataRepository: DataRepository,
    private val eventId: String,
    private val currentUserId: String,
) : ViewModel() {

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository

  private val _uiState = MutableStateFlow(EventDetailsUIState())
  val uiState: StateFlow<EventDetailsUIState> = _uiState.asStateFlow()

  /**
   * Loads [eventId] and updates the UI state with either the event and the permissions of the
   * current user in it, or a loading error. A current user who is not a member of the event gets a
   * loading error too.
   */
  fun fetchEvent() {
    if (_uiState.value.isUpdating) return
    viewModelScope.launch {
      _uiState.update { EventDetailsUIState() }
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update { it.copy(event = event, permissions = permissionsIn(event)) }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        _uiState.update { it.copy(errorMsg = R.string.event_details_fetch_error) }
      }
    }
  }

  /**
   * Gives [newRole] to the member [userId] and saves it, then shows the saved event with the
   * permissions recomputed. The role is set on the latest stored event, so a change made meanwhile
   * by someone else is kept. Does nothing while the event is not loaded or another change is being
   * saved; on failure, [EventDetailsUIState.errorMsg] is set and the event shown is unchanged.
   *
   * @param userId The [User.uid] of the member whose role changes.
   * @param newRole The [Role] they get, among [MemberPermissions.assignableRoles].
   */
  fun changeRole(userId: String, newRole: Role) {
    save(R.string.event_details_change_role_failed) {
      val updated = eventsRepository.getEvent(eventId).setRole(currentUserId, userId, newRole)
      eventsRepository.editEvent(eventId, updated)
      updated
    }
  }

  /**
   * Kicks the member [userId] out of the event with [DataRepository.kickMember], then shows the
   * event without them. Does nothing while the event is not loaded or another change is being
   * saved; on failure, [EventDetailsUIState.errorMsg] is set and the event shown is unchanged.
   *
   * @param userId The [User.uid] of the member to kick.
   */
  fun kickMember(userId: String) =
      save(R.string.event_details_kick_failed) {
        dataRepository.kickMember(eventId, currentUserId, userId)
        eventsRepository.getEvent(eventId)
      }

  /** Clears [EventDetailsUIState.errorMsg] once the screen has shown it. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  /**
   * Runs [applyAndResult] unless the event is not loaded or another change is being saved, then
   * shows the event it returns with the permissions recomputed, or [failedMsg] if it fails.
   * [EventDetailsUIState.isUpdating] is set before the save starts, so a second tap does nothing
   * until it ends.
   */
  private fun save(@StringRes failedMsg: Int, applyAndResult: suspend () -> Event) {
    val canSave = _uiState.value.event != null && !_uiState.value.isUpdating
    if (!canSave) return
    _uiState.update { it.copy(isUpdating = true, errorMsg = null) }

    viewModelScope.launch {
      try {
        val saved = applyAndResult()
        _uiState.update {
          it.copy(event = saved, permissions = permissionsIn(saved), isUpdating = false)
        }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error saving a change of event $eventId", e)
        _uiState.update { it.copy(isUpdating = false, errorMsg = failedMsg) }
      }
    }
  }

  /**
   * The permissions of the current user in [event].
   *
   * @throws IllegalArgumentException If the current user is not a member of [event].
   */
  private fun permissionsIn(event: Event): EventDetailsPermissions =
      EventDetailsPermissions(
          canEditEvent = event.canAct(currentUserId, Permission.CAN_EDIT_EVENT),
          canDeleteEvent = event.canAct(currentUserId, Permission.CAN_DELETE_EVENT),
          canSeeMembers = event.canAct(currentUserId, Permission.CAN_SEE_MEMBERS),
          canSeeTasks = event.canAct(currentUserId, Permission.CAN_SEE_TASK),
          canManageRoles = event.canAct(currentUserId, Permission.CAN_MANAGE_ROLES),
          members = event.members.associate { it.uid to memberPermissionsIn(event, it.uid) },
      )

  /** What the current user can do on the member [userId] of [event]. */
  private fun memberPermissionsIn(event: Event, userId: String): MemberPermissions =
      MemberPermissions(
          assignableRoles =
              Role.entries.reversed().filter { event.canSetRole(currentUserId, userId, it) },
          canKick = event.canKick(currentUserId, userId),
      )
}
