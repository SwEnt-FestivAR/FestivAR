// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
