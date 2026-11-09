package com.android.festivar.ui.event

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FestivarDateTimePickerDialog
import com.android.festivar.ui.components.FestivarDateTimeRow
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.components.FieldError
import java.time.LocalDate
import java.time.LocalTime

/**
 * Identifies the event fields and validation messages rendered by [EventFields].
 *
 * Each screen supplies tags unique to its own form.
 */
data class EventFieldsTestTags(
    val name: String,
    val startDate: String,
    val startTime: String,
    val endDate: String,
    val endTime: String,
    val venue: String,
    val notes: String,
    val nameError: String,
    val startDateError: String,
    val endDateError: String,
)

/**
 * Renders the shared title, start and end date/time, venue, and notes fields with a date/time
 * picker.
 *
 * The caller owns the form state, supplies screen-specific [testTags], and handles the callbacks.
 * [openPicker] selects which date/time picker is visible.
 */
@Composable
fun EventFields(
    name: String,
    nameError: FieldError?,
    dateTimeRange: DateTimeRange,
    startDateError: FieldError?,
    endDateError: FieldError?,
    venue: String,
    notes: String,
    openPicker: DateTimeField?,
    testTags: EventFieldsTestTags,
    onNameChange: (String) -> Unit,
    onShowPicker: (DateTimeField?) -> Unit,
    onVenueChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onTimePicked: (LocalTime) -> Unit,
) {
  Column {
    FestivarFieldWithError(nameError, testTags.nameError) {
      FestivarTextField(
          value = name,
          onValueChange = onNameChange,
          label = stringResource(R.string.create_event_field_name),
          isError = nameError != null,
          modifier = Modifier.testTag(testTags.name),
      )
    }
    Spacer(modifier = Modifier.height(14.dp))

    FestivarDateTimeRow(
        dateLabel = stringResource(R.string.create_event_field_start_date),
        timeLabel = stringResource(R.string.create_event_field_start_time),
        date = dateTimeRange.startDate,
        time = dateTimeRange.startTime,
        dateError = startDateError,
        onDateClick = { onShowPicker(DateTimeField.START_DATE) },
        onTimeClick = { onShowPicker(DateTimeField.START_TIME) },
        dateTag = testTags.startDate,
        dateErrorTag = testTags.startDateError,
        timeTag = testTags.startTime,
    )
    Spacer(modifier = Modifier.height(14.dp))

    FestivarDateTimeRow(
        dateLabel = stringResource(R.string.create_event_field_end_date),
        timeLabel = stringResource(R.string.create_event_field_end_time),
        date = dateTimeRange.endDate,
        time = dateTimeRange.endTime,
        dateError = endDateError,
        onDateClick = { onShowPicker(DateTimeField.END_DATE) },
        onTimeClick = { onShowPicker(DateTimeField.END_TIME) },
        dateTag = testTags.endDate,
        dateErrorTag = testTags.endDateError,
        timeTag = testTags.endTime,
    )
    Spacer(modifier = Modifier.height(14.dp))

    FestivarTextField(
        value = venue,
        onValueChange = onVenueChange,
        label = stringResource(R.string.create_event_field_venue),
        modifier = Modifier.testTag(testTags.venue),
    )
    Spacer(modifier = Modifier.height(8.dp))

    FestivarTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = stringResource(R.string.create_event_field_notes),
        singleLine = false,
        modifier = Modifier.testTag(testTags.notes),
    )
    Spacer(modifier = Modifier.height(14.dp))
  }

  FestivarDateTimePickerDialog(
      field = openPicker,
      range = dateTimeRange,
      onDatePicked = onDatePicked,
      onTimePicked = onTimePicked,
      onDismiss = { onShowPicker(null) },
  )
}
