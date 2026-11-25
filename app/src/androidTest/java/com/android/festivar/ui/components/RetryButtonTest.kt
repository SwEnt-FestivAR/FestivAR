// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetryButtonTest {
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Breaks if the button loses its tag or its "Retry" text, or if a tap does not call onClick
   * exactly once.
   */
  @Test
  fun showsRetryAndATapCallsOnClickOnce() {
    var clicks = 0
    composeTestRule.setContent { AppTheme { RetryButton(onClick = { clicks++ }) } }

    composeTestRule
        .onNodeWithTag(RetryButtonTestTags.BUTTON)
        .assertIsDisplayed()
        .assertTextEquals(
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getString(R.string.action_retry)
        )
        .performClick()

    assertEquals(1, clicks)
  }
}
