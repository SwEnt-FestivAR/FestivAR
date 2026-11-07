// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RefreshableStateListTest {
  @get:Rule val composeTestRule = createComposeRule()

  private companion object {
    const val LIST = "testList"
    const val EMPTY_STATE = "testEmptyState"
    const val FILTER_EMPTY_MESSAGE = "Nothing in this filter."
    const val ERROR = "Failed to load: Network down"

    fun itemTag(item: String) = "item_$item"
  }

  /** The arguments of the list, which a test can change after the list is shown. */
  private class ListState(
      isLoading: Boolean = false,
      hasNoItems: Boolean = false,
      hasNoShownItems: Boolean = false,
      errorMessage: String? = null,
      listKey: Any? = null,
      items: List<String> = listOf("First", "Second"),
  ) {
    var isLoading by mutableStateOf(isLoading)
    var hasNoItems by mutableStateOf(hasNoItems)
    var hasNoShownItems by mutableStateOf(hasNoShownItems)
    var errorMessage by mutableStateOf(errorMessage)
    var listKey by mutableStateOf(listKey)
    var items by mutableStateOf(items)
    @Volatile var refreshes = 0
  }

  /** Shows a [RefreshableStateList] following [state], each item being a tagged text. */
  private fun setContent(state: ListState): ListState {
    composeTestRule.setContent {
      AppTheme {
        RefreshableStateList(
            isLoading = state.isLoading,
            hasNoItems = state.hasNoItems,
            hasNoShownItems = state.hasNoShownItems,
            errorMessage = state.errorMessage,
            onRefresh = { state.refreshes++ },
            filterEmptyMessage = FILTER_EMPTY_MESSAGE,
            emptyState = { Text(text = "No items yet", modifier = Modifier.testTag(EMPTY_STATE)) },
            listKey = state.listKey,
            listTestTag = LIST,
        ) {
          items(state.items) { Text(text = it, modifier = Modifier.testTag(itemTag(it))) }
        }
      }
    }
    return state
  }

  private fun assertDoesNotExist(vararg tags: String) {
    tags.forEach { composeTestRule.onNodeWithTag(it).assertDoesNotExist() }
  }

  /** The number of spinning progress indicators, the first-load one or the pull-to-refresh one. */
  private fun assertSpinners(count: Int) {
    composeTestRule
        .onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertCountEquals(count)
  }

  /**
   * Test that the first load shows a single spinner, vertically centered in the list area, and none
   * of the other states, so that the empty state does not flash before the items arrive.
   */
  @Test
  fun firstLoadShowsOnlyACenteredSpinner() {
    setContent(ListState(isLoading = true, hasNoItems = true, hasNoShownItems = true))

    val spinner =
        composeTestRule
            .onNodeWithTag(RefreshableListTestTags.LOADING)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
    val listArea =
        composeTestRule
            .onNodeWithTag(RefreshableListTestTags.PULL_TO_REFRESH)
            .getUnclippedBoundsInRoot()
    assertEquals(
        ((listArea.top + listArea.bottom) / 2).value,
        ((spinner.top + spinner.bottom) / 2).value,
        1f,
    )
    // The pull-to-refresh indicator does not spin on top of the first-load spinner
    assertSpinners(1)
    assertDoesNotExist(
        EMPTY_STATE,
        RefreshableListTestTags.FILTER_EMPTY,
        RefreshableListTestTags.ERROR_STATE,
        LIST,
    )
  }

  /**
   * Test that refreshing items already shown keeps them, with the pull-to-refresh indicator
   * spinning instead of the first-load spinner, until the refresh ends.
   */
  @Test
  fun refreshKeepsTheItemsWithThePullToRefreshIndicator() {
    val state = setContent(ListState(isLoading = true))

    composeTestRule.onNodeWithTag(LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(itemTag("First")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RefreshableListTestTags.LOADING).assertDoesNotExist()
    assertSpinners(1)

    state.isLoading = false

    composeTestRule.onNodeWithTag(itemTag("First")).assertIsDisplayed()
    assertSpinners(0)
  }

  /**
   * Test that an error with nothing loaded shows the error state with the message and a retry
   * calling onRefresh, instead of the empty state, then that the retry shows the spinner again.
   */
  @Test
  fun errorWithoutItemsShowsTheErrorStateWhoseRetryRefreshes() {
    val state =
        setContent(ListState(hasNoItems = true, hasNoShownItems = true, errorMessage = ERROR))

    composeTestRule.onNodeWithTag(RefreshableListTestTags.ERROR_STATE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(RefreshableListTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals(ERROR)
    assertDoesNotExist(
        RefreshableListTestTags.INLINE_ERROR,
        RefreshableListTestTags.LOADING,
        EMPTY_STATE,
        LIST,
    )

    composeTestRule.onNodeWithTag(RefreshableListTestTags.RETRY).performClick()
    assertEquals(1, state.refreshes)

    // While retrying, the spinner replaces the error state, even if the error is still set
    state.isLoading = true
    composeTestRule.onNodeWithTag(RefreshableListTestTags.LOADING).assertIsDisplayed()
    assertDoesNotExist(RefreshableListTestTags.ERROR_STATE)
  }

  /**
   * Test that an error with items shown keeps them and only adds the message above them, with a
   * retry calling onRefresh.
   */
  @Test
  fun errorWithItemsKeepsThemWithAnInlineErrorWhoseRetryRefreshes() {
    val state = setContent(ListState(errorMessage = ERROR))

    composeTestRule
        .onNode(
            hasTestTag(RefreshableListTestTags.INLINE_ERROR).and(hasAnyDescendant(hasText(ERROR))),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(itemTag("First")).assertIsDisplayed()
    val inlineError =
        composeTestRule
            .onNodeWithTag(RefreshableListTestTags.INLINE_ERROR)
            .getUnclippedBoundsInRoot()
    val list = composeTestRule.onNodeWithTag(LIST).getUnclippedBoundsInRoot()
    assertTrue(inlineError.bottom <= list.top)
    assertDoesNotExist(RefreshableListTestTags.ERROR_STATE)

    composeTestRule.onNodeWithTag(RefreshableListTestTags.RETRY).performClick()
    assertEquals(1, state.refreshes)

    state.errorMessage = null
    assertDoesNotExist(RefreshableListTestTags.INLINE_ERROR, RefreshableListTestTags.RETRY)
  }

  /** Test that no item at all shows the empty state, even though the filter shows nothing too. */
  @Test
  fun noItemsShowsTheEmptyState() {
    setContent(ListState(hasNoItems = true, hasNoShownItems = true))

    composeTestRule.onNodeWithTag(EMPTY_STATE).assertIsDisplayed()
    assertDoesNotExist(
        RefreshableListTestTags.FILTER_EMPTY,
        RefreshableListTestTags.LOADING,
        RefreshableListTestTags.ERROR_STATE,
        LIST,
    )
  }

  /** Test that a filter showing nothing, while other items exist, shows the filter's message. */
  @Test
  fun filterShowingNothingShowsTheFilterMessage() {
    setContent(ListState(hasNoShownItems = true))

    composeTestRule
        .onNodeWithTag(RefreshableListTestTags.FILTER_EMPTY)
        .assertIsDisplayed()
        .assertTextEquals(FILTER_EMPTY_MESSAGE)
    assertDoesNotExist(EMPTY_STATE, itemTag("First"), LIST)
  }

  /** Test that the list can be pulled down to refresh, both from the empty state and its items. */
  @Test
  fun pullingDownRefreshesFromEveryState() {
    val state = setContent(ListState(hasNoItems = true, hasNoShownItems = true))
    val pullToRefresh = composeTestRule.onNodeWithTag(RefreshableListTestTags.PULL_TO_REFRESH)

    pullToRefresh.performTouchInput { swipeDown() }
    composeTestRule.waitUntil { state.refreshes == 1 }

    state.hasNoItems = false
    state.hasNoShownItems = false
    composeTestRule.onNodeWithTag(LIST).assertIsDisplayed()
    pullToRefresh.performTouchInput { swipeDown() }
    composeTestRule.waitUntil { state.refreshes == 2 }
  }

  /**
   * Test that the list is lazy: with 100 items, the last one is not composed until the list is
   * scrolled to it.
   */
  @Test
  fun listIsLazy() {
    val items = (1..100).map { "Item $it" }
    setContent(ListState(items = items))
    val lastTag = itemTag(items.last())

    composeTestRule.onNodeWithTag(lastTag, useUnmergedTree = true).assertDoesNotExist()

    composeTestRule.onNodeWithTag(LIST).performScrollToNode(hasTestTag(lastTag))

    composeTestRule.onNodeWithTag(lastTag).assertIsDisplayed()
  }

  /**
   * Test that the scroll is kept while the list key stays the same, e.g. during a refresh, and that
   * a new key starts back at the top.
   */
  @Test
  fun newListKeyStartsBackAtTheTop() {
    val items = (1..50).map { "Item $it" }
    val state = setContent(ListState(listKey = "a", items = items))
    val firstTag = itemTag(items.first())
    val lastTag = itemTag(items.last())
    composeTestRule.onNodeWithTag(LIST).performScrollToNode(hasTestTag(lastTag))

    state.isLoading = true
    composeTestRule.onNodeWithTag(lastTag).assertIsDisplayed()

    state.listKey = "b"
    composeTestRule.onNodeWithTag(firstTag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(lastTag).assertDoesNotExist()
  }
}
