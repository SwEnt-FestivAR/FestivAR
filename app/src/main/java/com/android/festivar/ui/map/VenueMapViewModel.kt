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
import com.android.festivar.model.structure.StructureKind
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

/** What the organizer's palette places on the next tap, with the footprint size in metres. */
enum class PaletteTool(val kind: StructureKind, val width: Double, val height: Double) {
  /** A 12 × 8 m marquee. */
  MARQUEE(StructureKind.MARQUEE, 12.0, 8.0),
  /** A 10 × 6 m stage. */
  STAGE(StructureKind.STAGE, 10.0, 6.0),
  /** A 3 × 3 m stand. */
  STAND(StructureKind.STAND, 3.0, 3.0),
  /** A 2 × 2 m table. */
  TABLE(StructureKind.TABLE, 2.0, 2.0),
  /** An element of the organizer's own, its name and size asked first; 4 × 4 m by default. */
  CUSTOM(StructureKind.CUSTOM, 4.0, 4.0),
}

/** The smallest and largest side a custom element may be given, in metres. */
private val CUSTOM_SIDE = 0.5..200.0

/**
 * A custom element being defined: its name and size, before it is placed.
 *
 * @property label the name typed.
 * @property width the width typed, in metres.
 * @property height the height typed, in metres.
 */
data class CustomDraft(val label: String = "", val width: String = "4", val height: String = "4") {
  /** True when the name is not blank and both sizes are numbers from 0.5 to 200 m. */
  val isValid: Boolean
    get() =
        label.isNotBlank() &&
            width.toDoubleOrNull()?.let { it in CUSTOM_SIDE } == true &&
            height.toDoubleOrNull()?.let { it in CUSTOM_SIDE } == true
}

/** A corner of a structure's footprint on the plan, north up. */
enum class Corner {
  /** North west. */
  TOP_LEFT,
  /** North east. */
  TOP_RIGHT,
  /** South west. */
  BOTTOM_LEFT,
  /** South east. */
  BOTTOM_RIGHT,
}

/** The smallest side of a footprint, in metres: one step of the snap grid. */
private const val MIN_SIDE = 0.5

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

/**
 * The name a new structure of [kind] gets: "Stand", then "Stand 2" when [existing] already holds a
 * stand, "Stand 3" with two, and so on.
 *
 * @param kind what is placed.
 * @param existing the structures already on the plan.
 * @return the default label.
 */
fun defaultLabel(kind: StructureKind, existing: List<Structure>): String {
  val name = kind.name.lowercase().replaceFirstChar { it.uppercase() }
  val count = existing.count { it.kind == kind }
  return if (count == 0) name else "$name ${count + 1}"
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
 * @property isOrganizer true when the current user is the first member of the event, the one the
 *   event details name as the organizer; only they may edit the plan.
 * @property tool what the next tap on the map places, or null.
 * @property canUndo true when a saved change can be undone.
 * @property isSaving true while an edit of the plan is being written.
 * @property renaming true while the rename dialog of the selected structure is open.
 * @property customDraft the custom element being defined while its dialog is open, or null.
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
    val isOrganizer: Boolean = false,
    val tool: PaletteTool? = null,
    val canUndo: Boolean = false,
    val isSaving: Boolean = false,
    val renaming: Boolean = false,
    val customDraft: CustomDraft? = null,
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

  /** The plan before each saved change, the latest last. */
  private val undoStack = ArrayDeque<List<Structure>>()

  /** The confirmed custom element that [PaletteTool.CUSTOM] places. */
  private var custom = CustomDraft()

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
          it.copy(
              eventTitle = event.title,
              eventLocation = event.location,
              venue = event.venue,
              isOrganizer = event.members.firstOrNull()?.uid == it.currentUserId,
          )
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

  /** True when the current user may change the plan now: the organizer, with no write running. */
  private fun canEdit(): Boolean = _uiState.value.isOrganizer && !_uiState.value.isSaving

  /**
   * Picks what the next tap on the map places. Picking the selected tool again, or null, puts it
   * down. Picking [PaletteTool.CUSTOM] first opens [VenueMapUiState.customDraft] to ask its name
   * and size; the tool is picked once the draft is confirmed. Organizer only.
   *
   * @param tool the palette item, or null for none.
   */
  fun selectTool(tool: PaletteTool?) {
    if (!_uiState.value.isOrganizer) return
    _uiState.update {
      when {
        tool == it.tool -> it.copy(tool = null)
        tool == PaletteTool.CUSTOM -> it.copy(customDraft = CustomDraft())
        else -> it.copy(tool = tool)
      }
    }
  }

  /**
   * Replaces the custom element being defined with what is typed.
   *
   * @param label the name typed.
   * @param width the width typed, in metres.
   * @param height the height typed, in metres.
   */
  fun updateCustomDraft(label: String, width: String, height: String) = _uiState.update {
    if (it.customDraft == null) it else it.copy(customDraft = CustomDraft(label, width, height))
  }

  /**
   * Closes the custom element dialog and picks [PaletteTool.CUSTOM] with its name and size, so the
   * next tap places it. Does nothing while the draft is not valid.
   */
  fun confirmCustomDraft() {
    val draft = _uiState.value.customDraft ?: return
    if (!draft.isValid) return
    custom = draft
    _uiState.update { it.copy(customDraft = null, tool = PaletteTool.CUSTOM) }
  }

  /** Closes the custom element dialog without picking a tool. */
  fun dismissCustomDraft() = _uiState.update { it.copy(customDraft = null) }

  /**
   * Places the selected tool's structure centred on the tapped point, snapped to the half metre
   * grid, then selects it and puts the tool down. Organizer only; does nothing without a tool or a
   * venue.
   *
   * @param latitude where the map was tapped.
   * @param longitude where the map was tapped.
   */
  fun placeAt(latitude: Double, longitude: Double) {
    val state = _uiState.value
    val tool = state.tool ?: return
    if (!canEdit()) return
    val (cx, cy) = planPoint(latitude, longitude) ?: return
    val draft = custom.takeIf { tool == PaletteTool.CUSTOM }
    val width = draft?.let { VenueGeometry.snap(it.width.toDouble()) } ?: tool.width
    val height = draft?.let { VenueGeometry.snap(it.height.toDouble()) } ?: tool.height
    val structure =
        Structure(
            structureId = structuresRepository.getNewUid(),
            eventId = eventId,
            kind = tool.kind,
            label = draft?.label?.trim() ?: defaultLabel(tool.kind, state.structures),
            x = VenueGeometry.snap(cx) - width / 2,
            y = VenueGeometry.snap(cy) - height / 2,
            width = width,
            height = height,
        )
    save(state.structures + structure, structure.structureId) {
      structuresRepository.addStructure(structure)
    }
  }

  /**
   * Moves the selected structure so its centre sits on the pressed point, snapped; its size is
   * kept. Organizer only.
   *
   * @param latitude where the map was long pressed.
   * @param longitude where the map was long pressed.
   */
  fun moveSelected(latitude: Double, longitude: Double) {
    val selected = _uiState.value.selected ?: return
    if (!canEdit()) return
    val (cx, cy) = planPoint(latitude, longitude) ?: return
    replace(
        selected.copy(
            x = VenueGeometry.snap(cx) - selected.width / 2,
            y = VenueGeometry.snap(cy) - selected.height / 2,
        )
    )
  }

  /**
   * Moves one corner of the selected structure to the dropped point while the opposite corner
   * stays; each side is snapped and kept at 0.5 m at least. Organizer only.
   *
   * @param corner the corner that was dragged.
   * @param latitude where the corner was dropped.
   * @param longitude where the corner was dropped.
   */
  fun resizeSelected(corner: Corner, latitude: Double, longitude: Double) {
    val s = _uiState.value.selected ?: return
    if (!canEdit()) return
    val (px, py) = planPoint(latitude, longitude) ?: return
    val right = s.x + s.width
    val bottom = s.y + s.height
    val west = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT
    val north = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT
    val width = VenueGeometry.snap(if (west) right - px else px - s.x).coerceAtLeast(MIN_SIDE)
    val height = VenueGeometry.snap(if (north) bottom - py else py - s.y).coerceAtLeast(MIN_SIDE)
    replace(
        s.copy(
            x = if (west) right - width else s.x,
            y = if (north) bottom - height else s.y,
            width = width,
            height = height,
        )
    )
  }

  /** Opens the rename dialog of the selected structure. Organizer only. */
  fun startRename() {
    if (!_uiState.value.isOrganizer || _uiState.value.selected == null) return
    _uiState.update { it.copy(renaming = true) }
  }

  /**
   * Renames the selected structure and closes the dialog. A blank label is refused and the dialog
   * stays open. Organizer only.
   *
   * @param label the new name, trimmed.
   */
  fun rename(label: String) {
    val selected = _uiState.value.selected ?: return
    val trimmed = label.trim()
    if (!canEdit() || trimmed.isEmpty()) return
    _uiState.update { it.copy(renaming = false) }
    replace(selected.copy(label = trimmed))
  }

  /** Closes the rename dialog without renaming. */
  fun cancelRename() = _uiState.update { it.copy(renaming = false) }

  /**
   * Deletes the selected structure and clears the selection. Tasks pinned on it keep their spot,
   * which the pins then skip. Organizer only.
   */
  fun deleteSelected() {
    val state = _uiState.value
    val selected = state.selected ?: return
    if (!canEdit()) return
    save(state.structures - selected, null) {
      structuresRepository.deleteStructure(selected.structureId)
    }
  }

  /**
   * Restores the plan as it was before the last saved change: what is missing is added back, what
   * changed is edited back and what is extra is deleted. Organizer only; does nothing when there is
   * nothing to undo.
   */
  fun undo() {
    if (!canEdit()) return
    val previous = undoStack.removeLastOrNull() ?: return
    val current = _uiState.value.structures
    runWrite(onFailure = { undoStack.addLast(previous) }) {
      val currentById = current.associateBy { it.structureId }
      val previousIds = previous.map { it.structureId }.toSet()
      previous.forEach { old ->
        val now = currentById[old.structureId]
        when {
          now == null -> structuresRepository.addStructure(old)
          now != old -> structuresRepository.editStructure(old.structureId, old)
        }
      }
      current
          .filter { it.structureId !in previousIds }
          .forEach { structuresRepository.deleteStructure(it.structureId) }
      _uiState.update { s ->
        s.copy(
            structures = previous,
            selectedStructureId = s.selectedStructureId?.takeIf { it in previousIds },
            canUndo = undoStack.isNotEmpty(),
        )
      }
    }
  }

  /** Where ([latitude], [longitude]) sits on the plan, or null without a venue. */
  private fun planPoint(latitude: Double, longitude: Double): Pair<Double, Double>? {
    val venue = _uiState.value.venue ?: return null
    return VenueGeometry.metresFrom(
        venue.latitude,
        venue.longitude,
        venue.bearing,
        latitude,
        longitude,
    )
  }

  /** Writes [changed] over the structure with its id, keeping the selection. */
  private fun replace(changed: Structure) {
    val structures = _uiState.value.structures
    save(
        structures.map { if (it.structureId == changed.structureId) changed else it },
        changed.structureId,
    ) {
      structuresRepository.editStructure(changed.structureId, changed)
    }
  }

  /**
   * Runs [write] against the repository; on success shows [next] with [selectedId] selected and the
   * tool put down.
   */
  private fun save(next: List<Structure>, selectedId: String?, write: suspend () -> Unit) {
    val previous = _uiState.value.structures
    runWrite {
      write()
      undoStack.addLast(previous)
      _uiState.update {
        it.copy(
            structures = next,
            selectedStructureId = selectedId,
            selectedTaskId = if (selectedId != null) null else it.selectedTaskId,
            tool = null,
            canUndo = true,
        )
      }
    }
  }

  /**
   * Runs [block] with [VenueMapUiState.isSaving] set. A failure is logged, sets `map_save_failed`,
   * calls [onFailure], and reloads the structures so the screen shows what the repository holds,
   * whatever part of the write went through.
   */
  private fun runWrite(onFailure: () -> Unit = {}, block: suspend () -> Unit) {
    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      try {
        block()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error saving the venue plan of event $eventId", e)
        onFailure()
        val stored =
            try {
              structuresRepository.getAllStructures(eventId)
            } catch (c: CancellationException) {
              throw c
            } catch (reload: Exception) {
              Log.e(LOG_TAG, "Error reloading the venue plan of event $eventId", reload)
              null
            }
        _uiState.update { s ->
          val structures = stored ?: s.structures
          s.copy(
              structures = structures,
              selectedStructureId =
                  s.selectedStructureId?.takeIf { id -> structures.any { it.structureId == id } },
              errorMsg = R.string.map_save_failed,
          )
        }
      } finally {
        _uiState.update { it.copy(isSaving = false) }
      }
    }
  }
}
