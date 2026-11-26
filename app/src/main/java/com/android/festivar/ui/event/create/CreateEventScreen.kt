package com.android.festivar.ui.event.create

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Gemini
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.FestivarDiscardDialog
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.datetime.DateTimeField
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.event.EventFields
import com.android.festivar.ui.event.EventFieldsTestTags
import com.android.festivar.ui.theme.AppTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import java.time.LocalDate
import java.time.LocalTime

object CreateEventScreenTestTags {
  const val DELETE_IMAGE_BUTTON: String = "DeleteImageButton"
  const val IMAGE_PREVIEW: String = "ImagePreview"
  const val DELETE_IMAGE_CONFIRM_DIALOG = "deleteImageConfirmDialog"
  const val DELETE_IMAGE_CONFIRM_BUTTON = "deleteImageConfirmButton"
  const val DELETE_IMAGE_DISMISS_BUTTON = "deleteImageDismissButton"
  const val NAVIGATION_BUTTON = "navigationButton"
  const val TITLE = "title"
  const val NAME_FIELD = "nameField"
  const val START_DATE_FIELD = "startDateField"
  const val START_TIME_FIELD = "startTimeField"
  const val END_DATE_FIELD = "endDateField"
  const val END_TIME_FIELD = "endTimeField"
  const val LOCATION_FIELD = "locationField"
  const val DESCRIPTION_FIELD = "descriptionField"
  const val NAME_ERROR = "nameError"
  const val START_DATE_ERROR = "startDateError"
  const val END_DATE_ERROR = "endDateError"
  const val CREATE_BUTTON = "createButton"
  const val SAVING_INDICATOR = "createEventSaving"
  const val SAVE_ERROR = "createEventSaveError"
  const val DISCARD_DIALOG = "createEventDiscardDialog"
  const val DISCARD_BUTTON = "createEventDiscardButton"
  const val KEEP_EDITING_BUTTON = "createEventKeepEditingButton"
  const val IMAGE_SELECT_BUTTON = "imageSelectButton"
}

private val createEventFieldTags =
    EventFieldsTestTags(
        name = CreateEventScreenTestTags.NAME_FIELD,
        startDate = CreateEventScreenTestTags.START_DATE_FIELD,
        startTime = CreateEventScreenTestTags.START_TIME_FIELD,
        endDate = CreateEventScreenTestTags.END_DATE_FIELD,
        endTime = CreateEventScreenTestTags.END_TIME_FIELD,
        venue = CreateEventScreenTestTags.LOCATION_FIELD,
        notes = CreateEventScreenTestTags.DESCRIPTION_FIELD,
        nameError = CreateEventScreenTestTags.NAME_ERROR,
        startDateError = CreateEventScreenTestTags.START_DATE_ERROR,
        endDateError = CreateEventScreenTestTags.END_DATE_ERROR,
    )

/** What the user can do on the Create event screen. Lets [CreateEventContent] stay stateless. */
class CreateEventActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onShowPicker: (DateTimeField?) -> Unit = {},
    val onVenueChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onDatePicked: (LocalDate) -> Unit = {},
    val onTimePicked: (LocalTime) -> Unit = {},
    val onCreate: () -> Unit = {},
    val onDeleteImageClick: () -> Unit = {},
)

/**
 * A screen for creating events.
 *
 * @param onCreated Called once the event is saved, so the caller can leave the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreated: () -> Unit = {},
    launchPhotoPicker: ((String) -> Unit)? = null,
) {
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(uiState.isDiscarded) { if (uiState.isDiscarded) onBack() }
  LaunchedEffect(uiState.createdEventId) { if (uiState.createdEventId != null) onCreated() }
  BackHandler(onBack = viewModel::requestDiscard)

  val context = LocalContext.current
  val photoPicker =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.updateImage(it, context.contentResolver, context.cacheDir) }
      }
  val launchPicker = launchPhotoPicker ?: { mimeType -> photoPicker.launch(mimeType) }

  CreateEventContent(
      state = uiState,
      actions =
          CreateEventActions(
              onBack = viewModel::requestDiscard,
              onNameChange = viewModel::updateName,
              onShowPicker = viewModel::showPicker,
              onVenueChange = viewModel::updateVenue,
              onNotesChange = viewModel::updateNotes,
              onDatePicked = viewModel::pickDate,
              onTimePicked = viewModel::pickTime,
              onCreate = viewModel::createEvent,
              onDeleteImageClick = viewModel::requestDeleteImage,
          ),
      modifier = modifier,
      launchPicker = launchPicker,
  )

  if (uiState.showDiscardConfirmation) {
    FestivarDiscardDialog(
        title = stringResource(R.string.create_event_discard_title),
        onConfirm = viewModel::confirmDiscard,
        onDismiss = viewModel::cancelDiscard,
        modifier = Modifier.testTag(CreateEventScreenTestTags.DISCARD_DIALOG),
        confirmModifier = Modifier.testTag(CreateEventScreenTestTags.DISCARD_BUTTON),
        dismissModifier = Modifier.testTag(CreateEventScreenTestTags.KEEP_EDITING_BUTTON),
    )
  }

  if (uiState.showDeleteImageConfirmation) {
    FestivarConfirmDialog(
        title = stringResource(R.string.create_event_delete_image_title),
        message = stringResource(R.string.create_event_delete_image_message),
        confirmText = stringResource(R.string.create_event_image_del),
        dismissText = stringResource(R.string.create_event_delete_image_cancel),
        onConfirm = viewModel::confirmDeleteImage,
        onDismiss = viewModel::cancelDeleteImage,
        modifier = Modifier.testTag(CreateEventScreenTestTags.DELETE_IMAGE_CONFIRM_DIALOG),
        confirmModifier = Modifier.testTag(CreateEventScreenTestTags.DELETE_IMAGE_CONFIRM_BUTTON),
        dismissModifier = Modifier.testTag(CreateEventScreenTestTags.DELETE_IMAGE_DISMISS_BUTTON),
    )
  }
}

enum class IconsType {
  CAM,
  WARNING,
  LOAD,
}

/**
 * Stateless Create event screen: draws [state] and reports user input through [actions]. The open
 * date picker dialog is part of [state] too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventContent(
    state: CreateEventUIState,
    actions: CreateEventActions,
    modifier: Modifier = Modifier,
    launchPicker: ((String) -> Unit) = {},
) {
  Scaffold(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { CreateEventTopBar(onBack = actions.onBack) },
      bottomBar = {
        CreateEventSaveBar(
            canCreate = state.canCreate,
            isSaving = state.isSaving,
            errorMsg = state.errorMsg,
            onCreate = actions.onCreate,
        )
      },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
    ) {
      if (state.hasImage && !state.imageUrl.isNullOrEmpty()) {
        ImageViewer(
            url = state.imageUrl,
            onEditImageClick = { launchPicker("image/*") },
            onDeleteImageClick = actions.onDeleteImageClick,
            uiState = state,
        )
      } else {
        ImagePlaceholderPicker(
            modifier =
                Modifier.padding(top = 5.dp).testTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON),
            icon = IconsType.CAM,
            text = stringResource(R.string.create_event_image_add),
            enabled = !state.isSaving,
            onEditImageClick = { launchPicker("image/*") },
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      EventFields(
          name = state.name,
          nameError = state.nameError,
          dateTimeRange =
              DateTimeRange(
                  startDate = state.startDate,
                  endDate = state.endDate,
                  startTime = state.startTime,
                  endTime = state.endTime,
              ),
          startDateError = state.startDateError,
          endDateError = state.endDateError,
          venue = state.venue,
          notes = state.notes,
          openPicker = state.openPicker,
          enabled = !state.isSaving,
          testTags = createEventFieldTags,
          onNameChange = actions.onNameChange,
          onShowPicker = actions.onShowPicker,
          onVenueChange = actions.onVenueChange,
          onNotesChange = actions.onNotesChange,
          onDatePicked = actions.onDatePicked,
          onTimePicked = actions.onTimePicked,
      )
    }
  }
}

/**
 * The save bar: the error line of a failed save above the Create button, a spinner while saving.
 */
@Composable
private fun CreateEventSaveBar(
    canCreate: Boolean,
    isSaving: Boolean,
    errorMsg: Int?,
    onCreate: () -> Unit,
) {
  Column {
    errorMsg?.let {
      FestivarFieldError(
          message = stringResource(it),
          modifier = Modifier.testTag(CreateEventScreenTestTags.SAVE_ERROR),
      )
    }
    Button(
        onClick = onCreate,
        enabled = canCreate,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .height(48.dp)
                .testTag(CreateEventScreenTestTags.CREATE_BUTTON),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
      if (isSaving) {
        CircularProgressIndicator(
            modifier =
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    .size(16.dp)
                    .testTag(CreateEventScreenTestTags.SAVING_INDICATOR),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 2.dp,
        )
      } else {
        Text(
            text = stringResource(R.string.create_event_button),
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

/**
 * This composable can be the "Add an Image" button, the fallback in the case the image fails to
 * load or the image loading
 *
 * @param icon the icon to display
 * @param text the text to display
 */
@Composable
fun ImagePlaceholderPicker(
    modifier: Modifier = Modifier,
    icon: IconsType,
    text: String,
    enabled: Boolean = true,
    onEditImageClick: () -> Unit = {},
) {
  // Source - https://stackoverflow.com/a/67039676
  // Posted by Gabriele Mariotti, modified by community. See post 'Timeline' for change history
  // Retrieved 2026-11-19, License - CC BY-SA 4.0
  val stroke =
      Stroke(
          width = 5f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
      )

  val outlineColor = MaterialTheme.colorScheme.outline
  Card(
      modifier =
          modifier.drawBehind {
            drawRoundRect(
                color = outlineColor,
                style = stroke,
                cornerRadius = CornerRadius(12.dp.toPx()),
            )
          },
      colors =
          CardColors(
              containerColor = MaterialTheme.colorScheme.outlineVariant,
              contentColor = MaterialTheme.colorScheme.onSurface,
              disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      enabled = enabled,
      onClick = onEditImageClick,
  ) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when (icon) {
        IconsType.CAM -> {
          Icon(
              imageVector = Icons.Outlined.CameraAlt,
              contentDescription = "Camera icon",
              modifier = Modifier.fillMaxWidth(1f).padding(20.dp),
          )
        }
        IconsType.WARNING -> {
          Icon(
              imageVector = Icons.Outlined.Warning,
              contentDescription = "Warning icon",
              modifier = Modifier.fillMaxWidth(1f).padding(20.dp),
          )
        }
        IconsType.LOAD -> {
          CircularProgressIndicator(
              modifier =
                  Modifier.padding(20.dp)
                      .size(16.dp)
                      .testTag(CreateEventScreenTestTags.SAVING_INDICATOR),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              strokeWidth = 2.dp,
          )
        }
      }
      Text(
          modifier = Modifier.fillMaxWidth(1f).padding(bottom = 20.dp),
          text = text,
          textAlign = TextAlign.Center,
      )
    }
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImageViewer(
    url: String,
    modifier: Modifier = Modifier,
    onEditImageClick: () -> Unit = {},
    onDeleteImageClick: () -> Unit = {},
    uiState: CreateEventUIState,
) {
  // This forces the image to not be shown taller than a square
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxWidth),
        enabled = !uiState.isSaving,
        onClick = onEditImageClick,
    ) {
      GlideImage(
          model = url,
          contentDescription = stringResource(R.string.create_event_title),
          contentScale = ContentScale.FillWidth,
          failure =
              placeholder {
                ImagePlaceholderPicker(
                    icon = IconsType.WARNING,
                    text = stringResource(R.string.create_event_image_fail),
                    enabled = !uiState.isSaving,
                )
              },
          loading =
              placeholder {
                ImagePlaceholderPicker(
                    icon = IconsType.LOAD,
                    text = stringResource(R.string.create_event_image_load),
                    enabled = !uiState.isSaving,
                )
              },
          modifier = Modifier.fillMaxWidth().testTag(CreateEventScreenTestTags.IMAGE_PREVIEW),
      )
    }
    IconButton(
        onClick = onDeleteImageClick,
        enabled = !uiState.isSaving && uiState.hasImage,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(64.dp)
                .testTag(CreateEventScreenTestTags.DELETE_IMAGE_BUTTON),
        colors =
            androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
      Icon(
          imageVector = Icons.Outlined.Delete,
          contentDescription = stringResource(R.string.create_event_image_del),
          modifier = Modifier.size(42.dp),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = stringResource(R.string.create_event_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(CreateEventScreenTestTags.TITLE),
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(CreateEventScreenTestTags.NAVIGATION_BUTTON),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.create_event_back),
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
  )
}

@Preview
@Composable
internal fun CreateEventScreenPreview() {
  AppTheme { CreateEventContent(CreateEventUIState(), CreateEventActions()) }
}
