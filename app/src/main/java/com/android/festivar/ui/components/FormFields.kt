// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

private val FieldShape = RoundedCornerShape(12.dp)

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
      visualTransformation = visualTransformation,
      trailingIcon = trailingIcon,
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

@Preview(showBackground = true)
@Composable
private fun FormFieldsPreview() {
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
