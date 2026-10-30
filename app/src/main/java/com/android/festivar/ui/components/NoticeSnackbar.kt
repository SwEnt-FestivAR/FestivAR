// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

/**
 * A dark snackbar-like notice showing [message] with one action [actionLabel]. It is placed by the
 * caller instead of going through a snackbar host, so it stays visible until the state changes, and
 * can sit in a stack of other content. It looks like the error snackbar of the sign up screen.
 */
@Composable
fun NoticeSnackbar(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
) {
  Surface(
      modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.inverseSurface,
      contentColor = MaterialTheme.colorScheme.inverseOnSurface,
  ) {
    Row(
        Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(painterResource(R.drawable.ic_pennant), contentDescription = null, Modifier.size(20.dp))
      Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
      TextButton(onClick = onAction, modifier = actionModifier) {
        Text(actionLabel, color = MaterialTheme.colorScheme.inversePrimary)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun NoticeSnackbarPreview() {
  AppTheme { NoticeSnackbar("Voice note not sent. Check your connection.", "Retry", onAction = {}) }
}
