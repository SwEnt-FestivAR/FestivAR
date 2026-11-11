// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.components.Hairline
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.components.PinKind
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.TaskRow
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.priorityLabelRes
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MyTasksScreenTestTags {
  const val TITLE = "myTasksTitle"
  const val SEARCH = "myTasksSearch"
  const val SEARCH_FIELD = "myTasksSearchField"
  const val FILTERS_TOGGLE = "myTasksFiltersToggle"
  const val FILTERS_SUMMARY = "myTasksFiltersSummary"
  const val FILTER_CHIPS = "myTasksFilterChips"
  const val FILTERS_CLEAR = "myTasksFiltersClear"
  const val EVENTS_TOGGLE = "myTasksEventsToggle"
  const val EVENTS_PANEL = "myTasksEventsPanel"
  const val EVENTS_CLEAR = "myTasksEventsClear"
  const val LOADING = "myTasksLoading"
  const val EMPTY = "myTasksEmpty"
  const val NO_MATCH = "myTasksNoMatch"
  const val CLEAR_FILTERS = "myTasksClearFilters"
  const val ERROR = "myTasksError"
  const val RETRY = "myTasksRetry"
  const val LIST = "myTasksList"

  fun statusChip(status: MyTaskStatus) = "myTasksStatus${status.name}"

  fun priorityChip(priority: Priority) = "myTasksPriority${priority.name}"

  fun eventOption(eventId: String) = "myTasksEvent$eventId"

  fun row(taskId: String) = "myTasksRow$taskId"

  /** One tag per day; the tasks without a start share the "anytime" one. */
  fun section(day: LocalDate?) = "myTasksSection_" + (day?.toString() ?: "anytime")
}

// The priority chips, most urgent first, as the design shows them.
private val PRIORITY_ORDER =
    listOf(Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.NONE)

/**
 * My tasks: every task the signed-in user has taken, across their events. The list reloads every
 * time the screen comes back to the front, so a task taken or given back elsewhere shows up on
 * return. The bottom bar is not part of this screen: the nav graph places it around it.
 *
 * @param viewModel holds the user's tasks; the nav graph builds it with the signed-in user.
 * @param onOpenTask Called with the task's id when a row is tapped, to open its task details.
 */
@Composable
fun MyTasksScreen(
    viewModel: MyTasksViewModel,
    onOpenTask: (taskId: String) -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
  MyTasksContent(
      state = state,
      onToggleSearch = viewModel::toggleSearch,
      onQueryChange = viewModel::setQuery,
      onToggleStatus = viewModel::toggleStatus,
      onTogglePriority = viewModel::togglePriority,
      onToggleEvent = viewModel::toggleEvent,
      onClearChips = viewModel::clearChips,
      onClearEvents = viewModel::clearEvents,
      onClearFilters = viewModel::clearFilters,
      onRetry = viewModel::refresh,
      onOpenTask = onOpenTask,
  )
}

/**
 * The stateless My tasks: everything it shows comes from [state]. Only whether the Filters zone and
 * the Events selector are unfolded lives here, since it is pure layout.
 *
 * @param state what the screen shows: the rows, the filters, the counts, loading and errors.
 * @param onToggleSearch when we click the search icon of the top bar; opens or closes the search
 *   field.
 * @param onQueryChange when we type in the search field, with the new text.
 * @param onToggleStatus when we click a status chip (To do, Done), with its status.
 * @param onTogglePriority when we click a priority chip, with its priority.
 * @param onToggleEvent when we click an event line of the events selector, with the event's id.
 * @param onClearChips when we click "Clear" in the unfolded Filters zone; unselects every status
 *   and priority chip.
 * @param onClearEvents when we click "Clear" in the unfolded events selector; unchecks every event.
 * @param onClearFilters when we click "Clear filters" in the "No tasks match" state; drops every
 *   filter and the search.
 * @param onRetry when we click "Retry" in the error snackbar; reloads the tasks.
 * @param onOpenTask when we click a task row, with the task's id; opens its details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksContent(
    state: MyTasksUiState,
    onToggleSearch: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onToggleStatus: (MyTaskStatus) -> Unit = {},
    onTogglePriority: (Priority) -> Unit = {},
    onToggleEvent: (String) -> Unit = {},
    onClearChips: () -> Unit = {},
    onClearEvents: () -> Unit = {},
    onClearFilters: () -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
  var filtersOpen by rememberSaveable { mutableStateOf(false) }
  var eventsOpen by rememberSaveable { mutableStateOf(false) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  stringResource(R.string.mytasks_title),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(MyTasksScreenTestTags.TITLE),
              )
            },
            actions = {
              IconButton(onClick = onToggleSearch, Modifier.testTag(MyTasksScreenTestTags.SEARCH)) {
                Icon(Icons.Filled.Search, stringResource(R.string.tasks_search))
              }
            },
        )
      },
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
        if (state.searchOpen) {
          OutlinedTextField(
              value = state.query,
              onValueChange = onQueryChange,
              placeholder = { Text(stringResource(R.string.tasks_search_hint)) },
              singleLine = true,
              shape = MaterialTheme.shapes.medium,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 20.dp, vertical = 4.dp)
                      .testTag(MyTasksScreenTestTags.SEARCH_FIELD),
          )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FiltersZone(
              state = state,
              open = filtersOpen,
              onToggleOpen = { filtersOpen = !filtersOpen },
              onToggleStatus = onToggleStatus,
              onTogglePriority = onTogglePriority,
              onClearChips = onClearChips,
          )
          EventsSelector(
              state = state,
              open = eventsOpen,
              onToggleOpen = { eventsOpen = !eventsOpen },
              onToggleEvent = onToggleEvent,
              onClearEvents = onClearEvents,
          )
        }
        Body(state, onClearFilters, onOpenTask)
      }
      state.errorMsg?.let { message ->
        NoticeSnackbar(
            message = stringResource(message),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag(MyTasksScreenTestTags.ERROR),
            actionModifier = Modifier.testTag(MyTasksScreenTestTags.RETRY),
        )
      }
    }
  }
}

/** What sits under the filters: the spinner, one of the two empty states, or the list. */
@Composable
private fun Body(
    state: MyTasksUiState,
    onClearFilters: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
  when {

    // What is shown when you are still loading.
    state.isLoading && state.totalTasks == 0 ->
        CenteredBox {
          CircularProgressIndicator(Modifier.size(32.dp).testTag(MyTasksScreenTestTags.LOADING))
        }

    // What is shown when you have no tasks.
    state.hasNoTasks ->
        CenteredBox {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(horizontal = 24.dp).testTag(MyTasksScreenTestTags.EMPTY),
          ) {
            Text(
                stringResource(R.string.mytasks_empty_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.mytasks_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
          }
        }

    // What is shown when, due to the filters, no tasks is displayed.
    state.nothingMatches ->
        CenteredBox {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.testTag(MyTasksScreenTestTags.NO_MATCH),
          ) {
            Text(
                stringResource(R.string.mytasks_no_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.testTag(MyTasksScreenTestTags.CLEAR_FILTERS),
            ) {
              Text(stringResource(R.string.mytasks_clear_filters))
            }
          }
        }

    // What is shown in the normal case.
    else -> TaskList(state, onOpenTask)
  }
}

/** Fills the rest of the screen and centres [content] in it. */
@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** The rows day by day, each one opening its task details when tapped. */
@Composable
private fun TaskList(state: MyTasksUiState, onOpenTask: (String) -> Unit) {

  val timeFormat = rememberTimeFormat()
  val today = LocalDate.now()

  // The bottom padding keeps the last row clear of the error snackbar
  LazyColumn(
      Modifier.fillMaxSize().testTag(MyTasksScreenTestTags.LIST),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp),
  ) {

    // We show each section one by one.
    state.sections.forEach { section ->
      val sectionTag = MyTasksScreenTestTags.section(section.day)

      // On the section, its label.
      item(key = sectionTag) {
        SectionLabel(dayLabel(section.day, today), Modifier.testTag(sectionTag))
      }

      // Each task row.
      section.rows.forEach { row ->
        item(key = row.task.taskId) {
          TaskRow(
              title = row.task.title,
              subtitle = row.subtitle(),
              time = row.startTime?.format(timeFormat) ?: "",
              trailing = row.trailing(),
              pinKind = if (row.task.completed) PinKind.DONE else PinKind.MINE,
              onClick = { onOpenTask(row.task.taskId) },
              modifier = Modifier.testTag(MyTasksScreenTestTags.row(row.task.taskId)),
          )
          Hairline()
        }
      }
    }
  }
}

/**
 * The "Filters" chip and, when [open], the status and priority chips one after the other, wrapping
 * to the next line, with a "Clear" that unselects them all. Folded, it recalls the active filters
 * on its right.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersZone(
    state: MyTasksUiState,
    open: Boolean,
    onToggleOpen: () -> Unit,
    onToggleStatus: (MyTaskStatus) -> Unit,
    onTogglePriority: (Priority) -> Unit,
    onClearChips: () -> Unit,
) {
  val count = state.activeFilterCount
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

      // The chip that allows to see all the filters.
      DropdownChip(
          label =
              if (count > 0) stringResource(R.string.mytasks_filters_count, count)
              else stringResource(R.string.mytasks_filters),
          active = count > 0,
          open = open,
          onClick = onToggleOpen,
          modifier = Modifier.testTag(MyTasksScreenTestTags.FILTERS_TOGGLE),
      )

      // When the filter chip is closed and that some filters are selected, writes a summary of
      // which are selected.
      if (!open && count > 0) {
        Spacer(Modifier.width(12.dp))
        Text(
            activeFiltersSummary(state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).testTag(MyTasksScreenTestTags.FILTERS_SUMMARY),
        )
      }

      // When it is opened, there is a button to clear the filter.
      if (open) {
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onClearChips,
            enabled = count > 0,
            modifier = Modifier.testTag(MyTasksScreenTestTags.FILTERS_CLEAR),
        ) {
          // Ink while it can clear something; disabled, it keeps the button's greyed-out color
          Text(
              stringResource(R.string.mytasks_clear),
              color = if (count > 0) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
          )
        }
      }
    }

    if (open) {
      FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().testTag(MyTasksScreenTestTags.FILTER_CHIPS),
      ) {
        MyTaskStatus.entries.forEach { status ->
          FilterChip(
              label = status.label(),
              selected = status in state.statusesSelected,
              count = state.statusCounts[status] ?: 0,
              onClick = { onToggleStatus(status) },
              modifier = Modifier.testTag(MyTasksScreenTestTags.statusChip(status)),
          )
        }
        PRIORITY_ORDER.forEach { priority ->
          FilterChip(
              label = priority.label(),
              selected = priority in state.prioritiesSelected,
              count = state.priorityCounts[priority] ?: 0,
              onClick = { onTogglePriority(priority) },
              modifier = Modifier.testTag(MyTasksScreenTestTags.priorityChip(priority)),
          )
        }
      }
    }
  }
}

/**
 * The events filter, apart under the Filters zone: one piece that unfolds into a checkbox list.
 * Tinted lime while it is open or while an event is checked.
 */
@Composable
private fun EventsSelector(
    state: MyTasksUiState,
    open: Boolean,
    onToggleOpen: () -> Unit,
    onToggleEvent: (String) -> Unit,
    onClearEvents: () -> Unit,
) {
  val active = state.eventIdsSelected.isNotEmpty()
  val tinted = open || active
  val shape = RoundedCornerShape(12.dp)
  val colors = MaterialTheme.colorScheme

  Column(
      Modifier.fillMaxWidth()
          .clip(shape)
          .border(1.dp, if (tinted) colors.onPrimaryContainer else colors.outline, shape)
  ) {
    Surface(
        onClick = onToggleOpen,
        color = if (tinted) colors.primaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth().testTag(MyTasksScreenTestTags.EVENTS_TOGGLE),
    ) {
      Row(
          Modifier.fillMaxWidth().height(44.dp).padding(start = 12.dp, end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val accent = if (tinted) colors.onPrimaryContainer else colors.onSurfaceVariant
        Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(18.dp), tint = accent)

        Text(
            stringResource(R.string.mytasks_events),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
        Text(
            eventsValue(state),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            Modifier.size(20.dp),
            tint = colors.onSurface,
        )
      }
    }

    if (open) {
      Hairline()
      Column(
          Modifier.fillMaxWidth()
              .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
              .testTag(MyTasksScreenTestTags.EVENTS_PANEL)
      ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              stringResource(R.string.mytasks_show_tasks_from),
              style = MaterialTheme.typography.labelMedium,
              color = colors.onSurfaceVariant,
              modifier = Modifier.weight(1f),
          )
          TextButton(
              onClick = onClearEvents,
              enabled = active,
              modifier = Modifier.testTag(MyTasksScreenTestTags.EVENTS_CLEAR),
          ) {
            Text(
                stringResource(R.string.mytasks_clear),
                color = if (active) colors.onPrimaryContainer else Color.Unspecified,
            )
          }
        }
        state.events.forEach { option ->
          Row(
              Modifier.fillMaxWidth()
                  .clickable(role = Role.Checkbox) { onToggleEvent(option.eventId) }
                  .testTag(MyTasksScreenTestTags.eventOption(option.eventId)),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            // The whole row toggles, so the box itself does not take the tap twice
            Checkbox(
                checked = option.selected,
                onCheckedChange = null,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        checkmarkColor = colors.onPrimary,
                    ),
                modifier = Modifier.padding(12.dp),
            )
            Text(
                stringResource(
                    R.string.mytasks_event_option,
                    option.title ?: stringResource(R.string.mytasks_unknown_event),
                    option.count,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
          }
        }
      }
    }
  }
}

/**
 * A chip that folds and unfolds a zone: ink when [active], outlined otherwise, with an arrow that
 * points up while [open].
 */
@Composable
private fun DropdownChip(
    label: String,
    active: Boolean,
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val content = if (active) colors.onSecondaryContainer else colors.onSurface
  Surface(
      onClick = onClick,
      shape = CircleShape,
      color = if (active) colors.secondaryContainer else Color.Transparent,
      border = if (active) null else BorderStroke(1.dp, colors.outline),
      modifier = modifier.height(32.dp),
  ) {
    Row(
        Modifier.padding(start = 12.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.labelMedium, color = content)
      Icon(
          if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
          contentDescription = null,
          Modifier.size(18.dp),
          tint = content,
      )
    }
  }
}

/** "14:05" or "2:05 PM", following the device's time format setting. */
@Composable
private fun rememberTimeFormat(): DateTimeFormatter {
  val context = LocalContext.current
  return remember(context) {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
  }
}

/** "Today · Sat 17 Oct", "Tomorrow · …", "Yesterday · …", "Mon 19 Oct", or "Any time". */
@Composable
private fun dayLabel(day: LocalDate?, today: LocalDate): String {
  if (day == null) return stringResource(R.string.mytasks_anytime)
  val relative =
      when (day) {
        today -> stringResource(R.string.mytasks_today)
        today.plusDays(1) -> stringResource(R.string.mytasks_tomorrow)
        today.minusDays(1) -> stringResource(R.string.mytasks_yesterday)
        else -> null
      }
  return relative?.let { stringResource(R.string.mytasks_relative_day, it, formatDate(day)) }
      ?: formatDate(day)
}

/** The priority (unless none), the place and the event, joined with a middle dot. */
@Composable
private fun MyTaskRowUi.subtitle(): String {
  val priority =
      task.priority.takeIf { it != Priority.NONE }?.let { stringResource(priorityLabelRes(it)) }
  return listOfNotNull(priority, task.location.takeIf { it.isNotBlank() }, eventTitle)
      .joinToString(" · ")
}

/** "Done" for a finished task, "in 28 min" for one starting within the hour, else nothing. */
@Composable
private fun MyTaskRowUi.trailing(): String =
    when {
      task.completed -> stringResource(R.string.mytasks_done)
      minutesUntilStart != null -> stringResource(R.string.mytasks_in_minutes, minutesUntilStart)
      else -> ""
    }

@Composable
private fun MyTaskStatus.label(): String =
    when (this) {
      MyTaskStatus.TODO -> stringResource(R.string.mytasks_status_todo)
      MyTaskStatus.DONE -> stringResource(R.string.overview_chip_done)
    }

@Composable
private fun Priority.label(): String =
    if (this == Priority.NONE) stringResource(R.string.mytasks_priority_none)
    else stringResource(priorityLabelRes(this))

/** The active statuses then priorities, for the folded Filters zone: "To do · Urgent". */
@Composable
private fun activeFiltersSummary(state: MyTasksUiState): String {
  val statuses = MyTaskStatus.entries.filter { it in state.statusesSelected }.map { it.label() }
  val priorities = PRIORITY_ORDER.filter { it in state.prioritiesSelected }.map { it.label() }
  return (statuses + priorities).joinToString(" · ")
}

/** "All", the one checked event's title, or "2 events". */
@Composable
private fun eventsValue(state: MyTasksUiState): String {
  val selected = state.events.filter { it.selected }
  return when (selected.size) {
    0 -> stringResource(R.string.mytasks_events_all)
    1 -> selected.single().title ?: stringResource(R.string.mytasks_unknown_event)
    else -> pluralStringResource(R.plurals.mytasks_events_selected, selected.size, selected.size)
  }
}

// ---- Previews: sample data of the design, around today so the day labels read as in it ----

/** One row of the Fête de l'Asso starting at [time] on [day]. */
private fun previewRow(
    id: String,
    title: String,
    day: LocalDate,
    time: LocalTime,
    priority: Priority,
    done: Boolean = false,
    minutesUntilStart: Int? = null,
): MyTaskRowUi {
  val start = day.atTime(time).atZone(ZoneId.systemDefault())
  val task =
      Task(
          id,
          "fete",
          title,
          startTime = start,
          location = "Stage",
          priority = priority,
          completed = done,
          ownerId = "owner",
      )
  return MyTaskRowUi(task, "Fête de l'Asso", time, minutesUntilStart)
}

private fun previewState(): MyTasksUiState {
  val today = LocalDate.now()
  val tomorrow = today.plusDays(1)
  return MyTasksUiState(
      statusCounts = mapOf(MyTaskStatus.TODO to 2, MyTaskStatus.DONE to 1),
      priorityCounts = mapOf(Priority.URGENT to 1, Priority.HIGH to 1, Priority.MEDIUM to 1),
      events = listOf(EventFilterOption("fete", "Fête de l'Asso", 3, selected = false)),
      sections =
          listOf(
              MyTasksDaySection(
                  today,
                  listOf(
                      previewRow(
                          "desk",
                          "Set up ticket desk",
                          today,
                          LocalTime.of(11, 0),
                          Priority.MEDIUM,
                          done = true,
                      ),
                      previewRow(
                          "power",
                          "Run power to stage",
                          today,
                          LocalTime.of(14, 30),
                          Priority.URGENT,
                          minutesUntilStart = 28,
                      ),
                  ),
              ),
              MyTasksDaySection(
                  tomorrow,
                  listOf(
                      previewRow(
                          "wrist",
                          "Hand out wristbands",
                          tomorrow,
                          LocalTime.of(15, 0),
                          Priority.HIGH,
                      )
                  ),
              ),
          ),
      totalTasks = 3,
      isLoading = false,
      hasLoaded = true,
  )
}

@Preview(name = "List", showBackground = true, heightDp = 915, widthDp = 412)
@Composable
internal fun MyTasksListPreview() {
  AppTheme(darkTheme = false) { MyTasksContent(previewState()) }
}
