// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val avatarTag = "avatar"

  private val sara = User("u1", name = "Sara", surname = "Keller")

  private fun setContent(
      user: User,
      modifier: Modifier = Modifier,
      onClick: (() -> Unit)? = null,
  ) {
    composeTestRule.setContent {
      AppTheme { Avatar(user = user, modifier = modifier, onClick = onClick) }
    }
  }

  /** Test that the avatar shows the first letter of the name and of the surname. */
  @Test
  fun showsInitialsOfNameAndSurname() {
    setContent(User("u1", name = "Sara", surname = "Keller"))
    composeTestRule.onNodeWithText("SK").assertIsDisplayed()
  }

  /** Test that the avatar shows the first two letters of the name if there is no surname. */
  @Test
  fun showsFirstTwoLettersOfNameWithoutSurname() {
    setContent(User("u1", name = "Sara"))
    composeTestRule.onNodeWithText("SA").assertIsDisplayed()
  }

  /** Test that the avatar shows the first two letters of the surname if there is no name. */
  @Test
  fun showsFirstTwoLettersOfSurnameWithoutName() {
    setContent(User("u1", surname = "Keller"))
    composeTestRule.onNodeWithText("KE").assertIsDisplayed()
  }

  /** Test that the avatar shows a person icon if the user has no name and no surname. */
  @Test
  fun showsPersonIconWithoutNameAndSurname() {
    setContent(User("u1", name = " ", surname = ""))
    composeTestRule
        .onNodeWithTag(AvatarTestTags.PERSON_ICON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /** Test that clicking on the avatar calls onClick. */
  @Test
  fun clickingCallsOnClick() {
    var clicks = 0
    setContent(User("u1", name = "Sara", surname = "Keller"), onClick = { clicks++ })

    composeTestRule.onNodeWithText("SK").performClick()

    assertEquals(1, clicks)
  }

  /** Test that a modifier passed to the avatar is applied to it. */
  @Test
  fun appliesModifier() {
    setContent(sara, modifier = Modifier.testTag(avatarTag))
    composeTestRule.onNodeWithTag(avatarTag).assertIsDisplayed()
  }

  /** Test that the avatar is 32.dp large if the modifier has no size. */
  @Test
  fun hasDefaultSizeWithoutSizeInModifier() {
    setContent(sara, modifier = Modifier.testTag(avatarTag))
    composeTestRule
        .onNodeWithTag(avatarTag)
        .assertWidthIsEqualTo(32.dp)
        .assertHeightIsEqualTo(32.dp)
  }

  /** Test that a size passed in the modifier replaces the default size of the avatar. */
  @Test
  fun sizeInModifierReplacesDefaultSize() {
    setContent(sara, modifier = Modifier.testTag(avatarTag).size(80.dp))
    composeTestRule
        .onNodeWithTag(avatarTag)
        .assertWidthIsEqualTo(80.dp)
        .assertHeightIsEqualTo(80.dp)
  }

  /** Test that the avatar is still clickable when a modifier is passed with onClick. */
  @Test
  fun clickingWithModifierCallsOnClick() {
    var clicks = 0
    setContent(sara, modifier = Modifier.testTag(avatarTag), onClick = { clicks++ })

    composeTestRule.onNodeWithTag(avatarTag).performClick()

    assertEquals(1, clicks)
  }
}
