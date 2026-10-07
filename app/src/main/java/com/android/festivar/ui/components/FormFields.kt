// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FieldShape = RoundedCornerShape(12.dp)

/**
 * Outlined text field in the FestivAR style, with its label sitting on the border.
 *
 * Colors and text style come from the app theme. Pass a test tag through [modifier].
 */
@Composable
fun FestivarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      singleLine = singleLine,
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
      shape = FieldShape,
      interactionSource = interactionSource,
  )
}
