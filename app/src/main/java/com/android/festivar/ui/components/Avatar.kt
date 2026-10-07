// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

/** Round avatar of the design, showing the given [text]. */
@Composable
fun Avatar(text: String) {
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          Modifier.size(32.dp)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
              .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
  ) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun AvatarPreview() {
  AppTheme { Avatar(text = "AK") }
}
