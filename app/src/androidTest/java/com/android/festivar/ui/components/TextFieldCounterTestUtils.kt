// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo

/**
 * Checks that the screen shows a single character counter, under the field tagged [fieldTag], and
 * that it reads [text], e.g. "0/500".
 *
 * The counter is a child of its [FestivarTextField], which merges it, so it is looked up in the
 * unmerged tree.
 */
fun ComposeTestRule.checkTheOnlyCounterIsUnder(fieldTag: String, text: String) {
  onAllNodesWithTag(FestivarTextFieldTestTags.COUNTER, useUnmergedTree = true).assertCountEquals(1)
  onNode(
          hasTestTag(FestivarTextFieldTestTags.COUNTER) and hasAnyAncestor(hasTestTag(fieldTag)),
          useUnmergedTree = true,
      )
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextEquals(text)
}
