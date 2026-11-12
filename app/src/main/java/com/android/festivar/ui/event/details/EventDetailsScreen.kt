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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.joinCodeOf
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.PillStyle
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.formatEventDates
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
  const val JOIN_CODE_ROW = "joinCodeRow"
  const val JOIN_CODE_VALUE = "joinCodeValue"
  const val STATUS_CHIP = "statusChip"
  const val SUBTITLE = "eventSubtitle"
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

/** Displays the loaded event, including its task summary and identifying information. */
@Composable
private fun EventInfo(
    event: Event,
    modifier: Modifier = Modifier,
) {
  val openTasks = event.tasks.count { !it.completed }

  Column(
      modifier = modifier.fillMaxSize().padding(start = 20.dp, top = 16.dp, end = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
            style = if (event.closed) PillStyle.OUTLINED else PillStyle.MUTED,
            modifier = Modifier.testTag(EventDetailsScreenTestTags.STATUS_CHIP),
        )
      }
      Text(
          text = formatEventDates(event),
          modifier = Modifier.testTag(EventDetailsScreenTestTags.SUBTITLE),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
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

    JoinCodeRow(code = joinCodeOf(event))
  }
}

/**
 * The event's join code as plain text on one line with its label, in capitals as in Figma. Everyone
 * in the event sees it, so the row has no share action. The code is selectable so it can be copied,
 * and the row is one node for screen readers, which read the label then the code.
 *
 * @param code the join code to show.
 */
@Composable
private fun JoinCodeRow(code: String) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 12.dp)
              .semantics(mergeDescendants = true) {}
              .testTag(EventDetailsScreenTestTags.JOIN_CODE_ROW),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
        text = stringResource(R.string.event_details_join_code).uppercase(),
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
    )
    SelectionContainer {
      Text(
          text = code,
          modifier = Modifier.testTag(EventDetailsScreenTestTags.JOIN_CODE_VALUE),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.headlineSmall,
          maxLines = 1,
          softWrap = false,
      )
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

/** An id like the ones the repository generates, so the preview shows a realistic join code. */
private const val PREVIEW_EVENT_ID = "3f9a1c2e-7b4d-4e8a-9c15-d2a6b0f84e37"

@Preview
@Composable
internal fun EventDetailsScreenPreview() {
  val eventStartDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  val eventEndDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  val event =
      Event(
          eventId = PREVIEW_EVENT_ID,
          ownerId = "user",
          title = "Fête de l'Asso 2026",
          members = listOf(User(uid = "user", name = "Sami K.")),
          startDate = eventStartDate,
          endDate = eventEndDate,
          location = "Parc de Vidy, Lausanne",
          tasks =
              List(
                  size = 10,
                  init = { index ->
                    Task(
                        taskId = index.toString(),
                        eventId = PREVIEW_EVENT_ID,
                        title = "Task $index",
                        ownerId = "user",
                        completed = index % 2 == 0,
                    )
                  },
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
