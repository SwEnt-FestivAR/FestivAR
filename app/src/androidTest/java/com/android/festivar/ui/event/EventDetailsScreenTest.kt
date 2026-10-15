package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    composeTestRule.setContent {
      EventDetailsScreen(event = event, onTasksButtonClicked = {}, onBack = {})
    }

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
    composeTestRule.setContent {
      EventDetailsScreen(event = eventSameDay, onTasksButtonClicked = {}, onBack = {})
    }
    composeTestRule.onNodeWithText("Fri 16 Oct").assertIsDisplayed()
  }

  @Test
  fun displaysDifferentMonthEventDate() {
    composeTestRule.setContent {
      EventDetailsScreen(event = eventDifferentMonth, onTasksButtonClicked = {}, onBack = {})
    }
    composeTestRule.onNodeWithText("Fri 16 Oct to Mon 16 Nov").assertIsDisplayed()
  }

  @Test
  fun displaysDifferentYearEventDate() {
    composeTestRule.setContent {
      EventDetailsScreen(event = eventDifferentYear, onTasksButtonClicked = {}, onBack = {})
    }
    composeTestRule.onNodeWithText("Fri 16 Oct 2026 to Sat 16 Oct 2027").assertIsDisplayed()
  }

  @Test
  fun displaysNoTasksEventStatistics() {
    composeTestRule.setContent {
      EventDetailsScreen(event = eventNoTasks, onTasksButtonClicked = {}, onBack = {})
    }
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
    composeTestRule.onNodeWithText("of 0").assertIsDisplayed()
  }

  @Test
  fun clickingSeeTasksButtonCallsOnTasksButtonClicked() {
    var tasksButtonClicked = false
    val event = eventSameMonth

    composeTestRule.setContent {
      EventDetailsScreen(
          event = event,
          onTasksButtonClicked = { tasksButtonClicked = true },
          onBack = {},
      )
    }

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()

    assertTrue(tasksButtonClicked)
  }

  @Test
  fun clickingBackButtonCallsOnBack() {
    var backClicked = false
    val event = eventSameMonth

    composeTestRule.setContent {
      EventDetailsScreen(
          event = event,
          onTasksButtonClicked = {},
          onBack = { backClicked = true },
      )
    }

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).performClick()

    assertTrue(backClicked)
  }
}
