// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TITLE = "Not on the map yet"
private const val SUBTITLE = "Volunteers find a pinned task faster."
private const val ACTION = "Pin it"

@RunWith(AndroidJUnit4::class)
class EmptyStateCardTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setCard(onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    composeTestRule.setContent {
      AppTheme {
        EmptyStateCard(
            title = TITLE,
            subtitle = SUBTITLE,
            actionLabel = ACTION,
            onAction = onAction,
            modifier = modifier,
        )
      }
    }
  }

  /** Breaks if the card does not show the title and the subtitle it is given. */
  @Test
  fun card_showsItsTitleAndSubtitle() {
    setCard()

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.TITLE).assertTextEquals(TITLE)
    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.SUBTITLE).assertTextEquals(SUBTITLE)
  }

  /** Breaks if the card does not show the label it is given on its button. */
  @Test
  fun card_showsItsActionLabel() {
    setCard()

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.ACTION).assertTextEquals(ACTION)
  }

  /** Breaks if clicking the button of the card does not call the given callback. */
  @Test
  fun card_clickingTheAction_callsTheCallback() {
    var clicks = 0
    setCard(onAction = { clicks++ })

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.ACTION).performClick()

    assertEquals(1, clicks)
  }

  /** Breaks if the card does not display, or needs a modifier from its caller. */
  @Test
  fun card_withoutModifier_isDisplayed() {
    setCard()

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.CARD).assertIsDisplayed()
  }

  /** Breaks if the card, or its button, is not on screen when it fills the width. */
  @Test
  fun card_fillingTheWidth_showsCardAndAction() {
    setCard(modifier = Modifier.fillMaxWidth())

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.ACTION).assertIsDisplayed()
  }

  /** Breaks if the preview of the card fails to compose or to display. */
  @Test
  fun preview_isDisplayed() {
    composeTestRule.setContent { EmptyStateCardPreview() }

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.CARD).assertIsDisplayed()
  }
}
