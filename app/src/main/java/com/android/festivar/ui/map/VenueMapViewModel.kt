// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.location.LocationSource
import com.android.festivar.model.location.LocationSourceProvider
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructuresRepository
import com.android.festivar.model.structure.StructuresRepositoryProvider
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "VenueMapViewModel"

/** Characters of a task title kept in its pin label before the ellipsis. */
private const val PIN_TITLE_MAX = 18

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Which task pins the map shows. */
enum class PinFilter {
  /** Only the tasks the current user holds. */
  MINE,
  /** Every task with a spot. */
  ALL,
  /** No task pin, only the structures. */
  STRUCTURES,
}

/** The look of a task pin, by the same rule as the badge of the task details. */
enum class PinStatus {
  /** Not done, and a place is left on it. */
  OPEN,
  /** Not done, and the current user holds a place on it. */
  MINE,
  /** Not done, and every place is held by someone else. */
  TAKEN,
  /** The task is completed. */
  DONE,
}

/**
 * A task drawn on the map.
 *
 * @property taskId the task the pin stands for.
 * @property title the full title of the task.
 * @property point where the pin sits on the earth.
 * @property status how the pin is tinted.
 * @property label the text of the pill next to the pin, "Power · 09:30".
 */
data class MapPin(
    val taskId: String,
    val title: String,
    val point: LatLngPoint,
    val status: PinStatus,
    val label: String,
)

/**
 * The task the sheet describes once its pin is tapped.
 *
 * @property taskId the task, for the "Open task" button.
 * @property title the title of the task.
 * @property place where the task takes place: its location text, else the label of its structure.
 * @property withName the short name of another person holding it, "Sami K.", or null.
 * @property time the start time, "09:30", or null without one.
 * @property minutesUntil minutes from now to the start, negative once started, or null without a
 *   start.
 * @property distanceMetres the straight line distance from the device, or null without a fix.
 * @property walkMinutes minutes on foot for [distanceMetres], or null without a fix.
 */
data class FocusTask(
    val taskId: String,
    val title: String,
    val place: String,
    val withName: String?,
    val time: String?,
    val minutesUntil: Long?,
    val distanceMetres: Int?,
    val walkMinutes: Int?,
)

/**
 * The short name of [user] shown next to a task, "Sami K.": the name, then the first letter of the
 * surname and a dot. Only the name when the surname is blank.
 *
 * @param user the person to name.
 * @return the short name, blank when the user has neither name nor surname.
 */
fun shortName(user: User): String {
  val name = user.name.trim()
  val surname = user.surname.trim()
  return if (surname.isEmpty()) name else "$name ${surname.first()}.".trim()
}

/** "Run power" stays whole, a title over 18 characters is cut there with an ellipsis. */
private fun pinLabel(task: Task): String {
  val title =
      if (task.title.length > PIN_TITLE_MAX) task.title.take(PIN_TITLE_MAX) + "…" else task.title
  return task.startTime?.let { "$title · ${it.format(TIME_FORMAT)}" } ?: title
}

/**
 * UI state of the read only venue map.
 *
 * @property isLoading true while the event and its structures are being loaded.
 * @property eventTitle the title of the event, shown in the title pill.
 * @property eventLocation the free text location of the event, shown in the sheet.
 * @property venue the centre and bearing of the venue, or null when the organizer has not placed
 *   it.
 * @property structures the footprints drawn on the venue.
 * @property me the last position fix of the device, or null while there is none.
 * @property selectedStructureId the structure the sheet describes, or null for the venue summary.
 * @property errorMsg the string resource of the last load failure, or null.
 * @property tasks the tasks of the event; those with a spot become pins.
 * @property filter which task pins are shown.
 * @property query the search text; pins whose title or location contain it are kept, any case.
 * @property selectedTaskId the task the sheet describes, or null.
 * @property currentUserId the uid of the signed-in user, to tell which tasks are theirs.
 * @property now the time the "in 40 min" line counts from, or null when unknown.
 */
data class VenueMapUiState(
    val isLoading: Boolean = true,
    val eventTitle: String = "",
    val eventLocation: String = "",
    val venue: Venue? = null,
    val structures: List<Structure> = emptyList(),
    val me: LocationFix? = null,
    val selectedStructureId: String? = null,
    @StringRes val errorMsg: Int? = null,
    val tasks: List<Task> = emptyList(),
    val filter: PinFilter = PinFilter.MINE,
    val query: String = "",
    val selectedTaskId: String? = null,
    val currentUserId: String = "",
    val now: ZonedDateTime? = null,
) {
  /** The structure matching [selectedStructureId], or null when none is selected or it is gone. */
  val selected: Structure?
    get() = structures.find { it.structureId == selectedStructureId }

  /**
   * Every task that can be placed, whatever the filter and the query: it has a spot, the venue is
   * placed and, for a spot on a structure, the structure still exists.
   */
  private val placed: List<Pair<Task, MapPin>>
    get() {
      val venue = venue ?: return emptyList()
      return tasks.mapNotNull { task ->
        val spot = task.spot ?: return@mapNotNull null
        val structure = spot.structureId?.let { id -> structures.find { it.structureId == id } }
        val x = if (spot.structureId != null) structure?.centerX else spot.x
        val y = if (spot.structureId != null) structure?.centerY else spot.y
        if (x == null || y == null) return@mapNotNull null
        val point = VenueGeometry.latLngFrom(venue.latitude, venue.longitude, venue.bearing, x, y)
        task to MapPin(task.taskId, task.title, point, statusOf(task), pinLabel(task))
      }
    }

  private fun isMine(task: Task) = task.assignees.any { it.uid == currentUserId }

  private fun statusOf(task: Task): PinStatus =
      when {
        task.completed -> PinStatus.DONE
        isMine(task) -> PinStatus.MINE
        task.assignees.size >= task.maxAssign -> PinStatus.TAKEN
        else -> PinStatus.OPEN
      }

  /** The task pins to draw: the placed tasks kept by [filter], then by [query]. */
  val pins: List<MapPin>
    get() {
      val needle = query.trim()
      return placed
          .filter { (task, _) ->
            when (filter) {
              PinFilter.MINE -> isMine(task)
              PinFilter.ALL -> true
              PinFilter.STRUCTURES -> false
            }
          }
          .filter { (task, _) ->
            needle.isEmpty() ||
                task.title.contains(needle, ignoreCase = true) ||
                task.location.contains(needle, ignoreCase = true)
          }
          .map { it.second }
    }

  /** How many placed tasks the current user holds, for the "My pins" chip. */
  val mineCount: Int
    get() = placed.count { (task, _) -> isMine(task) }

  /** How many tasks are placed, for the "All" chip. */
  val allCount: Int
    get() = placed.size

  /** The selected task as the sheet describes it, or null when no shown pin is selected. */
  val focus: FocusTask?
    get() {
      val id = selectedTaskId ?: return null
      val pin = pins.find { it.taskId == id } ?: return null
      val task = tasks.first { it.taskId == id }
      val partner = task.assignees.firstOrNull { it.uid != currentUserId }
      val place =
          task.location.ifBlank {
            structures.find { it.structureId == task.spot?.structureId }?.label.orEmpty()
          }
      val distance = venue?.let { v ->
        me?.let { fix ->
          val (mx, my) =
              VenueGeometry.metresFrom(v.latitude, v.longitude, fix.latitude, fix.longitude)
          val (px, py) =
              VenueGeometry.metresFrom(
                  v.latitude,
                  v.longitude,
                  pin.point.latitude,
                  pin.point.longitude,
              )
          VenueGeometry.distanceMetres(mx, my, px, py)
        }
      }
      val start = task.startTime
      return FocusTask(
          taskId = task.taskId,
          title = task.title,
          place = place,
          withName = partner?.let(::shortName)?.takeIf { it.isNotBlank() },
          time = start?.format(TIME_FORMAT),
          minutesUntil = start?.let { s -> now?.let { Duration.between(it, s).toMinutes() } },
          distanceMetres = distance?.roundToInt(),
          walkMinutes = distance?.let(VenueGeometry::walkingMinutes),
      )
    }
}

/**
 * Holds the state of the read only venue map: the event's venue, its structures and the device's
 * position.
 *
 * Nothing loads until [load] is called, and the position is only followed once [observeLocation] is
 * called, after the location permission is granted.
 *
 * @param eventId the event whose venue is shown.
 * @param currentUserId the uid of the signed-in user, to tell which tasks are theirs.
 * @param eventsRepository where the event is read.
 * @param structuresRepository where the structures of the event are read.
 * @param locationSource the device's position, or null when none is available.
 * @param tasksRepository where the tasks of the event are read.
 * @param clock the current time, for how long until a task starts.
 */
class VenueMapViewModel(
    private val eventId: String,
    currentUserId: String,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.repository,
    private val structuresRepository: StructuresRepository =
        StructuresRepositoryProvider.repository,
    private val locationSource: LocationSource? = LocationSourceProvider.source,
    private val tasksRepository: TasksRepository = TasksRepositoryProvider.repository,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) : ViewModel() {
  private val _uiState = MutableStateFlow(VenueMapUiState(currentUserId = currentUserId))

  /** The state the screen shows. */
  val uiState: StateFlow<VenueMapUiState> = _uiState.asStateFlow()

  // isLoading starts true, so it cannot tell a running load from the initial state.
  private var loadJob: Job? = null
  private var locationJob: Job? = null

  /**
   * Loads the event, then its structures, then its tasks. A failure sets [VenueMapUiState.errorMsg]
   * and keeps what already loaded; a success clears it. Does nothing while another load is running.
   */
  fun load() {
    if (loadJob?.isActive == true) return
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update {
          it.copy(eventTitle = event.title, eventLocation = event.location, venue = event.venue)
        }
        val structures = structuresRepository.getAllStructures(eventId)
        _uiState.update { it.copy(structures = structures) }
        val tasks = tasksRepository.getAllTasks(eventId)
        _uiState.update {
          it.copy(tasks = tasks, isLoading = false, errorMsg = null, now = clock())
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading the venue map of event $eventId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = R.string.map_load_failed) }
      }
    }
  }

  /**
   * Selects the structure with [structureId], or clears the selection when null. Selecting a
   * structure deselects the task.
   *
   * @param structureId the structure the sheet should describe, or null for the venue summary.
   */
  fun select(structureId: String?) = _uiState.update {
    it.copy(
        selectedStructureId = structureId,
        selectedTaskId = if (structureId != null) null else it.selectedTaskId,
    )
  }

  /**
   * Selects the task with [taskId], or clears the selection when null. Selecting a task deselects
   * the structure and refreshes the time its start is counted from.
   *
   * @param taskId the task the sheet should describe, or null.
   */
  fun selectTask(taskId: String?) = _uiState.update {
    if (taskId == null) it.copy(selectedTaskId = null)
    else it.copy(selectedTaskId = taskId, selectedStructureId = null, now = clock())
  }

  /**
   * Shows the task pins kept by [filter].
   *
   * @param filter which task pins to show.
   */
  fun setFilter(filter: PinFilter) = _uiState.update { it.copy(filter = filter) }

  /**
   * Keeps the task pins whose title or location contain [query], any case.
   *
   * @param query the search text, blank for every pin.
   */
  fun setQuery(query: String) = _uiState.update { it.copy(query = query) }

  /**
   * Starts following the device's position into [VenueMapUiState.me]. Only the first call
   * subscribes; later calls, and a null location source, do nothing.
   */
  fun observeLocation() {
    val source = locationSource ?: return
    if (locationJob != null) return
    locationJob = viewModelScope.launch {
      try {
        source.updates().collect { fix -> _uiState.update { it.copy(me = fix) } }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // A lost provider is the no fix state, not an error worth a message.
        Log.w(LOG_TAG, "Location updates stopped", e)
        _uiState.update { it.copy(me = null) }
      }
    }
  }

  /** Clears the error message. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }
}
