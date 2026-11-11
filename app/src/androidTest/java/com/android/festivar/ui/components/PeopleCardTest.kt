// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeopleCardTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val cardTag = "card"
  private val sara = User("u1", name = "Sara", surname = "Keller")
  private val liam = User("u2", name = "Liam", surname = "Meyer")
  private val nora = User("u3", name = "Nora", surname = "Favre")
  private val omar = User("u4", name = "Omar", surname = "Rochat")

  private fun setContent(people: List<User>, onClick: () -> Unit = {}) {
    composeTestRule.setContent {
      AppTheme { PeopleCard("Members", people, onClick, Modifier.testTag(cardTag)) }
    }
  }

  private fun count() = composeTestRule.onNodeWithTag(PeopleCardTestTags.COUNT, true)

  // Breaks if the card stops showing the title it is given, or the avatar of its one person, or
  // counts a single person in the plural.
  @Test
  fun onePersonShowsTheTitleTheAvatarAndTheSingular() {
    setContent(listOf(sara))

    composeTestRule.onNodeWithText("Members").assertIsDisplayed()
    composeTestRule.onNodeWithText("SK").assertIsDisplayed()
    count().assertTextEquals("1 person")
    composeTestRule.onNodeWithTag(PeopleCardTestTags.MORE, true).assertDoesNotExist()
  }

  // Breaks if two people get a "+N" bubble, which is only for the ones the stack cannot show.
  @Test
  fun twoPeopleShowBothAvatarsWithoutTheMoreBubble() {
    setContent(listOf(sara, liam))

    composeTestRule.onNodeWithText("SK").assertIsDisplayed()
    composeTestRule.onNodeWithText("LM").assertIsDisplayed()
    count().assertTextEquals("2 people")
    composeTestRule.onNodeWithTag(PeopleCardTestTags.MORE, true).assertDoesNotExist()
  }

  // Breaks if the stack draws more than two avatars, or the bubble miscounts the others.
  @Test
  fun fourPeopleShowTwoAvatarsAndTheOthersAsACount() {
    setContent(listOf(sara, liam, nora, omar))

    composeTestRule.onNodeWithText("SK").assertIsDisplayed()
    composeTestRule.onNodeWithText("LM").assertIsDisplayed()
    composeTestRule.onNodeWithText("NF").assertDoesNotExist()
    composeTestRule.onNodeWithTag(PeopleCardTestTags.MORE, true).assertTextEquals("+2")
    count().assertTextEquals("4 people")
  }

  // Breaks if a tap on the card no longer reaches the caller.
  @Test
  fun aTapCallsBack() {
    var taps = 0
    setContent(listOf(sara)) { taps++ }

    composeTestRule.onNodeWithTag(cardTag).performClick()

    assertEquals(1, taps)
  }
}
