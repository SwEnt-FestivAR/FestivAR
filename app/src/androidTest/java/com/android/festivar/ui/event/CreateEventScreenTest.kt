package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.EventsRepositoryLocal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel) }
  }

  @Test
  fun taggedElementsAreDisplayed() {
    listOf(
            CreateEventScreenTestTags.NAVIGATION_BUTTON,
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

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .assertTextEquals("Summer festival")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
        .assertTextEquals("Main square")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DESCRIPTION_FIELD)
        .assertTextEquals("Bring the stage equipment")

    /**
     * TODO: select the current day for the start date and the next day for the end date
     * TODO: and verify that the start date and end date fields have the right values
     */
    selectDate(CreateEventScreenTestTags.START_DATE_FIELD)
    selectDate(CreateEventScreenTestTags.END_DATE_FIELD)

    val expectedDate =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
    composeTestRule.onAllNodesWithText(expectedDate).assertCountEquals(2)

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun createButtonIsDisabledUntilRequiredFieldsAreFilled() {
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    fillRequiredFields()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createButtonIsDisabledWhenStartDateIsEmpty() {
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .performTextInput("Summer festival")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
        .performTextInput("Main square")
    selectDate(CreateEventScreenTestTags.END_DATE_FIELD)

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createButtonIsDisabledWhenEndDateIsEmpty() {
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .performTextInput("Summer festival")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
        .performTextInput("Main square")
    selectDate(CreateEventScreenTestTags.START_DATE_FIELD)

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createButtonIsDisabledWhenLocationIsEmpty() {
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .performTextInput("Summer festival")
    selectDate(CreateEventScreenTestTags.START_DATE_FIELD)
    selectDate(CreateEventScreenTestTags.END_DATE_FIELD)

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  private fun fillRequiredFields() {
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .performTextInput("Summer festival")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
        .performTextInput("Main square")
    selectDate(CreateEventScreenTestTags.START_DATE_FIELD)
    selectDate(CreateEventScreenTestTags.END_DATE_FIELD)
  }

  private fun selectDate(tag: String) {
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    composeTestRule.onNodeWithText("Today", substring = true).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }
}
