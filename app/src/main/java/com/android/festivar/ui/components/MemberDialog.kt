// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.festivar.model.user.User

/** Test tags of [MemberDialog]. */
object MemberDialogTestTags {
  const val DIALOG = "memberDialog"
  const val CLOSE_BUTTON = "memberDialogCloseButton"

  fun action(label: String) = "memberDialogAction_$label"
}

/**
 * A button of a [MemberDialog]. A [destructive] one, e.g. removing the member, is drawn in the
 * error color; a disabled one, e.g. while a change is being saved, cannot be tapped.
 */
data class MemberDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * A dialog showing one member, as in the "Member dialog" frames of the design: the avatar, the
 * name, the [badge] and [supportingText] under it, a close button, then the [actions] side by side.
 *
 * @param onDismiss Called by the close button or a tap outside the dialog.
 */
@Composable
fun MemberDialog(
    user: User,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    actions: List<MemberDialogAction> = emptyList(),
) {
  Dialog(onDismissRequest = onDismiss) {
    DialogSurface(modifier.testTag(MemberDialogTestTags.DIALOG)) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(user = user, modifier = Modifier.size(56.dp))
        Column(
            modifier = Modifier.weight(1f).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
              text = "${user.name} ${user.surname}".trim(),
              style = MaterialTheme.typography.titleLarge,
          )
          FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
              itemVerticalAlignment = Alignment.CenterVertically,
          ) {
            badge?.invoke()
            supportingText?.let {
              Text(
                  text = it,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        DialogCloseButton(onDismiss, Modifier.testTag(MemberDialogTestTags.CLOSE_BUTTON))
      }
      if (actions.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(top = 16.dp, end = 8.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          actions.forEach { ActionButton(it, Modifier.weight(1f).fillMaxHeight()) }
        }
      }
    }
  }
}

/**
 * Filled with ink, or outlined in the error color if [MemberDialogAction.destructive]. A label too
 * long for one line goes on two, then ends with "…".
 */
@Composable
private fun ActionButton(action: MemberDialogAction, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Button(
      onClick = action.onClick,
      modifier = modifier.heightIn(min = 48.dp).testTag(MemberDialogTestTags.action(action.label)),
      enabled = action.enabled,
      shape = RoundedCornerShape(12.dp),
      border = if (action.destructive) BorderStroke(1.dp, colors.outlineVariant) else null,
      colors =
          if (action.destructive)
              ButtonDefaults.buttonColors(
                  containerColor = colors.surface,
                  contentColor = colors.error,
              )
          else
              ButtonDefaults.buttonColors(
                  containerColor = colors.inverseSurface,
                  contentColor = colors.inverseOnSurface,
              ),
  ) {
    Text(
        text = action.label,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
    )
  }
}
