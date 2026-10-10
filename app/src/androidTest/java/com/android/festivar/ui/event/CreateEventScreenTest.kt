package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.EventsRepositoryLocal
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var viewModel: CreateEventViewModel

  @Before
  fun setUp() {
    viewModel = spyk(CreateEventViewModel(EventsRepositoryLocal()))
    composeTestRule.setContent { CreateEventScreen(viewModel) }
  }

  @Test
  fun nonErrorTaggedElementsAreDisplayed() {
    listOf(
            CreateEventScreenTestTags.NAVIGATION_BUTTON,
            CreateEventScreenTestTags.TITLE,
            CreateEventScreenTestTags.NAME_FIELD,
            CreateEventScreenTestTags.START_DATE_FIELD,
            CreateEventScreenTestTags.END_DATE_FIELD,
            CreateEventScreenTestTags.LOCATION_FIELD,
            CreateEventScreenTestTags.DESCRIPTION_FIELD,
            CreateEventScreenTestTags.CREATE_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }
  }

  @Test
  fun errorComponentsAreDisplayedWhenCreateButtonIsClickedWithInvalidFields() {
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.ENDS_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.ADDRESS_ERROR).assertIsDisplayed()
  }

  @Test
  fun createButtonDoesNotCallCreateEventWhenFieldsAreInvalid() {
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    verify(exactly = 0) { viewModel.createEvent() }
  }

  @Test
  fun fieldsReceiveInput() {
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .performTextInput("Summer festival")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
        .performTextInput("Main square")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DESCRIPTION_FIELD)
        .performTextInput("Bring the stage equipment")

    composeTestRule.onNodeWithText("Summer festival").assertIsDisplayed()
    composeTestRule.onNodeWithText("Main square").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bring the stage equipment").assertIsDisplayed()

    selectDate(CreateEventScreenTestTags.START_DATE_FIELD, LocalDate.now())
    selectDate(CreateEventScreenTestTags.END_DATE_FIELD, LocalDate.now().plusDays(1))

    val expectedStartDate =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(LocalDate.now())
    val expectedEndDate =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(LocalDate.now().plusDays(1))
    composeTestRule.onNodeWithText(expectedStartDate).assertIsDisplayed()
    composeTestRule.onNodeWithText(expectedEndDate).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun createButtonCallsCreateEventWhenValid() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(ZonedDateTime.parse("2026-10-08T10:00:00+02:00"))
    viewModel.updateEndDate(ZonedDateTime.parse("2026-10-08T18:00:00+02:00"))
    viewModel.updateVenue("Main square")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    verify(exactly = 1) { viewModel.createEvent() }
  }

  private fun selectDate(tag: String, date: LocalDate) {
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    if (date == LocalDate.now()) {
      composeTestRule.onNodeWithText("Today", substring = true).performClick()
    } else {
      val dateDescription =
          DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
              .withLocale(Locale.getDefault())
              .format(date)
      composeTestRule.onNodeWithText(dateDescription, substring = true).performClick()
    }
    composeTestRule.onNodeWithText("OK").performClick()
  }
}
