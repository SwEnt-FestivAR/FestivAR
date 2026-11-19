// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.R
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.AnnouncementFormActions
import com.android.festivar.ui.announcement.AnnouncementFormBackHandling
import com.android.festivar.ui.announcement.AnnouncementFormContent
import com.android.festivar.ui.announcement.AnnouncementFormLabels
import com.android.festivar.ui.announcement.AnnouncementFormState
import com.android.festivar.ui.announcement.AnnouncementFormTags
import com.android.festivar.ui.theme.AppTheme

object CreateAnnouncementScreenTestTags {
  const val BACK_BUTTON = "createAnnouncementBackButton"
  const val TITLE = "createAnnouncementTitle"
  const val TITLE_FIELD = "createAnnouncementTitleField"
  const val TITLE_ERROR = "createAnnouncementTitleError"
  const val DESCRIPTION_FIELD = "createAnnouncementDescriptionField"
  const val DESCRIPTION_ERROR = "createAnnouncementDescriptionError"
  const val PRIORITY_LABEL = "createAnnouncementPriorityLabel"
  const val SAVE_ERROR = "createAnnouncementSaveError"
  const val PUBLISH_BUTTON = "createAnnouncementPublishButton"
  const val SAVING_INDICATOR = "createAnnouncementSavingIndicator"
  const val DISCARD_DIALOG = "createAnnouncementDiscardDialog"
  const val KEEP_EDITING_BUTTON = "createAnnouncementKeepEditingButton"
  const val DISCARD_BUTTON = "createAnnouncementDiscardButton"

  /** Test tag of the chip of [priority]. */
  fun priorityChip(priority: AnnouncementPriority) = "createAnnouncementPriority_${priority.name}"
}

/** The test tags of the form when it creates an announcement. */
private val createAnnouncementTags =
    AnnouncementFormTags(
        backButton = CreateAnnouncementScreenTestTags.BACK_BUTTON,
        title = CreateAnnouncementScreenTestTags.TITLE,
        titleField = CreateAnnouncementScreenTestTags.TITLE_FIELD,
        titleError = CreateAnnouncementScreenTestTags.TITLE_ERROR,
        descriptionField = CreateAnnouncementScreenTestTags.DESCRIPTION_FIELD,
        descriptionError = CreateAnnouncementScreenTestTags.DESCRIPTION_ERROR,
        priorityLabel = CreateAnnouncementScreenTestTags.PRIORITY_LABEL,
        saveError = CreateAnnouncementScreenTestTags.SAVE_ERROR,
        submitButton = CreateAnnouncementScreenTestTags.PUBLISH_BUTTON,
        savingIndicator = CreateAnnouncementScreenTestTags.SAVING_INDICATOR,
        discardDialog = CreateAnnouncementScreenTestTags.DISCARD_DIALOG,
        keepEditingButton = CreateAnnouncementScreenTestTags.KEEP_EDITING_BUTTON,
        discardButton = CreateAnnouncementScreenTestTags.DISCARD_BUTTON,
        priorityChip = CreateAnnouncementScreenTestTags::priorityChip,
    )

/** The texts of the form when it creates an announcement. */
private val createAnnouncementLabels =
    AnnouncementFormLabels(
        title = R.string.create_announcement,
        submit = R.string.create_announcement_publish,
        submitting = R.string.create_announcement_publishing,
    )

/**
 * Create announcement screen, connected to its [viewModel], which knows the event the announcement
 * belongs to and its author.
 *
 * @param onBack Called when the user leaves without publishing.
 * @param onPublished Called once the announcement has been saved.
 */
@Composable
fun CreateAnnouncementScreen(
    viewModel: CreateAnnouncementViewModel,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()

  // The latest callback is called, even if the caller passed a new one during the save.
  val currentOnPublished by rememberUpdatedState(onPublished)
  LaunchedEffect(state.isPublished) { if (state.isPublished) currentOnPublished() }

  AnnouncementFormBackHandling(
      isSaving = { state.isSaving },
      needsConfirmation = { state.needsDiscardConfirmation },
      onBack = onBack,
      dialogTitle = R.string.create_announcement_discard_title,
      tags = createAnnouncementTags,
  ) { requestBack ->
    CreateAnnouncementContent(
        state = state,
        actions =
            AnnouncementFormActions(
                onBack = requestBack,
                onTitleChange = viewModel::updateTitle,
                onContentChange = viewModel::updateContent,
                onPriorityChange = viewModel::updatePriority,
                onSubmit = viewModel::publish,
            ),
        modifier = modifier,
    )
  }
}

/**
 * Stateless Create announcement screen: draws [state] with the shared [AnnouncementFormContent] and
 * reports user input through [actions]. The button at the bottom publishes the announcement.
 */
@Composable
fun CreateAnnouncementContent(
    state: CreateAnnouncementUiState,
    actions: AnnouncementFormActions,
    modifier: Modifier = Modifier,
) {
  AnnouncementFormContent(
      form = state.toForm(),
      isSaving = state.isSaving,
      isEditable = state.isEditable,
      canSubmit = state.canPublish,
      errorMsg = state.errorMsg,
      actions = actions,
      labels = createAnnouncementLabels,
      tags = createAnnouncementTags,
      modifier = modifier,
  )
}

/** The fields of [this] that the shared form draws. */
private fun CreateAnnouncementUiState.toForm(): AnnouncementFormState =
    AnnouncementFormState(
        title = title,
        content = content,
        priority = priority,
        titleEdited = titleEdited,
        contentEdited = contentEdited,
    )

@Preview(showBackground = true)
@Composable
internal fun CreateAnnouncementContentPreview() {
  AppTheme {
    CreateAnnouncementContent(
        state =
            CreateAnnouncementUiState(
                title = "Stage B is closed",
                content = "Strong wind on the hill. Send everyone to the north gate until 16:00.",
                priority = AnnouncementPriority.URGENT,
            ),
        actions = AnnouncementFormActions(),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun CreateAnnouncementContentSavingPreview() {
  AppTheme {
    CreateAnnouncementContent(
        state =
            CreateAnnouncementUiState(
                title = "Stage B is closed",
                content = "Strong wind on the hill. Send everyone to the north gate until 16:00.",
                priority = AnnouncementPriority.URGENT,
                isSaving = true,
            ),
        actions = AnnouncementFormActions(),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun CreateAnnouncementContentFieldErrorsPreview() {
  AppTheme {
    CreateAnnouncementContent(
        state = CreateAnnouncementUiState(titleEdited = true, contentEdited = true),
        actions = AnnouncementFormActions(),
    )
  }
}
