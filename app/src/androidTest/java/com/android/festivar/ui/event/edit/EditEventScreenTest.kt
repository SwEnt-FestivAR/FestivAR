package com.android.festivar.ui.event.edit

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun changedEventRendersFormAndSavesOnButtonClick() {
    var saveCount = 0
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = testState(),
            actions = EditEventActions(onSave = { saveCount++ }),
        )
      }
    }

    listOf(
            EditEventScreenTestTags.BACK_BUTTON,
            EditEventScreenTestTags.TITLE,
            EditEventScreenTestTags.NAME_FIELD,
            EditEventScreenTestTags.START_DATE_FIELD,
            EditEventScreenTestTags.START_TIME_FIELD,
            EditEventScreenTestTags.END_DATE_FIELD,
            EditEventScreenTestTags.END_TIME_FIELD,
            EditEventScreenTestTags.LOCATION_FIELD,
            EditEventScreenTestTags.DESCRIPTION_FIELD,
            EditEventScreenTestTags.SAVE_BUTTON,
        )
        .forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON)
        .assertIsEnabled()
        .performClick()
    assertEquals(1, saveCount)
  }

  @Test
  fun discardDialogButtonsCallTheirActions() {
    var confirmCount = 0
    var cancelCount = 0
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = testState().copy(showDiscardConfirmation = true),
            actions =
                EditEventActions(
                    onConfirmDiscard = { confirmCount++ },
                    onCancelDiscard = { cancelCount++ },
                ),
        )
      }
    }

    composeTestRule.onNodeWithText("Discard event edits?").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DISCARD_KEEP_EDITING).performClick()
    assertEquals(0, confirmCount)
    assertEquals(1, cancelCount)
  }

  private fun testState(): EditEventUIState {
    val zone = ZoneId.of("Europe/Zurich")
    val startDate = LocalDate.now().plusDays(5)
    val endDate = startDate.plusDays(2)
    val event =
        Event(
            eventId = "test-event",
            title = "Festival",
            description = "Original notes",
            startDate = ZonedDateTime.of(startDate, LocalTime.of(16, 0), zone),
            endDate = ZonedDateTime.of(endDate, LocalTime.of(10, 0), zone),
            location = "Parc de Vidy",
        )
    return EditEventUIState(
        event = event,
        name = event.title,
        startDate = startDate,
        endDate = endDate,
        startTime = LocalTime.of(16, 0),
        endTime = LocalTime.of(10, 0),
        venue = event.location,
        notes = "Updated notes",
        zone = zone,
        endZone = zone,
    )
  }
}
