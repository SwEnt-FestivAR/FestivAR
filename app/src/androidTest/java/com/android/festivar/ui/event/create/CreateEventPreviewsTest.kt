package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the Event Details previews so that preview regressions are caught by instrumented tests.
 */
@RunWith(AndroidJUnit4::class)
class CreateEventPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun createEventScreenPreview_ShowsAllContent() {
    composeTestRule.setContent { CreateEventScreenPreview() }
    composeTestRule.waitForIdle()

    listOf(
            CreateEventScreenTestTags.NAVIGATION_BUTTON,
            CreateEventScreenTestTags.TITLE,
            CreateEventScreenTestTags.NAME_FIELD,
            CreateEventScreenTestTags.START_DATE_FIELD,
            CreateEventScreenTestTags.START_TIME_FIELD,
            CreateEventScreenTestTags.END_DATE_FIELD,
            CreateEventScreenTestTags.END_TIME_FIELD,
            CreateEventScreenTestTags.LOCATION_FIELD,
            CreateEventScreenTestTags.DESCRIPTION_FIELD,
            CreateEventScreenTestTags.CREATE_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }
  }
}
