// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/*
 * Generic building blocks with no voice note logic inside: only texts and callbacks. They are
 * meant to be promoted to ui/components when another screen needs them.
 */

/** A bar fixed to the bottom of a screen, above the system navigation bar, holding [content]. */
@Composable
fun FixedBottomBar(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
    Column(
        Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
  }
}

/**
 * A dark snackbar-like notice showing [message] with one action [actionLabel]. It is placed by the
 * caller instead of going through a snackbar host, so it stays visible until the state changes.
 */
@Composable
fun NoticeSnackbar(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
) {
  Snackbar(
      modifier = modifier,
      action = {
        TextButton(onClick = onAction, modifier = actionModifier) {
          Text(actionLabel, color = MaterialTheme.colorScheme.inversePrimary)
        }
      },
  ) {
    Text(message)
  }
}

/** An information note: [message] on a muted card, with an optional [action] under it. */
@Composable
fun InfoNote(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
  Surface(
      modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
          message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      action?.invoke()
    }
  }
}

/** A dialog asking to confirm an action: [dismissLabel] backs out, [confirmLabel] goes on. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmModifier: Modifier = Modifier,
    dismissModifier: Modifier = Modifier,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = modifier,
      title = { Text(title) },
      text = { Text(text) },
      confirmButton = {
        TextButton(onClick = onConfirm, modifier = confirmModifier) {
          Text(confirmLabel, color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss, modifier = dismissModifier) { Text(dismissLabel) }
      },
  )
}
