// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag

/** The item of [tab] in the bottom bar under test. */
fun ComposeTestRule.tab(tab: NavigationTab): SemanticsNodeInteraction =
    onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(tab))

/**
 * The lime disc of [subScreen], inside the merged tab item, so it is found in the unmerged tree.
 */
fun ComposeTestRule.subScreen(subScreen: NavigationSubScreen): SemanticsNodeInteraction =
    onNodeWithTag(
        BottomNavigationBarTestTags.getTestTagForSubScreen(subScreen),
        useUnmergedTree = true,
    )
