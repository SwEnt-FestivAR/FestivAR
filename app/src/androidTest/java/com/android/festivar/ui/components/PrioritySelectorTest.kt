// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Priority
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrioritySelectorTest {
  @get:Rule val composeTestRule = createComposeRule()

  // The labels the user must see, written out here and not taken from the production mapping
  // (`priorityLabelRes`), so that a wrong mapping fails these tests.
  private val labels =
      mapOf(
          Priority.NONE to "None",
          Priority.LOW to "Low",
          Priority.MEDIUM to "Medium",
          Priority.HIGH to "High",
          Priority.URGENT to "Urgent",
      )

  private fun label(priority: Priority): String = labels.getValue(priority)

  private fun setSelector(selected: Priority, onSelect: (Priority) -> Unit = {}) {
    composeTestRule.setContent {
      AppTheme {
        FestivarPrioritySelector(selected = selected, onSelect = onSelect, label = "Priority")
      }
    }
  }

  @Test
  fun showsTheLabelAndEveryPriority() {
    setSelector(Priority.NONE)

    // A new priority must get its label here too.
    assertEquals(Priority.entries.toSet(), labels.keys)
    composeTestRule.onNodeWithText("PRIORITY").assertIsDisplayed()
    // The chips are in a horizontally scrolling row: the last ones can be off screen on a narrow
    // device, so each one is scrolled into view first.
    Priority.entries.forEach {
      composeTestRule.onNodeWithText(label(it)).performScrollTo().assertIsDisplayed()
    }
  }

  @Test
  fun onlyTheSelectedPriorityIsMarkedSelected() {
    setSelector(Priority.HIGH)

    // Off-screen chips are scrolled into view first (see showsTheLabelAndEveryPriority).
    Priority.entries.forEach {
      val chip = composeTestRule.onNodeWithText(label(it)).performScrollTo()
      if (it == Priority.HIGH) chip.assertIsSelected() else chip.assertIsNotSelected()
    }
  }

  @Test
  fun tappingAPriorityReportsIt() {
    var picked: Priority? = null
    setSelector(Priority.NONE) { picked = it }

    composeTestRule.onNodeWithText(label(Priority.URGENT)).performScrollTo().performClick()

    assertEquals(Priority.URGENT, picked)
  }
}
