package com.android.festivar.ui.event.edit

// Co-authored-by: Gemini <noreply@google.com>

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [EditEventScreen] running on Robolectric to ensure code coverage. */
@RunWith(AndroidJUnit4::class)
class EditEventScreenRobolectricTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val event =
      Event(
          eventId = "test-event",
          ownerId = "owner",
          title = "Festival",
          description = "Original notes",
          startDate =
              ZonedDateTime.of(
                  LocalDate.now().plusDays(2),
                  LocalTime.of(16, 0),
                  ZoneId.of("Europe/Zurich"),
              ),
          endDate =
              ZonedDateTime.of(
                  LocalDate.now().plusDays(3),
                  LocalTime.of(10, 0),
                  ZoneId.of("Europe/Zurich"),
              ),
          location = "Parc de Vidy",
      )

  /** Breaks if the edit event screen preview shows core elements. */
  @Test
  fun editEventScreenPreviewShowsCoreElements() {
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = EditEventUIState().load(event),
            actions = EditEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.NAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.SAVE_BUTTON).assertIsDisplayed()
  }

  /** Breaks if image viewer and delete button display when an image is present. */
  @Test
  fun editEventScreenDisplaysImageViewerWhenImageIsPresent() {
    val state = EditEventUIState().load(event.copy(imageUrl = "https://example.com/image.png"))
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = state,
            actions = EditEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_BUTTON).assertIsDisplayed()
  }

  /** Breaks if image select button displays when no image is set. */
  @Test
  fun editEventScreenDisplaysImageSelectButtonWhenNoImage() {
    val state = EditEventUIState().load(event.copy(imageUrl = ""))
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = state,
            actions = EditEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.IMAGE_SELECT_BUTTON).assertIsDisplayed()
  }

  /** Breaks if delete image confirmation dialog displays when requested. */
  @Test
  fun editEventScreenDisplaysDeleteImageConfirmationDialog() {
    val state =
        EditEventUIState()
            .load(event.copy(imageUrl = "https://example.com/image.png"))
            .copy(showDeleteImageConfirmation = true)
    composeTestRule.setContent {
      AppTheme {
        EditEventContent(
            state = state,
            actions = EditEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.DELETE_IMAGE_CONFIRM_DIALOG)
        .assertIsDisplayed()
  }
}
