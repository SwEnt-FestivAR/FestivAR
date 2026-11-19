// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Unit tests of [MemberDialog]. */
@RunWith(AndroidJUnit4::class)
class MemberDialogTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val yasser = User("u1", name = "Yasser", surname = "Ameur")

  /** Shows a [MemberDialog] of [yasser] with a "Staff" badge, "3 tasks" and the [actions]. */
  private fun setMemberDialog(
      actions: List<MemberDialogAction> = emptyList(),
      onDismiss: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        MemberDialog(
            user = yasser,
            onDismiss = onDismiss,
            badge = { Text("Staff") },
            supportingText = "3 tasks",
            actions = actions,
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if the dialog does not show the name, the badge and the supporting text. */
  @Test
  fun showsTheMemberWithTheBadgeAndTheSupportingText() {
    setMemberDialog()

    composeTestRule.onNodeWithTag(MemberDialogTestTags.DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Yasser Ameur").assertIsDisplayed()
    composeTestRule.onNodeWithText("Staff").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 tasks").assertIsDisplayed()
  }

  /** Breaks if the close button does not call onDismiss exactly once. */
  @Test
  fun closeButtonCallsOnDismiss() {
    var dismissed = 0
    setMemberDialog(onDismiss = { dismissed++ })

    composeTestRule.onNodeWithTag(MemberDialogTestTags.CLOSE_BUTTON).performClick()

    assertEquals(1, dismissed)
  }

  /** Breaks if an action button calls another callback than its own, or none. */
  @Test
  fun eachActionCallsItsOwnCallback() {
    val tapped = mutableListOf<String>()
    setMemberDialog(
        actions =
            listOf(
                MemberDialogAction("Change role", { tapped += "role" }),
                MemberDialogAction("Kick", { tapped += "kick" }, destructive = true),
            )
    )

    composeTestRule.onNodeWithTag(MemberDialogTestTags.action("Kick")).performClick()
    composeTestRule.onNodeWithTag(MemberDialogTestTags.action("Change role")).performClick()

    assertEquals(listOf("kick", "role"), tapped)
  }

  /** Breaks if a disabled action can still be tapped, e.g. while a change is being saved. */
  @Test
  fun aDisabledActionCannotBeTapped() {
    var tapped = 0
    setMemberDialog(actions = listOf(MemberDialogAction("Kick", { tapped++ }, enabled = false)))

    composeTestRule
        .onNodeWithTag(MemberDialogTestTags.action("Kick"))
        .assertIsNotEnabled()
        .performClick()

    assertEquals(0, tapped)
  }

  /** Breaks if a label too long for one line is not wrapped, or makes the buttons uneven. */
  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE) // Measures the real text, so that the label wraps.
  fun aLongLabelWrapsAndKeepsBothButtonsTheSameHeight() {
    val long = "Change the role of this member"
    setMemberDialog(
        actions =
            listOf(
                MemberDialogAction(long, {}),
                MemberDialogAction("Kick", {}, destructive = true),
            )
    )

    val longBounds =
        composeTestRule.onNodeWithTag(MemberDialogTestTags.action(long)).getUnclippedBoundsInRoot()
    val kickBounds =
        composeTestRule
            .onNodeWithTag(MemberDialogTestTags.action("Kick"))
            .getUnclippedBoundsInRoot()

    assertTrue(longBounds.height > 48.dp)
    assertEquals(longBounds.height, kickBounds.height)
  }
}
