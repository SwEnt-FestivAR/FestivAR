// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.festivar.ui.theme.AppTheme

private val StepperShape = RoundedCornerShape(12.dp)

/**
 * Number stepper in the FestivAR style, as in the Figma "Stepper": the same outlined surface and
 * floating label as [FestivarTextField], with the number between a minus and a plus button. The
 * number can only change by one at a time, so it is never empty or invalid, and it never goes below
 * [minValue]: the minus button is disabled there.
 *
 * Pass a test tag through [modifier]; its semantics carry the [value] as state description. The
 * buttons take their own tags through [decreaseModifier] and [increaseModifier].
 *
 * @param value The current number.
 * @param label The label sitting on the border, e.g. "People needed".
 * @param onDecrease Called when the minus button is tapped. It is never called at [minValue].
 * @param onIncrease Called when the plus button is tapped.
 * @param decreaseDescription Accessibility description of the minus button.
 * @param increaseDescription Accessibility description of the plus button.
 * @param minValue The lowest number, where the minus button is disabled.
 */
@Composable
fun FestivarStepper(
    value: Int,
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    decreaseModifier: Modifier = Modifier,
    increaseModifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Box(
      modifier =
          modifier.fillMaxWidth().semantics {
            contentDescription = label
            stateDescription = value.toString()
          }
  ) {
    Surface(
        shape = StepperShape,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp),
      ) {
        StepperButton(
            icon = Icons.Rounded.Remove,
            description = decreaseDescription,
            enabled = value > minValue,
            onClick = onDecrease,
            modifier = decreaseModifier,
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepperButton(
            icon = Icons.Rounded.Add,
            description = increaseDescription,
            enabled = true,
            onClick = onIncrease,
            modifier = increaseModifier,
        )
      }
    }
    // The label sits on the border, on the surface color so that it cuts the line.
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        color = colors.onSurfaceVariant,
        modifier =
            Modifier.offset(x = 11.dp, y = (-9).dp)
                .background(colors.surfaceContainerLowest)
                .padding(horizontal = 4.dp),
    )
  }
}

/**
 * Tonal round button of the stepper: 32dp, with a 20dp icon. When disabled it takes Material's own
 * disabled colors.
 */
@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  FilledTonalIconButton(
      onClick = onClick,
      enabled = enabled,
      shape = CircleShape,
      colors =
          IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = colors.surfaceContainerHigh,
              contentColor = colors.onSurface,
          ),
      modifier = modifier.size(32.dp),
  ) {
    Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
  }
}

@Preview(showBackground = true)
@Composable
internal fun FestivarStepperPreview() {
  AppTheme {
    Box(modifier = Modifier.padding(20.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FestivarStepper(
            value = 3,
            label = "People needed",
            onDecrease = {},
            onIncrease = {},
            decreaseDescription = "Decrease",
            increaseDescription = "Increase",
        )
        FestivarStepper(
            value = 1,
            label = "People needed",
            onDecrease = {},
            onIncrease = {},
            decreaseDescription = "Decrease",
            increaseDescription = "Increase",
        )
      }
    }
  }
}
