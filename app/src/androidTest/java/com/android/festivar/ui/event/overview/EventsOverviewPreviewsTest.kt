package com.android.festivar.ui.event.overview

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the Events Overview previews so that preview regressions are caught by instrumented
 * tests.
 */
@RunWith(AndroidJUnit4::class)
class EventsOverviewPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun eventsOverviewContentPreview_showsEventsAndActions() {
    composeTestRule.setContent { EventsOverviewContentPreview() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.USER_AVATAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).assertIsDisplayed()
    composeTestRule.onNodeWithText("Fête de l'Asso 2026").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.EVENT_ACTIONS_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun eventsOverviewEmptyPreview_showsEmptyStateActions() {
    composeTestRule.setContent { EventsOverviewEmptyPreview() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.USER_AVATAR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(targetContext.getString(R.string.events_overview_empty_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(targetContext.getString(R.string.events_overview_empty_subtitle))
        .assertIsDisplayed()
  }
}
