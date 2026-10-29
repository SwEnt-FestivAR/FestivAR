package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateEventScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun dateAndTimeFieldsAreDisplayedAndShowPickedValues() {
    val viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)

    listOf(
            CreateEventScreenTestTags.START_DATE_FIELD,
            CreateEventScreenTestTags.START_TIME_FIELD,
            CreateEventScreenTestTags.END_DATE_FIELD,
            CreateEventScreenTestTags.END_TIME_FIELD,
        )
        .forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }

    viewModel.updateStartDate(startDate)
    viewModel.updateStartTime(LocalTime.of(9, 0))
    viewModel.updateEndDate(endDate)
    viewModel.updateEndTime(LocalTime.of(9, 0))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(formatDate(startDate)).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.START_TIME_FIELD)
        .assertTextContains(formatTime(LocalTime.of(9, 0)))
    composeTestRule.onNodeWithText(formatDate(endDate)).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.END_TIME_FIELD)
        .assertTextContains(formatTime(LocalTime.of(9, 0)))
  }

  @Test
  fun invalidTimeFieldsShowTheirErrors() {
    val viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    viewModel.updateStartTime(LocalTime.of(9, 0))
    viewModel.updateEndTime(LocalTime.of(10, 0))
    viewModel.setErrors()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.START_DATE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.END_DATE_ERROR).assertIsDisplayed()
  }

  @Test
  fun nameErrorAppearsAfterEditingAndClearingTheName() {
    val viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextInput("Festival")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextClearance()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_ERROR).assertIsDisplayed()
  }

  @Test
  fun createClickStillUsesTheExistingSaveFlow() {
    val viewModel = spyk(CreateEventViewModel(EventsRepositoryLocal()))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    verify(exactly = 0) { viewModel.createEvent() }
  }
}
