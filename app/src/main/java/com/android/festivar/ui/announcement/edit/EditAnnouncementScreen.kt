// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.AnnouncementFormActions
import com.android.festivar.ui.announcement.AnnouncementFormBackHandling
import com.android.festivar.ui.announcement.AnnouncementFormContent
import com.android.festivar.ui.announcement.AnnouncementFormLabels
import com.android.festivar.ui.announcement.AnnouncementFormState
import com.android.festivar.ui.announcement.AnnouncementFormTags
import com.android.festivar.ui.announcement.AnnouncementFormTopBar
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Test tags of the Edit announcement screen. The form's nodes have their own tags here, not the
 * ones of the Create announcement screen, so a test of one screen never finds a node of the other.
 */
object EditAnnouncementScreenTestTags {
  const val SCREEN = "editAnnouncementScreen"
  const val BACK_BUTTON = "editAnnouncementBackButton"
  const val TITLE = "editAnnouncementTitle"
  const val TITLE_FIELD = "editAnnouncementTitleField"
  const val TITLE_ERROR = "editAnnouncementTitleError"
  const val DESCRIPTION_FIELD = "editAnnouncementDescriptionField"
  const val DESCRIPTION_ERROR = "editAnnouncementDescriptionError"
  const val PRIORITY_LABEL = "editAnnouncementPriorityLabel"
  const val SAVE_ERROR = "editAnnouncementSaveError"
  const val SAVE_BUTTON = "editAnnouncementSaveButton"
  const val SAVING_INDICATOR = "editAnnouncementSavingIndicator"
  const val DISCARD_DIALOG = "editAnnouncementDiscardDialog"
  const val KEEP_EDITING_BUTTON = "editAnnouncementKeepEditingButton"
  const val DISCARD_BUTTON = "editAnnouncementDiscardButton"
  const val LOADING = "editAnnouncementLoading"
  const val LOAD_ERROR = "editAnnouncementLoadError"
  const val LOAD_RETRY = "editAnnouncementLoadRetry"
  const val NOT_ORGANIZER = "editAnnouncementNotOrganizer"

  /** Test tag of the chip of [priority]. */
  fun priorityChip(priority: AnnouncementPriority) = "editAnnouncementPriority_${priority.name}"
}

// The tags and texts of the form when it edits an announcement.
private val editAnnouncementTags =
    AnnouncementFormTags(
        backButton = EditAnnouncementScreenTestTags.BACK_BUTTON,
        title = EditAnnouncementScreenTestTags.TITLE,
        titleField = EditAnnouncementScreenTestTags.TITLE_FIELD,
        titleError = EditAnnouncementScreenTestTags.TITLE_ERROR,
        descriptionField = EditAnnouncementScreenTestTags.DESCRIPTION_FIELD,
        descriptionError = EditAnnouncementScreenTestTags.DESCRIPTION_ERROR,
        priorityLabel = EditAnnouncementScreenTestTags.PRIORITY_LABEL,
        saveError = EditAnnouncementScreenTestTags.SAVE_ERROR,
        submitButton = EditAnnouncementScreenTestTags.SAVE_BUTTON,
        savingIndicator = EditAnnouncementScreenTestTags.SAVING_INDICATOR,
        discardDialog = EditAnnouncementScreenTestTags.DISCARD_DIALOG,
        keepEditingButton = EditAnnouncementScreenTestTags.KEEP_EDITING_BUTTON,
        discardButton = EditAnnouncementScreenTestTags.DISCARD_BUTTON,
        priorityChip = EditAnnouncementScreenTestTags::priorityChip,
    )

private val editAnnouncementLabels =
    AnnouncementFormLabels(
        title = R.string.edit_announcement,
        submit = R.string.edit_announcement_save,
        submitting = R.string.edit_announcement_saving,
    )

/**
 * Edit announcement screen, connected to its [viewModel], which knows the announcement to edit and
 * who is signed in.
 *
 * @param onBack Called when the user leaves without saving.
 * @param onSaved Called once the edit has been saved.
 */
@Composable
fun EditAnnouncementScreen(
    viewModel: EditAnnouncementViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()
  LaunchedEffect(Unit) { viewModel.load() }

  // The latest callback is called, even if the caller passed a new one during the save.
  val currentOnSaved by rememberUpdatedState(onSaved)
  LaunchedEffect(state.isSaved) { if (state.isSaved) currentOnSaved() }

  AnnouncementFormBackHandling(
      isSaving = { state.isSaving },
      needsConfirmation = { state.needsDiscardConfirmation },
      onBack = onBack,
      dialogTitle = R.string.edit_announcement_discard_title,
      dialogMessage = R.string.edit_announcement_discard_message,
      tags = editAnnouncementTags,
  ) { requestBack ->
    EditAnnouncementContent(
        state = state,
        actions =
            AnnouncementFormActions(
                onBack = requestBack,
                onTitleChange = viewModel::updateTitle,
                onContentChange = viewModel::updateContent,
                onPriorityChange = viewModel::updatePriority,
                onSubmit = viewModel::save,
            ),
        onRetry = viewModel::load,
        modifier = modifier,
    )
  }
}

/**
 * Stateless Edit announcement screen: the shared announcement form filled with the announcement
 * once it is loaded, the load error with a retry when a load failed, a short notice instead of the
 * form for a user who is not the organizer of the event, and otherwise a spinner.
 *
 * @param actions The form's input; `onSubmit` saves the edit.
 * @param onRetry Loads the announcement again after a failed load.
 */
@Composable
fun EditAnnouncementContent(
    state: EditAnnouncementUiState,
    actions: AnnouncementFormActions,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.testTag(EditAnnouncementScreenTestTags.SCREEN)) {
    val loadError = state.errorMsg
    when {
      // A form that looks editable but can never be saved would not say why.
      state.announcement != null && !state.isOrganizer() ->
          NoForm(actions.onBack) {
            Text(
                text = stringResource(R.string.edit_announcement_not_organizer),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.padding(horizontal = 32.dp)
                        .testTag(EditAnnouncementScreenTestTags.NOT_ORGANIZER),
            )
          }
      state.announcement != null ->
          // The saving and saved flags come from the screen state: they lock the inputs, show the
          // spinner in the button, and keep a saved form final.
          AnnouncementFormContent(
              form = state.form,
              isSaving = state.isSaving,
              isEditable = state.isEditable,
              canSubmit = state.canSave,
              errorMsg = state.errorMsg,
              actions = actions,
              labels = editAnnouncementLabels,
              tags = editAnnouncementTags,
          )
      loadError != null && !state.isLoading ->
          NoForm(actions.onBack) { LoadError(loadError, onRetry) }
      // The spinner also covers the first frames, before load() has run.
      else ->
          NoForm(actions.onBack) {
            CircularProgressIndicator(Modifier.testTag(EditAnnouncementScreenTestTags.LOADING))
          }
    }
  }
}

/** The form's top bar over [content] centered, for the states without a form. */
@Composable
private fun NoForm(onBack: () -> Unit, content: @Composable () -> Unit) {
  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        AnnouncementFormTopBar(editAnnouncementLabels.title, onBack, editAnnouncementTags)
      },
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
  }
}

/** The load failure [message] in the theme's error colour, with its retry button. */
@Composable
private fun LoadError(@StringRes message: Int, onRetry: () -> Unit) {
  Column(
      modifier = Modifier.padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(EditAnnouncementScreenTestTags.LOAD_ERROR),
    )
    Button(
        onClick = onRetry,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag(EditAnnouncementScreenTestTags.LOAD_RETRY),
    ) {
      Text(stringResource(R.string.action_retry))
    }
  }
}

// A fixed zone and date keep the previews the same on every machine. The stored announcement is a
// warning and the form makes it urgent: that one edit is what enables "Save changes".
private val previewAnnouncement =
    Announcement(
        announcementId = "a1",
        title = "Stage B is closed",
        authorId = "organizer-1",
        content = "Strong wind on the hill. Send everyone to the north gate until 16:00.",
        priority = AnnouncementPriority.WARNING,
        updatedAt = ZonedDateTime.of(2037, 10, 17, 9, 0, 0, 0, ZoneOffset.UTC),
    )

private val previewForm =
    AnnouncementFormState(
        title = previewAnnouncement.title,
        content = previewAnnouncement.content,
        priority = AnnouncementPriority.URGENT,
    )

private val previewLoaded =
    EditAnnouncementUiState(
        announcement = previewAnnouncement,
        form = previewForm,
        isOrganizer = { true },
    )

@Composable
private fun EditAnnouncementPreview(state: EditAnnouncementUiState) {
  AppTheme {
    EditAnnouncementContent(state = state, actions = AnnouncementFormActions(), onRetry = {})
  }
}

/**
 * The form filled with the announcement, with the priority changed so "Save changes" is enabled.
 */
@Preview(showBackground = true)
@Composable
internal fun EditAnnouncementContentPreview() {
  EditAnnouncementPreview(previewLoaded)
}

/** The title and the description emptied: two errors, and nothing can be saved. */
@Preview(showBackground = true)
@Composable
internal fun EditAnnouncementContentFieldErrorsPreview() {
  EditAnnouncementPreview(
      previewLoaded.copy(
          form =
              previewForm.copy(title = "", content = "", titleEdited = true, contentEdited = true)
      )
  )
}

/** The save failed: its error is above the button, which can be pressed again. */
@Preview(showBackground = true)
@Composable
internal fun EditAnnouncementContentSaveErrorPreview() {
  EditAnnouncementPreview(
      previewLoaded.copy(errorMsg = R.string.edit_announcement_error_save_failed)
  )
}

/** The announcement could not be loaded. */
@Preview(showBackground = true)
@Composable
internal fun EditAnnouncementContentLoadErrorPreview() {
  EditAnnouncementPreview(
      EditAnnouncementUiState(errorMsg = R.string.edit_announcement_error_load_failed)
  )
}
