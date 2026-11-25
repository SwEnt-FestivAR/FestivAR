// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.datetime

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
import java.time.LocalDate
import java.time.LocalTime

/**
 * A date field and a time field side by side, each opening its picker when tapped. As in Figma, an
 * error marks both fields, and its message is shown under the date.
 *
 * @param dateLabel Label of the date field.
 * @param timeLabel Label of the time field.
 * @param date The date shown, or `null` for an empty field.
 * @param time The time shown, or `null` for an empty field.
 * @param dateError The error marking both fields, or `null` for none. Its message is shown under
 *   the date.
 * @param onDateClick Called when the date field is tapped.
 * @param onTimeClick Called when the time field is tapped.
 * @param dateTag Test tag of the date field.
 * @param dateErrorTag Test tag of the error message.
 * @param timeTag Test tag of the time field.
 * @param modifier Modifier of the row.
 * @param enabled Whether the fields can be tapped.
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
    enabled: Boolean = true,
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
