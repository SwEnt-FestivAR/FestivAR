// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests of [MemberDialog] and [OptionPickerDialog]. */
@RunWith(AndroidJUnit4::class)
class MemberDialogsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val yasser = User("u1", name = "Yasser", surname = "Ameur")

  private val options = listOf("Team lead", "Staff", "Visitor")

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

  /** Shows an [OptionPickerDialog] of [options] where "Staff" is the current one. */
  private fun setOptionPicker(
      onPick: (String) -> Unit = {},
      onDismiss: () -> Unit = {},
      enabled: Boolean = true,
  ) {
    composeTestRule.setContent {
      AppTheme {
        OptionPickerDialog(
            title = "Change the role of Yasser",
            options = options,
            onPick = onPick,
            onDismiss = onDismiss,
            current = "Staff",
            currentLabel = "Current",
            enabled = enabled,
        ) {
          Text(it)
        }
      }
    }
    composeTestRule.waitForIdle()
  }

  // ---------------------------------------------------------------------------------------- //
  // MemberDialog
  // ---------------------------------------------------------------------------------------- //

  /** Breaks if the dialog does not show the name, the badge and the supporting text. */
  @Test
  fun memberDialog_showsTheMemberWithTheBadgeAndTheSupportingText() {
    setMemberDialog()

    composeTestRule.onNodeWithTag(MemberDialogTestTags.MEMBER_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Yasser Ameur").assertIsDisplayed()
    composeTestRule.onNodeWithText("Staff").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 tasks").assertIsDisplayed()
  }

  /** Breaks if the close button does not call onDismiss exactly once. */
  @Test
  fun memberDialog_closeButtonCallsOnDismiss() {
    var dismissed = 0
    setMemberDialog(onDismiss = { dismissed++ })

    composeTestRule.onNodeWithTag(MemberDialogTestTags.CLOSE_BUTTON).performClick()

    assertEquals(1, dismissed)
  }

  /** Breaks if an action button calls another callback than its own, or none. */
  @Test
  fun memberDialog_eachActionCallsItsOwnCallback() {
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
  fun memberDialog_aDisabledActionCannotBeTapped() {
    var tapped = 0
    setMemberDialog(actions = listOf(MemberDialogAction("Kick", { tapped++ }, enabled = false)))

    composeTestRule
        .onNodeWithTag(MemberDialogTestTags.action("Kick"))
        .assertIsNotEnabled()
        .performClick()

    assertEquals(0, tapped)
  }

  /** Breaks if the dialog shows a button when it has no action. */
  @Test
  fun memberDialog_withoutActionsShowsNoButton() {
    setMemberDialog()

    composeTestRule.onAllNodesWithText("Change role").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Kick").assertCountEquals(0)
  }

  // ---------------------------------------------------------------------------------------- //
  // OptionPickerDialog
  // ---------------------------------------------------------------------------------------- //

  /** Breaks if an option is missing, or the current one is not marked. */
  @Test
  fun optionPicker_showsEveryOptionAndMarksTheCurrentOne() {
    setOptionPicker()

    composeTestRule.onNodeWithText("Change the role of Yasser").assertIsDisplayed()
    options.forEachIndexed { index, option ->
      composeTestRule.onNodeWithTag(MemberDialogTestTags.option(index)).assertIsDisplayed()
      composeTestRule.onNodeWithText(option).assertIsDisplayed()
    }
    composeTestRule.onAllNodesWithText("Current").assertCountEquals(1)
  }

  /** Breaks if tapping an option does not pick that one. */
  @Test
  fun optionPicker_tappingAnOptionPicksIt() {
    var picked: String? = null
    setOptionPicker(onPick = { picked = it })

    composeTestRule.onNodeWithTag(MemberDialogTestTags.option(2)).performClick()

    assertEquals("Visitor", picked)
  }

  /** Breaks if the current option can be picked again. */
  @Test
  fun optionPicker_theCurrentOptionCannotBePicked() {
    var picked: String? = null
    setOptionPicker(onPick = { picked = it })

    composeTestRule.onNodeWithTag(MemberDialogTestTags.option(1)).performClick()

    assertNull(picked)
  }

  /** Breaks if an option can be picked while the picker is disabled. */
  @Test
  fun optionPicker_disabledOptionsCannotBePicked() {
    var picked: String? = null
    setOptionPicker(onPick = { picked = it }, enabled = false)

    composeTestRule.onNodeWithTag(MemberDialogTestTags.option(0)).performClick()

    assertNull(picked)
  }

  /** Breaks if the close button does not call onDismiss exactly once. */
  @Test
  fun optionPicker_closeButtonCallsOnDismiss() {
    var dismissed = 0
    setOptionPicker(onDismiss = { dismissed++ })

    composeTestRule.onNodeWithTag(MemberDialogTestTags.CLOSE_BUTTON).performClick()

    assertEquals(1, dismissed)
  }
}
