package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDetailsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun displaysEventDataAndTaggedElements() {
    val event = createEvent()

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
  }

  @Test
  fun clickingSeeTasksButtonCallsOnTasksButtonClicked() {
    var tasksButtonClicked = false
    val event = createEvent()

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
    val event = createEvent()

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

  private fun createEvent() =
      Event(
          eventId = "event-id",
          title = "Fête de l'Asso 2026",
          members = listOf(User("Sami K.")),
          startDate = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich")),
          endDate = ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich")),
          location = "Parc de Vidy, Lausanne",
      )
}
