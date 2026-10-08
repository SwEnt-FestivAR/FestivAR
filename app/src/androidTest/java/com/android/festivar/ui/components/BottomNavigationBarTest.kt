// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
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
class BottomNavigationBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setBar(
      selectedTab: NavigationTab = NavigationTab.EVENTS,
      onTabSelected: (NavigationTab) -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme { BottomNavigationBar(selectedTab = selectedTab, onTabSelected = onTabSelected) }
    }
  }

  @Test
  fun showsEveryTabWithItsLabel() {
    setBar()
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertIsDisplayed()
    NavigationTab.entries.forEach {
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(it))
          .assertIsDisplayed()
      composeTestRule.onNodeWithText(it.label).assertIsDisplayed()
    }
  }

  @Test
  fun onlySelectedTabIsSelected() {
    setBar(selectedTab = NavigationTab.MAP)
    NavigationTab.entries.forEach {
      val node = composeTestRule.onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(it))
      if (it == NavigationTab.MAP) node.assertIsSelected() else node.assertIsNotSelected()
    }
  }

  @Test
  fun clickOnTabCallsOnTabSelectedWithIt() {
    val clicked = mutableListOf<NavigationTab>()
    setBar(onTabSelected = { clicked += it })
    NavigationTab.entries.forEach {
      composeTestRule.onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(it)).performClick()
    }
    assertEquals(NavigationTab.entries, clicked)
  }
}
