package com.android.festivar.ui.event.edit

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.ui.components.DateTimeField
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

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun previewShowsTheFilledForm() {
    composeTestRule.setContent { EditEventScreenPreview() }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD).assertIsDisplayed()
  }

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

  @Test
  fun loadingAndFetchFailureStatesAreShown() {
    val state = mutableStateOf(EditEventUIState(isLoading = true))
    var retryCount = 0
    composeTestRule.setContent {
      AppTheme { EditEventContent(state.value, EditEventActions(onRetry = { retryCount++ })) }
    }
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.FETCH_ERROR).assertDoesNotExist()

    state.value = EditEventUIState(errorFetchMsg = R.string.edit_event_error_fetch_failed)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.FETCH_ERROR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.FETCH_RETRY)
        .assertIsDisplayed()
        .performClick()
    assertEquals(1, retryCount)
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).assertDoesNotExist()

    state.value = EditEventUIState()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.FETCH_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.FETCH_RETRY).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun invalidAndSavingFormsShowErrorsAndDisableSave() {
    val state =
        mutableStateOf(
            testState()
                .copy(
                    name = "",
                    nameEdited = true,
                    startDate = null,
                    endDate = null,
                    startTime = LocalTime.NOON,
                    endTime = LocalTime.NOON,
                    errorMsg = R.string.edit_event_error_save_failed,
                )
        )
    composeTestRule.setContent { AppTheme { EditEventContent(state.value, EditEventActions()) } }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.START_DATE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.END_DATE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()

    state.value = testState().copy(isSaving = true)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun dateAndTimeFieldsDispatchPickerRequests() {
    val requests = mutableListOf<DateTimeField?>()
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = testState(),
            actions = EditEventActions(onShowPicker = requests::add),
        )
      }
    }

    listOf(
            EditEventScreenTestTags.START_DATE_FIELD to DateTimeField.START_DATE,
            EditEventScreenTestTags.START_TIME_FIELD to DateTimeField.START_TIME,
            EditEventScreenTestTags.END_DATE_FIELD to DateTimeField.END_DATE,
            EditEventScreenTestTags.END_TIME_FIELD to DateTimeField.END_TIME,
        )
        .forEach { (tag, field) -> composeTestRule.onNodeWithTag(tag).performClick() }

    assertEquals(
        listOf(
            DateTimeField.START_DATE,
            DateTimeField.START_TIME,
            DateTimeField.END_DATE,
            DateTimeField.END_TIME,
        ),
        requests,
    )
  }

  @Test
  fun discardConfirmationCallsConfirmAction() {
    var confirmCount = 0
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = testState().copy(showDiscardConfirmation = true),
            actions = EditEventActions(onConfirmDiscard = { confirmCount++ }),
        )
      }
    }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DISCARD_CONFIRM).performClick()
    assertEquals(1, confirmCount)
  }

  @Test
  fun screenCallsBackWhenDiscardIsConfirmed() {
    val event = testState().event!!
    val viewModel = EditEventViewModel(EventsRepositoryLocal(listOf(event)), { "user" })
    var backCount = 0
    composeTestRule.setContent {
      AppTheme {
        EditEventScreen(
            eventId = event.eventId,
            viewModel = viewModel,
            onBack = { backCount++ },
            onSaved = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.NAME_FIELD)
        .performTextInput("Changed festival")
    composeTestRule.runOnUiThread {
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DISCARD_CONFIRM).assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DISCARD_CONFIRM).performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, backCount)
  }

  @Test
  fun screenLoadsEventAndCallsSavedCallbackAfterSave() {
    val event = testState().event!!
    val viewModel = EditEventViewModel(EventsRepositoryLocal(listOf(event)), { "user" })
    var savedCount = 0
    var backCount = 0
    composeTestRule.setContent {
      AppTheme {
        EditEventScreen(
            eventId = event.eventId,
            viewModel = viewModel,
            onBack = { backCount++ },
            onSaved = { savedCount++ },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.NAME_FIELD)
        .performTextInput("Updated festival")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.LOCATION_FIELD)
        .performTextInput(" entrance")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.DESCRIPTION_FIELD)
        .performTextInput(" details")
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, savedCount)
    assertEquals(0, backCount)
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
