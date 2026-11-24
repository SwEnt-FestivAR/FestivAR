// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Test tags of [OptionPickerDialog]. */
object OptionPickerDialogTestTags {
  const val DIALOG = "optionPickerDialog"
  const val CLOSE_BUTTON = "optionPickerCloseButton"

  fun option(index: Int) = "optionPickerOption_$index"
}

/**
 * A dialog to pick one of [options], as in the "Change role" frames of the design: a [title] with a
 * close button, then one row per option, drawn by [option]. Tapping a row calls [onPick], except
 * for the [current] one, marked with [currentLabel], and except while not [enabled].
 *
 * @param onDismiss Called by the close button or a tap outside the dialog.
 */
@Composable
fun <T> OptionPickerDialog(
    title: String,
    options: List<T>,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    current: T? = null,
    currentLabel: String = "",
    enabled: Boolean = true,
    option: @Composable (T) -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    DialogSurface(modifier.testTag(OptionPickerDialogTestTags.DIALOG)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        DialogCloseButton(onDismiss, Modifier.testTag(OptionPickerDialogTestTags.CLOSE_BUTTON))
      }
      Column(
          modifier = Modifier.padding(top = 12.dp, end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        options.forEachIndexed { index, value ->
          val isCurrent = value == current
          val shape = RoundedCornerShape(12.dp)
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(shape)
                      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                      .then(
                          if (isCurrent)
                              Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                          else Modifier.clickable(enabled = enabled) { onPick(value) }
                      )
                      .padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
                      .testTag(OptionPickerDialogTestTags.option(index)),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(modifier = Modifier.weight(1f)) { option(value) }
            if (isCurrent) {
              Text(currentLabel, style = MaterialTheme.typography.labelMedium)
            } else {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
          }
        }
      }
    }
  }
}
