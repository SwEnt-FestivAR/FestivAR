// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.dates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarPickerField
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
    FestivarFieldWithError(dateError, dateErrorTag, Modifier.weight(1f)) {
      FestivarPickerField(
          value = date?.let(::formatDate).orEmpty(),
          label = dateLabel,
          onClick = onDateClick,
          enabled = enabled,
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
        enabled = enabled,
        modifier = Modifier.weight(1f).testTag(timeTag),
    )
  }
}
