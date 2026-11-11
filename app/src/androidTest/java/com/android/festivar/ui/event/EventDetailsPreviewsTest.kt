package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.ui.event.details.EventDetailsScreenPreview
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.details.FetchErrorPreview
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the Event Details previews so that preview regressions are caught by instrumented tests.
 */
@RunWith(AndroidJUnit4::class)
class EventDetailsPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun eventDetailsScreenPreview_showsEventInformation() {
    composeTestRule.setContent { EventDetailsScreenPreview() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithText("Fête de l'Asso 2026").assertIsDisplayed()
    composeTestRule.onNodeWithText("Parc de Vidy, Lausanne").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).assertIsDisplayed()
  }

  @Test
  fun fetchErrorPreview_showsFetchError() {
    composeTestRule.setContent { FetchErrorPreview() }
    composeTestRule.waitForIdle()

    val errorMessage =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.event_details_fetch_error)
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.FETCH_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(errorMessage)
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.EVENT_NAME)
            .fetchSemanticsNodes()
            .isEmpty()
    )
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON)
            .fetchSemanticsNodes()
            .isEmpty()
    )
  }
}
