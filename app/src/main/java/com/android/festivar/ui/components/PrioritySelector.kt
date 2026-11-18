// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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
 * Titled row of chips to pick one of [priorities]; [selected] is the current one. The title is the
 * design's [SectionLabel] and the chips are the design's [FilterChip] without a count. Pass a test
 * tag through [modifier], [labelModifier] or [chipModifier].
 *
 * @param label The title above the chips.
 * @param chipLabelRes Gives the string resource that names a priority on its chip.
 * @param enabled Whether the chips react to taps.
 * @param labelModifier Applied to the title.
 * @param chipModifier Gives the modifier of the chip of a priority.
 */
@Composable
fun <T> FestivarPrioritySelector(
    priorities: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    chipLabelRes: (T) -> Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelModifier: Modifier = Modifier,
    chipModifier: (T) -> Modifier = { Modifier },
) {
  Column(modifier = modifier) {
    SectionLabel(label, modifier = labelModifier)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
      priorities.forEach { priority ->
        FilterChip(
            label = stringResource(chipLabelRes(priority)),
            selected = priority == selected,
            onClick = { onSelect(priority) },
            enabled = enabled,
            modifier = chipModifier(priority),
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
        priorities = Priority.entries,
        selected = Priority.HIGH,
        onSelect = {},
        label = "Priority",
        chipLabelRes = ::priorityLabelRes,
        modifier = Modifier.padding(16.dp),
    )
  }
}
