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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.temporary.User
import com.android.festivar.ui.components.Avatar
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.theme.AppTheme
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object EventsOverviewScreenTestTags {
  const val TITLE = "eventsOverviewTitle"
  const val USER_AVATAR = "eventsOverviewUserAvatar"
  const val ONGOING_CHIP = "ongoingEventsChip"
  const val UPCOMING_CHIP = "upcomingEventsChip"
  const val PAST_CHIP = "pastEventsChip"
  const val EVENT_LIST = "eventList"
  const val EMPTY_EVENT_LIST_MSG = "emptyEventList"
  const val LOADING_INDICATOR = "eventsLoadingIndicator"
  const val CREATE_EVENT_BUTTON = "createEventFab"

  fun getTestTagForEventItem(event: Event): String = "eventItem${event.eventId}"
}

/**
 * Formats the days and the location of an [event], e.g. "Fri 16 to Sun 18 Oct · Parc de Vidy". The
 * end day is only shown if the [event] ends on another day, and its month is only repeated if it
 * ends in another month.
 */
internal fun formatEventDates(event: Event, locale: Locale = Locale.getDefault()): String {
  val fullDay = DateTimeFormatter.ofPattern("EEE d MMM", locale)
  val start = event.startDate.toLocalDate()
  val end = event.endDate.toLocalDate()
  val dates =
      when {
        end == start -> start.format(fullDay)
        YearMonth.from(end) == YearMonth.from(start) ->
            "${start.format(DateTimeFormatter.ofPattern("EEE d", locale))} to ${end.format(fullDay)}"
        else -> "${start.format(fullDay)} to ${end.format(fullDay)}"
      }
  return if (event.location.isBlank()) dates else "$dates · ${event.location}"
}

/**
 * Screen showing the [Event] items of the user, split between ongoing, upcoming and past ones.
 *
 * @param eventsOverviewViewModel The ViewModel providing the [Event] items.
 * @param onSelectEvent Called when the user clicks on an [Event] card.
 * @param onAddEvent Called when the user clicks on the "New event" button.
 * @param onSelectAvatar Called when the user clicks on its avatar in the top bar.
 */
@Composable
fun EventsOverviewScreen(
    eventsOverviewViewModel: EventsOverviewViewModel,
    onSelectEvent: (Event) -> Unit = {},
    // TODO: navigate to the new event screen once the app has navigation
    onAddEvent: () -> Unit = {},
    // TODO: navigate to the profile screen once the app has navigation
    onSelectAvatar: () -> Unit = {},
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
      onSelectAvatar = onSelectAvatar,
  )
}

/**
 * Stateless content of the [EventsOverviewScreen], displaying the given [uiState].
 *
 * @param uiState The state to display.
 * @param onSelectFilter Called when the user clicks on the "Ongoing", "Upcoming" or "Past" chip.
 * @param onSelectEvent Called when the user clicks on an [Event] card.
 * @param onAddEvent Called when the user clicks on the "New event" button.
 * @param onSelectAvatar Called when the user clicks on its avatar in the top bar.
 */
@Composable
fun EventsOverviewContent(
    uiState: EventsOverviewUIState,
    onSelectFilter: (EventsFilter) -> Unit = {},
    onSelectEvent: (Event) -> Unit = {},
    onAddEvent: () -> Unit = {},
    onSelectAvatar: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.statusBarsPadding()
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 20.dp, end = 12.dp),
        ) {
          Text(
              text = "Events overview",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f).testTag(EventsOverviewScreenTestTags.TITLE),
          )
          uiState.user?.let { user ->
            Box(modifier = Modifier.testTag(EventsOverviewScreenTestTags.USER_AVATAR)) {
              Avatar(user = user, onClick = onSelectAvatar)
            }
          }
        }
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            text = { Text(text = "New event", style = MaterialTheme.typography.labelLarge) },
            icon = {
              Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
            },
            onClick = onAddEvent,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            // The design has its own floating shadow instead of the Material elevation
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier =
                Modifier.dropShadow(
                        shape = CircleShape,
                        shadow =
                            Shadow(
                                radius = 24.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                offset = DpOffset(0.dp, 8.dp),
                                alpha = 0.14f,
                            ),
                    )
                    .testTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON),
        )
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
          // A new list state for each filter, so that switching filter starts back at the top
          val listState = key(uiState.selectedFilter) { rememberLazyListState() }
          LazyColumn(
              state = listState,
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

/** Card showing the [Event] of an [item]: its title, then its days and location. */
@Composable
fun EventItem(item: EventItemUIState, onClick: () -> Unit) {
  val event = item.event
  val shape = RoundedCornerShape(20.dp)
  Column(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier =
          Modifier.testTag(EventsOverviewScreenTestTags.getTestTagForEventItem(event))
              .fillMaxWidth()
              .clip(shape)
              .background(MaterialTheme.colorScheme.surfaceContainerLowest)
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
              .clickable(onClick = onClick)
              .padding(16.dp),
  ) {
    Text(
        text = event.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = formatEventDates(event),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** Preview * */
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun EventsOverviewContentPreview() {
  val zone = ZoneId.of("Europe/Zurich")
  fun date(month: Int, day: Int, hour: Int) =
      ZonedDateTime.of(2026, month, day, hour, 0, 0, 0, zone)
  val upcomingEvents =
      listOf(
          Event(
              eventId = "1",
              title = "Fête de l'Asso 2026",
              startDate = date(10, 16, 10),
              endDate = date(10, 18, 23),
              location = "Parc de Vidy, Lausanne",
          ),
          Event(
              eventId = "2",
              title = "Soirée Jazz",
              startDate = date(11, 6, 19),
              endDate = date(11, 6, 23),
              location = "Salle Polyvalente",
          ),
          Event(
              eventId = "3",
              title = "Marché de Noël",
              startDate = date(12, 12, 10),
              endDate = date(12, 13, 20),
              location = "Place de la Riponne",
          ),
      )
  val ongoingEvent =
      Event(
          eventId = "4",
          title = "Concert au Lac",
          startDate = date(10, 8, 18),
          endDate = date(10, 8, 23),
      )
  val pastEvents =
      (5..12).map {
        Event(
            eventId = "$it",
            title = "Event $it",
            startDate = date(9, it, 10),
            endDate = date(9, it, 20),
        )
      }
  val uiState =
      EventsOverviewUIState(
          ongoingEvents =
              listOf(EventItemUIState(ongoingEvent, openTasksCount = 0, isOngoing = true)),
          upcomingEvents = upcomingEvents.map { EventItemUIState(it, openTasksCount = 0) },
          pastEvents = pastEvents.map { EventItemUIState(it, openTasksCount = 0) },
          selectedFilter = EventsFilter.UPCOMING,
          user = User("u0", name = "Yasmine", surname = "Amrani"),
      )
  AppTheme { EventsOverviewContent(uiState = uiState) }
}
