// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.LocalTime

/** A date or time field of a form with a start and an end, each opening its own picker. */
enum class DateTimeField {
  START_DATE,
  START_TIME,
  END_DATE,
  END_TIME,
}

/**
 * Shows the picker dialog of [field], or nothing when [field] is `null`. The dialogs open on the
 * values of [range], and the end ones on the start while no end is picked. The caller closes the
 * dialog after [onDatePicked] and [onTimePicked].
 *
 * @param field The field being picked, or `null` when no dialog is open.
 * @param range The values the dialogs open on.
 * @param onDatePicked Called with the date chosen in a date dialog.
 * @param onTimePicked Called with the time chosen in a time dialog.
 * @param onDismiss Called when the dialog is cancelled.
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
