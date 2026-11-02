package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.FETCH_ERROR).assertIsDisplayed()
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.EVENT_NAME)
            .fetchSemanticsNodes()
            .isEmpty()
    )
  }
}
