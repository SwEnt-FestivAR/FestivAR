package com.android.festivar.ui.event.edit

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>

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
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.ui.components.datetime.DateRangePickerTestTags
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.pickDays
import com.android.festivar.ui.theme.AppTheme
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditEventScreenTest {
  /** A day long over, to show that the edit screen accepts the days of an event that ended. */
  private val pastDay: LocalDate = LocalDate.of(2020, 3, 10)

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
            EditEventScreenTestTags.DATES_FIELD,
            EditEventScreenTestTags.START_TIME_FIELD,
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

  /**
   * Breaks if the edit screen hides the errors of a blank name, an end not after the start or a
   * failed save, or keeps Save enabled with them.
   */
  @Test
  fun invalidAndSavingFormsShowErrorsAndDisableSave() {
    val state =
        mutableStateOf(
            testState()
                .copy(
                    name = "",
                    nameEdited = true,
                    range = endBeforeStartRange(),
                    errorMsg = R.string.edit_event_error_save_failed,
                )
        )
    composeTestRule.setContent { AppTheme { EditEventContent(state.value, EditEventActions()) } }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.END_DATE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()

    state.value = testState().copy(isSaving = true)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVING_INDICATOR).assertIsDisplayed()
  }

  /** Breaks if the edit screen does not keep the days of an event that already ended selected. */
  @Test
  fun anEventThatEndedOpensItsDaysSelected() {
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = testState().copy(range = pastRange()),
            actions = EditEventActions(),
        )
      }
    }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DATES_FIELD).performClick()

    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertIsDisplayed()
  }

  /** Breaks if the saved days are not reported as one range that keeps the times. */
  @Test
  fun savingTheDatesReportsOneRangeWithTheSameTimes() {
    val state = testState()
    val ranges = mutableListOf<DateTimeRange>()
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(state = state, actions = EditEventActions(onRangeChange = ranges::add))
      }
    }
    val start = state.range.startDate!!.plusDays(1)
    val end = start.plusDays(2)

    composeTestRule.pickDays(EditEventScreenTestTags.DATES_FIELD, start, end)

    assertEquals(listOf(state.range.copy(startDate = start, endDate = end)), ranges)
  }

  /** Breaks if the dates picker of the edit screen refuses days that are over. */
  @Test
  fun pastDaysCanBePicked() {
    val ranges = mutableListOf<DateTimeRange>()
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = testState(),
            actions = EditEventActions(onRangeChange = ranges::add),
        )
      }
    }
    composeTestRule.pickDays(EditEventScreenTestTags.DATES_FIELD, pastDay)

    assertEquals(pastDay, ranges.single().startDate)
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
    val viewModel =
        EditEventViewModel(
            DataRepositoryLocal(
                FakeAuthRepository(),
                eventsRepository = EventsRepositoryLocal(listOf(event)),
            ),
            { event.ownerId },
        )
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
    val viewModel =
        EditEventViewModel(
            DataRepositoryLocal(
                FakeAuthRepository(),
                eventsRepository = EventsRepositoryLocal(listOf(event)),
            ),
            { event.ownerId },
        )
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

  /** Breaks if the screen stays open, or shows the form, for a user who cannot edit the event. */
  @Test
  fun screenCallsBackWhenTheUserCannotEditTheEvent() {
    val event = testState().event!!
    val viewModel =
        EditEventViewModel(
            DataRepositoryLocal(
                FakeAuthRepository(),
                eventsRepository = EventsRepositoryLocal(listOf(event)),
            ),
            { "stranger" },
        )
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

    assertEquals(1, backCount)
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD).assertDoesNotExist()
  }

  /**
   * Breaks if clicking the add image placeholder does not trigger the photo picker on edit event.
   */
  @Test
  fun addImageButtonTriggersPhotoPickerOnEditEvent() {
    val launchPhotoPicker = mockk<(String) -> Unit>(relaxed = true)
    val event = testState().event!!
    val viewModel =
        EditEventViewModel(
            DataRepositoryLocal(
                FakeAuthRepository(),
                eventsRepository = EventsRepositoryLocal(listOf(event)),
            ),
            { event.ownerId },
        )
    composeTestRule.setContent {
      AppTheme {
        EditEventScreen(
            eventId = event.eventId,
            viewModel = viewModel,
            onBack = {},
            onSaved = {},
            launchPhotoPicker = launchPhotoPicker,
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.IMAGE_SELECT_BUTTON).performClick()
    verify(exactly = 1) { launchPhotoPicker("image/*") }
  }

  /**
   * Breaks if clicking the delete image button when an image is present on edit event does not
   * display the delete confirmation dialog and handle confirmation correctly.
   */
  @Test
  fun deleteImageButtonOpensConfirmationDialogAndConfirmsDeletionOnEditEvent() {
    val event = testState().event!!.copy(imageUrl = "https://example.com/image.png")
    val viewModel =
        EditEventViewModel(
            DataRepositoryLocal(
                FakeAuthRepository(),
                eventsRepository = EventsRepositoryLocal(listOf(event)),
            ),
            { event.ownerId },
        )
    composeTestRule.setContent {
      AppTheme {
        EditEventScreen(
            eventId = event.eventId,
            viewModel = viewModel,
            onBack = {},
            onSaved = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_CONFIRM_DIALOG)
        .assertIsDisplayed()

    // Test dismiss/cancel first
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_DISMISS_BUTTON)
        .performClick()
    composeTestRule.waitForIdle()
    assertNotNull(viewModel.uiState.value.imageUrl)

    // Open dialog again and confirm deletion
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_CONFIRM_BUTTON)
        .performClick()
    composeTestRule.waitForIdle()

    assertNull(viewModel.uiState.value.imageUrl)
  }

  /** A range that ended long ago, which the edit screen must still open with its days selected. */
  private fun pastRange(): DateTimeRange {
    return DateTimeRange(pastDay, LocalTime.of(9, 0), pastDay, LocalTime.of(10, 0))
  }

  /** A range whose end is before its start, which the edit screen reports under the times. */
  private fun endBeforeStartRange(): DateTimeRange {
    val day = LocalDate.now().plusWeeks(1)
    return DateTimeRange(day, LocalTime.of(17, 0), day, LocalTime.of(9, 0))
  }

  private fun testState(): EditEventUIState {
    val zone = ZoneId.of("Europe/Zurich")
    val startDate = LocalDate.now().plusDays(5)
    val endDate = startDate.plusDays(2)
    val event =
        Event(
            eventId = "test-event",
            ownerId = "owner",
            title = "Festival",
            description = "Original notes",
            startDate = ZonedDateTime.of(startDate, LocalTime.of(16, 0), zone),
            endDate = ZonedDateTime.of(endDate, LocalTime.of(10, 0), zone),
            location = "Parc de Vidy",
        )
    return EditEventUIState(
        event = event,
        name = event.title,
        range = DateTimeRange(startDate, LocalTime.of(16, 0), endDate, LocalTime.of(10, 0)),
        venue = event.location,
        notes = "Updated notes",
        zone = zone,
        endZone = zone,
    )
  }
}
