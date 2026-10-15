// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme
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
        DialogButton(
            text = "OK",
            onClick = { state.selectedDateMillis?.let { onConfirm(it.toPickerDate()) } },
            enabled = state.selectedDateMillis != null,
        )
      },
      dismissButton = { DialogButton(text = "Cancel", onClick = onDismiss) },
  ) {
    DatePicker(state = state, colors = readableDatePickerColors())
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
      rememberTimePickerState(
          initialHour = start.hour,
          initialMinute = start.minute,
          is24Hour = true,
      )
  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        DialogButton(
            text = "OK",
            onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
        )
      },
      dismissButton = { DialogButton(text = "Cancel", onClick = onDismiss) },
      text = { TimePicker(state = state, colors = readableTimePickerColors()) },
  )
}

/** A date or time field of a form with a start and an end, each opening its own picker. */
enum class DateTimeField {
  START_DATE,
  START_TIME,
  END_DATE,
  END_TIME,
}

/** The start and end picked in a form. Any part may still be unset. */
data class DateTimeRange(
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
)

/**
 * Shows the picker dialog of [field], or nothing when [field] is `null`. The dialogs open on the
 * values of [range], and the end ones on the start while no end is picked. The caller closes the
 * dialog after [onDatePicked] and [onTimePicked].
 */
@Composable
fun FestivarDateTimePickerDialog(
    field: DateTimeField?,
    range: DateTimeRange,
    onDatePicked: (LocalDate) -> Unit,
    onTimePicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
  when (field) {
    DateTimeField.START_DATE ->
        FestivarDatePickerDialog(range.startDate, onConfirm = onDatePicked, onDismiss = onDismiss)
    DateTimeField.END_DATE ->
        FestivarDatePickerDialog(
            initial = range.endDate ?: range.startDate,
            onConfirm = onDatePicked,
            onDismiss = onDismiss,
        )
    DateTimeField.START_TIME ->
        FestivarTimePickerDialog(range.startTime, onConfirm = onTimePicked, onDismiss = onDismiss)
    DateTimeField.END_TIME ->
        FestivarTimePickerDialog(
            initial = range.endTime ?: range.startTime,
            onConfirm = onTimePicked,
            onDismiss = onDismiss,
        )
    null -> Unit
  }
}

/**
 * A date field and a time field side by side, each opening its picker when tapped. [dateError]
 * marks both fields, and its message is shown under the date. The tags are the test tags of the
 * date, of the error and of the time.
 */
@Composable
fun FestivarDateTimeRow(
    dateLabel: String,
    timeLabel: String,
    date: LocalDate?,
    time: LocalTime?,
    dateError: FieldError?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    dateTag: String,
    dateErrorTag: String,
    timeTag: String,
    modifier: Modifier = Modifier,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
    FestivarFieldWithError(dateError, dateErrorTag, Modifier.weight(1f)) {
      FestivarPickerField(
          value = date?.let(::formatDate).orEmpty(),
          label = dateLabel,
          onClick = onDateClick,
          isError = dateError != null,
          modifier = Modifier.testTag(dateTag),
      )
    }
    FestivarPickerField(
        value = time?.let(::formatTime).orEmpty(),
        label = timeLabel,
        onClick = onTimeClick,
        // As in Figma, an error marks the whole row; its message stays under the date.
        isError = dateError != null,
        modifier = Modifier.weight(1f).testTag(timeTag),
    )
  }
}

/**
 * Text button of the dialogs. Material draws these in `primary`, which is the lime of the app and
 * cannot be read on the dialog background, so the dark green of the theme is used instead.
 */
@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
  TextButton(
      onClick = onClick,
      enabled = enabled,
      colors =
          ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
  ) {
    Text(text)
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
internal fun FestivarDatePickerDialogPreview() {
  AppTheme {
    FestivarDatePickerDialog(initial = LocalDate.of(2026, 10, 17), onConfirm = {}, onDismiss = {})
  }
}

@Preview
@Composable
internal fun FestivarTimePickerDialogPreview() {
  AppTheme {
    FestivarTimePickerDialog(initial = LocalTime.of(9, 30), onConfirm = {}, onDismiss = {})
  }
}
