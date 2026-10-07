// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

/** Look of a [StatusPill], from the status pills of the design. */
enum class PillStyle {
  /** White with a thin border, e.g. "Closed". */
  OUTLINED,
  /** Light lime, e.g. "7 tasks open". */
  LIME,
  /** Ink with light text, e.g. "Ongoing". */
  INK,
}

/** Status pill of the design, showing [label] with the given [style]. */
@Composable
fun StatusPill(label: String, style: PillStyle) {
  val colors = MaterialTheme.colorScheme
  val (container, content) =
      when (style) {
        PillStyle.OUTLINED -> colors.surfaceContainerLowest to colors.onSurface
        PillStyle.LIME -> colors.primaryContainer to colors.onPrimaryContainer
        PillStyle.INK -> colors.secondaryContainer to colors.onSecondaryContainer
      }
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          Modifier.height(24.dp)
              .background(container, CircleShape)
              .then(
                  if (style == PillStyle.OUTLINED)
                      Modifier.border(1.dp, colors.outlineVariant, CircleShape)
                  else Modifier
              )
              .padding(horizontal = 8.dp),
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun StatusPillPreview() {
  AppTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
      StatusPill(label = "Closed", style = PillStyle.OUTLINED)
      StatusPill(label = "7 tasks open", style = PillStyle.LIME)
      StatusPill(label = "Ongoing", style = PillStyle.INK)
    }
  }
}
