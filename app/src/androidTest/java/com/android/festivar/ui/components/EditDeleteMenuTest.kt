// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class EditDeleteMenuTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val moreActionsTag = "more"
  private val editTag = "edit"
  private val deleteTag = "delete"
  private var edits = 0
  private var deletes = 0

  private fun setMenu(canDelete: Boolean = true) {
    composeTestRule.setContent {
      AppTheme {
        EditDeleteMenu(
            moreActionsDescription = "More actions",
            moreActionsTag = moreActionsTag,
            editLabel = "Edit thing",
            editTag = editTag,
            onEdit = { edits++ },
            delete =
                if (canDelete) {
                  DeleteMenuItem("Delete thing", deleteTag) { deletes++ }
                } else {
                  null
                },
        )
      }
    }
  }

  /** Breaks if the button loses its accessibility label. */
  @Test
  fun buttonIsDescribedForScreenReaders() {
    setMenu()

    composeTestRule.onNodeWithContentDescription("More actions").assertIsDisplayed()
  }

  /** Breaks if the items show before the button is clicked or the labels are swapped. */
  @Test
  fun itemsAppearWithTheirLabelsAfterTheButtonIsClicked() {
    setMenu()
    composeTestRule.onNodeWithTag(editTag).assertDoesNotExist()

    composeTestRule.onNodeWithTag(moreActionsTag).performClick()

    composeTestRule.onNodeWithTag(editTag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(deleteTag).assertIsDisplayed()
    composeTestRule.onNodeWithText("Edit thing").assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete thing").assertIsDisplayed()
  }

  /** Breaks if a menu given no delete item still shows one. */
  @Test
  fun withoutDeleteOnlyTheEditItemAppears() {
    setMenu(canDelete = false)

    composeTestRule.onNodeWithTag(moreActionsTag).performClick()

    composeTestRule.onNodeWithTag(editTag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(deleteTag).assertDoesNotExist()
  }

  /** Breaks if the edit item calls the delete callback, or leaves the menu open. */
  @Test
  fun editCallsOnlyOnEditAndClosesTheMenu() {
    setMenu()

    composeTestRule.onNodeWithTag(moreActionsTag).performClick()
    composeTestRule.onNodeWithTag(editTag).performClick()

    assertEquals(1, edits)
    assertEquals(0, deletes)
    composeTestRule.onNodeWithTag(editTag).assertDoesNotExist()
  }

  /** Breaks if the delete item calls the edit callback, or leaves the menu open. */
  @Test
  fun deleteCallsOnlyOnDeleteAndClosesTheMenu() {
    setMenu()

    composeTestRule.onNodeWithTag(moreActionsTag).performClick()
    composeTestRule.onNodeWithTag(deleteTag).performClick()

    assertEquals(0, edits)
    assertEquals(1, deletes)
    composeTestRule.onNodeWithTag(deleteTag).assertDoesNotExist()
  }
}
