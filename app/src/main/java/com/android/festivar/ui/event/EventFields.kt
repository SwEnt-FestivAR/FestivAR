package com.android.festivar.ui.event

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.LONG_TEXT_MAX_LENGTH
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.DatesAndTimesFieldTags
import com.android.festivar.ui.components.datetime.FestivarDatesAndTimes

/**
 * Identifies the event fields and validation messages rendered by [EventFields].
 *
 * Each screen supplies tags unique to its own form.
 */
data class EventFieldsTestTags(
    val name: String,
    val dates: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val notes: String,
    val nameError: String,
    val timesError: String,
)

/**
 * Renders the shared title, dates and times, venue, and notes fields.
 *
 * The caller owns the form state, supplies screen-specific [testTags], and handles the callbacks.
 * [timesError] is shown under the times; [allowPast] lets the dates picker choose days that are
 * over, for the screen that edits an existing event.
 */
@Composable
fun EventFields(
    name: String,
    nameError: FieldError?,
    range: DateTimeRange,
    timesError: FieldError?,
    venue: String,
    notes: String,
    allowPast: Boolean,
    enabled: Boolean = true,
    testTags: EventFieldsTestTags,
    onNameChange: (String) -> Unit,
    onRangeChange: (DateTimeRange) -> Unit,
    onVenueChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
) {
  Column {
    FestivarFieldWithError(nameError, testTags.nameError) {
      FestivarTextField(
          value = name,
          onValueChange = onNameChange,
          label = stringResource(R.string.create_event_field_name),
          enabled = enabled,
          isError = nameError != null,
          modifier = Modifier.testTag(testTags.name),
      )
    }
    Spacer(modifier = Modifier.height(14.dp))

    FestivarDatesAndTimes(
        range = range,
        onRangeChange = onRangeChange,
        enabled = enabled,
        timesError = timesError,
        allowPast = allowPast,
        tags =
            DatesAndTimesFieldTags(
                dates = testTags.dates,
                startTime = testTags.startTime,
                endTime = testTags.endTime,
                timesError = testTags.timesError,
            ),
    )
    Spacer(modifier = Modifier.height(14.dp))

    FestivarTextField(
        value = venue,
        onValueChange = onVenueChange,
        label = stringResource(R.string.create_event_field_venue),
        enabled = enabled,
        modifier = Modifier.testTag(testTags.venue),
    )
    Spacer(modifier = Modifier.height(8.dp))

    FestivarTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = stringResource(R.string.create_event_field_notes),
        singleLine = false,
        maxLength = LONG_TEXT_MAX_LENGTH,
        showCounter = true,
        enabled = enabled,
        modifier = Modifier.testTag(testTags.notes),
    )
    Spacer(modifier = Modifier.height(14.dp))
  }
}
