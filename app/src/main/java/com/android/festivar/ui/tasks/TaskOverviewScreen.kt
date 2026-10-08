// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.ui.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
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
import java.time.LocalDate
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
  const val LOADING = "overviewLoading"
  const val EMPTY = "overviewEmpty"
  const val ERROR_MESSAGE = "overviewError"
  const val RETRY = "overviewRetry"

  fun row(taskId: String) = "overviewRow$taskId"

  /** One tag per section: the same group can appear once per day. */
  fun section(group: OverviewGroup, day: LocalDate? = null) =
      "overviewSection${group.name}" + (day?.let { "_$it" } ?: "")
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
      onRetry = viewModel::refresh,
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
    onRetry: () -> Unit = {},
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_back))
              }
            },
            actions = {
              IconButton(
                  onClick = onToggleSearch,
                  Modifier.testTag(TaskOverviewScreenTestTags.SEARCH),
              ) {
                Icon(Icons.Filled.Search, stringResource(R.string.tasks_search))
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
      state.errorMsg?.let { ErrorLine(it, onRetry) }
      if (state.isLoading && state.sections.isEmpty()) {
        CircularProgressIndicator(
            modifier =
                Modifier.align(Alignment.CenterHorizontally)
                    .padding(20.dp)
                    .size(32.dp)
                    .testTag(TaskOverviewScreenTestTags.LOADING)
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
      val timeFormat = rememberTimeFormat()
      LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp)) {
        state.sections.forEach { section ->
          val sectionTag = TaskOverviewScreenTestTags.section(section.group, section.day)
          item(key = sectionTag) { SectionLabel(section.label(), Modifier.testTag(sectionTag)) }
          section.rows.forEach { row ->
            item(key = row.task.taskId) {
              TaskRow(
                  title = row.task.title,
                  subtitle = row.subtitle(),
                  time = row.startTime?.format(timeFormat) ?: "",
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

/** The load error in the theme's error colour, with a retry beside it. */
@Composable
private fun ErrorLine(message: String, onRetry: () -> Unit) {
  Row(
      Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f).testTag(TaskOverviewScreenTestTags.ERROR_MESSAGE),
    )
    TextButton(onClick = onRetry, Modifier.testTag(TaskOverviewScreenTestTags.RETRY)) {
      Text(stringResource(R.string.overview_retry))
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

/** "14:05" or "2:05 PM", following the device's time format setting. */
@Composable
private fun rememberTimeFormat(): DateTimeFormatter {
  val context = LocalContext.current
  val locale = LocalLocale.current.platformLocale
  return remember(locale) {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    DateTimeFormatter.ofPattern(pattern, locale)
  }
}

private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/** The part of the day, then the date when the section has one: "Morning · Sun 11 Oct". */
@Composable
private fun OverviewSectionUi.label(): String {
  val part = stringResource(group.label())
  val locale = LocalLocale.current.platformLocale
  return day?.let { "$part · ${it.format(DAY.withLocale(locale))}" } ?: part
}

/**
 * Where, how many people (only when more than one) and how long (only when known and more than
 * zero), joined with a middle dot.
 */
@Composable
private fun OverviewRowUi.subtitle(): String {
  val people =
      if (peopleNeeded > 1) {
        pluralStringResource(R.plurals.overview_people_needed, peopleNeeded, peopleNeeded)
      } else null
  val length =
      estimatedMinutes
          ?.takeIf { it > 0 }
          ?.let { total ->
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
