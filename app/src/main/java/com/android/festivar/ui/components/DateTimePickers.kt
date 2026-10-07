// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
        TextButton(
            onClick = { state.selectedDateMillis?.let { onConfirm(it.toPickerDate()) } },
            enabled = state.selectedDateMillis != null,
        ) {
          Text("OK")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  ) {
    DatePicker(state = state)
  }
}

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
      rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
      text = { TimePicker(state = state) },
  )
}
