package com.android.festivar.ui.event.edit

// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FestivarDateTimePickerDialog
import com.android.festivar.ui.components.FestivarDateTimeRow
import com.android.festivar.ui.components.FestivarDiscardDialog
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object EditEventScreenTestTags {
  const val BACK_BUTTON = "editEventBackButton"
  const val TITLE = "editEventTitle"
  const val NAME_FIELD = "editEventNameField"
  const val START_DATE_FIELD = "editEventStartDateField"
  const val START_TIME_FIELD = "editEventStartTimeField"
  const val END_DATE_FIELD = "editEventEndDateField"
  const val END_TIME_FIELD = "editEventEndTimeField"
  const val LOCATION_FIELD = "editEventLocationField"
  const val DESCRIPTION_FIELD = "editEventDescriptionField"
  const val NAME_ERROR = "editEventNameError"
  const val START_DATE_ERROR = "editEventStartDateError"
  const val END_DATE_ERROR = "editEventEndDateError"
  const val SAVE_BUTTON = "editEventSaveButton"
  const val SAVING_INDICATOR = "editEventSaving"
  const val FETCH_ERROR = "editEventFetchError"
  const val SAVE_ERROR = "editEventSaveError"
  const val DISCARD_CONFIRM = "editEventDiscardConfirm"
  const val DISCARD_KEEP_EDITING = "editEventDiscardKeepEditing"
}

/** Callbacks handled by the Edit event screen. */
class EditEventActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onShowPicker: (DateTimeField?) -> Unit = {},
    val onVenueChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onDatePicked: (LocalDate) -> Unit = {},
    val onTimePicked: (LocalTime) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancelDiscard: () -> Unit = {},
    val onConfirmDiscard: () -> Unit = {},
)

/** Observes the edit form and reports completion through [onSaved] or [onBack]. */
@Composable
fun EditEventScreen(
    eventId: String,
    viewModel: EditEventViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(eventId) { viewModel.fetchEvent(eventId) }
  LaunchedEffect(uiState.isDiscarded) { if (uiState.isDiscarded) onBack() }
  LaunchedEffect(uiState.savedEventId) { if (uiState.savedEventId != null) onSaved() }

  EditEventContent(
      state = uiState,
      actions =
          EditEventActions(
              onBack = viewModel::requestDiscard,
              onNameChange = viewModel::updateName,
              onShowPicker = viewModel::showPicker,
              onVenueChange = viewModel::updateVenue,
              onNotesChange = viewModel::updateNotes,
              onDatePicked = viewModel::pickDate,
              onTimePicked = viewModel::pickTime,
              onSave = viewModel::saveChanges,
              onCancelDiscard = viewModel::cancelDiscard,
              onConfirmDiscard = viewModel::confirmDiscard,
          ),
      modifier = modifier,
  )
}

/** Stateless edit form. All visible state and user actions come from [state] and [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventContent(
    state: EditEventUIState,
    actions: EditEventActions,
    modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier.imePadding(),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { EditEventTopBar(onBack = actions.onBack) },
      bottomBar = {
        if (state.event != null) {
          EditEventSaveBar(
              canSave = state.canSave,
              isSaving = state.isSaving,
              errorMsg = state.errorMsg,
              onSave = actions.onSave,
          )
        }
      },
  ) { padding ->
    when {
      state.event != null ->
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(padding)
                      .verticalScroll(rememberScrollState())
                      .padding(horizontal = 12.dp),
          ) {
            FestivarFieldWithError(state.nameError, EditEventScreenTestTags.NAME_ERROR) {
              FestivarTextField(
                  value = state.name,
                  onValueChange = actions.onNameChange,
                  label = stringResource(R.string.create_event_field_name),
                  isError = state.nameError != null,
                  modifier = Modifier.testTag(EditEventScreenTestTags.NAME_FIELD),
              )
            }
            Spacer(modifier = Modifier.height(14.dp))

            FestivarDateTimeRow(
                dateLabel = stringResource(R.string.create_event_field_start_date),
                timeLabel = stringResource(R.string.create_event_field_start_time),
                date = state.startDate,
                time = state.startTime,
                dateError = state.startDateError,
                onDateClick = { actions.onShowPicker(DateTimeField.START_DATE) },
                onTimeClick = { actions.onShowPicker(DateTimeField.START_TIME) },
                dateTag = EditEventScreenTestTags.START_DATE_FIELD,
                dateErrorTag = EditEventScreenTestTags.START_DATE_ERROR,
                timeTag = EditEventScreenTestTags.START_TIME_FIELD,
            )
            Spacer(modifier = Modifier.height(14.dp))

            FestivarDateTimeRow(
                dateLabel = stringResource(R.string.create_event_field_end_date),
                timeLabel = stringResource(R.string.create_event_field_end_time),
                date = state.endDate,
                time = state.endTime,
                dateError = state.endDateError,
                onDateClick = { actions.onShowPicker(DateTimeField.END_DATE) },
                onTimeClick = { actions.onShowPicker(DateTimeField.END_TIME) },
                dateTag = EditEventScreenTestTags.END_DATE_FIELD,
                dateErrorTag = EditEventScreenTestTags.END_DATE_ERROR,
                timeTag = EditEventScreenTestTags.END_TIME_FIELD,
            )
            Spacer(modifier = Modifier.height(14.dp))

            FestivarTextField(
                value = state.venue,
                onValueChange = actions.onVenueChange,
                label = stringResource(R.string.create_event_field_venue),
                modifier = Modifier.testTag(EditEventScreenTestTags.LOCATION_FIELD),
            )
            Spacer(modifier = Modifier.height(8.dp))

            FestivarTextField(
                value = state.notes,
                onValueChange = actions.onNotesChange,
                label = stringResource(R.string.create_event_field_notes),
                singleLine = false,
                modifier = Modifier.testTag(EditEventScreenTestTags.DESCRIPTION_FIELD),
            )
            Spacer(modifier = Modifier.height(14.dp))
          }
      state.isLoading ->
          androidx.compose.foundation.layout.Box(
              modifier = Modifier.fillMaxSize().padding(padding),
              contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
      state.errorFetchMsg != null ->
          androidx.compose.foundation.layout.Box(
              modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
              contentAlignment = Alignment.Center,
          ) {
            Text(
                text = stringResource(state.errorFetchMsg),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(EditEventScreenTestTags.FETCH_ERROR),
            )
          }
    }
  }

  FestivarDateTimePickerDialog(
      field = state.openPicker,
      range =
          DateTimeRange(
              startDate = state.startDate,
              endDate = state.endDate,
              startTime = state.startTime,
              endTime = state.endTime,
          ),
      onDatePicked = actions.onDatePicked,
      onTimePicked = actions.onTimePicked,
      onDismiss = { actions.onShowPicker(null) },
  )

  if (state.showDiscardConfirmation) {
    FestivarDiscardDialog(
        title = stringResource(R.string.edit_event_discard_title),
        onConfirm = actions.onConfirmDiscard,
        onDismiss = actions.onCancelDiscard,
        confirmModifier = Modifier.testTag(EditEventScreenTestTags.DISCARD_CONFIRM),
        dismissModifier = Modifier.testTag(EditEventScreenTestTags.DISCARD_KEEP_EDITING),
    )
  }
}

@Composable
private fun EditEventSaveBar(
    canSave: Boolean,
    isSaving: Boolean,
    errorMsg: Int?,
    onSave: () -> Unit,
) {
  Column {
    errorMsg?.let {
      FestivarFieldError(
          message = stringResource(it),
          modifier = Modifier.testTag(EditEventScreenTestTags.SAVE_ERROR),
      )
    }
    Button(
        onClick = onSave,
        enabled = canSave,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .height(48.dp)
                .testTag(EditEventScreenTestTags.SAVE_BUTTON),
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
            modifier = Modifier.size(16.dp).testTag(EditEventScreenTestTags.SAVING_INDICATOR),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 2.dp,
        )
      } else {
        Text(
            text = stringResource(R.string.edit_event_button_save),
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEventTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = stringResource(R.string.edit_event_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(EditEventScreenTestTags.TITLE),
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(EditEventScreenTestTags.BACK_BUTTON),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.edit_event_back),
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
  )
}

@Preview(showBackground = true)
@Composable
private fun EditEventScreenPreview() {
  val zone = ZoneId.of("Europe/Zurich")
  val startDate = LocalDate.now().plusDays(10)
  val endDate = startDate.plusDays(2)
  val event =
      Event(
          eventId = "preview",
          title = "Fête de l'Asso 2026",
          description = "Enter by the main gate.",
          members = listOf(User(uid = "organizer", name = "Alice")),
          startDate = ZonedDateTime.of(startDate, LocalTime.of(16, 0), zone),
          endDate = ZonedDateTime.of(endDate, LocalTime.of(10, 0), zone),
          location = "Parc de Vidy, Lausanne",
      )

  AppTheme {
    EditEventContent(
        state =
            EditEventUIState(
                event = event,
                name = event.title,
                startDate = startDate,
                endDate = endDate,
                startTime = LocalTime.of(16, 0),
                endTime = LocalTime.of(10, 0),
                venue = event.location,
                notes = "Access by the lake side gate.",
                zone = zone,
                endZone = zone,
            ),
        actions = EditEventActions(),
    )
  }
}
