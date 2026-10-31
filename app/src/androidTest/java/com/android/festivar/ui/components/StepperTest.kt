// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class StepperTest {
  @get:Rule val composeTestRule = createComposeRule()

  private var decreased = 0

  /** A stepper that keeps its own value, like a screen would, starting at [start]. */
  private fun setStepper(start: Int, min: Int = 1) {
    composeTestRule.setContent {
      AppTheme {
        var value by remember { mutableIntStateOf(start) }
        FestivarStepper(
            value = value,
            label = "People needed",
            onDecrease = {
              decreased++
              value--
            },
            onIncrease = { value++ },
            decreaseDescription = "Fewer",
            increaseDescription = "More",
            minValue = min,
            modifier = Modifier.testTag(STEPPER),
            decreaseModifier = Modifier.testTag(MINUS),
            increaseModifier = Modifier.testTag(PLUS),
        )
      }
    }
  }

  private fun checkValueIs(value: Int) {
    composeTestRule
        .onNodeWithTag(STEPPER)
        .assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value.toString())
        )
    composeTestRule.onNodeWithText(value.toString()).assertIsDisplayed()
  }

  @Test
  fun showsTheLabelAndTheValue() {
    setStepper(start = 3)

    composeTestRule.onNodeWithText("People needed").assertIsDisplayed()
    checkValueIs(3)
  }

  @Test
  fun plusAddsOneEachTime() {
    setStepper(start = 1)

    composeTestRule.onNodeWithTag(PLUS).performClick()
    checkValueIs(2)
    composeTestRule.onNodeWithTag(PLUS).performClick()
    checkValueIs(3)
  }

  @Test
  fun minusRemovesOneEachTime() {
    setStepper(start = 3)

    composeTestRule.onNodeWithTag(MINUS).performClick()
    checkValueIs(2)
    composeTestRule.onNodeWithTag(MINUS).performClick()
    checkValueIs(1)
  }

  @Test
  fun minusIsDisabledAtTheMinimumAndDoesNothing() {
    setStepper(start = 1)

    composeTestRule.onNodeWithTag(MINUS).assertIsNotEnabled().performClick()

    assertEquals(0, decreased)
    checkValueIs(1)
    composeTestRule.onNodeWithTag(PLUS).assertIsEnabled()
  }

  @Test
  fun minusIsEnabledAboveTheMinimumAndDisabledAgainBackAtIt() {
    setStepper(start = 1)

    composeTestRule.onNodeWithTag(PLUS).performClick()
    composeTestRule.onNodeWithTag(MINUS).assertIsEnabled()

    composeTestRule.onNodeWithTag(MINUS).performClick()
    composeTestRule.onNodeWithTag(MINUS).assertIsNotEnabled()
  }

  @Test
  fun theMinimumCanBeChosen() {
    setStepper(start = 5, min = 5)

    composeTestRule.onNodeWithTag(MINUS).assertIsNotEnabled()
  }

  private companion object {
    const val STEPPER = "stepper"
    const val MINUS = "stepperMinus"
    const val PLUS = "stepperPlus"
  }
}
