// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

object EmptyStateTestTags {
  const val TITLE = "emptyStateTitle"
}

/**
 * Empty state of a screen with nothing to show yet: a large [icon], a [title] and a [subtitle],
 * then optional [actions] sharing the full width, e.g. "Join" and "Create" buttons.
 *
 * @param icon The drawable shown above the title.
 * @param title The headline, e.g. "No events yet".
 * @param subtitle The explanation below the title, e.g. "Join or create an event.".
 * @param modifier The modifier of the whole state.
 * @param actions The buttons below the texts, in a [Row] filling the width; none if null.
 */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
  ) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(56.dp),
    )
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(EmptyStateTestTags.TITLE),
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (actions != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
          content = actions,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun EmptyStatePreview() {
  AppTheme {
    EmptyState(
        icon = R.drawable.ic_event,
        title = "No events yet",
        subtitle = "Join or create an event.",
    ) {
      Button(
          onClick = {},
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(52.dp),
      ) {
        Text(text = "Create")
      }
    }
  }
}
