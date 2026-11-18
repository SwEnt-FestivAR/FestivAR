// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.AnnouncementFormBackHandling
import com.android.festivar.ui.announcement.AnnouncementFormLabels
import com.android.festivar.ui.announcement.AnnouncementFormTags
import com.android.festivar.ui.announcement.AnnouncementFormTopBar
import com.android.festivar.ui.announcement.announcementPriorityLabelRes
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.components.LONG_TEXT_MAX_LENGTH
import com.android.festivar.ui.components.SectionLabel
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

// The tags and texts of the form when it creates an announcement, its defaults.
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

private val createAnnouncementLabels =
    AnnouncementFormLabels(
        title = R.string.create_announcement,
        submit = R.string.create_announcement_publish,
        submitting = R.string.create_announcement_publishing,
    )

/**
 * What the user can do on the Create announcement screen. Lets [CreateAnnouncementContent] stay
 * stateless.
 */
class CreateAnnouncementActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onContentChange: (String) -> Unit = {},
    val onPriorityChange: (AnnouncementPriority) -> Unit = {},
    val onPublish: () -> Unit = {},
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
            CreateAnnouncementActions(
                onBack = requestBack,
                onTitleChange = viewModel::updateTitle,
                onContentChange = viewModel::updateContent,
                onPriorityChange = viewModel::updatePriority,
                onPublish = viewModel::publish,
            ),
        modifier = modifier,
    )
  }
}

/**
 * Stateless Create announcement screen: draws [state] and reports user input through [actions]. The
 * Edit announcement screen draws its form with it as well.
 *
 * @param labels The top bar title, the button text and the description of its spinner.
 * @param tags The test tags of the form's nodes.
 * @param canSubmit Whether the button at the bottom can be pressed.
 */
@Composable
fun CreateAnnouncementContent(
    state: CreateAnnouncementUiState,
    actions: CreateAnnouncementActions,
    modifier: Modifier = Modifier,
    labels: AnnouncementFormLabels = createAnnouncementLabels,
    tags: AnnouncementFormTags = createAnnouncementTags,
    canSubmit: Boolean = state.canPublish,
) {
  Scaffold(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { AnnouncementFormTopBar(labels.title, actions.onBack, tags) },
      bottomBar = {
        PublishBar(
            state = state,
            onPublish = actions.onPublish,
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
      FestivarFieldWithError(state.titleError, tags.titleError) {
        FestivarTextField(
            value = state.title,
            onValueChange = actions.onTitleChange,
            label = stringResource(R.string.create_announcement_field_title),
            enabled = state.isEditable,
            isError = state.titleError != null,
            modifier = Modifier.testTag(tags.titleField),
        )
      }
      FestivarFieldWithError(state.contentError, tags.descriptionError) {
        FestivarTextField(
            value = state.content,
            onValueChange = actions.onContentChange,
            label = stringResource(R.string.create_announcement_field_description),
            enabled = state.isEditable,
            singleLine = false,
            maxLength = LONG_TEXT_MAX_LENGTH,
            showCounter = true,
            isError = state.contentError != null,
            modifier = Modifier.heightIn(min = 96.dp).testTag(tags.descriptionField),
        )
      }
      AnnouncementPrioritySelector(
          selected = state.priority,
          onSelect = actions.onPriorityChange,
          enabled = state.isEditable,
          tags = tags,
      )
    }
  }
}

/** The bottom of the screen: the save error, when there is one, above the submit button. */
@Composable
private fun PublishBar(
    state: CreateAnnouncementUiState,
    onPublish: () -> Unit,
    labels: AnnouncementFormLabels,
    tags: AnnouncementFormTags,
    canSubmit: Boolean,
) {
  Column {
    state.errorMsg?.let {
      FestivarFieldError(
          message = stringResource(it),
          modifier = Modifier.padding(horizontal = 20.dp).testTag(tags.saveError),
      )
    }
    Button(
        onClick = onPublish,
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
      if (state.isSaving) {
        val publishing = stringResource(labels.submitting)
        CircularProgressIndicator(
            modifier =
                Modifier.size(16.dp)
                    .semantics { contentDescription = publishing }
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

/**
 * Titled row of chips to pick an [AnnouncementPriority]; [selected] is the current one. The chips
 * ignore taps unless [enabled].
 */
@Composable
private fun AnnouncementPrioritySelector(
    selected: AnnouncementPriority,
    onSelect: (AnnouncementPriority) -> Unit,
    enabled: Boolean,
    tags: AnnouncementFormTags,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SectionLabel(
        stringResource(R.string.create_announcement_field_priority),
        modifier = Modifier.testTag(tags.priorityLabel),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
      AnnouncementPriority.entries.forEach { priority ->
        FilterChip(
            label = stringResource(announcementPriorityLabelRes(priority)),
            selected = priority == selected,
            onClick = { onSelect(priority) },
            enabled = enabled,
            modifier = Modifier.testTag(tags.priorityChip(priority)),
        )
      }
    }
  }
}

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
        actions = CreateAnnouncementActions(),
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
        actions = CreateAnnouncementActions(),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun CreateAnnouncementContentFieldErrorsPreview() {
  AppTheme {
    CreateAnnouncementContent(
        state = CreateAnnouncementUiState(titleEdited = true, contentEdited = true),
        actions = CreateAnnouncementActions(),
    )
  }
}
