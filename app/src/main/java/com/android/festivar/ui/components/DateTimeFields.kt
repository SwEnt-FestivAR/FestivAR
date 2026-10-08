// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime

/**
 * Field that shows a date and opens a [FestivarDatePickerDialog] when tapped. It keeps track of
 * whether the dialog is open, so a form only has to store the date.
 *
 * @param value The date shown in the field, or `null` while nothing is picked.
 * @param label The label of the field.
 * @param onValueChange Called with the date the user confirmed.
 * @param isError Draws the field as invalid. Show the message with [FestivarFieldError].
 * @param initialWhenEmpty The date selected when the dialog opens while [value] is `null`, e.g. the
 *   start date for an end date field.
 */
@Composable
fun FestivarDateField(
    value: LocalDate?,
    label: String,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    initialWhenEmpty: LocalDate? = null,
) {
  var showDialog by rememberSaveable { mutableStateOf(false) }

  FestivarPickerField(
      value = value?.let(::formatDate).orEmpty(),
      label = label,
      onClick = { showDialog = true },
      modifier = modifier,
      isError = isError,
  )
  if (showDialog) {
    FestivarDatePickerDialog(
        initial = value ?: initialWhenEmpty,
        onConfirm = {
          onValueChange(it)
          showDialog = false
        },
        onDismiss = { showDialog = false },
    )
  }
}

/**
 * Field that shows a time and opens a [FestivarTimePickerDialog] when tapped. It keeps track of
 * whether the dialog is open, so a form only has to store the time.
 *
 * @param value The time shown in the field, or `null` while nothing is picked.
 * @param label The label of the field.
 * @param onValueChange Called with the time the user confirmed.
 * @param isError Draws the field as invalid. Show the message with [FestivarFieldError].
 * @param initialWhenEmpty The time selected when the dialog opens while [value] is `null`. Without
 *   it the dialog proposes 09:00.
 */
@Composable
fun FestivarTimeField(
    value: LocalTime?,
    label: String,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    initialWhenEmpty: LocalTime? = null,
) {
  var showDialog by rememberSaveable { mutableStateOf(false) }

  FestivarPickerField(
      value = value?.let(::formatTime).orEmpty(),
      label = label,
      onClick = { showDialog = true },
      modifier = modifier,
      isError = isError,
  )
  if (showDialog) {
    FestivarTimePickerDialog(
        initial = value ?: initialWhenEmpty,
        onConfirm = {
          onValueChange(it)
          showDialog = false
        },
        onDismiss = { showDialog = false },
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun DateTimeFieldsPreview() {
  AppTheme {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
      FestivarDateField(value = LocalDate.of(2026, 10, 17), label = "Start date", onValueChange = {})
      FestivarTimeField(value = LocalTime.of(9, 30), label = "Start time", onValueChange = {})
    }
  }
}
