package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.PillStyle
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.formatEventDatesAndPlace
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.theme.NumeralLarge
import java.time.ZoneId
import java.time.ZonedDateTime

/** Test tags named after the tags of the "Event details" frames in Figma. */
object EventDetailsScreenTestTags {
  const val BACK_BUTTON = "backButton"
  const val EVENT_NAME = "eventName"
  const val FETCH_ERROR = "eventDetailsFetchError"
  const val OPEN_TASKS_STAT = "openTasksStat"
  const val STATUS_PILL = "eventStatusPill"
  const val SUBTITLE = "eventSubtitle"
  const val DESCRIPTION = "eventDescription"
  const val SEE_TASKS_BUTTON = "seeTasksButton"
}

/** Callbacks handled by the Event Details screen. */
class EventDetailsActions(
    val onBack: () -> Unit = {},
    val onTasksButtonClicked: () -> Unit = {},
)

/**
 * Displays the details of an event loaded by [viewModel].
 *
 * The screen is stateless with respect to data loading: it observes the ViewModel's state and
 * forwards user actions through [EventDetailsActions].
 *
 * @param viewModel the ViewModel providing the event and loading error state.
 * @param onBack called when the back button is clicked.
 * @param onTasksButtonClicked called when the "See tasks" button is clicked.
 * @param modifier modifier applied to the event content.
 */
@Composable
fun EventDetailsScreen(
    viewModel: EventDetailsViewModel,
    onBack: () -> Unit,
    onTasksButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(Unit) { viewModel.fetchEvent() }

  EventDetailsContent(
      state = uiState,
      actions = EventDetailsActions(onBack = onBack, onTasksButtonClicked = onTasksButtonClicked),
      modifier = modifier,
  )
}

/** The stateless details screen: everything it shows comes from [state]. */
@Composable
private fun EventDetailsContent(
    state: EventDetailsUIState,
    actions: EventDetailsActions,
    modifier: Modifier = Modifier,
) {
  val event = state.event
  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { EventDetailsTopBar(onBack = actions.onBack) },
      bottomBar = {
        if (event != null) {
          EventDetailsBottomBar(onTasksButtonClicked = actions.onTasksButtonClicked)
        }
      },
  ) { paddingValues ->
    when {
      event != null ->
          EventInfo(
              event = event,
              modifier = modifier.padding(paddingValues),
          )
      state.errorMsg != null ->
          Box(
              modifier = Modifier.fillMaxSize().padding(10.dp),
              contentAlignment = Alignment.Center,
          ) {
            Text(
                text = stringResource(state.errorMsg),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(EventDetailsScreenTestTags.FETCH_ERROR),
            )
          }
    }
  }
}

/**
 * Displays the loaded event: its title and status, its dates and place, its description and its
 * task summary. The content scrolls so that a long description stays readable.
 */
@Composable
private fun EventInfo(
    event: Event,
    modifier: Modifier = Modifier,
) {
  val openTasks = event.tasks.count { !it.completed }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(start = 20.dp, top = 16.dp, end = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
          text = event.title,
          modifier = Modifier.weight(1f).testTag(EventDetailsScreenTestTags.EVENT_NAME),
          color = MaterialTheme.colorScheme.onBackground,
          style = MaterialTheme.typography.headlineMedium,
      )
      StatusPill(
          label =
              stringResource(
                  if (event.closed) R.string.event_details_status_closed
                  else R.string.event_details_status_open
              ),
          style = if (event.closed) PillStyle.OUTLINED else PillStyle.LIME,
          modifier = Modifier.testTag(EventDetailsScreenTestTags.STATUS_PILL),
      )
    }

    Text(
        text = formatEventDatesAndPlace(event),
        modifier = Modifier.fillMaxWidth().testTag(EventDetailsScreenTestTags.SUBTITLE),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )

    if (event.description.isNotBlank()) {
      Text(
          text = event.description,
          modifier = Modifier.fillMaxWidth().testTag(EventDetailsScreenTestTags.DESCRIPTION),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyLarge,
      )
    }

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
  }
}

/** Top app bar containing the navigation button for leaving the details screen. */
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

/** The 'See tasks' button that opens the event's task list. */
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

@Preview
@Composable
internal fun EventDetailsScreenPreview() {
  val eventStartDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  val eventEndDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  val event =
      Event(
          eventId = "0",
          title = "Fête de l'Asso 2026",
          description =
              "Our yearly association party by the lake. Three days of concerts, food stands " +
                  "and games. Volunteers help with setup, bar shifts and cleanup.",
          members =
              List(12) { index ->
                User(
                    uid = "user$index",
                    name = "Member",
                    surname = "$index",
                    email = "member$index@festivar.ch",
                    joinedEvents = listOf("0"),
                )
              },
          startDate = eventStartDate,
          endDate = eventEndDate,
          location = "Parc de Vidy, Lausanne",
          tasks =
              List(31) { index ->
                Task(
                    taskId = index.toString(),
                    eventId = "0",
                    title = "Task $index",
                    description = "Help the team with task $index.",
                    ownerId = "user0",
                    completed = index >= 7,
                )
              },
          closed = false,
          announcements =
              listOf(
                  Announcement(
                      announcementId = "0",
                      title = "Bar shifts moved",
                      authorId = "user0",
                      content = "The Friday bar shifts now start at 18:00.",
                      updatedAt = eventStartDate.minusHours(2),
                  ),
              ),
      )

  AppTheme {
    EventDetailsContent(
        state = EventDetailsUIState(event = event),
        actions = EventDetailsActions(onBack = {}, onTasksButtonClicked = {}),
    )
  }
}

@Preview
@Composable
internal fun FetchErrorPreview() {
  AppTheme {
    EventDetailsContent(
        state = EventDetailsUIState(errorMsg = R.string.event_details_fetch_error),
        actions = EventDetailsActions(onBack = {}, onTasksButtonClicked = {}),
    )
  }
}
