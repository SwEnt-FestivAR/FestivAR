// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarPickerField
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.formatDateRange
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime

/** The default test tags of [FestivarDatesAndTimes], for a screen that has a single row. */
object DatesAndTimesTestTags {
  const val DATES_FIELD = "datesAndTimesDatesField"
  const val START_TIME_FIELD = "datesAndTimesStartTimeField"
  const val END_TIME_FIELD = "datesAndTimesEndTimeField"
  const val TIMES_ERROR = "datesAndTimesTimesError"
}

/**
 * The test tags of the fields of [FestivarDatesAndTimes]. Each screen passes its own, so that its
 * tests can tell the rows apart; the defaults are those of [DatesAndTimesTestTags].
 */
data class DatesAndTimesTags(
    val dates: String = DatesAndTimesTestTags.DATES_FIELD,
    val startTime: String = DatesAndTimesTestTags.START_TIME_FIELD,
    val endTime: String = DatesAndTimesTestTags.END_TIME_FIELD,
    val timesError: String = DatesAndTimesTestTags.TIMES_ERROR,
)

/** What the dates and times can open: the range picker, or one of its time pickers. */
private enum class DatesAndTimesPicker {
  DATES,
  START_TIME,
  END_TIME,
}

/**
 * The days and times of a form: one Dates field that opens the range picker, and the Start time and
 * End time fields side by side. The row adds no rule on the times: it shows [range] and what the
 * user picks, and it shows [timesError], which the form computes, under the times.
 *
 * The times wait for a day, so they are disabled until one is chosen, and saving the picker with no
 * day clears the times too.
 *
 * @param range The current days and times; the form owns it.
 * @param onRangeChange Called with the new range after a picker is confirmed.
 * @param modifier Applied to the whole row.
 * @param enabled Whether the fields react to taps; the forms turn it off while they are saving. The
 *   times also need a day, whatever [enabled] says.
 * @param timesError The error to show under the times, which also marks both time fields.
 * @param allowPast Whether days before [today] can be chosen, for the screens that edit an existing
 *   event or task.
 * @param today The first day that can be chosen unless [allowPast]; a parameter for the tests.
 * @param tags The test tags of the fields.
 */
@Composable
fun FestivarDatesAndTimes(
    range: DateTimeRange,
    onRangeChange: (DateTimeRange) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    timesError: FieldError? = null,
    allowPast: Boolean = false,
    today: LocalDate = LocalDate.now(),
    tags: DatesAndTimesTags = DatesAndTimesTags(),
) {
  var openPicker by rememberSaveable { mutableStateOf<DatesAndTimesPicker?>(null) }
  val join = stringResource(R.string.date_range_join)
  val timesEnabled = enabled && range.startDate != null

  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier.fillMaxWidth()) {
    FestivarPickerField(
        value = range.startDate?.let { formatDateRange(it, range.endDate ?: it, join) }.orEmpty(),
        label = stringResource(R.string.dates_and_times_dates),
        onClick = { openPicker = DatesAndTimesPicker.DATES },
        enabled = enabled,
        modifier = Modifier.testTag(tags.dates),
    )
    FestivarFieldWithError(timesError, tags.timesError) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FestivarPickerField(
            value = range.startTime?.let(::formatTime).orEmpty(),
            label = stringResource(R.string.dates_and_times_start_time),
            onClick = { openPicker = DatesAndTimesPicker.START_TIME },
            enabled = timesEnabled,
            isError = timesError != null,
            modifier = Modifier.weight(1f).testTag(tags.startTime),
        )
        FestivarPickerField(
            value = range.endTime?.let(::formatTime).orEmpty(),
            label = stringResource(R.string.dates_and_times_end_time),
            onClick = { openPicker = DatesAndTimesPicker.END_TIME },
            enabled = timesEnabled,
            isError = timesError != null,
            modifier = Modifier.weight(1f).testTag(tags.endTime),
        )
      }
    }
  }

  when (openPicker) {
    DatesAndTimesPicker.DATES ->
        FestivarDateRangePickerDialog(
            initialStart = range.startDate,
            initialEnd = range.endDate,
            onConfirm = { days ->
              onRangeChange(
                  if (days == null) DateTimeRange()
                  else range.copy(startDate = days.first, endDate = days.second)
              )
              openPicker = null
            },
            onDismiss = { openPicker = null },
            allowPast = allowPast,
            today = today,
        )
    DatesAndTimesPicker.START_TIME ->
        FestivarTimePickerDialog(
            initial = range.startTime,
            onConfirm = {
              onRangeChange(range.copy(startTime = it))
              openPicker = null
            },
            onDismiss = { openPicker = null },
        )
    DatesAndTimesPicker.END_TIME ->
        FestivarTimePickerDialog(
            initial = range.endTime,
            onConfirm = {
              onRangeChange(range.copy(endTime = it))
              openPicker = null
            },
            onDismiss = { openPicker = null },
        )
    null -> Unit
  }
}

private val PREVIEW_DAY: LocalDate = LocalDate.of(2026, 11, 21)

/**
 * The dates and times alone, on the background and with the side margin of a screen. The row can be
 * tapped, but [timesError] stays as given: the form, not the row, computes it.
 */
@Composable
private fun DatesAndTimesPreview(
    initial: DateTimeRange,
    timesError: FieldError? = null,
    enabled: Boolean = true,
) {
  var range by remember { mutableStateOf(initial) }
  AppTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
      Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
        FestivarDatesAndTimes(
            range = range,
            onRangeChange = { range = it },
            enabled = enabled,
            timesError = timesError,
        )
      }
    }
  }
}

@Preview(name = "Nothing chosen, times greyed", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesNothingChosenPreview() {
  DatesAndTimesPreview(DateTimeRange())
}

@Preview(name = "Day chosen, times empty", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesDayChosenPreview() {
  DatesAndTimesPreview(DateTimeRange(startDate = PREVIEW_DAY, endDate = PREVIEW_DAY))
}

@Preview(name = "One day with times", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesOneDayPreview() {
  DatesAndTimesPreview(
      DateTimeRange(PREVIEW_DAY, LocalTime.of(18, 0), PREVIEW_DAY, LocalTime.of(20, 0)),
  )
}

@Preview(name = "Range with times", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesRangePreview() {
  DatesAndTimesPreview(
      DateTimeRange(
          PREVIEW_DAY.minusDays(1),
          LocalTime.of(18, 0),
          PREVIEW_DAY.plusDays(1),
          LocalTime.of(2, 0),
      ),
  )
}

@Preview(name = "Error, ends before it starts", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesEndBeforeStartPreview() {
  DatesAndTimesPreview(
      DateTimeRange(PREVIEW_DAY, LocalTime.of(20, 0), PREVIEW_DAY, LocalTime.of(18, 0)),
      timesError = FieldError.END_BEFORE_START,
  )
}

@Preview(name = "Error, ends in the past", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesOutdatedPreview() {
  DatesAndTimesPreview(
      DateTimeRange(PREVIEW_DAY, LocalTime.of(9, 0), PREVIEW_DAY, LocalTime.of(10, 0)),
      timesError = FieldError.OUTDATED,
  )
}

@Preview(name = "Disabled, while saving", widthDp = 412)
@Composable
internal fun FestivarDatesAndTimesDisabledPreview() {
  DatesAndTimesPreview(
      DateTimeRange(PREVIEW_DAY, LocalTime.of(18, 0), PREVIEW_DAY, LocalTime.of(20, 0)),
      enabled = false,
  )
}
