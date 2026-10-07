// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val FieldShape = RoundedCornerShape(12.dp)

/**
 * Outlined text field in the FestivAR style, with its label sitting on the border.
 *
 * Colors and text style come from the app theme. Pass a [Modifier.testTag] through [modifier].
 */
@Composable
fun FestivarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      singleLine = singleLine,
      minLines = minLines,
      shape = FieldShape,
  )
}

/**
 * Field that looks like [FestivarTextField] but is not typed into: tapping it calls [onClick], to
 * open a picker. It shows [value] (empty while nothing is picked).
 *
 * @param onClickLabel Spoken by screen readers for the tap action, e.g. "Pick start date".
 */
@Composable
fun FestivarPickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        shape = FieldShape,
    )
    // A read-only text field does not report taps, so a transparent layer on top does.
    Box(
        modifier =
            Modifier.matchParentSize()
                .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    )
  }
}
