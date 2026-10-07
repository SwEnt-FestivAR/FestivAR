// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.event.Event
import com.android.festivar.model.task.Task
import com.android.festivar.model.temporary.User
import com.android.festivar.ui.components.Avatar
import com.android.festivar.ui.components.BottomNavigationBar
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.components.NavigationTab
import com.android.festivar.ui.components.PillStyle
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.theme.AppTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object EventsOverviewScreenTestTags {
  const val TITLE = "eventsOverviewTitle"
  const val ONGOING_CHIP = "ongoingEventsChip"
  const val UPCOMING_CHIP = "upcomingEventsChip"
  const val PAST_CHIP = "pastEventsChip"
  const val EVENT_LIST = "eventList"
  const val EMPTY_EVENT_LIST_MSG = "emptyEventList"
  const val LOADING_INDICATOR = "eventsLoadingIndicator"
  const val CREATE_EVENT_BUTTON = "createEventFab"

  fun getTestTagForEventItem(event: Event): String = "eventItem${event.eventId}"
}

/** Maximum number of member avatars shown on an event card before the "+N" avatar. */
private const val MAX_SHOWN_AVATARS = 2

/**
 * Formats the dates and the location of an [event], e.g. "Sat 17 Oct, 10:00 to 23:00 · Parc de
 * Vidy". The end day is only repeated if the [event] ends on another day.
 */
private fun formatEventDates(event: Event): String {
  val day = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
  val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
  val start = "${event.startDate.format(day)}, ${event.startDate.format(time)}"
  val end =
      if (event.endDate.toLocalDate() == event.startDate.toLocalDate()) {
        event.endDate.format(time)
      } else {
        "${event.endDate.format(day)}, ${event.endDate.format(time)}"
      }
  val dates = "$start to $end"
  return if (event.location.isBlank()) dates else "$dates · ${event.location}"
}

/**
 * Initials shown in the avatar of a [user]: the first letter of its name and of its surname, e.g.
 * "SK" for Sara Keller. Falls back to the start of its identifier if both are blank.
 */
private fun initials(user: User): String {
  val letters = listOf(user.name, user.surname).mapNotNull { it.trim().firstOrNull() }
  return (if (letters.isEmpty()) user.uid.take(2) else letters.joinToString("")).uppercase()
}

/**
 * Screen showing the [Event] items of the user, split between upcoming and past ones.
 *
 * @param eventsOverviewViewModel The ViewModel providing the [Event] items.
 * @param onSelectEvent Called when the user clicks on an [Event] card.
 * @param onAddEvent Called when the user clicks on the "New event" button.
 * @param onSelectTab Called when the user clicks on a tab of the bottom navigation bar.
 */
@Composable
fun EventsOverviewScreen(
    eventsOverviewViewModel: EventsOverviewViewModel,
    onSelectEvent: (Event) -> Unit = {},
    onAddEvent: () -> Unit = {},
    onSelectTab: (NavigationTab) -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by eventsOverviewViewModel.uiState.collectAsState()

  // Fetch the events again when the screen is shown, e.g. after adding a new one
  LaunchedEffect(Unit) { eventsOverviewViewModel.refreshUIState() }

  // Show error message if fetching the events fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      eventsOverviewViewModel.clearErrorMsg()
    }
  }

  EventsOverviewContent(
      uiState = uiState,
      onSelectFilter = eventsOverviewViewModel::selectFilter,
      onSelectEvent = onSelectEvent,
      onAddEvent = onAddEvent,
      onSelectTab = onSelectTab,
  )
}

/**
 * Stateless content of the [EventsOverviewScreen], displaying the given [uiState].
 *
 * @param uiState The state to display.
 * @param onSelectFilter Called when the user clicks on the "Upcoming" or "Past" chip.
 * @param onSelectEvent Called when the user clicks on an [Event] card.
 * @param onAddEvent Called when the user clicks on the "New event" button.
 * @param onSelectTab Called when the user clicks on a tab of the bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsOverviewContent(
    uiState: EventsOverviewUIState,
    onSelectFilter: (EventsFilter) -> Unit = {},
    onSelectEvent: (Event) -> Unit = {},
    onAddEvent: () -> Unit = {},
    onSelectTab: (NavigationTab) -> Unit = {},
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "Events",
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(EventsOverviewScreenTestTags.TITLE),
              )
            }
        )
      },
      bottomBar = {
        BottomNavigationBar(selectedTab = NavigationTab.EVENTS, onTabSelected = onSelectTab)
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = onAddEvent,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier =
                Modifier.height(56.dp).testTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON),
        ) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge)
            Text(text = "New event", style = MaterialTheme.typography.labelLarge)
          }
        }
      },
  ) { pd ->
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(pd).padding(top = 4.dp),
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 20.dp),
      ) {
        FilterChip(
            label = "Ongoing",
            count = uiState.ongoingEvents.size,
            selected = uiState.selectedFilter == EventsFilter.ONGOING,
            onClick = { onSelectFilter(EventsFilter.ONGOING) },
            modifier = Modifier.testTag(EventsOverviewScreenTestTags.ONGOING_CHIP),
        )
        FilterChip(
            label = "Upcoming",
            count = uiState.upcomingEvents.size,
            selected = uiState.selectedFilter == EventsFilter.UPCOMING,
            onClick = { onSelectFilter(EventsFilter.UPCOMING) },
            modifier = Modifier.testTag(EventsOverviewScreenTestTags.UPCOMING_CHIP),
        )
        FilterChip(
            label = "Past",
            count = uiState.pastEvents.size,
            selected = uiState.selectedFilter == EventsFilter.PAST,
            onClick = { onSelectFilter(EventsFilter.PAST) },
            modifier = Modifier.testTag(EventsOverviewScreenTestTags.PAST_CHIP),
        )
      }

      when {
        uiState.isLoading && uiState.hasNoEvents -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(EventsOverviewScreenTestTags.LOADING_INDICATOR),
            )
          }
        }
        uiState.shownEvents.isEmpty() -> {
          Text(
              text =
                  when (uiState.selectedFilter) {
                    EventsFilter.ONGOING -> "You have no ongoing event."
                    EventsFilter.UPCOMING -> "You have no upcoming event."
                    EventsFilter.PAST -> "You have no past event."
                  },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier =
                  Modifier.padding(horizontal = 20.dp)
                      .testTag(EventsOverviewScreenTestTags.EMPTY_EVENT_LIST_MSG),
          )
        }
        else -> {
          LazyColumn(
              verticalArrangement = Arrangement.spacedBy(16.dp),
              // Bottom padding so that the last card can scroll above the floating button
              contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp),
              modifier = Modifier.fillMaxSize().testTag(EventsOverviewScreenTestTags.EVENT_LIST),
          ) {
            items(uiState.shownEvents, key = { it.event.eventId }) { item ->
              EventItem(item = item, onClick = { onSelectEvent(item.event) })
            }
          }
        }
      }
    }
  }
}

/** Card showing the [Event] of an [item]: its title, dates, location, team and open tasks. */
@Composable
fun EventItem(item: EventItemUIState, onClick: () -> Unit) {
  val event = item.event
  val shape = RoundedCornerShape(20.dp)
  Column(
      modifier =
          Modifier.testTag(EventsOverviewScreenTestTags.getTestTagForEventItem(event))
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
              .clickable(onClick = onClick)
  ) {
    // Title, dates and location
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatEventDates(event),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (item.isOngoing) {
        StatusPill(label = "Ongoing", style = PillStyle.INK)
      } else if (event.closed) {
        StatusPill(label = "Closed", style = PillStyle.OUTLINED)
      }
    }

    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

    // Team and open tasks
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
    ) {
      if (event.members.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
          event.members.take(MAX_SHOWN_AVATARS).forEach { Avatar(text = initials(it)) }
          if (event.members.size > MAX_SHOWN_AVATARS) {
            Avatar(text = "+${event.members.size - MAX_SHOWN_AVATARS}")
          }
        }
      }
      Text(
          text =
              if (event.members.isEmpty()) "No one on the team yet."
              else "${event.members.size} on the team",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
      )
      val openTasks = item.openTasksCount
      if (openTasks > 0) {
        StatusPill(
            label = if (openTasks == 1) "1 task open" else "$openTasks tasks open",
            style = PillStyle.LIME,
        )
      }
    }
  }
}

/** Preview * */
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun EventsOverviewContentPreview() {
  val now = ZonedDateTime.now()
  val members =
      listOf(
          User("u1", name = "Sara", surname = "Keller"),
          User("u2", name = "Marc", surname = "Dubois"),
          User("u3", name = "Alice", surname = "Lambert"),
          User("u4", name = "Bob", surname = "Morel"),
          User("u5", name = "Chloé", surname = "Rochat"),
      )
  val upcomingEvents =
      listOf(
          Event(
              eventId = "1",
              title = "Fête de l'Asso 2026",
              members = members,
              startDate = now.plusDays(10).withHour(10).withMinute(0),
              endDate = now.plusDays(10).withHour(23).withMinute(0),
              location = "Parc de Vidy, Lausanne",
              tasks = (1..7).map { Task(taskId = "t$it", eventId = "1", title = "Task $it") },
          ),
          Event(
              eventId = "2",
              title = "Soirée Jazz",
              members = members.take(1),
              startDate = now.plusDays(30).withHour(19).withMinute(0),
              endDate = now.plusDays(31).withHour(1).withMinute(0),
              location = "Salle Polyvalente",
          ),
      )
  val ongoingEvent =
      Event(
          eventId = "4",
          title = "Concert au Lac",
          members = members.take(3),
          startDate = now.minusHours(2),
          endDate = now.plusHours(3),
          location = "Quai d'Ouchy",
          tasks = listOf(Task(taskId = "t9", eventId = "4", title = "Stage")),
      )
  val pastEvent =
      Event(
          eventId = "3",
          title = "Marché de Noël",
          startDate = now.minusDays(5),
          endDate = now.minusDays(4),
          closed = true,
      )
  val uiState =
      EventsOverviewUIState(
          ongoingEvents =
              listOf(EventItemUIState(ongoingEvent, openTasksCount = 1, isOngoing = true)),
          upcomingEvents =
              upcomingEvents.map { EventItemUIState(it, openTasksCount = it.tasks.size) },
          pastEvents = listOf(EventItemUIState(pastEvent, openTasksCount = 0)),
      )
  AppTheme { EventsOverviewContent(uiState = uiState) }
}
