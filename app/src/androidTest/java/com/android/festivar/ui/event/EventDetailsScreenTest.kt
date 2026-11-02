package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import android.annotation.SuppressLint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
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

  private val TASKS_LIST_SIZE = 10

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
                  size = TASKS_LIST_SIZE,
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

  @Test
  fun displaysEventDataAndTaggedElements() {
    val event = eventSameMonth

    setContent(event)

    listOf(
            EventDetailsScreenTestTags.BACK_BUTTON,
            EventDetailsScreenTestTags.EVENT_NAME,
            EventDetailsScreenTestTags.OPEN_TASKS_STAT,
            EventDetailsScreenTestTags.OWNER_ROW,
            EventDetailsScreenTestTags.DATE_ROW,
            EventDetailsScreenTestTags.ADDRESS_ROW,
            EventDetailsScreenTestTags.SEE_TASKS_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }

    composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sami K.").assertIsDisplayed()
    composeTestRule.onNodeWithText(event.location).assertIsDisplayed()
    composeTestRule.onNodeWithText("${TASKS_LIST_SIZE / 2}").assertIsDisplayed()
    composeTestRule.onNodeWithText("of $TASKS_LIST_SIZE").assertIsDisplayed()
    composeTestRule.onNodeWithText("tasks still open").assertIsDisplayed()
    composeTestRule.onNodeWithText("Fri 16 to Sun 18 Oct").assertIsDisplayed()
  }

  @Test
  fun displaysSameDayEventDate() {
    setContent(eventSameDay)
    composeTestRule.onNodeWithText("Fri 16 Oct").assertIsDisplayed()
  }

  @Test
  fun displaysDifferentMonthEventDate() {
    setContent(eventDifferentMonth)
    composeTestRule.onNodeWithText("Fri 16 Oct to Mon 16 Nov").assertIsDisplayed()
  }

  @Test
  fun displaysDifferentYearEventDate() {
    setContent(eventDifferentYear)
    composeTestRule.onNodeWithText("Fri 16 Oct 2026 to Sat 16 Oct 2027").assertIsDisplayed()
  }

  @Test
  fun displaysNoTasksEventStatistics() {
    setContent(eventNoTasks)
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
    composeTestRule.onNodeWithText("of 0").assertIsDisplayed()
  }

  @Test
  fun displaysFallbackWhenEventHasNoLocation() {
    setContent(eventSameMonth.copy(location = ""))

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ADDRESS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithText("No location specified").assertIsDisplayed()
  }

  @SuppressLint("ViewModelConstructorInComposable")
  @Test
  fun displaysErrorWhenEventCannotBeFetched() {
    composeTestRule.setContent {
      EventDetailsScreen(
          viewModel =
              EventDetailsViewModel(
                  object : EventsRepository by EventsRepositoryLocal() {
                    override suspend fun getEvent(eventId: String): Event {
                      throw IllegalStateException("fetch failed")
                    }
                  },
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

  @Test
  fun clickingSeeTasksButtonCallsOnTasksButtonClicked() {
    val onTasksButtonClicked = mockk<() -> Unit>(relaxed = true)
    val event = eventSameMonth

    setContent(event, onTasksButtonClicked = onTasksButtonClicked)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()

    verify(exactly = 1) { onTasksButtonClicked() }
  }

  @Test
  fun clickingBackButtonCallsOnBack() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    val event = eventSameMonth

    setContent(event, onBack = onBack)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).performClick()

    verify(exactly = 1) { onBack() }
  }

  @SuppressLint("ViewModelConstructorInComposable")
  private fun setContent(
      event: Event,
      onTasksButtonClicked: () -> Unit = {},
      onBack: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      EventDetailsScreen(
          viewModel = EventDetailsViewModel(EventsRepositoryLocal(listOf(event)), event.eventId),
          onTasksButtonClicked = onTasksButtonClicked,
          onBack = onBack,
      )
    }
    composeTestRule.waitForIdle()
  }
}
