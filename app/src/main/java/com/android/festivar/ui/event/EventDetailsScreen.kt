package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.formatEventDate
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.theme.NumeralLarge
import java.time.ZoneId
import java.time.ZonedDateTime

object EventDetailsScreenTestTags {
  const val BACK_BUTTON = "backButton"
  const val EVENT_NAME = "eventName"
  const val OPEN_TASKS_STAT = "openTasksStat"
  const val OWNER_ROW = "ownerRow"
  const val DATE_ROW = "dateRow"
  const val ADDRESS_ROW = "addressRow"
  const val SEE_TASKS_BUTTON = "seeTasksButton"
}

/**
 * A screen for viewing event details.
 *
 * @param eventId The ID of the event whose details will be shown
 * @param onBack A callback to be invoked when the back button is clicked
 * @param onTasksButtonClicked A callback to be invoked when the "See tasks" button is clicked
 */
@Composable
fun EventDetailsScreen(
    eventId: String,
    viewModel: EventDetailsViewModel = viewModel(),
    onBack: () -> Unit,
    onTasksButtonClicked: () -> Unit,
) {
  LaunchedEffect(Unit) { viewModel.fetchEvent(eventId) }
  val uiState by viewModel.uiState.collectAsState()
  val event = uiState.event

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { EventDetailsTopBar(onBack = onBack) },
      bottomBar = {
        if (event != null) {
          EventDetailsBottomBar(onTasksButtonClicked = onTasksButtonClicked)
        }
      },
  ) { paddingValues ->
    when {
      event != null ->
          EventDetailsContent(
              event = event,
              modifier = Modifier.padding(paddingValues),
          )
      uiState.errorFetch ->
          Box(
              modifier = Modifier.fillMaxSize().padding(10.dp),
              contentAlignment = Alignment.Center,
          ) {
            Text(
                text = stringResource(R.string.event_details_fetch_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
    }
  }
}

@Composable
private fun EventDetailsContent(
    event: Event,
    modifier: Modifier = Modifier,
) {
  val openTasks = event.tasks.count { !it.completed }
  val date = formatEventDate(event)
  val owner = event.members.firstOrNull()?.name.orEmpty()

  Column(
      modifier = modifier.fillMaxSize().padding(start = 20.dp, top = 16.dp, end = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = event.title,
        modifier = Modifier.fillMaxWidth().testTag(EventDetailsScreenTestTags.EVENT_NAME),
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineMedium,
    )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(14.dp)
                .testTag(EventDetailsScreenTestTags.OPEN_TASKS_STAT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = openTasks.toString(),
              color = MaterialTheme.colorScheme.onSurface,
              style = NumeralLarge,
          )
          Text(
              text = stringResource(R.string.event_details_tasks_count, event.tasks.size),
              modifier = Modifier.padding(start = 4.dp, top = 12.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(
            text = stringResource(R.string.event_details_tasks_still_open),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp),
                ),
    ) {
      EventDetailsRow(
          label = stringResource(R.string.event_details_owner),
          value = owner,
          tag = EventDetailsScreenTestTags.OWNER_ROW,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      EventDetailsRow(
          label = stringResource(R.string.event_details_date),
          value = date,
          tag = EventDetailsScreenTestTags.DATE_ROW,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      EventDetailsRow(
          label = stringResource(R.string.event_details_address),
          value = event.location,
          tag = EventDetailsScreenTestTags.ADDRESS_ROW,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailsTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {},
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(EventDetailsScreenTestTags.BACK_BUTTON),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.event_details_navigate_back),
              tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
          ),
  )
}

@Composable
private fun EventDetailsBottomBar(onTasksButtonClicked: () -> Unit) {
  Button(
      onClick = onTasksButtonClicked,
      modifier =
          Modifier.fillMaxWidth()
              .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp)
              .height(44.dp)
              .testTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON),
      shape = RoundedCornerShape(10.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
  ) {
    Icon(
        imageVector = Icons.Outlined.TaskAlt,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
    )
    Text(
        text = stringResource(R.string.event_details_see_tasks),
        modifier = Modifier.padding(start = 8.dp),
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun EventDetailsRow(
    label: String,
    value: String,
    tag: String,
) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(tag).padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun EventDetailsScreenPreview(event: Event) {
  AppTheme {
    EventDetailsScreen(
        eventId = event.eventId,
        viewModel = EventDetailsViewModel(EventsRepositoryLocal(listOf(event))),
        onTasksButtonClicked = {},
        onBack = {},
    )
  }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
private fun FetchErrorPreview() {
  AppTheme {
    EventDetailsScreen(
        eventId = eventSameMonth.eventId,
        viewModel = EventDetailsViewModel(EventsRepositoryLocal(emptyList())),
        onTasksButtonClicked = {},
        onBack = {},
    )
  }
}

@Preview
@Composable
private fun EventDetailsScreenPreviewSameMonth() = EventDetailsScreenPreview(eventSameMonth)

@Preview
@Composable
private fun EventDetailsScreenPreviewSameDay() = EventDetailsScreenPreview(eventSameDay)

@Preview
@Composable
private fun EventDetailsScreenPreviewDifferentMonth() =
    EventDetailsScreenPreview(eventDifferentMonth)

@Preview
@Composable
private fun EventDetailsScreenPreviewNoTasks() = EventDetailsScreenPreview(eventNoTasks)

@Preview
@Composable
private fun EventDetailsScreenPreviewDifferentYear() = EventDetailsScreenPreview(eventDifferentYear)

val eventStartDate: ZonedDateTime =
    ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
val eventEndDate: ZonedDateTime =
    ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich"))
const val tasksListSize = 10
val eventSameMonth =
    Event(
        eventId = "0",
        title = "Fête de l'Asso 2026",
        members = listOf(User(uid = "user", name = "Sami K.")),
        startDate = eventStartDate,
        endDate = eventEndDate,
        location = "Parc de Vidy, Lausanne",
        tasks =
            List(
                size = tasksListSize,
                init = { index ->
                  Task(
                      taskId = index.toString(),
                      eventId = "0",
                      title = "Task $index",
                      completed = index % 2 == 0,
                  )
                },
            ),
    )

val eventSameDay =
    eventSameMonth.copy(endDate = eventEndDate.withDayOfMonth(eventStartDate.dayOfMonth))
val eventDifferentMonth = eventSameMonth.copy(endDate = eventStartDate.plusMonths(1))
val eventDifferentYear = eventSameMonth.copy(endDate = eventStartDate.plusYears(1))
val eventNoTasks = eventSameMonth.copy(tasks = emptyList())
