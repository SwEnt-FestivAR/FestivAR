// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

object RefreshableListTestTags {
  const val PULL_TO_REFRESH = "refreshableListPullToRefresh"
  const val LOADING = "refreshableListLoading"
  const val ERROR_STATE = "refreshableListErrorState"
  const val ERROR_MESSAGE = "refreshableListErrorMessage"
  const val INLINE_ERROR = "refreshableListInlineError"
  const val RETRY = "refreshableListRetry"
  const val FILTER_EMPTY = "refreshableListFilterEmpty"
}

/**
 * List that the user can pull down to refresh, showing the loading, error and empty states of its
 * items. Every state is in the list, so that the user can always pull it down to refresh.
 *
 * In order, it shows a spinner on the first load, an error state with a retry when nothing could be
 * loaded, the [emptyState] when there is no item at all, the [filterEmptyMessage] when the selected
 * filter shows nothing, and the [content] otherwise. With items shown, a failed refresh keeps them
 * and only adds an error line with a retry above the list.
 *
 * @param isLoading Whether the items are being loaded.
 * @param hasNoItems Whether there is no item at all, whatever the filter.
 * @param hasNoShownItems Whether the selected filter shows no item. Ignored when [hasNoItems] is
 *   true, since no item is shown then either.
 * @param errorMessage The error to show, worded by the screen; null if there is none.
 * @param onRefresh Called when the user pulls the list down or clicks on "Retry".
 * @param filterEmptyMessage The text shown when the selected filter shows no item.
 * @param emptyState The state shown when there is no item at all, filling the list.
 * @param modifier The modifier of the error line and the list together.
 * @param listKey A new scroll state is used for each key, e.g. so that a new filter starts at the
 *   top.
 * @param listTestTag The test tag of the list, applied only when it shows items.
 * @param contentPadding The padding around the items. By default, the bottom leaves room for the
 *   last item to scroll above a floating button. The states without items keep only its start and
 *   end, so that they stay centered in the list.
 * @param content The items of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableStateList(
    isLoading: Boolean,
    hasNoItems: Boolean,
    hasNoShownItems: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    filterEmptyMessage: String,
    emptyState: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listKey: Any? = null,
    listTestTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp),
    content: LazyListScope.() -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
    // With items shown, a failed refresh keeps them and only adds a line above them
    if (errorMessage != null && !hasNoItems) {
      InlineError(message = errorMessage, onRetry = onRefresh)
    }

    PullToRefreshBox(
        // With no items, the loading state below already shows a progress indicator
        isRefreshing = isLoading && !hasNoItems,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().testTag(RefreshableListTestTags.PULL_TO_REFRESH),
    ) {
      val listState = key(listKey) { rememberLazyListState() }
      val showsItems = !hasNoItems && !hasNoShownItems
      val layoutDirection = LocalLayoutDirection.current
      // Only a list of items gets the tag: the loading, error and empty states are not a list for
      // the user
      LazyColumn(
          state = listState,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding =
              if (showsItems) contentPadding
              else
                  PaddingValues(
                      start = contentPadding.calculateStartPadding(layoutDirection),
                      end = contentPadding.calculateEndPadding(layoutDirection),
                  ),
          modifier =
              Modifier.fillMaxSize()
                  .then(
                      if (showsItems && listTestTag != null) Modifier.testTag(listTestTag)
                      else Modifier
                  ),
      ) {
        when {
          // Only on the first load: a later refresh keeps the current list instead of a spinner
          isLoading && hasNoItems ->
              item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillParentMaxSize()) {
                  CircularProgressIndicator(
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.testTag(RefreshableListTestTags.LOADING),
                  )
                }
              }
          errorMessage != null && hasNoItems ->
              item { ErrorState(message = errorMessage, onRetry = onRefresh) }
          hasNoItems ->
              item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillParentMaxSize()) {
                  emptyState()
                }
              }
          hasNoShownItems ->
              item {
                Text(
                    text = filterEmptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(RefreshableListTestTags.FILTER_EMPTY),
                )
              }
          else -> content()
        }
      }
    }
  }
}

/** The error in the theme's error colour, with a retry beside it, shown above the items. */
@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp)
              .testTag(RefreshableListTestTags.INLINE_ERROR),
  ) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f),
    )
    TextButton(
        onClick = onRetry,
        colors =
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.testTag(RefreshableListTestTags.RETRY),
    ) {
      Text(text = stringResource(R.string.action_retry))
    }
  }
}

/**
 * Error state shown instead of the empty state when nothing could be loaded. It offers to retry.
 */
@Composable
private fun LazyItemScope.ErrorState(message: String, onRetry: () -> Unit) {
  Column(
      verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.fillParentMaxSize()
              .padding(horizontal = 24.dp)
              .testTag(RefreshableListTestTags.ERROR_STATE),
  ) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(RefreshableListTestTags.ERROR_MESSAGE),
    )
    OutlinedButton(
        onClick = onRetry,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors =
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.testTag(RefreshableListTestTags.RETRY),
    ) {
      Text(
          text = stringResource(R.string.action_retry),
          style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 600)
@Composable
internal fun RefreshableStateListPreview() {
  AppTheme {
    RefreshableStateList(
        isLoading = false,
        hasNoItems = false,
        hasNoShownItems = false,
        errorMessage = "Failed to load: no connection",
        onRefresh = {},
        filterEmptyMessage = "Nothing here.",
        emptyState = {},
    ) {
      items(listOf("First item", "Second item", "Third item")) { Text(text = it) }
    }
  }
}
