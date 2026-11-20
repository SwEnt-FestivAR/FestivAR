// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.R
import com.android.festivar.ui.components.DialogButton
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalTime

/**
 * Dialog to pick a time on a 24-hour clock.
 *
 * @param initial The time selected when the dialog opens, or `null` for 09:00.
 * @param onConfirm Called with the chosen time.
 * @param onDismiss Called when the dialog is cancelled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivarTimePickerDialog(
    initial: LocalTime?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
  val start = initial ?: LocalTime.of(9, 0)
  val state =
      rememberTimePickerState(
          initialHour = start.hour,
          initialMinute = start.minute,
          is24Hour = true,
      )
  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        DialogButton(
            text = stringResource(R.string.action_ok),
            onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
        )
      },
      dismissButton = {
        DialogButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
      },
      text = { TimePicker(state = state, colors = readableTimePickerColors()) },
  )
}

/**
 * Time picker colors: the clock hand and the circle around the selected number are drawn in
 * `primary` by default, a thin lime line that cannot be read on the dial. They use the dark green
 * of the theme, with white text inside the circle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun readableTimePickerColors(): TimePickerColors =
    TimePickerDefaults.colors(
        selectorColor = MaterialTheme.colorScheme.onPrimaryContainer,
        clockDialSelectedContentColor = MaterialTheme.colorScheme.surfaceBright,
    )

@Preview
@Composable
internal fun FestivarTimePickerDialogPreview() {
  AppTheme {
    FestivarTimePickerDialog(initial = LocalTime.of(9, 30), onConfirm = {}, onDismiss = {})
  }
}
