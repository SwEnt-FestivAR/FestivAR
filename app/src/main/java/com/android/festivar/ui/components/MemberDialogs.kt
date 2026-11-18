// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.festivar.R
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme

/** Test tags of [MemberDialog] and [OptionPickerDialog]. */
object MemberDialogTestTags {
  const val MEMBER_DIALOG = "memberDialog"
  const val OPTION_PICKER_DIALOG = "optionPickerDialog"
  const val CLOSE_BUTTON = "memberDialogCloseButton"

  fun action(label: String) = "memberDialogAction_$label"

  fun option(index: Int) = "optionPickerOption_$index"
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
    DialogSurface(modifier.testTag(MemberDialogTestTags.MEMBER_DIALOG)) {
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
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
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
        CloseButton(onDismiss)
      }
      if (actions.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(top = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          actions.forEach { ActionButton(it, Modifier.weight(1f)) }
        }
      }
    }
  }
}

/**
 * A dialog to pick one of [options], as in the "Change role" frames of the design: a [title] with a
 * close button, then one row per option, drawn by [option]. Tapping a row calls [onPick], except
 * for the [current] one, marked with [currentLabel], and except while not [enabled].
 *
 * @param onDismiss Called by the close button or a tap outside the dialog.
 */
@Composable
fun <T> OptionPickerDialog(
    title: String,
    options: List<T>,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    current: T? = null,
    currentLabel: String = "",
    enabled: Boolean = true,
    option: @Composable (T) -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    DialogSurface(modifier.testTag(MemberDialogTestTags.OPTION_PICKER_DIALOG)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        CloseButton(onDismiss)
      }
      Column(
          modifier = Modifier.padding(top = 12.dp, end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        options.forEachIndexed { index, value ->
          val isCurrent = value == current
          val shape = RoundedCornerShape(12.dp)
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(shape)
                      .then(
                          if (isCurrent)
                              Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                          else Modifier.clickable(enabled = enabled) { onPick(value) }
                      )
                      .padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
                      .testTag(MemberDialogTestTags.option(index)),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(modifier = Modifier.weight(1f)) { option(value) }
            if (isCurrent) {
              Text(currentLabel, style = MaterialTheme.typography.labelMedium)
            } else {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DialogSurface(modifier: Modifier, content: @Composable () -> Unit) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 20.dp)) { content() }
  }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
  IconButton(
      onClick = onClick,
      modifier = Modifier.size(40.dp).testTag(MemberDialogTestTags.CLOSE_BUTTON),
  ) {
    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
  }
}

/** Filled with ink, or outlined in the error color if [MemberDialogAction.destructive]. */
@Composable
private fun ActionButton(action: MemberDialogAction, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Button(
      onClick = action.onClick,
      modifier = modifier.height(48.dp).testTag(MemberDialogTestTags.action(action.label)),
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
    Text(action.label, maxLines = 1)
  }
}

@Preview
@Composable
internal fun MemberDialogPreview() {
  AppTheme {
    MemberDialog(
        user = User("u1", name = "Yasser", surname = "Ameur"),
        onDismiss = {},
        badge = { StatusPill(label = "Staff", style = PillStyle.OUTLINED) },
        supportingText = "3 tasks",
        actions =
            listOf(
                MemberDialogAction("Change role", {}),
                MemberDialogAction("Kick", {}, destructive = true),
            ),
    )
  }
}
