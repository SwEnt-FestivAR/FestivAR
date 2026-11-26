// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

private val BORDER_WIDTH = 1.dp
private val DASH_LENGTH = 6.dp
private val DASH_GAP = 4.dp

/** Test tags of [EmptyStateCard]. */
object EmptyStateCardTestTags {
  const val CARD = "empty_state_card"
  const val TITLE = "empty_state_card_title"
  const val SUBTITLE = "empty_state_card_subtitle"
  const val ACTION = "empty_state_card_action"
}

/**
 * A dashed card that stands in for something that does not exist yet, with a button to create it.
 * Each screen gives it its own texts and action, so that every empty spot of the app looks the
 * same.
 *
 * It takes the size of its [modifier]; by default it is as tall as its content.
 *
 * @param title What is missing, e.g. "Not on the map yet".
 * @param subtitle Why it is worth adding.
 * @param actionLabel The label of the button, e.g. "Pin it".
 * @param onAction Called when the button is clicked.
 */
@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val shape = MaterialTheme.shapes.large
  val dashColor = MaterialTheme.colorScheme.outlineVariant
  Row(
      modifier =
          modifier
              .testTag(EmptyStateCardTestTags.CARD)
              .clip(shape)
              .background(MaterialTheme.colorScheme.surfaceContainerLow)
              .drawBehind {
                // The card clips what is drawn outside of it, so only the inner half of the line
                // shows: it is drawn twice as thick as the border it must look like.
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = dashColor,
                    style =
                        Stroke(
                            width = 2 * BORDER_WIDTH.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(DASH_LENGTH.toPx(), DASH_GAP.toPx())
                                ),
                        ),
                )
              }
              .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag(EmptyStateCardTestTags.TITLE),
      )
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(EmptyStateCardTestTags.SUBTITLE),
      )
    }
    Button(
        onClick = onAction,
        modifier = Modifier.height(52.dp).testTag(EmptyStateCardTestTags.ACTION),
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
      Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
    }
  }
}

/** Preview of the empty state card, with the wording of the task screen. */
@Preview(showBackground = true)
@Composable
internal fun EmptyStateCardPreview() {
  AppTheme {
    EmptyStateCard(
        title = "Not on the map yet",
        subtitle = "Volunteers find a pinned task faster.",
        actionLabel = "Pin it",
        onAction = {},
        modifier = Modifier.fillMaxWidth().padding(20.dp),
    )
  }
}
