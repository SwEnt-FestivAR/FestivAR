// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

/*
 * Generic building blocks with no voice note logic inside: only texts and callbacks. They are
 * meant to be promoted to ui/components when another screen needs them.
 */

private val ButtonHeight = 52.dp

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

/**
 * An information note: [message] on a muted card with an info icon, and an optional [action] under
 * it (a full width button, as in the design).
 */
@Composable
fun InfoNote(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      Row(
          Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            message,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    action?.invoke()
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
      shape = MaterialTheme.shapes.extraLarge,
      title = { Text(title) },
      text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
      confirmButton = {
        TextButton(onClick = onConfirm, modifier = confirmModifier.height(ButtonHeight)) {
          Text(confirmLabel, color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss, modifier = dismissModifier.height(ButtonHeight)) {
          Text(dismissLabel, color = MaterialTheme.colorScheme.onSurface)
        }
      },
  )
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

@Preview(showBackground = true)
@Composable
internal fun InfoNotePreview() {
  AppTheme {
    InfoNote(
        "Microphone is off for FestivAR. Turn it on in Settings to record a note.",
        action = {
          OutlinedButton(
              onClick = {},
              modifier = Modifier.fillMaxWidth().height(ButtonHeight),
              shape = MaterialTheme.shapes.medium,
          ) {
            Text("Open settings")
          }
        },
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun ConfirmDialogPreview() {
  AppTheme {
    ConfirmDialog(
        title = "Delete this voice note?",
        text = "The volunteers on this task will no longer be able to hear it.",
        confirmLabel = "Delete",
        dismissLabel = "Keep it",
        onConfirm = {},
        onDismiss = {},
    )
  }
}
