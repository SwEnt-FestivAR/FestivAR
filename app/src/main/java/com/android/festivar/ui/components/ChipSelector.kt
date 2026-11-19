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
import com.android.festivar.model.task.Priority
import com.android.festivar.ui.task.priorityLabelRes
import com.android.festivar.ui.theme.AppTheme

/**
 * Gives the string resource that names an option of a [FestivarChipSelector]. A function type
 * cannot carry `@StringRes` on its result, so this interface does: the label is marked as a string
 * resource for lint, and a function reference such as `::priorityLabelRes` still fits.
 */
fun interface OptionLabelRes<T> {
  /** The string resource that names [option] on its chip. */
  @StringRes fun of(option: T): Int
}

/**
 * Titled single-choice row of chips: one chip per entry of [options], and the chip of [selected] is
 * the selected one. The title is the design's [SectionLabel] and the chips are the design's
 * [FilterChip] without a count. The options are enum entries, so comparing an option with
 * [selected] is an identity comparison. Pass a test tag through [modifier], [labelModifier] or
 * [chipModifier].
 *
 * @param options The choices, one chip each, in this order.
 * @param selected The current choice.
 * @param onSelect Called with the option whose chip was tapped.
 * @param label The title above the chips.
 * @param optionLabelRes Gives the string resource that names an option on its chip.
 * @param enabled Whether the chips react to taps.
 * @param labelModifier Applied to the title.
 * @param chipModifier Gives the modifier of the chip of an option.
 */
@Composable
fun <T : Enum<T>> FestivarChipSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    optionLabelRes: OptionLabelRes<T>,
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
      options.forEach { option ->
        FilterChip(
            label = stringResource(optionLabelRes.of(option)),
            selected = option == selected,
            onClick = { onSelect(option) },
            enabled = enabled,
            modifier = chipModifier(option),
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun FestivarChipSelectorPreview() {
  AppTheme {
    FestivarChipSelector(
        options = Priority.entries,
        selected = Priority.HIGH,
        onSelect = {},
        label = "Priority",
        optionLabelRes = ::priorityLabelRes,
        modifier = Modifier.padding(16.dp),
    )
  }
}
