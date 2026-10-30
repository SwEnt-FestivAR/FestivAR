// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
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
 * @param confirmModifier Modifier of the confirm button, e.g. to give it a test tag.
 * @param dismissModifier Modifier of the dismiss button, e.g. to give it a test tag.
 * @param destructive Whether the confirm button does something that cannot be undone. It is then
 *   drawn in the error color, as in the Figma dialogs; the dismiss button keeps the text color.
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
    confirmModifier: Modifier = Modifier,
    dismissModifier: Modifier = Modifier,
    destructive: Boolean = true,
) {
  val colors = MaterialTheme.colorScheme
  AlertDialog(
      onDismissRequest = onDismiss,
      shape = RoundedCornerShape(20.dp),
      title = { Text(title) },
      text = { Text(message) },
      confirmButton = {
        DialogButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = confirmModifier,
            contentColor = if (destructive) colors.error else colors.onPrimaryContainer,
        )
      },
      dismissButton = {
        DialogButton(
            text = dismissText,
            onClick = onDismiss,
            modifier = dismissModifier,
            contentColor = colors.onSurface,
        )
      },
      modifier = modifier,
  )
}

/**
 * Dialog that asks whether to throw away what the user typed, for any screen with a form.
 *
 * It is a [FestivarConfirmDialog] with the shared "Discard" and "Keep editing" buttons.
 *
 * @param title What is asked, e.g. "Discard this task?".
 * @param onConfirm Called when the user discards.
 * @param onDismiss Called when the user keeps editing or taps outside the dialog.
 * @param message What happens if the user discards.
 * @param confirmModifier Modifier of the "Discard" button, e.g. to give it a test tag.
 * @param dismissModifier Modifier of the "Keep editing" button, e.g. to give it a test tag.
 */
@Composable
fun FestivarDiscardDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.discard_message),
    confirmModifier: Modifier = Modifier,
    dismissModifier: Modifier = Modifier,
) {
  FestivarConfirmDialog(
      title = title,
      message = message,
      confirmText = stringResource(R.string.discard_confirm),
      dismissText = stringResource(R.string.discard_keep),
      onConfirm = onConfirm,
      onDismiss = onDismiss,
      modifier = modifier,
      confirmModifier = confirmModifier,
      dismissModifier = dismissModifier,
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
