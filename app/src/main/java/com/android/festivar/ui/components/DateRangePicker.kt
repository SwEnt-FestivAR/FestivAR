// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate

/** Test tags of [FestivarDateRangePickerDialog]. */
object DateRangePickerTestTags {
  const val BACK_BUTTON = "dateRangePickerBackButton"
  const val TITLE = "dateRangePickerTitle"
  const val SUMMARY = "dateRangePickerSummary"
  const val CALENDAR = "dateRangePickerCalendar"
  const val SAVE_BUTTON = "dateRangePickerSaveButton"
  const val CLEAR_BUTTON = "dateRangePickerClearButton"
}

/** The opacity of a disabled element, as in the design. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Full-screen dialog to pick a start and an end day in one calendar. One tap picks a single day
 * (the end is the start), a second tap on a later day sets the end, a tap before the start begins a
 * new range.
 *
 * @param initialStart The first day selected when the dialog opens, or `null` for none. The
 *   calendar opens on its month, or on today's.
 * @param initialEnd The last day selected when the dialog opens: the start when `null` or before
 *   [initialStart]. Ignored without [initialStart].
 * @param onConfirm Called with the first and the last day when the user saves. Never called while
 *   no day is selected.
 * @param onDismiss Called when the user goes back, which discards the choice.
 * @param today The month shown while nothing is selected. The ring around today is drawn by
 *   Material from the system date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivarDateRangePickerDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
  val first = initialStart
  val last = first?.let { day -> initialEnd?.takeIf { !it.isBefore(day) } ?: day }
  val state =
      rememberDateRangePickerState(
          initialSelectedStartDateMillis = first?.toPickerMillis(),
          initialSelectedEndDateMillis = last?.toPickerMillis(),
          initialDisplayedMonthMillis = (first ?: today).toPickerMillis(),
      )
  val start = state.selectedStartDateMillis?.toPickerDate()
  // The calendar leaves the end empty after one tap; for the form that day is also the end.
  val end = state.selectedEndDateMillis?.toPickerDate() ?: start

  Dialog(
      onDismissRequest = onDismiss,
      properties =
          DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Column(Modifier.statusBarsPadding()) {
        DateRangePickerTopBar(
            showClear = start != null,
            onBack = onDismiss,
            onClear = { state.setSelection(null, null) },
        )
        DateRangeCalendar(state, Modifier.weight(1f))
        FixedBottomBar {
          DateRangeSaveButton(enabled = start != null) {
            if (start != null && end != null) onConfirm(start, end)
          }
        }
      }
    }
  }
}

@Composable
private fun DateRangePickerTopBar(showClear: Boolean, onBack: () -> Unit, onClear: () -> Unit) {
  Row(
      Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    IconButton(onClick = onBack, Modifier.testTag(DateRangePickerTestTags.BACK_BUTTON)) {
      Icon(
          Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = stringResource(R.string.date_range_picker_back),
      )
    }
    Text(
        stringResource(R.string.date_range_picker_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.weight(1f).testTag(DateRangePickerTestTags.TITLE),
    )
    if (showClear) {
      TextButton(
          onClick = onClear,
          colors =
              ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
          modifier = Modifier.testTag(DateRangePickerTestTags.CLEAR_BUTTON),
      ) {
        Text(
            stringResource(R.string.date_range_picker_clear),
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

/** The calendar in a white card. Today is ringed in the error colour, never filled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeCalendar(state: DateRangePickerState, modifier: Modifier) {
  val scheme = MaterialTheme.colorScheme
  Surface(
      modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
      shape = RoundedCornerShape(20.dp),
      color = scheme.surfaceBright,
      border = BorderStroke(1.dp, scheme.outlineVariant),
  ) {
    DateRangePickerCalendar(state, Modifier)
  }
}

/** Material's range picker without its own header, in the colours of the design. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerCalendar(state: DateRangePickerState, modifier: Modifier) {
  DateRangePicker(
      state = state,
      modifier = modifier.testTag(DateRangePickerTestTags.CALENDAR),
      title = null,
      headline = null,
      showModeToggle = false,
      colors = calendarColors(),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun calendarColors(): DatePickerColors {
  val scheme = MaterialTheme.colorScheme
  return DatePickerDefaults.colors(
      containerColor = scheme.surfaceBright,
      todayContentColor = scheme.error,
      todayDateBorderColor = scheme.error,
      selectedDayContainerColor = scheme.primary,
      selectedDayContentColor = scheme.onPrimary,
      dayInSelectionRangeContainerColor = scheme.primaryContainer,
      dayInSelectionRangeContentColor = scheme.onSurface,
      disabledDayContentColor = scheme.onSurface.copy(alpha = DISABLED_ALPHA),
      disabledSelectedDayContentColor = scheme.onSurface.copy(alpha = DISABLED_ALPHA),
  )
}

/** The lime button that saves the days; it fades while no day is chosen. */
@Composable
private fun DateRangeSaveButton(enabled: Boolean, onClick: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  Button(
      onClick = onClick,
      enabled = enabled,
      shape = RoundedCornerShape(12.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = scheme.primary,
              contentColor = scheme.onPrimary,
              disabledContainerColor = scheme.primary.copy(alpha = DISABLED_ALPHA),
              disabledContentColor = scheme.onPrimary.copy(alpha = DISABLED_ALPHA),
          ),
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag(DateRangePickerTestTags.SAVE_BUTTON),
  ) {
    Text(
        stringResource(R.string.date_range_picker_save),
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Preview
@Composable
internal fun FestivarDateRangePickerDialogPreview() {
  AppTheme {
    FestivarDateRangePickerDialog(
        initialStart = LocalDate.now().plusDays(9),
        initialEnd = LocalDate.now().plusDays(15),
        onConfirm = { _, _ -> },
        onDismiss = {},
    )
  }
}
