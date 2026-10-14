// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventsOverviewScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * An [EventsRepositoryLocal] serving [events]. It throws [error] on every fetch of the events of
   * a user when it is set, and waits for [gate] to be completed before answering when it is set.
   */
  private class EventsRepositoryImpl(
      events: List<Event> = listOf(),
      val error: Exception? = null,
      @Volatile var gate: CompletableDeferred<Unit>? = null,
      private val local: EventsRepository = EventsRepositoryLocal(events),
  ) : EventsRepository by local {
    override suspend fun getEventsForUser(userId: String): List<Event> {
      gate?.await()
      error?.let { throw it }
      return local.getEventsForUser(userId)
    }
  }

  private fun getDate(year: Int, month: Int, day: Int, hour: Int = 0): ZonedDateTime {
    return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  }

  /** The fixed current time of every test: 17/10/2026 at 18:00. */
  private val now = getDate(2026, 10, 17, hour = 18)

  /** The signed-in user of every test, member of every event. */
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
          members = listOf(sara),
          startDate = getDate(2026, 5, 8, hour = 17),
          endDate = getDate(2026, 5, 9, hour = 5),
          closed = true,
      )

  private fun createViewModel(withRepository: EventsRepository): EventsOverviewViewModel {
    return EventsOverviewViewModel(eventsRepository = withRepository, user = sara, now = { now })
  }

  private fun setContent(
      withRepository: EventsRepository,
      onSelectEvent: (Event) -> Unit = {},
      onJoinEvent: () -> Unit = {},
      onAddEvent: () -> Unit = {},
      onSelectAvatar: () -> Unit = {},
      viewModel: EventsOverviewViewModel = createViewModel(withRepository),
  ): EventsOverviewViewModel {
    composeTestRule.setContent {
      AppTheme {
        EventsOverviewScreen(
            eventsOverviewViewModel = viewModel,
            onSelectEvent = onSelectEvent,
            onJoinEvent = onJoinEvent,
            onAddEvent = onAddEvent,
            onSelectAvatar = onSelectAvatar,
        )
      }
    }
    return viewModel
  }

  /** Asserts that neither the "Join" nor the "Create" button is in the screen. */
  private fun assertJoinAndCreateButtonsDoNotExist() {
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
        .assertDoesNotExist()
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

  /**
   * Test that the title, the avatar, the filter chips, the list and the "+" button are displayed,
   * and that the "Join" and "Create" buttons stay hidden until the "+" button is clicked.
   */
  @Test
  fun displaysAllComponents() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.USER_AVATAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.ONGOING_CHIP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST).assertIsDisplayed()
    assertJoinAndCreateButtonsDoNotExist()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE).assertDoesNotExist()
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

  /** Test that the avatar of the top bar shows the initials of the signed-in user. */
  @Test
  fun avatarShowsTheInitialsOfTheUser() {
    setContent(withRepository = EventsRepositoryImpl())

    composeTestRule
        .onNode(
            hasTestTag(EventsOverviewScreenTestTags.USER_AVATAR)
                .and(hasAnyDescendant(hasText("SK"))),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
  }

  /** Test that the ongoing events are shown by default. */
  @Test
  fun ongoingEventsAreShownByDefault() {
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    )

    onEventItem(ongoingEvent, hasText(ongoingEvent.title))
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

  /** Test that clicking on the past chip shows only the past events. */
  @Test
  fun pastChipShowsPastEvents() {
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()

    onEventItem(pastEvent, hasText(pastEvent.title))
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(ongoingEvent))
        .assertDoesNotExist()
  }

  /**
   * Test that the ongoing and the upcoming categories show their own message when they have no
   * event while another category has one, and that the "+" button is still there.
   */
  @Test
  fun emptyOngoingAndUpcomingCategoriesShowAMessage() {
    setContent(withRepository = EventsRepositoryImpl(listOf(pastEvent)))
    val message = composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_EVENT_LIST_MSG)

    message.assertIsDisplayed().assertTextEquals("You have no ongoing event.")
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
    message.assertIsDisplayed().assertTextEquals("You have no upcoming event.")

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE).assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON)
        .assertIsDisplayed()
  }

  /** Test that the past category shows its own message when only the other ones have events. */
  @Test
  fun emptyPastCategoryShowsAMessage() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_EVENT_LIST_MSG)
        .assertIsDisplayed()
        .assertTextEquals("You have no past event.")
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE).assertDoesNotExist()
  }

  /**
   * Test that a user without any event sees the empty state with its "Join" and "Create" buttons,
   * instead of the message of a category and of the "+" button.
   */
  @Test
  fun emptyStateIsShownWhenTheUserHasNoEvent() {
    setContent(withRepository = EventsRepositoryImpl())

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("No events yet")
    composeTestRule.onNodeWithText("Join or create an event.").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
        .assertIsDisplayed()
        .assertTextEquals("Join")
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
        .assertIsDisplayed()
        .assertTextEquals("Create")

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON)
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_EVENT_LIST_MSG)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST).assertDoesNotExist()
  }

  /** Test that the empty state stays whichever filter chip is selected, each one counting 0. */
  @Test
  fun emptyStateStaysOnEveryFilter() {
    setContent(withRepository = EventsRepositoryImpl())

    listOf(
            EventsOverviewScreenTestTags.ONGOING_CHIP,
            EventsOverviewScreenTestTags.UPCOMING_CHIP,
            EventsOverviewScreenTestTags.PAST_CHIP,
        )
        .forEach { chip ->
          composeTestRule
              .onNode(hasTestTag(chip).and(hasText("0")))
              .assertIsDisplayed()
              .performClick()
          composeTestRule
              .onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE)
              .assertIsDisplayed()
          composeTestRule
              .onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_EVENT_LIST_MSG)
              .assertDoesNotExist()
        }
  }

  /** Test that an event ending on its starting day shows this day only, then its location. */
  @Test
  fun sameDayEventShowsItsDayAndLocation() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))

    onEventItem(ongoingEvent, hasText("Sat 17 Oct · Esplanade EPFL", substring = false))
  }

  /**
   * Test that an event ending on another day of the same month shows both days with the month once,
   * and that an event without location shows only its days.
   */
  @Test
  fun multiDayEventShowsEndDayAndNoLocationIfBlank() {
    setContent(withRepository = EventsRepositoryImpl(listOf(pastEvent)))
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.PAST_CHIP).performClick()

    onEventItem(pastEvent, hasText("Fri 8 to Sat 9 May", substring = false))
  }

  /** Test that an event ending in another month shows the month of both its days. */
  @Test
  fun eventEndingInAnotherMonthShowsBothMonths() {
    val event =
        upcomingEvent.copy(
            startDate = getDate(2026, 10, 31, hour = 20),
            endDate = getDate(2026, 11, 1, hour = 4),
        )
    setContent(withRepository = EventsRepositoryImpl(listOf(event)))
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()

    onEventItem(event, hasText("Sat 31 Oct to Sun 1 Nov · Satellite", substring = false))
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

  /** Test that switching filter shows the new list from its top, whatever the previous scroll. */
  @Test
  fun switchingFilterStartsBackAtTheTopOfTheList() {
    val ongoingEvents = (1..30).map { i -> ongoingEvent.copy(eventId = "ongoing$i") }
    val upcomingEvents = (1..30).map { i -> upcomingEvent.copy(eventId = "upcoming$i") }
    setContent(withRepository = EventsRepositoryImpl(ongoingEvents + upcomingEvents))

    val lastOngoingTag = EventsOverviewScreenTestTags.getTestTagForEventItem(ongoingEvents.last())
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag(lastOngoingTag))
    composeTestRule.onNodeWithTag(lastOngoingTag).assertIsDisplayed()

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(upcomingEvents.first()))
        .assertIsDisplayed()
  }

  /**
   * Test that a loading indicator is shown, and not the empty state, until the events are fetched.
   */
  @Test
  fun loadingIndicatorIsShownUntilEventsAreFetched() {
    val gate = CompletableDeferred<Unit>()
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent), gate = gate))

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.LOADING_INDICATOR)
        .assertIsDisplayed()
    // The empty state must not flash while the first events are still being fetched
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE).assertDoesNotExist()
    assertJoinAndCreateButtonsDoNotExist()

    gate.complete(Unit)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.LOADING_INDICATOR)
        .assertDoesNotExist()
    onEventItem(ongoingEvent, hasText(ongoingEvent.title))
  }

  /**
   * Test that refreshing events already loaded keeps showing them instead of the loading indicator,
   * e.g. when coming back to the screen.
   */
  @Test
  fun refreshKeepsTheEventsShownInsteadOfTheLoadingIndicator() {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent))
    val viewModel = createViewModel(withRepository = repository)
    composeTestRule.waitUntil { !viewModel.uiState.value.isLoading }

    // Hold the refresh started when the screen is shown
    val gate = CompletableDeferred<Unit>()
    repository.gate = gate
    setContent(withRepository = repository, viewModel = viewModel)
    composeTestRule.waitForIdle()
    assertTrue(viewModel.uiState.value.isLoading)

    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.LOADING_INDICATOR)
        .assertDoesNotExist()
    onEventItem(ongoingEvent, hasText(ongoingEvent.title))

    gate.complete(Unit)
    composeTestRule.waitUntil { !viewModel.uiState.value.isLoading }
    onEventItem(ongoingEvent, hasText(ongoingEvent.title))
  }

  /** Test that the error message is shown then cleared when fetching the events fails. */
  @Test
  fun fetchFailureClearsTheErrorMsg() {
    val repository = EventsRepositoryImpl(error = RuntimeException("Network down"))
    val viewModel = setContent(withRepository = repository)

    composeTestRule.waitForIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.LOADING_INDICATOR)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EMPTY_TITLE).assertIsDisplayed()
  }

  /**
   * Test that the events are fetched again when the screen is shown, so that an event added since
   * the ViewModel was created appears.
   */
  @Test
  fun eventsAreFetchedAgainWhenTheScreenIsShown() {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent))
    val viewModel = createViewModel(withRepository = repository)
    composeTestRule.waitUntil { !viewModel.uiState.value.isLoading }
    assertEquals(listOf<Event>(), viewModel.uiState.value.upcomingEvents)

    runBlocking { repository.addEvent(upcomingEvent) }
    setContent(withRepository = repository, viewModel = viewModel)

    composeTestRule
        .onNode(hasTestTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).and(hasText("1")))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
    onEventItem(upcomingEvent, hasText(upcomingEvent.title))
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

  /**
   * Test that the "+" button turns into a close button showing the "Join" and "Create" buttons, and
   * that clicking on it again hides them.
   */
  @Test
  fun actionsButtonShowsThenHidesJoinAndCreateButtons() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))
    val actionsButton =
        composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON)

    actionsButton.assert(hasContentDescription("Join or create an event"))
    assertJoinAndCreateButtonsDoNotExist()

    actionsButton.performClick()

    actionsButton.assertIsDisplayed().assert(hasContentDescription("Close"))
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
        .assertIsDisplayed()

    actionsButton.performClick()

    actionsButton.assert(hasContentDescription("Join or create an event"))
    assertJoinAndCreateButtonsDoNotExist()
  }

  /** Test that the back button of the device hides the "Join" and "Create" buttons again. */
  @Test
  fun backButtonHidesJoinAndCreateButtons() {
    setContent(withRepository = EventsRepositoryImpl(listOf(ongoingEvent)))
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
        .assertIsDisplayed()

    Espresso.pressBack()

    assertJoinAndCreateButtonsDoNotExist()
    // The back button only closed the menu: the screen is still there
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON)
        .assert(hasContentDescription("Join or create an event"))
  }

  /** Test that the "Join" button of the "+" button calls onJoinEvent only, then hides again. */
  @Test
  fun clickingJoinButtonCallsOnJoinEventAndHidesTheButtons() {
    var joins = 0
    var adds = 0
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent)),
        onJoinEvent = { joins++ },
        onAddEvent = { adds++ },
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()

    assertEquals(1, joins)
    assertEquals(0, adds)
    assertJoinAndCreateButtonsDoNotExist()
  }

  /** Test that the "Create" button of the "+" button calls onAddEvent only, then hides again. */
  @Test
  fun clickingCreateButtonCallsOnAddEventAndHidesTheButtons() {
    var joins = 0
    var adds = 0
    setContent(
        withRepository = EventsRepositoryImpl(listOf(ongoingEvent)),
        onJoinEvent = { joins++ },
        onAddEvent = { adds++ },
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assertEquals(0, joins)
    assertEquals(1, adds)
    assertJoinAndCreateButtonsDoNotExist()
  }

  /** Test that the "Join" button of the empty state calls onJoinEvent only. */
  @Test
  fun clickingJoinButtonOfTheEmptyStateCallsOnJoinEvent() {
    var joins = 0
    var adds = 0
    setContent(
        withRepository = EventsRepositoryImpl(),
        onJoinEvent = { joins++ },
        onAddEvent = { adds++ },
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()

    assertEquals(1, joins)
    assertEquals(0, adds)
  }

  /** Test that the "Create" button of the empty state calls onAddEvent only. */
  @Test
  fun clickingCreateButtonOfTheEmptyStateCallsOnAddEvent() {
    var joins = 0
    var adds = 0
    setContent(
        withRepository = EventsRepositoryImpl(),
        onJoinEvent = { joins++ },
        onAddEvent = { adds++ },
    )

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assertEquals(0, joins)
    assertEquals(1, adds)
  }

  /** Test that clicking on the avatar of the top bar calls onSelectAvatar. */
  @Test
  fun clickingAvatarCallsOnSelectAvatar() {
    var clicks = 0
    setContent(withRepository = EventsRepositoryImpl(), onSelectAvatar = { clicks++ })

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.USER_AVATAR).performClick()

    assertEquals(1, clicks)
  }
}
