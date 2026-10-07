// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterChipTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setChip(selected: Boolean, onClick: () -> Unit = {}) {
    composeTestRule.setContent {
      AppTheme {
        FilterChip(
            label = "Upcoming",
            count = 3,
            selected = selected,
            onClick = onClick,
            modifier = Modifier.testTag("chip"),
        )
      }
    }
  }

  @Test
  fun showsLabelAndCountWhenSelected() {
    setChip(selected = true)
    composeTestRule.onNodeWithText("Upcoming").assertIsDisplayed()
    composeTestRule.onNodeWithText("3").assertIsDisplayed()
  }

  @Test
  fun showsLabelAndCountWhenNotSelected() {
    setChip(selected = false)
    composeTestRule.onNodeWithText("Upcoming").assertIsDisplayed()
    composeTestRule.onNodeWithText("3").assertIsDisplayed()
  }

  @Test
  fun clickCallsOnClick() {
    var clicks = 0
    setChip(selected = false, onClick = { clicks++ })
    composeTestRule.onNodeWithTag("chip").performClick()
    assertEquals(1, clicks)
  }
}
