// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.model.task.Priority
import com.android.festivar.ui.task.priorityLabelRes
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Behaviour of the generic chip selector, shown here with the task priorities. Which label names
 * which priority is checked in `TaskPriorityUiTest`.
 */
@RunWith(AndroidJUnit4::class)
class ChipSelectorTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun label(priority: Priority): String =
      InstrumentationRegistry.getInstrumentation()
          .targetContext
          .getString(priorityLabelRes(priority))

  private fun setSelector(
      selected: Priority,
      enabled: Boolean = true,
      onSelect: (Priority) -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        FestivarChipSelector(
            options = Priority.entries,
            selected = selected,
            onSelect = onSelect,
            label = "Priority",
            optionLabelRes = ::priorityLabelRes,
            enabled = enabled,
        )
      }
    }
  }

  /** Breaks if the title is missing or if an option gets no chip. */
  @Test
  fun showsTheLabelAndEveryOption() {
    setSelector(Priority.NONE)

    composeTestRule.onNodeWithText("PRIORITY").assertIsDisplayed()
    // The chips are in a horizontally scrolling row: the last ones can be off screen on a narrow
    // device, so each one is scrolled into view first.
    Priority.entries.forEach {
      composeTestRule.onNodeWithText(label(it)).performScrollTo().assertIsDisplayed()
    }
  }

  /** Breaks if another chip than the selected option's, or more than one, is marked selected. */
  @Test
  fun onlyTheSelectedOptionIsMarkedSelected() {
    setSelector(Priority.HIGH)

    // Off-screen chips are scrolled into view first (see showsTheLabelAndEveryOption).
    Priority.entries.forEach {
      val chip = composeTestRule.onNodeWithText(label(it)).performScrollTo()
      if (it == Priority.HIGH) chip.assertIsSelected() else chip.assertIsNotSelected()
    }
  }

  /** Breaks if a tap reports another option than the one whose chip was tapped. */
  @Test
  fun tappingAnOptionReportsIt() {
    var picked: Priority? = null
    setSelector(Priority.NONE) { picked = it }

    composeTestRule.onNodeWithText(label(Priority.URGENT)).performScrollTo().performClick()

    assertEquals(Priority.URGENT, picked)
  }

  /** Breaks if the chips still react to taps when the selector is disabled. */
  @Test
  fun aDisabledSelectorIgnoresTaps() {
    var picked: Priority? = null
    setSelector(Priority.NONE, enabled = false) { picked = it }

    composeTestRule
        .onNodeWithText(label(Priority.URGENT))
        .performScrollTo()
        .assertIsNotEnabled()
        .performClick()

    assertNull(picked)
  }
}
