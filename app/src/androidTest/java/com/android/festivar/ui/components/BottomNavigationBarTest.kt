// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
      subScreen: NavigationSubScreen? = null,
  ) {
    composeTestRule.setContent {
      AppTheme {
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            subScreen = subScreen,
        )
      }
    }
  }

  private fun text(id: Int): String =
      InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

  /** Breaks if a tab or its label goes missing, or a fourth tab comes back. */
  @Test
  fun showsThreeTabsWithTheirLabels() {
    setBar()
    assertEquals(3, NavigationTab.entries.size)
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertIsDisplayed()
    NavigationTab.entries.forEach {
      composeTestRule.tab(it).assertIsDisplayed()
      composeTestRule.onNodeWithText(text(it.label)).assertIsDisplayed()
    }
  }

  /** Breaks if the selected state is wrong for any tab. */
  @Test
  fun onlySelectedTabIsSelected() {
    var selectedTab by mutableStateOf(NavigationTab.EVENTS)
    composeTestRule.setContent {
      AppTheme { BottomNavigationBar(selectedTab = selectedTab, onTabSelected = {}) }
    }
    NavigationTab.entries.forEach { expected ->
      composeTestRule.runOnIdle { selectedTab = expected }
      NavigationTab.entries.forEach {
        if (it == expected) composeTestRule.tab(it).assertIsSelected()
        else composeTestRule.tab(it).assertIsNotSelected()
      }
    }
  }

  /** Breaks if an item is wired to the wrong tab. */
  @Test
  fun clickOnTabCallsOnTabSelectedWithIt() {
    val clicked = mutableListOf<NavigationTab>()
    setBar(onTabSelected = { clicked += it })
    NavigationTab.entries.forEach { composeTestRule.tab(it).performClick() }
    assertEquals(NavigationTab.entries, clicked)
  }

  /** Breaks if a tab shrinks under the 48 dp tap target the design asks for. */
  @Test
  fun eachTabIsAtLeast48dpTall() {
    setBar()
    NavigationTab.entries.forEach { composeTestRule.tab(it).assertHeightIsAtLeast(48.dp) }
  }

  /** Breaks if a tab on its start screen is read with a sub-screen name. */
  @Test
  fun onTheStartScreenTheTabIsReadByItsNameAlone() {
    setBar(selectedTab = NavigationTab.MY_TASKS)
    composeTestRule
        .tab(NavigationTab.MY_TASKS)
        .assertContentDescriptionEquals(text(NavigationTab.MY_TASKS.label))
  }

  /** Breaks if a sub-screen disc leaks into the bar of a start screen. */
  @Test
  fun noSubScreenDiscOnAStartScreen() {
    setBar()
    NavigationSubScreen.entries.forEach { composeTestRule.subScreen(it).assertDoesNotExist() }
  }

  /**
   * Breaks if a nested screen no longer shows its disc on the selected tab, unselects the tab,
   * drops the sub-screen name from the accessibility description or swallows the tap.
   */
  @Test
  fun nestedScreenShowsItsDiscOnTheSelectedTabWhichStaysSelected() {
    val clicked = mutableListOf<NavigationTab>()
    setBar(
        selectedTab = NavigationTab.EVENTS,
        onTabSelected = { clicked += it },
        subScreen = NavigationSubScreen.EVENT_TASKS,
    )
    composeTestRule.subScreen(NavigationSubScreen.EVENT_TASKS).assertIsDisplayed()
    composeTestRule.tab(NavigationTab.EVENTS).assertIsSelected()
    composeTestRule
        .tab(NavigationTab.EVENTS)
        .assertContentDescriptionEquals(
            "${text(NavigationTab.EVENTS.label)}, ${text(NavigationSubScreen.EVENT_TASKS.label)}"
        )
    composeTestRule.tab(NavigationTab.MY_TASKS).assertIsNotSelected()
    composeTestRule.tab(NavigationTab.PROFILE).assertIsNotSelected()
    composeTestRule.tab(NavigationTab.EVENTS).performClick()
    assertEquals(listOf(NavigationTab.EVENTS), clicked)
  }

  /** Breaks if a sub-screen shows another one's disc, or two discs at once. */
  @Test
  fun eachSubScreenShowsOnlyItsOwnDisc() {
    var shown by mutableStateOf(NavigationSubScreen.EVENT_DETAILS)
    composeTestRule.setContent {
      AppTheme {
        BottomNavigationBar(
            selectedTab = NavigationTab.EVENTS,
            onTabSelected = {},
            subScreen = shown,
        )
      }
    }
    NavigationSubScreen.entries.forEach { expected ->
      composeTestRule.runOnIdle { shown = expected }
      NavigationSubScreen.entries.forEach {
        if (it == expected) composeTestRule.subScreen(it).assertIsDisplayed()
        else composeTestRule.subScreen(it).assertDoesNotExist()
      }
    }
  }

  /** Breaks if the task details disc stops following the My tasks tab. */
  @Test
  fun taskDetailsFromMyTasksNestsUnderMyTasks() {
    setBar(selectedTab = NavigationTab.MY_TASKS, subScreen = NavigationSubScreen.TASK_DETAILS)
    composeTestRule.subScreen(NavigationSubScreen.TASK_DETAILS).assertIsDisplayed()
    composeTestRule.tab(NavigationTab.MY_TASKS).assertIsSelected()
    composeTestRule
        .tab(NavigationTab.MY_TASKS)
        .assertContentDescriptionEquals(
            "${text(NavigationTab.MY_TASKS.label)}, ${text(NavigationSubScreen.TASK_DETAILS.label)}"
        )
    composeTestRule.tab(NavigationTab.EVENTS).assertIsNotSelected()
  }
}
