// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

/**
 * A bar fixed to the bottom of a screen, above the system navigation bar, holding [content]. It
 * keeps the colour of the page, as the "composer (fixed)" frames of the S3 design do: a stack of
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

@Preview(showBackground = true)
@Composable
internal fun FixedBottomBarPreview() {
  AppTheme {
    FixedBottomBar {
      Text("Hold to record")
      Text("Give it back")
    }
  }
}
