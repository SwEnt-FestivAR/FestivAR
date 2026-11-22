// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests of [OptionPickerDialog]. */
@RunWith(AndroidJUnit4::class)
class OptionPickerDialogTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val options = listOf("Team lead", "Staff", "Visitor")

  /** Shows an [OptionPickerDialog] of [options] where "Staff" is the current one. */
  private fun setOptionPicker(
      onPick: (String) -> Unit = {},
      onDismiss: () -> Unit = {},
      enabled: Boolean = true,
  ) {
    composeTestRule.setContent {
      AppTheme {
        OptionPickerDialog(
            title = "Change the role of Yasser",
            options = options,
            onPick = onPick,
            onDismiss = onDismiss,
            current = "Staff",
            currentLabel = "Current",
            enabled = enabled,
        ) {
          Text(it)
        }
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if an option is missing, or the current one is not marked. */
  @Test
  fun showsEveryOptionAndMarksTheCurrentOne() {
    setOptionPicker()

    composeTestRule.onNodeWithText("Change the role of Yasser").assertIsDisplayed()
    options.forEachIndexed { index, option ->
      composeTestRule.onNodeWithTag(OptionPickerDialogTestTags.option(index)).assertIsDisplayed()
      composeTestRule.onNodeWithText(option).assertIsDisplayed()
    }
    composeTestRule.onAllNodesWithText("Current").assertCountEquals(1)
  }

  /** Breaks if tapping an option does not pick that one. */
  @Test
  fun tappingAnOptionPicksIt() {
    var picked: String? = null
    setOptionPicker(onPick = { picked = it })

    composeTestRule.onNodeWithTag(OptionPickerDialogTestTags.option(2)).performClick()

    assertEquals("Visitor", picked)
  }

  /** Breaks if the current option can be picked again. */
  @Test
  fun theCurrentOptionCannotBePicked() {
    var picked: String? = null
    setOptionPicker(onPick = { picked = it })

    composeTestRule.onNodeWithTag(OptionPickerDialogTestTags.option(1)).performClick()

    assertNull(picked)
  }

  /** Breaks if an option can be picked while the picker is disabled. */
  @Test
  fun disabledOptionsCannotBePicked() {
    var picked: String? = null
    setOptionPicker(onPick = { picked = it }, enabled = false)

    composeTestRule.onNodeWithTag(OptionPickerDialogTestTags.option(0)).performClick()

    assertNull(picked)
  }

  /** Breaks if the close button does not call onDismiss exactly once. */
  @Test
  fun closeButtonCallsOnDismiss() {
    var dismissed = 0
    setOptionPicker(onDismiss = { dismissed++ })

    composeTestRule.onNodeWithTag(OptionPickerDialogTestTags.CLOSE_BUTTON).performClick()

    assertEquals(1, dismissed)
  }
}
