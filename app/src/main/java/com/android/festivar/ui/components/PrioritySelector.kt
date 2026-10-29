// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.ui.theme.AppTheme

/** The string resource that names [priority] on screen. */
@StringRes
fun priorityLabelRes(priority: Priority): Int =
    when (priority) {
      Priority.NONE -> R.string.priority_none
      Priority.LOW -> R.string.priority_low
      Priority.MEDIUM -> R.string.priority_medium
      Priority.HIGH -> R.string.priority_high
      Priority.URGENT -> R.string.priority_urgent
    }

/**
 * Titled row of chips to pick a [Priority]; [selected] is the current one. The title is the
 * design's [SectionLabel] and the chips are the design's [FilterChip] without a count. Pass a test
 * tag through [modifier].
 */
@Composable
fun FestivarPrioritySelector(
    selected: Priority,
    onSelect: (Priority) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    SectionLabel(label)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
      Priority.entries.forEach { priority ->
        FilterChip(
            label = stringResource(priorityLabelRes(priority)),
            selected = priority == selected,
            onClick = { onSelect(priority) },
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun FestivarPrioritySelectorPreview() {
  AppTheme {
    FestivarPrioritySelector(
        selected = Priority.HIGH,
        onSelect = {},
        label = "Priority",
        modifier = Modifier.padding(16.dp),
    )
  }
}
