// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

/**
 * A bar fixed to the bottom of a screen, above the system navigation bar, holding [content]. It
 * keeps the colour of the page, as the "composer (fixed)" frames of the S4 design do: a stack of
 * the record bar and the notices above it.
 */
@Composable
fun FixedBottomBar(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
    Column(
        Modifier.navigationBarsPadding()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
  }
}

/** The "Voice notes, organizer, recording" frame of S4: the hint above the recording bar. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
internal fun FixedBottomBarPreview() {
  AppTheme {
    Scaffold(
        bottomBar = {
          FixedBottomBar {
            Text(
                "Release to send. Slide left to cancel.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                Modifier.fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                  "Recording 0:03",
                  Modifier.weight(1f),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text("Release to send", style = MaterialTheme.typography.labelLarge)
              Box(
                  Modifier.padding(start = 12.dp)
                      .size(10.dp)
                      .background(MaterialTheme.colorScheme.error, CircleShape)
              )
            }
          }
        }
    ) { padding ->
      Text("Run power to stage", Modifier.padding(padding).padding(20.dp))
    }
  }
}
