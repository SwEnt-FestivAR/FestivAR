// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserListTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sami = User("u0", name = "Sami", surname = "Kabbaj")
  private val lea = User("u1", name = "Léa", surname = "Martin", tasksAssigned = listOf("t1"))
  private val sara = User("u2", name = "Sara")
  private val users = listOf(sami, lea, sara)

  private fun categoryOf(user: User): String? =
      when (user) {
        sami -> "Organizer"
        lea -> "Volunteers"
        else -> null
      }

  private fun setList(
      users: List<User> = this.users,
      title: String? = null,
      subtitle: String? = null,
      categoryOf: ((User) -> String?)? = null,
      supportingText: (User) -> String? = { null },
      tags: (User) -> List<UserTag> = { emptyList() },
      onUserClick: ((User) -> Unit)? = null,
  ) {
    composeTestRule.setContent {
      AppTheme {
        UserList(
            users = users,
            title = title,
            subtitle = subtitle,
            categoryOf = categoryOf,
            supportingText = supportingText,
            tags = tags,
            onUserClick = onUserClick,
        )
      }
    }
  }

  /** Test that the title, the subtitle and one header per category with its count are shown. */
  @Test
  fun showsTitleSubtitleAndCategoryHeaders() {
    setList(title = "Members", subtitle = "3 members", categoryOf = ::categoryOf)

    composeTestRule.onNodeWithTag(UserListTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("3 members").assertIsDisplayed()
    composeTestRule.onNodeWithText("ORGANIZER, 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("VOLUNTEERS, 1").assertIsDisplayed()
    // The users without category are under the default label of the shared strings file.
    val unknown =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.user_list_uncategorized)
    composeTestRule.onNodeWithText("${unknown.uppercase()}, 1").assertIsDisplayed()
  }

  /** Test that by default only the full names are shown, with no title and no header. */
  @Test
  fun byDefaultShowsOnlyTheNames() {
    setList()

    composeTestRule.onNodeWithText("Sami Kabbaj").assertIsDisplayed()
    // A user without surname has no trailing space after its name.
    composeTestRule.onNodeWithText("Sara").assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserListTestTags.TITLE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(UserListTestTags.SUBTITLE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(UserListTestTags.header("")).assertDoesNotExist()
  }

  /** Test that each user gets its own supporting text and tags, up to the maximum of tags. */
  @Test
  fun showsTheSupportingTextAndTagsOfEachUser() {
    setList(
        supportingText = { if (it == lea) "Volunteer · 1 task" else null },
        tags = {
          if (it == sami) List(MAX_TAGS_PER_USER) { i -> UserTag("Tag $i") } else emptyList()
        },
    )

    composeTestRule.onNodeWithText("Volunteer · 1 task").assertIsDisplayed()
    repeat(MAX_TAGS_PER_USER) { composeTestRule.onNodeWithText("Tag $it").assertIsDisplayed() }
  }

  /** Test that a user with more tags than the maximum is refused. */
  @Test
  fun moreTagsThanTheMaximumThrows() {
    assertThrows(IllegalArgumentException::class.java) {
      setList(tags = { List(MAX_TAGS_PER_USER + 1) { i -> UserTag("Tag $i") } })
    }
  }

  /** Test that clicking a row gives its user to onUserClick. */
  @Test
  fun clickingARowGivesItsUser() {
    val clicked = mutableListOf<User>()
    setList(onUserClick = { clicked += it })

    composeTestRule.onNodeWithTag(UserListTestTags.row(lea.uid)).performClick()

    assertEquals(listOf(lea), clicked)
  }

  /** Test that the rows are not clickable without onUserClick. */
  @Test
  fun rowsAreNotClickableWithoutOnUserClick() {
    setList()
    composeTestRule.onNodeWithTag(UserListTestTags.row(lea.uid)).assertHasNoClickAction()
  }

  /** Test that the list scrolls down to a user far below the screen. */
  @Test
  fun scrollsToTheLastUser() {
    setList(users = List(50) { User("u$it", name = "User $it") })

    composeTestRule
        .onNodeWithTag(UserListTestTags.LIST)
        .performScrollToNode(hasTestTag(UserListTestTags.row("u49")))
    composeTestRule.onNodeWithText("User 49").assertIsDisplayed()
  }

  /** Test that the sheet shows the list, forwards clicks, and reports when back closes it. */
  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun bottomSheetShowsTheListAndReportsItsDismissal() {
    val clicked = mutableListOf<User>()
    var dismissals = 0
    composeTestRule.setContent {
      AppTheme {
        UserListBottomSheet(
            users = users,
            onDismissRequest = { dismissals++ },
            title = "Members",
            onUserClick = { clicked += it },
        )
      }
    }

    composeTestRule.onNodeWithText("Members").assertIsDisplayed()
    composeTestRule.onNodeWithText("Léa Martin").performClick()
    assertEquals(listOf(lea), clicked)

    Espresso.pressBack()
    composeTestRule.waitForIdle()
    assertEquals(1, dismissals)
  }
}
