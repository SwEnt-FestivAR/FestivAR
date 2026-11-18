// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

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
import androidx.compose.material3.OutlinedButton
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
  const val MEMBER_NAME = "memberDialogName"

  /** The tag of the button of the action labeled [label]. */
  fun action(label: String) = "memberDialogAction_$label"

  /** The tag of the row of the option at [index] in the list. */
  fun option(index: Int) = "optionPickerOption_$index"
}

/**
 * A button of a [MemberDialog].
 *
 * @property label The text of the button.
 * @property onClick Called when the button is tapped.
 * @property destructive Whether the action cannot be undone, e.g. removing the member. It is then
 *   drawn in the error color instead of the ink of the design.
 * @property enabled Whether the button can be tapped, e.g. `false` while a change is being saved.
 */
data class MemberDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * A dialog showing one member, as in the "Member dialog" frames of the design: the avatar, the
 * name, an optional [badge] and [supportingText] next to it, a close button, then the [actions]
 * side by side. Without actions, the dialog only shows the member.
 *
 * @param user The member shown.
 * @param onDismiss Called when the close button is tapped or the user taps outside the dialog.
 * @param modifier Applied to the dialog surface.
 * @param badge Shown under the name, e.g. the role of the member. Nothing if `null`.
 * @param supportingText Shown next to the [badge], e.g. "3 tasks". Nothing if `null`.
 * @param actions The buttons under the member, at most two to keep them side by side.
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
  require(actions.size <= MAX_MEMBER_DIALOG_ACTIONS) {
    "A member dialog shows at most $MAX_MEMBER_DIALOG_ACTIONS actions."
  }
  Dialog(onDismissRequest = onDismiss) {
    DialogSurface(modifier.testTag(MemberDialogTestTags.MEMBER_DIALOG)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Avatar(user = user, modifier = Modifier.size(56.dp))
        Column(
            modifier = Modifier.weight(1f).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
              text = "${user.name} ${user.surname}".trim(),
              modifier = Modifier.testTag(MemberDialogTestTags.MEMBER_NAME),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
          )
          if (badge != null || supportingText != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              badge?.invoke()
              if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        CloseButton(onClick = onDismiss)
      }
      if (actions.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          actions.forEach { MemberActionButton(it, Modifier.weight(1f)) }
        }
      }
    }
  }
}

/**
 * A dialog to pick one of [options], as in the "Change role" frames of the design: a [title] with a
 * close button, then one row per option. Tapping an option other than [current] calls [onPick]; the
 * [current] one is marked with [currentLabel] and cannot be picked.
 *
 * @param title What is picked, e.g. "Change the role of Yasser".
 * @param options The options, in the order they are shown.
 * @param onPick Called with the option the user taps.
 * @param onDismiss Called when the close button is tapped or the user taps outside the dialog.
 * @param modifier Applied to the dialog surface.
 * @param current The option already in place, if it is among [options].
 * @param currentLabel The text that marks [current], e.g. "Current".
 * @param enabled Whether the options can be picked, e.g. `false` while a change is being saved.
 * @param option How one option is drawn, e.g. as a role badge.
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
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        CloseButton(onClick = onDismiss)
      }
      Column(
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        options.forEachIndexed { index, value ->
          OptionRow(
              isCurrent = value == current,
              currentLabel = currentLabel,
              onClick = { onPick(value) },
              enabled = enabled,
              modifier = Modifier.testTag(MemberDialogTestTags.option(index)),
          ) {
            option(value)
          }
        }
      }
    }
  }
}

/** The most actions a [MemberDialog] shows, so that they fit side by side. */
const val MAX_MEMBER_DIALOG_ACTIONS = 2

/** The rounded surface shared by the dialogs of this file, with the colors of the design. */
@Composable
private fun DialogSurface(modifier: Modifier, content: @Composable () -> Unit) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 20.dp)) {
      content()
    }
  }
}

/** The close button at the top right of the dialogs of this file. */
@Composable
private fun CloseButton(onClick: () -> Unit) {
  IconButton(
      onClick = onClick,
      modifier = Modifier.size(40.dp).testTag(MemberDialogTestTags.CLOSE_BUTTON),
  ) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = stringResource(R.string.action_close),
        tint = MaterialTheme.colorScheme.onSurface,
    )
  }
}

/**
 * One button of a [MemberDialog]: filled with ink, or outlined in the error color if destructive.
 */
@Composable
private fun MemberActionButton(action: MemberDialogAction, modifier: Modifier) {
  val shape = RoundedCornerShape(12.dp)
  val buttonModifier =
      modifier.padding(end = 8.dp).height(48.dp).testTag(MemberDialogTestTags.action(action.label))
  if (action.destructive) {
    OutlinedButton(
        onClick = action.onClick,
        modifier = buttonModifier,
        enabled = action.enabled,
        shape = shape,
        colors =
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
      Text(action.label, maxLines = 1)
    }
  } else {
    Button(
        onClick = action.onClick,
        modifier = buttonModifier,
        enabled = action.enabled,
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ),
    ) {
      Text(action.label, maxLines = 1)
    }
  }
}

/**
 * One option of an [OptionPickerDialog]: the [content] on the left, then a chevron, or
 * [currentLabel] and a thin border if it is the current one, which cannot be tapped.
 */
@Composable
private fun OptionRow(
    isCurrent: Boolean,
    currentLabel: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(12.dp)
  Row(
      modifier =
          modifier
              .padding(end = 8.dp)
              .fillMaxWidth()
              .clip(shape)
              .then(
                  if (isCurrent)
                      Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                  else Modifier.clickable(enabled = enabled, onClick = onClick)
              )
              .padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(modifier = Modifier.weight(1f)) { content() }
    if (isCurrent) {
      Text(
          text = currentLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private val previewUser = User("u1", name = "Yasser", surname = "Ameur")

@Preview
@Composable
internal fun MemberDialogPreview() {
  AppTheme {
    MemberDialog(
        user = previewUser,
        onDismiss = {},
        badge = { StatusPill(label = "Staff", style = PillStyle.OUTLINED) },
        supportingText = "3 tasks",
        actions =
            listOf(
                MemberDialogAction("Change role", onClick = {}),
                MemberDialogAction("Kick", onClick = {}, destructive = true),
            ),
    )
  }
}

@Preview
@Composable
internal fun OptionPickerDialogPreview() {
  AppTheme {
    OptionPickerDialog(
        title = "Change the role of Yasser",
        options = listOf("Team lead", "Staff", "Visitor"),
        onPick = {},
        onDismiss = {},
        current = "Staff",
        currentLabel = "Current",
    ) {
      StatusPill(label = it, style = PillStyle.MUTED)
    }
  }
}
