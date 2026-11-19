// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.FestivarChipSelector
import com.android.festivar.ui.components.FestivarDiscardDialog
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.components.LONG_TEXT_MAX_LENGTH

/**
 * The texts that differ between the forms built on [AnnouncementFormContent].
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
 * The test tags of a form built on [AnnouncementFormContent]. Each screen passes its own, so a test
 * of one screen never finds a node of the other.
 *
 * @property backButton The back button in the top bar.
 * @property title The title in the top bar.
 * @property titleField The title field.
 * @property titleError The error shown under the title field.
 * @property descriptionField The description field.
 * @property descriptionError The error shown under the description field.
 * @property priorityLabel The title above the priority chips.
 * @property saveError The error shown above the button when a save fails.
 * @property submitButton The button at the bottom.
 * @property savingIndicator The spinner shown in the button during a save.
 * @property discardDialog The dialog asking whether to discard the changes.
 * @property keepEditingButton The button of the discard dialog that stays on the form.
 * @property discardButton The button of the discard dialog that leaves the form.
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
              contentDescription = stringResource(R.string.announcement_form_back),
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
 * What the user can do on an announcement form. Lets [AnnouncementFormContent] stay stateless.
 *
 * @property onBack Called when the back button of the top bar is pressed.
 * @property onTitleChange Called with the title as typed.
 * @property onContentChange Called with the description as typed.
 * @property onPriorityChange Called with the priority whose chip was tapped.
 * @property onSubmit Called when the button at the bottom is pressed.
 */
class AnnouncementFormActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onContentChange: (String) -> Unit = {},
    val onPriorityChange: (AnnouncementPriority) -> Unit = {},
    val onSubmit: () -> Unit = {},
)

/**
 * Stateless announcement form, shared by the create and edit screens: the top bar, the title and
 * description fields, the priority chips and the submit button. It draws [form] and reports user
 * input through [actions].
 *
 * @param form What the user typed, with the field errors.
 * @param isSaving Whether a save is running. The button then shows a spinner described by
 *   [AnnouncementFormLabels.submitting].
 * @param isEditable Whether the inputs can be changed.
 * @param canSubmit Whether the button at the bottom can be pressed.
 * @param errorMsg The save error shown above the button, or null when there is none.
 * @param actions What the user can do on the form.
 * @param labels The top bar title, the button text and the description of its spinner.
 * @param tags The test tags of the form's nodes.
 */
@Composable
fun AnnouncementFormContent(
    form: AnnouncementFormState,
    isSaving: Boolean,
    isEditable: Boolean,
    canSubmit: Boolean,
    @StringRes errorMsg: Int?,
    actions: AnnouncementFormActions,
    labels: AnnouncementFormLabels,
    tags: AnnouncementFormTags,
    modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { AnnouncementFormTopBar(labels.title, actions.onBack, tags) },
      bottomBar = {
        AnnouncementFormSubmitBar(
            isSaving = isSaving,
            errorMsg = errorMsg,
            onSubmit = actions.onSubmit,
            labels = labels,
            tags = tags,
            canSubmit = canSubmit,
        )
      },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      FestivarFieldWithError(form.titleError, tags.titleError) {
        FestivarTextField(
            value = form.title,
            onValueChange = actions.onTitleChange,
            label = stringResource(R.string.announcement_form_field_title),
            enabled = isEditable,
            isError = form.titleError != null,
            modifier = Modifier.testTag(tags.titleField),
        )
      }
      FestivarFieldWithError(form.contentError, tags.descriptionError) {
        FestivarTextField(
            value = form.content,
            onValueChange = actions.onContentChange,
            label = stringResource(R.string.announcement_form_field_description),
            enabled = isEditable,
            singleLine = false,
            maxLength = LONG_TEXT_MAX_LENGTH,
            showCounter = true,
            isError = form.contentError != null,
            modifier = Modifier.heightIn(min = 96.dp).testTag(tags.descriptionField),
        )
      }
      FestivarChipSelector(
          options = AnnouncementPriority.entries,
          selected = form.priority,
          onSelect = actions.onPriorityChange,
          label = stringResource(R.string.announcement_form_field_priority),
          optionLabelRes = ::announcementPriorityLabelRes,
          enabled = isEditable,
          labelModifier = Modifier.testTag(tags.priorityLabel),
          chipModifier = { Modifier.testTag(tags.priorityChip(it)) },
      )
    }
  }
}

/** The bottom of the form: the save error, when there is one, above the submit button. */
@Composable
private fun AnnouncementFormSubmitBar(
    isSaving: Boolean,
    @StringRes errorMsg: Int?,
    onSubmit: () -> Unit,
    labels: AnnouncementFormLabels,
    tags: AnnouncementFormTags,
    canSubmit: Boolean,
) {
  Column {
    errorMsg?.let {
      FestivarFieldError(
          message = stringResource(it),
          modifier = Modifier.padding(horizontal = 20.dp).testTag(tags.saveError),
      )
    }
    Button(
        onClick = onSubmit,
        enabled = canSubmit,
        shape = RoundedCornerShape(12.dp),
        // As in Figma, the disabled button is the beige of the hairlines with dark text; Material
        // would draw it grey with faded text.
        colors =
            ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp)
                .height(52.dp)
                .testTag(tags.submitButton),
    ) {
      if (isSaving) {
        val submitting = stringResource(labels.submitting)
        CircularProgressIndicator(
            modifier =
                Modifier.size(16.dp)
                    .semantics { contentDescription = submitting }
                    .testTag(tags.savingIndicator),
            color = MaterialTheme.colorScheme.onSurface,
            strokeWidth = 2.dp,
        )
      } else {
        Text(text = stringResource(labels.submit), style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}
