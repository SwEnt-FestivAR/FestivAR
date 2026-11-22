// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R

/** The rounded card of the custom dialogs, e.g. [MemberDialog] and [OptionPickerDialog]. */
@Composable
internal fun DialogSurface(modifier: Modifier, content: @Composable () -> Unit) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 20.dp)) { content() }
  }
}

/** The close button at the top right of the custom dialogs. */
@Composable
internal fun DialogCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
  }
}

/**
 * Text button of the dialogs. Material draws these in `primary`, which is the lime of the app and
 * cannot be read on the dialog background, so the dark green of the theme is used instead.
 *
 * @param text Label of the button.
 * @param onClick Called when the button is tapped.
 * @param modifier Modifier of the button.
 * @param enabled Whether the button can be tapped.
 * @param contentColor Color of the label; the dark green of the theme by default.
 */
@Composable
internal fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
  TextButton(
      onClick = onClick,
      modifier = modifier,
      enabled = enabled,
      colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
  ) {
    Text(text)
  }
}
