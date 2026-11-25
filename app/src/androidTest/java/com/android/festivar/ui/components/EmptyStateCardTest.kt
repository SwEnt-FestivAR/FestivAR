// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

  /** The arguments of the card, as states, so that a test can change them one at a time. */
  private class Arguments {
    var title by mutableStateOf(TITLE)
    var subtitle by mutableStateOf(SUBTITLE)
    var actionLabel by mutableStateOf(ACTION)
    var onAction by mutableStateOf({})
    var modifier by mutableStateOf<Modifier>(Modifier)
    var tick by mutableIntStateOf(0)
  }

  private fun setCard(arguments: Arguments) {
    composeTestRule.setContent {
      AppTheme {
        Column {
          // Read only to recompose this scope without changing what the card is given.
          Text(arguments.tick.toString())
          EmptyStateCard(
              title = arguments.title,
              subtitle = arguments.subtitle,
              actionLabel = arguments.actionLabel,
              onAction = arguments.onAction,
              modifier = arguments.modifier,
          )
        }
      }
    }
  }

  /** Breaks if the card keeps showing the old title when only the title changes. */
  @Test
  fun card_whenTheTitleChanges_showsTheNewTitle() {
    val arguments = Arguments()
    setCard(arguments)

    composeTestRule.runOnIdle { arguments.title = "Not pinned yet" }

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.TITLE).assertTextEquals("Not pinned yet")
  }

  /** Breaks if the card keeps showing the old subtitle when only the subtitle changes. */
  @Test
  fun card_whenTheSubtitleChanges_showsTheNewSubtitle() {
    val arguments = Arguments()
    setCard(arguments)

    composeTestRule.runOnIdle { arguments.subtitle = "It is easier to find." }

    composeTestRule
        .onNodeWithTag(EmptyStateCardTestTags.SUBTITLE)
        .assertTextEquals("It is easier to find.")
  }

  /** Breaks if the card keeps showing the old label when only the label changes. */
  @Test
  fun card_whenTheActionLabelChanges_showsTheNewLabel() {
    val arguments = Arguments()
    setCard(arguments)

    composeTestRule.runOnIdle { arguments.actionLabel = "Pin now" }

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.ACTION).assertTextEquals("Pin now")
  }

  /** Breaks if the button still calls the old callback after the callback changes. */
  @Test
  fun card_whenTheCallbackChanges_clickingCallsTheNewOne() {
    val arguments = Arguments()
    var oldClicks = 0
    var newClicks = 0
    arguments.onAction = { oldClicks++ }
    setCard(arguments)

    composeTestRule.runOnIdle { arguments.onAction = { newClicks++ } }
    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.ACTION).performClick()

    assertEquals(0, oldClicks)
    assertEquals(1, newClicks)
  }

  /** Breaks if the card disappears or crashes when only its modifier changes. */
  @Test
  fun card_whenTheModifierChanges_staysDisplayed() {
    val arguments = Arguments()
    setCard(arguments)

    composeTestRule.runOnIdle { arguments.modifier = Modifier.fillMaxWidth() }

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.CARD).assertIsDisplayed()
  }

  /** Breaks if the card loses its texts when its parent recomposes with the same arguments. */
  @Test
  fun card_whenItsParentRecomposesWithTheSameArguments_keepsItsTexts() {
    val arguments = Arguments()
    setCard(arguments)

    composeTestRule.runOnIdle { arguments.tick++ }

    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.TITLE).assertTextEquals(TITLE)
    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.SUBTITLE).assertTextEquals(SUBTITLE)
    composeTestRule.onNodeWithTag(EmptyStateCardTestTags.ACTION).assertTextEquals(ACTION)
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
