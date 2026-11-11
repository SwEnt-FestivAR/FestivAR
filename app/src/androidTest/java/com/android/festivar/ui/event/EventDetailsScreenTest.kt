package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import android.annotation.SuppressLint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.event.details.EventDetailsScreen
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.details.EventDetailsViewModel
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDetailsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val eventStartDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  private val eventEndDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private val tasksListSize = 10

  private val eventSameMonth =
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
                        ownerId = "user",
                        completed = index % 2 == 0,
                    )
                  },
              ),
      )

  private val eventSameDay =
      eventSameMonth.copy(endDate = eventEndDate.withDayOfMonth(eventStartDate.dayOfMonth))
  private val eventDifferentMonth = eventSameMonth.copy(endDate = eventStartDate.plusMonths(1))
  private val eventDifferentYear = eventSameMonth.copy(endDate = eventStartDate.plusYears(1))
  private val eventNoTasks = eventSameMonth.copy(tasks = emptyList())

  /** Breaks if the screen does not display the event information and expected controls. */
  @Test
  fun displaysEventDataAndTaggedElements() {
    val event = eventSameMonth

    setContent(event)

    listOf(
            EventDetailsScreenTestTags.BACK_BUTTON,
            EventDetailsScreenTestTags.EVENT_NAME,
            EventDetailsScreenTestTags.OPEN_TASKS_STAT,
            EventDetailsScreenTestTags.STATUS_PILL,
            EventDetailsScreenTestTags.SUBTITLE,
            EventDetailsScreenTestTags.SEE_TASKS_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }

    composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
    composeTestRule
        .onNode(
            hasTestTag(EventDetailsScreenTestTags.STATUS_PILL) and hasAnyDescendant(hasText("Open"))
        )
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("${tasksListSize / 2}").assertIsDisplayed()
    composeTestRule.onNodeWithText("of $tasksListSize").assertIsDisplayed()
    composeTestRule.onNodeWithText("tasks still open").assertIsDisplayed()
    assertSubtitle("Fri 16 to Sun 18 Oct · Parc de Vidy, Lausanne")
  }

  /** Breaks if an event ending on its start day is formatted as a multi-day event. */
  @Test
  fun displaysSameDayEventDate() {
    setContent(eventSameDay)
    assertSubtitle("Fri 16 Oct · Parc de Vidy, Lausanne")
  }

  /** Breaks if an event spanning one month is formatted without both endpoint dates. */
  @Test
  fun displaysDifferentMonthEventDate() {
    setContent(eventDifferentMonth)
    assertSubtitle("Fri 16 Oct to Mon 16 Nov · Parc de Vidy, Lausanne")
  }

  /** Breaks if an event spanning one year omits either year from the displayed date. */
  @Test
  fun displaysDifferentYearEventDate() {
    setContent(eventDifferentYear)
    assertSubtitle("Fri 16 Oct 2026 to Sat 16 Oct 2027 · Parc de Vidy, Lausanne")
  }

  /** Red if an event with no tasks displays incorrect task statistics. */
  @Test
  fun displaysNoTasksEventStatistics() {
    setContent(eventNoTasks)
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
    composeTestRule.onNodeWithText("of 0").assertIsDisplayed()
  }

  /** Breaks if an event without a place still shows a dangling separator after its dates. */
  @Test
  fun subtitleShowsOnlyTheDatesWhenTheLocationIsEmpty() {
    setContent(eventSameMonth.copy(location = ""))

    assertSubtitle("Fri 16 to Sun 18 Oct")
  }

  /** Breaks if a whitespace-only location is shown as a place after the dates. */
  @Test
  fun subtitleShowsOnlyTheDatesWhenTheLocationIsBlank() {
    setContent(eventSameMonth.copy(location = "     "))

    assertSubtitle("Fri 16 to Sun 18 Oct")
  }

  /** Breaks if an open event is not marked Open, or a closed one is not marked Closed. */
  @Test
  fun statusPillFollowsWhetherTheEventIsClosed() {
    setContent(eventSameMonth.copy(closed = true))

    composeTestRule
        .onNode(
            hasTestTag(EventDetailsScreenTestTags.STATUS_PILL) and
                hasAnyDescendant(hasText("Closed"))
        )
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Open").assertDoesNotExist()
  }

  /** Breaks if a long title pushes the status pill out of the screen. */
  @Test
  fun statusPillStaysVisibleWithALongTitle() {
    setContent(eventSameMonth.copy(title = "Annual open-air association festival ".repeat(6)))

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.STATUS_PILL).assertIsDisplayed()
  }

  /** Red if a fetch failure leaves event content or actions visible. */
  @SuppressLint("ViewModelConstructorInComposable")
  @Test
  fun displaysErrorWhenEventCannotBeFetched() {
    composeTestRule.setContent {
      EventDetailsScreen(
          viewModel =
              EventDetailsViewModel(
                  dataRepositoryWith(
                      object : EventsRepository by EventsRepositoryLocal() {
                        override suspend fun getEvent(eventId: String): Event {
                          throw IllegalStateException("fetch failed")
                        }
                      }
                  ),
                  eventId = "missing",
              ),
          onTasksButtonClicked = {},
          onBack = {},
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.FETCH_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.EVENT_NAME)
            .fetchSemanticsNodes()
            .isEmpty()
    )
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON)
            .fetchSemanticsNodes()
            .isEmpty()
    )
  }

  /** Red if clicking See tasks does not invoke the supplied callback exactly once. */
  @Test
  fun clickingSeeTasksButtonCallsOnTasksButtonClicked() {
    val onTasksButtonClicked = mockk<() -> Unit>(relaxed = true)
    val event = eventSameMonth

    setContent(event, onTasksButtonClicked = onTasksButtonClicked)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()

    verify(exactly = 1) { onTasksButtonClicked() }
  }

  /** Red if clicking Back does not invoke the supplied callback exactly once. */
  @Test
  fun clickingBackButtonCallsOnBack() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    val event = eventSameMonth

    setContent(event, onBack = onBack)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).performClick()

    verify(exactly = 1) { onBack() }
  }

  private fun dataRepositoryWith(eventsRepository: EventsRepository) =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
      )

  private fun assertSubtitle(text: String) {
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE).assertTextEquals(text)
  }

  @SuppressLint("ViewModelConstructorInComposable")
  private fun setContent(
      event: Event,
      onTasksButtonClicked: () -> Unit = {},
      onBack: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      EventDetailsScreen(
          viewModel =
              EventDetailsViewModel(
                  dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
                  event.eventId,
              ),
          onTasksButtonClicked = onTasksButtonClicked,
          onBack = onBack,
      )
    }
    composeTestRule.waitForIdle()
  }
}
