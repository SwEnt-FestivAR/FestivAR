// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.task.Task
import com.android.festivar.model.temporary.User
import com.android.festivar.ui.components.BottomNavigationBarTestTags
import com.android.festivar.ui.components.NavigationTab
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventsOverviewScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * A fake [EventsRepository] serving [eventList]. It throws [error] on every fetch when it is set,
   * and waits for [gate] to be completed before answering when it is set.
   */
  private class EventsRepositoryImpl(
      val eventList: List<Event> = listOf(),
      val error: Exception? = null,
      val gate: CompletableDeferred<Unit>? = null,
  ) : EventsRepository {
    override fun getNewUid(): String {
      return "${eventList.size}"
    }

    override suspend fun getAllEvents(): List<Event> {
      gate?.await()
      error?.let { throw it }
      return eventList
    }

    override suspend fun getEvent(eventId: String): Event {
      TODO("Not yet implemented")
    }

    override suspend fun getEventsForUser(userId: String): List<Event> {
      TODO("Not yet implemented")
    }

    override suspend fun addEvent(event: Event) {
      TODO("Not yet implemented")
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      TODO("Not yet implemented")
    }

    override suspend fun deleteEvent(eventId: String) {
      TODO("Not yet implemented")
    }
  }

  private fun getDate(year: Int, month: Int, day: Int, hour: Int = 0): ZonedDateTime {
    return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  }

  /** The fixed current time of every test: 17/10/2026 at 18:00. */
  private val now = getDate(2026, 10, 17, hour = 18)

  private val sara = User(uid = "u1", name = "Sara", surname = "Keller")
  private val marc = User(uid = "u2", name = "Marc", surname = "Dubois")
  private val alice = User(uid = "u3", name = "Alice", surname = "Lambert")
  private val bob = User(uid = "u4", name = "Bob", surname = "Morel")

  private val ongoingEvent =
      Event(
          eventId = "0",
          title = "Fête de l'Asso",
          description = "The yearly party of the association",
          members = listOf(sara, marc, alice, bob),
          startDate = getDate(2026, 10, 17, hour = 16),
          endDate = getDate(2026, 10, 17, hour = 23),
          location = "Esplanade EPFL",
          tasks =
              listOf(
                  Task(taskId = "t1", eventId = "0", title = "Set up the bar"),
                  Task(taskId = "t2", eventId = "0", title = "Sound check", completed = true),
                  Task(taskId = "t3", eventId = "0", title = "Clean up"),
              ),
      )

  private val upcomingEvent =
      Event(
          eventId = "1",
          title = "Soirée Jazz",
          description = "A jazz concert at Satellite",
          members = listOf(sara),
          startDate = getDate(2026, 10, 19, hour = 20),
          endDate = getDate(2026, 10, 20, hour = 1),
          location = "Satellite",
          tasks = listOf(Task(taskId = "t4", eventId = "1", title = "Book the band")),
      )

  private val pastEvent =
      Event(
          eventId = "2",
          title = "Balélec",
          description = "The EPFL music festival",
          startDate = getDate(2026, 5, 8, hour = 17),
          endDate = getDate(2026, 5, 9, hour = 5),
          closed = true,
      )

  private fun setContent(
      withRepository: EventsRepository,
      onSelectEvent: (Event) -> Unit = {},
      onAddEvent: () -> Unit = {},
      onSelectTab: (NavigationTab) -> Unit = {},
  ): EventsOverviewViewModel {
    val viewModel = EventsOverviewViewModel(eventsRepository = withRepository, now = { now })
    composeTestRule.setContent {
      AppTheme {
        EventsOverviewScreen(
            eventsOverviewViewModel = viewModel,
            onSelectEvent = onSelectEvent,
            onAddEvent = onAddEvent,
            onSelectTab = onSelectTab,
        )
      }
    }
    return viewModel
  }

  /**
   * Asserts that [event]'s item contains a descendant matching [matcher], looking at the unmerged
   * tree so that each text of the card can be matched on its own.
   */
  private fun onEventItem(event: Event, matcher: SemanticsMatcher) {
    composeTestRule
        .onNode(
            hasTestTag(EventsOverviewScreenTestTags.getTestTagForEventItem(event))
                .and(hasAnyDescendant(matcher)),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
  }

  /** Formats a date the way the event cards do, e.g. "Sat 17 Oct, 16:00". */
  private fun formatDate(date: ZonedDateTime): String {
    val day = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return "${date.format(day)}, ${date.format(time)}"
  }

  /** Test that the title, the filter chips, the button and the navigation bar are displayed. */
  @Test
  fun displaysAllComponents() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.ONGOING_CHIP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  /** Test that each filter chip shows the number of events of its category. */
  @Test
  fun chipsShowTheNumberOfEventsOfTheirCategory() {
    val laterUpcoming =
        upcomingEvent.copy(
            eventId = "3",
            startDate = getDate(2026, 12, 1),
            endDate = getDate(2026, 12, 2),
        )
    setContent(
        withRepository =
            EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, laterUpcoming, pastEvent))
    )

    composeTestRule
        .onNode(hasTestTag(EventsOverviewScreenTestTags.ONGOING_CHIP).and(hasText("1")))
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasTestTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).and(hasText("2")))
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasTestTag(EventsOverviewScreenTestTags.PAST_CHIP).and(hasText("1")))
        .assertIsDisplayed()
  }

  /** Test that the ongoing events are shown by default, flagged as ongoing. */
  @Test
  fun ongoingEventsAreShownByDefault() {
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    )

    onEventItem(ongoingEvent, hasText(ongoingEvent.title))
    onEventItem(ongoingEvent, hasText("Ongoing"))
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(upcomingEvent))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(pastEvent))
        .assertDoesNotExist()
  }

  /** Test that clicking on the upcoming chip shows only the upcoming events. */
  @Test
  fun upcomingChipShowsUpcomingEvents() {
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()

    onEventItem(upcomingEvent, hasText(upcomingEvent.title))
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(ongoingEvent))
        .assertDoesNotExist()
  }

  /** Test that clicking on the past chip shows only the past events, flagged as closed. */
  @Test
  fun pastChipShowsPastEvents() {
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()

    onEventItem(pastEvent, hasText(pastEvent.title))
    onEventItem(pastEvent, hasText("Closed"))
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(ongoingEvent))
        .assertDoesNotExist()
  }

  /** Test that each category shows its own message when it has no event. */
  @Test
  fun emptyCategoriesShowAMessage() {
    setContent(withRepository = EventsRepositoryImpl())

    composeTestRule.onNodeWithText("You have no ongoing event.").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
    composeTestRule.onNodeWithText("You have no upcoming event.").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()
    composeTestRule.onNodeWithText("You have no past event.").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST).assertDoesNotExist()
  }

  /**
   * Test that an event ending on its starting day shows the end time only, followed by its
   * location.
   */
  @Test
  fun sameDayEventShowsEndTimeAndLocation() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))

    val dates = "${formatDate(ongoingEvent.startDate)} to 23:00 · Esplanade EPFL"
    onEventItem(ongoingEvent, hasText(dates, substring = false))
  }

  /**
   * Test that an event ending on another day repeats the end day, and that an event without
   * location shows only its dates.
   */
  @Test
  fun multiDayEventShowsEndDayAndNoLocationIfBlank() {
    setContent(withRepository = EventsRepositoryImpl(listOf(pastEvent)))
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()

    val dates = "${formatDate(pastEvent.startDate)} to ${formatDate(pastEvent.endDate)}"
    onEventItem(pastEvent, hasText(dates, substring = false))
  }

  /**
   * Test that the team of an event shows the initials of the first two members, a "+N" avatar for
   * the other ones, and the size of the team.
   */
  @Test
  fun eventItemShowsTheTeam() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))

    onEventItem(ongoingEvent, hasText("SK"))
    onEventItem(ongoingEvent, hasText("MD"))
    onEventItem(ongoingEvent, hasText("+2"))
    onEventItem(ongoingEvent, hasText("4 on the team"))
  }

  /** Test that an event without members says that no one is on the team yet. */
  @Test
  fun eventItemWithoutMembersShowsEmptyTeam() {
    setContent(withRepository = EventsRepositoryImpl(listOf(pastEvent)))
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()

    onEventItem(pastEvent, hasText("No one on the team yet."))
  }

  /** Test that the number of open tasks is shown, with the singular form for a single task. */
  @Test
  fun eventItemShowsTheOpenTasks() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent)))

    onEventItem(ongoingEvent, hasText("2 tasks open"))
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
    onEventItem(upcomingEvent, hasText("1 task open"))
  }

  /**
   * Test that the list of events is lazy loaded. We create 100 events and check that the last one
   * is not displayed without scrolling.
   */
  @Test
  fun eventListIsLazy() {
    val events =
        (1..100).map { i ->
          ongoingEvent.copy(eventId = "lazy$i", title = "${ongoingEvent.title} #$i")
        }
    setContent(withRepository = EventsRepositoryImpl(events))

    val listTag = EventsOverviewScreenTestTags.EVENT_LIST
    val lastTag = EventsOverviewScreenTestTags.getTestTagForEventItem(events.last())

    // Before any scrolling, the last item should NOT be composed at all.
    composeTestRule.onNodeWithTag(lastTag, useUnmergedTree = true).assertDoesNotExist()

    // Scroll until the last item comes into view.
    composeTestRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag(lastTag))

    // After scrolling, it should now exist and be visible.
    composeTestRule
        .onNodeWithTag(lastTag, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  /** Test that a loading indicator is shown until the events are fetched. */
  @Test
  fun loadingIndicatorIsShownUntilEventsAreFetched() {
    val gate = CompletableDeferred<Unit>()
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent), gate = gate))

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.LOADING_INDICATOR)
        .assertIsDisplayed()

    gate.complete(Unit)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.LOADING_INDICATOR)
        .assertDoesNotExist()
    onEventItem(ongoingEvent, hasText(ongoingEvent.title))
  }

  /** Test that the error message is shown then cleared when fetching the events fails. */
  @Test
  fun fetchFailureClearsTheErrorMsg() {
    val repository = EventsRepositoryImpl(error = RuntimeException("Network down"))
    val viewModel = setContent(withRepository = repository)

    composeTestRule.waitForIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    composeTestRule.onNodeWithText("You have no ongoing event.").assertIsDisplayed()
  }

  /** Test that clicking on an event card gives the clicked event. */
  @Test
  fun clickingAnEventCallsOnSelectEvent() {
    var selected: Event? = null
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent)),
        onSelectEvent = { selected = it },
    )

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(ongoingEvent))
        .performClick()

    assertEquals(ongoingEvent, selected)
  }

  /** Test that clicking on the "New event" button calls onAddEvent. */
  @Test
  fun clickingCreateButtonCallsOnAddEvent() {
    var clicks = 0
    setContent(withRepository = EventsRepositoryImpl(), onAddEvent = { clicks++ })

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assertEquals(1, clicks)
  }

  /** Test that clicking on a tab of the navigation bar gives the clicked tab. */
  @Test
  fun clickingATabCallsOnSelectTab() {
    var selected: NavigationTab? = null
    setContent(withRepository = EventsRepositoryImpl(), onSelectTab = { selected = it })

    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(NavigationTab.MAP))
        .performClick()

    assertEquals(NavigationTab.MAP, selected)
  }
}
