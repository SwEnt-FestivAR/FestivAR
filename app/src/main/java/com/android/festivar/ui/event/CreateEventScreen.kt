package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import android.annotation.SuppressLint
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FestivarDateTimePickerDialog
import com.android.festivar.ui.components.FestivarDateTimeRow
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime

object CreateEventScreenTestTags {
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
}

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
)

/**
 * A screen for creating events.
 *
 * @param onCreated Called once the event is saved, so the caller can leave the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreated: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(uiState.createdEventId) { if (uiState.createdEventId != null) onCreated() }

  CreateEventContent(
      state = uiState,
      actions =
          CreateEventActions(
              onBack = onBack,
              onNameChange = viewModel::updateName,
              onShowPicker = viewModel::showPicker,
              onVenueChange = viewModel::updateVenue,
              onNotesChange = viewModel::updateNotes,
              onDatePicked = viewModel::pickDate,
              onTimePicked = viewModel::pickTime,
              onCreate = viewModel::createEvent,
          ),
      modifier = modifier,
  )
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
) {
  Scaffold(
      modifier = modifier.imePadding(),
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
      FestivarFieldWithError(state.nameError, CreateEventScreenTestTags.NAME_ERROR) {
        FestivarTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.create_event_field_name),
            isError = state.nameError != null,
            modifier = Modifier.testTag(CreateEventScreenTestTags.NAME_FIELD),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      FestivarDateTimeRow(
          dateLabel = stringResource(R.string.create_event_field_start_date),
          timeLabel = stringResource(R.string.create_event_field_start_time),
          date = state.startDate,
          time = null,
          dateError = state.startDateError,
          onDateClick = { actions.onShowPicker(DateTimeField.START_DATE) },
          onTimeClick = { actions.onShowPicker(DateTimeField.START_TIME) },
          dateTag = CreateEventScreenTestTags.START_DATE_FIELD,
          dateErrorTag = CreateEventScreenTestTags.START_DATE_ERROR,
          timeTag = CreateEventScreenTestTags.START_TIME_FIELD,
      )
      Spacer(modifier = Modifier.height(14.dp))

      FestivarDateTimeRow(
          dateLabel = stringResource(R.string.create_event_field_end_date),
          timeLabel = stringResource(R.string.create_event_field_end_time),
          date = state.endDate,
          time = null,
          dateError = state.endDateError,
          onDateClick = { actions.onShowPicker(DateTimeField.END_DATE) },
          onTimeClick = { actions.onShowPicker(DateTimeField.END_TIME) },
          dateTag = CreateEventScreenTestTags.END_DATE_FIELD,
          dateErrorTag = CreateEventScreenTestTags.END_DATE_ERROR,
          timeTag = CreateEventScreenTestTags.END_TIME_FIELD,
      )
      Spacer(modifier = Modifier.height(14.dp))

      FestivarTextField(
          value = state.venue,
          onValueChange = actions.onVenueChange,
          label = stringResource(R.string.create_event_field_venue),
          modifier = Modifier.testTag(CreateEventScreenTestTags.LOCATION_FIELD),
      )
      Spacer(modifier = Modifier.height(8.dp))

      FestivarTextField(
          value = state.notes,
          onValueChange = actions.onNotesChange,
          label = stringResource(R.string.create_event_field_notes),
          singleLine = false,
          modifier = Modifier.testTag(CreateEventScreenTestTags.DESCRIPTION_FIELD),
      )
      Spacer(modifier = Modifier.height(14.dp))
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
            modifier = Modifier.size(16.dp).testTag(CreateEventScreenTestTags.SAVING_INDICATOR),
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

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
private fun CreateEventScreenPreview() {
  AppTheme { CreateEventScreen(CreateEventViewModel(EventsRepositoryLocal())) }
}
