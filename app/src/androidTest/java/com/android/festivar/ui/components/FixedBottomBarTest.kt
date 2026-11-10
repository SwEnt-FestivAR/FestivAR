// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FixedBottomBarTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the bar no longer shows every line placed in it. */
  @Test
  fun showsItsContent() {
    composeTestRule.setContent {
      AppTheme {
        FixedBottomBar {
          Text("first line")
          Text("second line")
        }
      }
    }

    composeTestRule.onNodeWithText("first line").assertIsDisplayed()
    composeTestRule.onNodeWithText("second line").assertIsDisplayed()
  }

  /**
   * Breaks if the bar leaves the bottom of the page, or if its content is not kept above the system
   * navigation bar. The navigation bar is simulated with 48dp of insets, so the test does not
   * depend on the navigation mode of the device or on the padding of the bar.
   */
  @Test
  fun staysAtTheBottomAboveTheNavigationBar() {
    val navigationBar = 48.dp
    composeTestRule.setContent {
      AppTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
          FixedBottomBar(Modifier.testTag("bar"), WindowInsets(bottom = navigationBar)) {
            Text("last line", Modifier.testTag("line"))
          }
        }
      }
    }

    val pageBottom = composeTestRule.onRoot().getUnclippedBoundsInRoot().bottom
    val barBottom = composeTestRule.onNodeWithTag("bar").getUnclippedBoundsInRoot().bottom
    val lineBottom = composeTestRule.onNodeWithTag("line").getUnclippedBoundsInRoot().bottom
    assertEquals(
        "the bar must end at the bottom of the page",
        pageBottom.value,
        barBottom.value,
        1f,
    )
    assertTrue(
        "the content must end above the navigation bar",
        lineBottom <= pageBottom - navigationBar,
    )
  }

  /**
   * Breaks if the preview no longer compiles or draws, which Android Studio alone would not catch.
   */
  @Test
  fun preview_showsTheBar() {
    composeTestRule.setContent { FixedBottomBarPreview() }

    composeTestRule.onNodeWithText("First line of the bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second line of the bar").assertIsDisplayed()
  }
}
