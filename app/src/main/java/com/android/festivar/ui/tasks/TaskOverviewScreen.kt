package com.android.festivar.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.components.Hairline
import com.android.festivar.ui.components.PinKind
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.TaskRow
import java.time.format.DateTimeFormatter

object TaskOverviewScreenTestTags {
  const val BACK = "overviewBack"
  const val TITLE = "overviewTitle"
  const val SEARCH = "overviewSearch"
  const val SEARCH_FIELD = "overviewSearchField"
  const val CHIP_OPEN = "overviewChipOpen"
  const val CHIP_MINE = "overviewChipMine"
  const val CHIP_DONE = "overviewChipDone"
  const val CHIP_ALL = "overviewChipAll"
  const val EMPTY = "overviewEmpty"
  const val ERROR_MESSAGE = "overviewError"

  fun row(taskId: String) = "overviewRow$taskId"

  fun section(group: OverviewGroup) = "overviewSection${group.name}"
}

/** The task overview of [eventId], where a volunteer picks an open task. */
@Composable
fun TaskOverviewScreen(
    eventId: String,
    userId: String? = null,
    viewModel: TaskOverviewViewModel =
        viewModel(key = "$eventId:$userId") { TaskOverviewViewModel(eventId, userId) },
    onBack: () -> Unit = {},
    onOpenTask: (taskId: String) -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  TaskOverviewContent(
      state = state,
      onBack = onBack,
      onSelectFilter = viewModel::selectFilter,
      onToggleSearch = viewModel::toggleSearch,
      onQueryChange = viewModel::setQuery,
      onOpenTask = onOpenTask,
  )
}

/** The stateless overview: everything it shows comes from [state]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOverviewContent(
    state: TaskOverviewUiState,
    onBack: () -> Unit = {},
    onSelectFilter: (OverviewFilter) -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  stringResource(R.string.overview_title),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(TaskOverviewScreenTestTags.TITLE),
              )
            },
            navigationIcon = {
              IconButton(onClick = onBack, Modifier.testTag(TaskOverviewScreenTestTags.BACK)) {
                Icon(BackArrow, stringResource(R.string.tasks_back))
              }
            },
            actions = {
              IconButton(
                  onClick = onToggleSearch,
                  Modifier.testTag(TaskOverviewScreenTestTags.SEARCH),
              ) {
                Icon(Magnifier, stringResource(R.string.tasks_search))
              }
            },
        )
      }
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(top = 4.dp)) {
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
                    .testTag(TaskOverviewScreenTestTags.SEARCH_FIELD),
        )
      }
      Row(
          Modifier.fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CHIPS.forEach { (filter, label, tag) ->
          FilterChip(
              label = stringResource(label),
              count = state.counts[filter] ?: 0,
              selected = state.filter == filter,
              onClick = { onSelectFilter(filter) },
              modifier = Modifier.testTag(tag),
          )
        }
      }
      state.errorMsg?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag(TaskOverviewScreenTestTags.ERROR_MESSAGE),
        )
      }
      if (!state.isLoading && state.errorMsg == null && state.sections.isEmpty()) {
        Text(
            stringResource(R.string.overview_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth().padding(20.dp).testTag(TaskOverviewScreenTestTags.EMPTY),
        )
      }
      LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
        state.sections.forEach { section ->
          item(key = section.group.name) {
            SectionLabel(
                stringResource(section.group.label()),
                Modifier.testTag(TaskOverviewScreenTestTags.section(section.group)),
            )
          }
          section.rows.forEach { row ->
            item(key = row.task.taskId) {
              TaskRow(
                  title = row.task.title,
                  subtitle = row.subtitle(),
                  time = row.startTime?.format(HOUR_MINUTE) ?: "",
                  trailing =
                      stringResource(R.string.overview_people, row.assigned, row.peopleNeeded),
                  pinKind = row.pinKind(),
                  onClick = { onOpenTask(row.task.taskId) },
                  modifier = Modifier.testTag(TaskOverviewScreenTestTags.row(row.task.taskId)),
              )
              Hairline()
            }
          }
        }
      }
    }
  }
}

private data class Chip(val filter: OverviewFilter, val label: Int, val tag: String)

// The chips in order, each with its count.
private val CHIPS =
    listOf(
        Chip(
            OverviewFilter.OPEN,
            R.string.overview_chip_open,
            TaskOverviewScreenTestTags.CHIP_OPEN,
        ),
        Chip(
            OverviewFilter.MINE,
            R.string.overview_chip_mine,
            TaskOverviewScreenTestTags.CHIP_MINE,
        ),
        Chip(
            OverviewFilter.DONE,
            R.string.overview_chip_done,
            TaskOverviewScreenTestTags.CHIP_DONE,
        ),
        Chip(OverviewFilter.ALL, R.string.overview_chip_all, TaskOverviewScreenTestTags.CHIP_ALL),
    )

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Where, how many people (only when more than one) and how long, joined with a middle dot. */
@Composable
private fun OverviewRowUi.subtitle(): String {
  val people =
      if (peopleNeeded > 1) {
        pluralStringResource(R.plurals.overview_people_needed, peopleNeeded, peopleNeeded)
      } else null
  val length = estimatedMinutes?.let { total ->
    val hours = total / 60
    val minutes = total % 60
    when {
      hours == 0 -> stringResource(R.string.overview_minutes, minutes)
      minutes == 0 -> stringResource(R.string.overview_hours, hours)
      else -> stringResource(R.string.overview_hours_minutes, hours, minutes)
    }
  }
  return listOfNotNull(location.ifBlank { null }, people, length).joinToString(" · ")
}

private fun OverviewGroup.label(): Int =
    when (this) {
      OverviewGroup.NOW -> R.string.overview_group_now
      OverviewGroup.MORNING -> R.string.overview_group_morning
      OverviewGroup.AFTERNOON -> R.string.overview_group_afternoon
      OverviewGroup.EVENING -> R.string.overview_group_evening
      OverviewGroup.ANYTIME -> R.string.overview_group_anytime
    }

/** Done wins, then mine, then full (taken by others), else an open structure pin. */
private fun OverviewRowUi.pinKind(): PinKind =
    when {
      task.completed -> PinKind.DONE
      isMine -> PinKind.MINE
      isFull -> PinKind.TAKEN
      else -> PinKind.STRUCTURE
    }

// Material's arrow back and search glyphs, drawn here because the team's build has no icon
// library. The black is a placeholder: Icon tints the whole vector.
private fun glyph(name: String, pathData: String, autoMirror: Boolean = false): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = autoMirror)
        .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

private val BackArrow =
    glyph("BackArrow", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z", true)

private val Magnifier =
    glyph(
        "Magnifier",
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 " +
            "3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19" +
            "l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    )
