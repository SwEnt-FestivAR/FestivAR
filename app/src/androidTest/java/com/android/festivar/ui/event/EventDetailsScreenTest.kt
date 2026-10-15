package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import android.annotation.SuppressLint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDetailsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

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
    composeTestRule.onNodeWithText("${tasksListSize / 2}").assertIsDisplayed()
    composeTestRule.onNodeWithText("of $tasksListSize").assertIsDisplayed()
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

  @SuppressLint("ViewModelConstructorInComposable")
  @Test
  fun displaysErrorWhenEventCannotBeFetched() {
    composeTestRule.setContent {
      EventDetailsScreen(
          eventId = "missing",
          viewModel =
              EventDetailsViewModel(
                  object : EventsRepository by EventsRepositoryLocal() {
                    override suspend fun getEvent(eventId: String): Event {
                      throw IllegalStateException("fetch failed")
                    }
                  }
              ),
          onTasksButtonClicked = {},
          onBack = {},
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText(
            "Something wrong happened while fetching the event. Please try again",
        )
        .assertIsDisplayed()
  }

  @Test
  fun clickingSeeTasksButtonCallsOnTasksButtonClicked() {
    var tasksButtonClicked = false
    val event = eventSameMonth

    setContent(event, onTasksButtonClicked = { tasksButtonClicked = true })

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()

    assertTrue(tasksButtonClicked)
  }

  @Test
  fun clickingBackButtonCallsOnBack() {
    var backClicked = false
    val event = eventSameMonth

    setContent(event, onBack = { backClicked = true })

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).performClick()

    assertTrue(backClicked)
  }

  @SuppressLint("ViewModelConstructorInComposable")
  private fun setContent(
      event: Event,
      onTasksButtonClicked: () -> Unit = {},
      onBack: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      EventDetailsScreen(
          eventId = event.eventId,
          viewModel = EventDetailsViewModel(EventsRepositoryLocal(listOf(event))),
          onTasksButtonClicked = onTasksButtonClicked,
          onBack = onBack,
      )
    }
    composeTestRule.waitForIdle()
  }
}
