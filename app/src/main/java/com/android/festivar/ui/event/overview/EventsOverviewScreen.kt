// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.overview

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.Avatar
import com.android.festivar.ui.components.EmptyState
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.components.RefreshableStateList
import com.android.festivar.ui.components.floatingShadow
import com.android.festivar.ui.components.formatEventDate
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime

object EventsOverviewScreenTestTags {
  const val TITLE = "eventsOverviewTitle"
  const val USER_AVATAR = "eventsOverviewUserAvatar"
  const val ONGOING_CHIP = "ongoingEventsChip"
  const val UPCOMING_CHIP = "upcomingEventsChip"
  const val PAST_CHIP = "pastEventsChip"
  const val EVENT_LIST = "eventList"
  const val EVENT_ACTIONS_BUTTON = "eventActionsFab"
  const val JOIN_EVENT_BUTTON = "joinEventFab"
  const val CREATE_EVENT_BUTTON = "createEventFab"

  fun getTestTagForEventItem(event: Event): String = "eventItem${event.eventId}"
}

/**
 * Formats the days and the location of an [event], e.g. "Fri 16 to Sun 18 Oct · Parc de Vidy". The
 * days come from [formatEventDate].
 */
@Composable
private fun formatEventDates(event: Event): String {
  val dates = formatEventDate(event)
  return if (event.location.isBlank()) dates
  else stringResource(R.string.events_overview_dates_location, dates, event.location)
}

/**
 * Screen showing the [Event] items of the user, split between ongoing, upcoming and past ones. The
 * [Event] items are fetched again every time the screen comes back to the front, e.g. after adding
 * a new one, and when the user pulls the list down.
 *
 * @param eventsOverviewViewModel The ViewModel providing the [Event] items.
 * @param onSelectEvent Called when the user clicks on an [Event] card.
 * @param onJoinEvent Called when the user clicks on the "Join" button.
 * @param onAddEvent Called when the user clicks on the "Create" button.
 * @param onSelectAvatar Called when the user clicks on its avatar in the top bar.
 */
@Composable
fun EventsOverviewScreen(
    eventsOverviewViewModel: EventsOverviewViewModel,
    onSelectEvent: (Event) -> Unit = {},
    onJoinEvent: () -> Unit = {},
    onAddEvent: () -> Unit = {},
    onSelectAvatar: () -> Unit = {},
) {
  val uiState by eventsOverviewViewModel.uiState.collectAsState()

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { eventsOverviewViewModel.refreshUIState() }

  EventsOverviewContent(
      uiState = uiState,
      onSelectFilter = eventsOverviewViewModel::selectFilter,
      onRefresh = eventsOverviewViewModel::refreshUIState,
      onSelectEvent = onSelectEvent,
      onJoinEvent = onJoinEvent,
      onAddEvent = onAddEvent,
      onSelectAvatar = onSelectAvatar,
  )
}

/**
 * Stateless content of the [EventsOverviewScreen], displaying the given [uiState].
 *
 * @param uiState The state to display.
 * @param onSelectFilter Called when the user clicks on the "Ongoing", "Upcoming" or "Past" chip.
 * @param onRefresh Called when the user pulls the list down or clicks on "Retry".
 * @param onSelectEvent Called when the user clicks on an [Event] card.
 * @param onJoinEvent Called when the user clicks on the "Join" button.
 * @param onAddEvent Called when the user clicks on the "Create" button.
 * @param onSelectAvatar Called when the user clicks on its avatar in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsOverviewContent(
    uiState: EventsOverviewUIState,
    onSelectFilter: (EventsFilter) -> Unit = {},
    onRefresh: () -> Unit = {},
    onSelectEvent: (Event) -> Unit = {},
    onJoinEvent: () -> Unit = {},
    onAddEvent: () -> Unit = {},
    onSelectAvatar: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.events_overview_title),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(EventsOverviewScreenTestTags.TITLE),
              )
            },
            actions = {
              uiState.user?.let { user ->
                Box(
                    modifier =
                        Modifier.padding(end = 12.dp)
                            .testTag(EventsOverviewScreenTestTags.USER_AVATAR)
                ) {
                  Avatar(user = user, onClick = onSelectAvatar)
                }
              }
            },
        )
      },
      floatingActionButton = {
        // The empty state has its own "Join" and "Create" buttons, the error state has none
        if (uiState.hasError || !uiState.hasNoEvents) {
          EventActionsMenu(onJoinEvent = onJoinEvent, onAddEvent = onAddEvent)
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
            label = stringResource(R.string.events_overview_chip_ongoing),
            count = uiState.ongoingEvents.size,
            selected = uiState.selectedFilter == EventsFilter.ONGOING,
            onClick = { onSelectFilter(EventsFilter.ONGOING) },
            modifier = Modifier.testTag(EventsOverviewScreenTestTags.ONGOING_CHIP),
        )
        FilterChip(
            label = stringResource(R.string.events_overview_chip_upcoming),
            count = uiState.upcomingEvents.size,
            selected = uiState.selectedFilter == EventsFilter.UPCOMING,
            onClick = { onSelectFilter(EventsFilter.UPCOMING) },
            modifier = Modifier.testTag(EventsOverviewScreenTestTags.UPCOMING_CHIP),
        )
        FilterChip(
            label = stringResource(R.string.events_overview_chip_past),
            count = uiState.pastEvents.size,
            selected = uiState.selectedFilter == EventsFilter.PAST,
            onClick = { onSelectFilter(EventsFilter.PAST) },
            modifier = Modifier.testTag(EventsOverviewScreenTestTags.PAST_CHIP),
        )
      }

      RefreshableStateList(
          isLoading = uiState.isLoading,
          hasNoItems = uiState.hasNoEvents,
          hasNoShownItems = uiState.shownEvents.isEmpty(),
          errorMessage = if (uiState.hasError) errorMessage(uiState) else null,
          onRefresh = onRefresh,
          filterEmptyMessage =
              stringResource(
                  when (uiState.selectedFilter) {
                    EventsFilter.ONGOING -> R.string.events_overview_no_ongoing
                    EventsFilter.UPCOMING -> R.string.events_overview_no_upcoming
                    EventsFilter.PAST -> R.string.events_overview_no_past
                  }
              ),
          emptyState = { EmptyEventsState(onJoinEvent = onJoinEvent, onAddEvent = onAddEvent) },
          modifier = Modifier.fillMaxSize(),
          // A new list state for each filter, so that switching filter starts back at the top
          listKey = uiState.selectedFilter,
          listTestTag = EventsOverviewScreenTestTags.EVENT_LIST,
      ) {
        items(uiState.shownEvents, key = { it.eventId }) { event ->
          EventItem(event = event, onClick = { onSelectEvent(event) })
        }
      }
    }
  }
}

/** The error of the [uiState], with its cause if the repository gave one. */
@Composable
private fun errorMessage(uiState: EventsOverviewUIState): String =
    uiState.errorMsg?.let { stringResource(R.string.events_overview_error, it) }
        ?: stringResource(R.string.events_overview_error_no_cause)

/**
 * Empty state of the [EventsOverviewScreen], shown when the user has no [Event] at all. It offers
 * to join or to create one, both buttons sharing the width equally.
 */
@Composable
private fun EmptyEventsState(onJoinEvent: () -> Unit, onAddEvent: () -> Unit) {
  val buttonShape = RoundedCornerShape(12.dp)
  val buttonPadding = PaddingValues(horizontal = 12.dp)
  EmptyState(
      icon = R.drawable.ic_event,
      title = stringResource(R.string.events_overview_empty_title),
      subtitle = stringResource(R.string.events_overview_empty_subtitle),
  ) {
    OutlinedButton(
        onClick = onJoinEvent,
        shape = buttonShape,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = buttonPadding,
        modifier =
            Modifier.weight(1f)
                .height(52.dp)
                .testTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON),
    ) {
      Icon(painter = painterResource(R.drawable.ic_groups), contentDescription = null)
      Spacer(modifier = Modifier.width(10.dp))
      Text(
          text = stringResource(R.string.events_overview_join),
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
      )
    }
    Button(
        onClick = onAddEvent,
        shape = buttonShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        contentPadding = buttonPadding,
        modifier =
            Modifier.weight(1f)
                .height(52.dp)
                .testTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON),
    ) {
      Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
      Spacer(modifier = Modifier.width(10.dp))
      Text(
          text = stringResource(R.string.events_overview_create),
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
      )
    }
  }
}

/**
 * Floating "+" button of the [EventsOverviewScreen]. Clicking on it turns it into a close button
 * and shows the "Join" and "Create" buttons above it.
 */
@Composable
private fun EventActionsMenu(onJoinEvent: () -> Unit, onAddEvent: () -> Unit) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  // The design has its own floating shadow instead of the Material elevation
  val elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
  val floatingShadow = Modifier.floatingShadow(MaterialTheme.colorScheme.onSurface)

  BackHandler(enabled = expanded) { expanded = false }

  Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
      Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        EventActionButton(
            label = stringResource(R.string.events_overview_join),
            icon = R.drawable.ic_groups,
            onClick = {
              expanded = false
              onJoinEvent()
            },
            elevation = elevation,
            modifier = floatingShadow.testTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON),
        )
        EventActionButton(
            label = stringResource(R.string.events_overview_create),
            icon = R.drawable.ic_add,
            onClick = {
              expanded = false
              onAddEvent()
            },
            elevation = elevation,
            modifier = floatingShadow.testTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON),
        )
      }
    }
    FloatingActionButton(
        onClick = { expanded = !expanded },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = elevation,
        modifier = floatingShadow.testTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON),
    ) {
      Icon(
          painter = painterResource(if (expanded) R.drawable.ic_close else R.drawable.ic_add),
          contentDescription =
              stringResource(
                  if (expanded) R.string.events_overview_close else R.string.events_overview_actions
              ),
      )
    }
  }
}

/** Button of the [EventActionsMenu]: an [icon] followed by a [label]. */
@Composable
private fun EventActionButton(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    elevation: FloatingActionButtonElevation,
    modifier: Modifier = Modifier,
) {
  ExtendedFloatingActionButton(
      text = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
      icon = { Icon(painter = painterResource(icon), contentDescription = null) },
      onClick = onClick,
      shape = CircleShape,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      elevation = elevation,
      modifier = modifier,
  )
}

/** Card showing an [event]: its title, then its days and location. */
@Composable
private fun EventItem(event: Event, onClick: () -> Unit) {
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
              ownerId = "owner-1",
              title = "Fête de l'Asso 2026",
              startDate = date(10, 16, 10),
              endDate = date(10, 18, 23),
              location = "Parc de Vidy, Lausanne",
          ),
          Event(
              eventId = "2",
              ownerId = "owner-2",
              title = "Soirée Jazz",
              startDate = date(11, 6, 19),
              endDate = date(11, 6, 23),
              location = "Salle Polyvalente",
          ),
          Event(
              eventId = "3",
              ownerId = "owner-3",
              title = "Marché de Noël",
              startDate = date(12, 12, 10),
              endDate = date(12, 13, 20),
              location = "Place de la Riponne",
          ),
      )
  val ongoingEvent =
      Event(
          eventId = "4",
          ownerId = "owner-4",
          title = "Concert au Lac",
          startDate = date(10, 8, 18),
          endDate = date(10, 8, 23),
      )
  val pastEvents =
      (5..12).map {
        Event(
            eventId = "$it",
            ownerId = "owner-$it",
            title = "Event $it",
            startDate = date(9, it, 10),
            endDate = date(9, it, 20),
        )
      }
  val uiState =
      EventsOverviewUIState(
          ongoingEvents = listOf(ongoingEvent),
          upcomingEvents = upcomingEvents,
          pastEvents = pastEvents,
          selectedFilter = EventsFilter.UPCOMING,
          user = User("u0", name = "Yasmine", surname = "Amrani"),
      )
  AppTheme { EventsOverviewContent(uiState = uiState) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun EventsOverviewEmptyPreview() {
  AppTheme {
    EventsOverviewContent(
        uiState = EventsOverviewUIState(user = User("u0", name = "Yasmine", surname = "Amrani"))
    )
  }
}
