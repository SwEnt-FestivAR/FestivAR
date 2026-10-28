// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.ui.theme.AppTheme

/**
 * Dialog that asks the user to confirm an action.
 *
 * @param title What is asked, e.g. "Discard this task?".
 * @param message What happens if the user confirms.
 * @param confirmText Label of the button that does the action.
 * @param dismissText Label of the button that keeps things as they are.
 * @param onConfirm Called when the user confirms.
 * @param onDismiss Called when the user cancels or taps outside the dialog.
 */
@Composable
fun FestivarConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(title) },
      text = { Text(message) },
      confirmButton = { DialogButton(text = confirmText, onClick = onConfirm) },
      dismissButton = { DialogButton(text = dismissText, onClick = onDismiss) },
      modifier = modifier,
  )
}

@Preview
@Composable
internal fun FestivarConfirmDialogPreview() {
  AppTheme {
    FestivarConfirmDialog(
        title = "Discard this task?",
        message = "What you typed will be lost.",
        confirmText = "Discard",
        dismissText = "Keep editing",
        onConfirm = {},
        onDismiss = {},
    )
  }
}
