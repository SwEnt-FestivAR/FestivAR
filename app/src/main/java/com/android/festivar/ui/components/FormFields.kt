// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

private val FieldShape = RoundedCornerShape(12.dp)

/**
 * Colors of the fields. Material draws the focused border, label and cursor in `primary`, the lime
 * of the app, which is too flashy and hard to read on the light background. They use the dark green
 * of the theme instead; the other states keep the Material defaults.
 */
@Composable
private fun festivarFieldColors(): TextFieldColors {
  val accent = MaterialTheme.colorScheme.onPrimaryContainer
  return OutlinedTextFieldDefaults.colors(
      focusedBorderColor = accent,
      focusedLabelColor = accent,
      cursorColor = accent,
  )
}

/**
 * Outlined text field in the FestivAR style, with its label sitting on the border.
 *
 * Colors and text style come from the app theme. Pass a test tag through [modifier]. Show an error
 * message under the field with [FestivarFieldError], and set [isError] to draw the field as
 * invalid.
 *
 * @param keyboardOptions The keyboard to show, e.g. `KeyboardOptions(keyboardType =
 *   KeyboardType.Email)`.
 * @param visualTransformation How the text is displayed, e.g. [PasswordVisualTransformation] to
 *   hide it.
 * @param trailingIcon Content at the end of the field, e.g. a show/hide password button.
 */
@Composable
fun FestivarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      singleLine = singleLine,
      isError = isError,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      visualTransformation = visualTransformation,
      trailingIcon = trailingIcon,
      colors = festivarFieldColors(),
      shape = FieldShape,
  )
}

/**
 * Field that looks like [FestivarTextField] but is not typed into: tapping it calls [onClick], to
 * open a picker. It shows [value] (empty while nothing is picked).
 */
@Composable
fun FestivarPickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  val interactionSource = remember { MutableInteractionSource() }
  // A read-only text field has no click callback, but it still reports the press on its source.
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if (interaction is PressInteraction.Release) currentOnClick()
    }
  }

  OutlinedTextField(
      value = value,
      onValueChange = {},
      modifier = modifier.fillMaxWidth(),
      readOnly = true,
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      singleLine = true,
      isError = isError,
      colors = festivarFieldColors(),
      shape = FieldShape,
      interactionSource = interactionSource,
  )
}

/**
 * Supporting line shown under a field that has an error, as in the Figma "Supporting text": Body
 * small in the theme error color, inset like the field's own text.
 */
@Composable
fun FestivarFieldError(message: String, modifier: Modifier = Modifier) {
  Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.error,
      modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
  )
}

/** What is wrong with a field of a form, shown as a message under that field. */
enum class FieldError {
  /** The title was edited and is now blank. */
  EMPTY_TITLE,

  /** A time was picked without its date. */
  MISSING_DATE,

  /** The end is before the start. */
  END_BEFORE_START,
}

/** The string resource of the message shown under a field for [error]. */
@StringRes
fun fieldErrorMessageRes(error: FieldError): Int =
    when (error) {
      FieldError.EMPTY_TITLE -> R.string.field_error_empty_title
      FieldError.MISSING_DATE -> R.string.field_error_missing_date
      FieldError.END_BEFORE_START -> R.string.field_error_end_before_start
    }

/**
 * Shows [field] with the message for [error] under it, when there is one.
 *
 * @param errorTag Test tag of the error message.
 */
@Composable
fun FestivarFieldWithError(
    error: FieldError?,
    errorTag: String,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
  Column(modifier = modifier) {
    field()
    error?.let {
      FestivarFieldError(
          message = stringResource(fieldErrorMessageRes(it)),
          modifier = Modifier.testTag(errorTag),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun FormFieldsPreview() {
  AppTheme {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
      FestivarTextField(value = "", onValueChange = {}, label = "Title")
      FestivarTextField(value = "Run power to stage", onValueChange = {}, label = "Title")
      FestivarTextField(
          value = "secret",
          onValueChange = {},
          label = "Password",
          visualTransformation = PasswordVisualTransformation(),
      )
      Column {
        FestivarTextField(value = "", onValueChange = {}, label = "Title", isError = true)
        FestivarFieldError(message = "Title cannot be empty")
      }
      FestivarPickerField(value = "Sat 17 Oct", label = "Start date", onClick = {})
    }
  }
}
