// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.task

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
import com.android.festivar.ui.components.FestivarDiscardDialog
import com.android.festivar.ui.task.create.CreateTaskActions
import com.android.festivar.ui.task.create.CreateTaskContent
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags

/** The actions of a task form whose fields are set through [editor]. */
fun taskFormActions(
    editor: TaskFormEditor,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
): CreateTaskActions =
    CreateTaskActions(
        onBack = onBack,
        onTitleChange = editor::updateTitle,
        onDescriptionChange = editor::updateDescription,
        onLocationChange = editor::updateLocation,
        onPriorityChange = editor::updatePriority,
        onMaxAssignDecrease = editor::decrementMaxAssign,
        onMaxAssignIncrease = editor::incrementMaxAssign,
        onEstimatedMinutesChange = editor::updateEstimatedMinutes,
        onShowPicker = editor::showPicker,
        onDatePicked = editor::pickDate,
        onTimePicked = editor::pickTime,
        onCreate = onSubmit,
    )

/**
 * Back handling of the task forms: going back, by the system or by the top bar button passed to
 * [content] as `requestBack`, asks first in the discard dialog when [needsConfirmation].
 *
 * @param onBack Called when the user leaves, at once or after confirming the discard.
 * @param dialogTitle The question the discard dialog asks.
 */
@Composable
internal fun TaskFormBackHandling(
    needsConfirmation: Boolean,
    isSaving: Boolean = false,
    onBack: () -> Unit,
    @StringRes dialogTitle: Int,
    content: @Composable (requestBack: () -> Unit) -> Unit,
) {
  var askToDiscard by rememberSaveable { mutableStateOf(false) }
  val requestBack = {
    if (!isSaving) {
      if (needsConfirmation) askToDiscard = true else onBack()
    }
  }
  BackHandler(onBack = requestBack)

  content(requestBack)

  if (askToDiscard) {
    DiscardTaskDialog(
        onConfirm = {
          askToDiscard = false
          onBack()
        },
        onDismiss = { askToDiscard = false },
        title = dialogTitle,
    )
  }
}

/**
 * The discard dialog of the task forms, shown when going back from an edited form.
 *
 * @param title The question asked, "Discard this task?" by default.
 */
@Composable
internal fun DiscardTaskDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes title: Int = R.string.create_task_discard_title,
) {
  FestivarDiscardDialog(
      title = stringResource(title),
      onConfirm = onConfirm,
      onDismiss = onDismiss,
      modifier = Modifier.testTag(CreateTaskScreenTestTags.DISCARD_DIALOG),
      confirmModifier = Modifier.testTag(CreateTaskScreenTestTags.DISCARD_BUTTON),
      dismissModifier = Modifier.testTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON),
  )
}

/** The top bar of the task forms: the back button and the [title]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskFormTopBar(@StringRes title: Int, onBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(CreateTaskScreenTestTags.TITLE),
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(CreateTaskScreenTestTags.BACK_BUTTON),
        ) {
          Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.create_task_back),
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
          ),
  )
}

/**
 * The texts that differ between the forms built on [CreateTaskContent].
 *
 * @property title The title in the top bar.
 * @property submit The text of the button at the bottom.
 */
data class TaskFormLabels(@StringRes val title: Int, @StringRes val submit: Int)
