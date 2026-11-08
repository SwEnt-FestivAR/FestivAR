// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
      inEventTasks: Boolean = false,
  ) {
    composeTestRule.setContent {
      AppTheme {
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            inEventTasks = inEventTasks,
        )
      }
    }
  }

  private fun label(tab: NavigationTab): String =
      InstrumentationRegistry.getInstrumentation().targetContext.getString(tab.label)

  @Test
  fun showsThreeTabsWithTheirLabels() {
    setBar()
    assertEquals(3, NavigationTab.entries.size)
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertIsDisplayed()
    NavigationTab.entries.forEach {
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(it))
          .assertIsDisplayed()
      composeTestRule.onNodeWithText(label(it)).assertIsDisplayed()
    }
  }

  @Test
  fun onlySelectedTabIsSelected() {
    var selectedTab by mutableStateOf(NavigationTab.EVENTS)
    composeTestRule.setContent {
      AppTheme { BottomNavigationBar(selectedTab = selectedTab, onTabSelected = {}) }
    }
    NavigationTab.entries.forEach { expected ->
      composeTestRule.runOnIdle { selectedTab = expected }
      NavigationTab.entries.forEach {
        val node = composeTestRule.onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(it))
        if (it == expected) node.assertIsSelected() else node.assertIsNotSelected()
      }
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

  @Test
  fun tasksBadgeIsHiddenByDefault() {
    setBar()
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.TASKS_BADGE, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun inEventTasksShowsTheBadgeAndSelectsNoTab() {
    val clicked = mutableListOf<NavigationTab>()
    setBar(
        selectedTab = NavigationTab.EVENTS,
        onTabSelected = { clicked += it },
        inEventTasks = true,
    )
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.TASKS_BADGE, useUnmergedTree = true)
        .assertIsDisplayed()
    NavigationTab.entries.forEach {
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(it))
          .assertIsNotSelected()
    }
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(NavigationTab.EVENTS))
        .performClick()
    assertEquals(listOf(NavigationTab.EVENTS), clicked)
  }
}
