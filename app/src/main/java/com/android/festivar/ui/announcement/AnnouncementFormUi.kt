// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.festivar.R
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.create.CreateAnnouncementContent
import com.android.festivar.ui.components.FestivarDiscardDialog

/**
 * The texts that differ between the forms built on [CreateAnnouncementContent].
 *
 * @property title The title in the top bar.
 * @property submit The text of the button at the bottom.
 * @property submitting The content description of the spinner shown in the button during a save.
 */
data class AnnouncementFormLabels(
    @StringRes val title: Int,
    @StringRes val submit: Int,
    @StringRes val submitting: Int,
)

/**
 * The test tags of a form built on [CreateAnnouncementContent]. Each screen passes its own, so a
 * test of one screen never finds a node of the other.
 *
 * @property title The title in the top bar.
 * @property submitButton The button at the bottom.
 * @property savingIndicator The spinner shown in the button during a save.
 * @property priorityChip Gives the tag of the chip of a priority.
 */
data class AnnouncementFormTags(
    val backButton: String,
    val title: String,
    val titleField: String,
    val titleError: String,
    val descriptionField: String,
    val descriptionError: String,
    val priorityLabel: String,
    val saveError: String,
    val submitButton: String,
    val savingIndicator: String,
    val discardDialog: String,
    val keepEditingButton: String,
    val discardButton: String,
    val priorityChip: (AnnouncementPriority) -> String,
)

/**
 * Back handling of the announcement forms: going back, by the system or by the top bar button
 * passed to [content] as `requestBack`, asks first in the discard dialog when [needsConfirmation].
 *
 * @param isSaving Gives whether a save is running, asked when back is pressed. Back does nothing
 *   meanwhile.
 * @param needsConfirmation Gives whether leaving would lose something, asked when back is pressed.
 * @param onBack Called when the user leaves, at once or after confirming the discard.
 * @param dialogTitle The question the discard dialog asks.
 * @param tags Gives the tags of the discard dialog and of its two buttons.
 * @param dialogMessage What the discard dialog says is lost.
 */
@Composable
internal fun AnnouncementFormBackHandling(
    isSaving: () -> Boolean,
    needsConfirmation: () -> Boolean,
    onBack: () -> Unit,
    @StringRes dialogTitle: Int,
    tags: AnnouncementFormTags,
    @StringRes dialogMessage: Int = R.string.discard_message,
    content: @Composable (requestBack: () -> Unit) -> Unit,
) {
  var askToDiscard by rememberSaveable { mutableStateOf(false) }
  // Back does nothing during a save: leaving would not undo a write that has started, so the
  // announcement could still be published after the user chose to discard it.
  // Both are read when back is pressed, not at composition: a back that comes right after a state
  // change, before the next recomposition, would otherwise act on the old values.
  val requestBack: () -> Unit = {
    if (!isSaving()) {
      if (needsConfirmation()) askToDiscard = true else onBack()
    }
  }
  // Stays enabled during a save, as a disabled handler would let system back close the screen.
  BackHandler(onBack = requestBack)

  content(requestBack)

  if (askToDiscard) {
    FestivarDiscardDialog(
        title = stringResource(dialogTitle),
        message = stringResource(dialogMessage),
        onConfirm = {
          askToDiscard = false
          onBack()
        },
        onDismiss = { askToDiscard = false },
        modifier = Modifier.testTag(tags.discardDialog),
        confirmModifier = Modifier.testTag(tags.discardButton),
        dismissModifier = Modifier.testTag(tags.keepEditingButton),
    )
  }
}

/** The top bar of the announcement forms: the back button and the [title]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnnouncementFormTopBar(
    @StringRes title: Int,
    onBack: () -> Unit,
    tags: AnnouncementFormTags,
) {
  TopAppBar(
      title = {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(tags.title),
        )
      },
      navigationIcon = {
        IconButton(onClick = onBack, modifier = Modifier.testTag(tags.backButton)) {
          Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.create_announcement_back),
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
          ),
  )
}
