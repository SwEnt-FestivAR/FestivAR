// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.R
import com.android.festivar.ui.components.DialogButton
import com.android.festivar.ui.theme.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The date picker works in UTC milliseconds; this is the value that shows [this] date. */
internal fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Inverse of [toPickerMillis]. */
internal fun Long.toPickerDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Dialog to pick a date.
 *
 * @param initial The date selected when the dialog opens, or `null` for none.
 * @param onConfirm Called with the chosen date. Not called if nothing is selected.
 * @param onDismiss Called when the dialog is cancelled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivarDatePickerDialog(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
  val state = rememberDatePickerState(initialSelectedDateMillis = initial?.toPickerMillis())
  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        DialogButton(
            text = stringResource(R.string.action_ok),
            onClick = { state.selectedDateMillis?.let { onConfirm(it.toPickerDate()) } },
            enabled = state.selectedDateMillis != null,
        )
      },
      dismissButton = {
        DialogButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
      },
  ) {
    DatePicker(state = state, colors = readableDatePickerColors())
  }
}

/**
 * Date picker colors: today's date and the current year are outlined in `primary` by default, which
 * is the unreadable lime, so they use the dark green of the theme. The selected day keeps the lime
 * background with dark text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun readableDatePickerColors(): DatePickerColors {
  val accent = MaterialTheme.colorScheme.onPrimaryContainer
  return DatePickerDefaults.colors(
      todayContentColor = accent,
      todayDateBorderColor = accent,
      currentYearContentColor = accent,
  )
}

@Preview
@Composable
internal fun FestivarDatePickerDialogPreview() {
  AppTheme {
    FestivarDatePickerDialog(initial = LocalDate.of(2026, 10, 17), onConfirm = {}, onDismiss = {})
  }
}
