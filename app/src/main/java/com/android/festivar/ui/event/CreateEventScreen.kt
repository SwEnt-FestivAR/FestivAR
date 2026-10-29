package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.FestivarDatePickerDialog
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarPickerField
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

object CreateEventScreenTestTags {
  const val NAVIGATION_BUTTON = "navigationButton"
  const val TITLE = "title"
  const val NAME_FIELD = "nameField"
  const val START_DATE_FIELD = "startDateField"
  const val END_DATE_FIELD = "endDateField"
  const val LOCATION_FIELD = "locationField"
  const val DESCRIPTION_FIELD = "descriptionField"
  const val NAME_ERROR = "nameError"
  const val ENDS_ERROR = "endsError"
  const val ADDRESS_ERROR = "addressError"
  const val CREATE_BUTTON = "createButton"
  const val SAVING = "createEventSaving"
  const val SAVE_ERROR = "createEventSaveError"
}

/** What the user can do on the Create event screen. Lets [CreateEventContent] stay stateless. */
class CreateEventActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onStartDateClick: () -> Unit = {},
    val onEndDateClick: () -> Unit = {},
    val onVenueChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onDatePicked: (DateField, LocalDate) -> Unit = { _, _ -> },
    val onDismissDatePicker: () -> Unit = {},
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
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreated: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(uiState.createdEventId) { if (uiState.createdEventId != null) onCreated() }

  CreateEventContent(
      state = uiState,
      actions =
          CreateEventActions(
              onBack = onBack,
              onNameChange = {
                if (uiState.errorName) {
                  viewModel.removeErrorName()
                }
                viewModel.updateName(it)
              },
              onStartDateClick = {
                if (uiState.errorDate) {
                  viewModel.removeErrorDate()
                }
                viewModel.updateActiveDatePicker(DateField.START)
              },
              onEndDateClick = {
                if (uiState.errorDate) {
                  viewModel.removeErrorDate()
                }
                viewModel.updateActiveDatePicker(DateField.END)
              },
              onVenueChange = {
                if (uiState.errorVenue) {
                  viewModel.removeErrorVenue()
                }
                viewModel.updateVenue(it)
              },
              onNotesChange = viewModel::updateNotes,
              onDatePicked = { dateField, selectedDate ->
                val zone = ZoneId.systemDefault()
                if (dateField == DateField.START) {
                  val start = selectedDate.atStartOfDay(zone)
                  viewModel.updateStartDateMillis(start.toInstant().toEpochMilli())
                  viewModel.updateStartDate(start)
                } else {
                  val end = selectedDate.plusDays(1).atStartOfDay(zone).minusSeconds(1)
                  viewModel.updateEndDateMillis(end.toInstant().toEpochMilli())
                  viewModel.updateEndDate(end)
                }
                viewModel.updateActiveDatePicker(null)
              },
              onDismissDatePicker = { viewModel.updateActiveDatePicker(null) },
              onCreate = {
                if (viewModel.validEvent()) viewModel.createEvent() else viewModel.setErrors()
              },
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
      FestivarTextField(
          value = state.name,
          onValueChange = actions.onNameChange,
          label = stringResource(R.string.create_event_field_name),
          isError = state.errorName,
          modifier = Modifier.testTag(CreateEventScreenTestTags.NAME_FIELD),
      )
      if (state.errorName) {
        FestivarFieldError(
            message = stringResource(R.string.create_event_error_name),
            modifier = Modifier.testTag(CreateEventScreenTestTags.NAME_ERROR),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FestivarPickerField(
            value = state.startDate.toDisplayDate(),
            onClick = actions.onStartDateClick,
            label = stringResource(R.string.create_event_field_start_date),
            isError = state.errorDate && state.startDate == null,
            modifier = Modifier.weight(1f).testTag(CreateEventScreenTestTags.START_DATE_FIELD),
        )
        FestivarPickerField(
            value = state.endDate.toDisplayDate(),
            onClick = actions.onEndDateClick,
            label = stringResource(R.string.create_event_field_end_date),
            isError = state.errorDate,
            modifier = Modifier.weight(1f).testTag(CreateEventScreenTestTags.END_DATE_FIELD),
        )
      }
      if (state.errorDate) {
        FestivarFieldError(
            message =
                stringResource(
                    if (state.startDate == null || state.endDate == null) {
                      R.string.create_event_error_date_missing
                    } else {
                      R.string.create_event_error_date_order
                    }
                ),
            modifier = Modifier.testTag(CreateEventScreenTestTags.ENDS_ERROR),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      FestivarTextField(
          value = state.venue,
          onValueChange = actions.onVenueChange,
          label = stringResource(R.string.create_event_field_venue),
          isError = state.errorVenue,
          modifier = Modifier.testTag(CreateEventScreenTestTags.LOCATION_FIELD),
      )
      if (state.errorVenue) {
        FestivarFieldError(
            message = stringResource(R.string.create_event_error_venue),
            modifier = Modifier.testTag(CreateEventScreenTestTags.ADDRESS_ERROR),
        )
      }
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

  state.activeDatePicker?.let { dateField ->
    CreateEventDatePicker(
        dateField = dateField,
        state = state,
        onDatePicked = actions.onDatePicked,
        onDismiss = actions.onDismissDatePicker,
    )
  }
}

/**
 * The save bar: the error line of a failed save above the Create button, a spinner while saving.
 */
@Composable
private fun CreateEventSaveBar(isSaving: Boolean, errorMsg: Int?, onCreate: () -> Unit) {
  Column {
    errorMsg?.let {
      FestivarFieldError(
          message = stringResource(it),
          modifier = Modifier.testTag(CreateEventScreenTestTags.SAVE_ERROR),
      )
    }
    Button(
        onClick = onCreate,
        enabled = !isSaving,
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
            modifier = Modifier.size(16.dp).testTag(CreateEventScreenTestTags.SAVING),
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
 * The date dialog of [dateField]. The form takes days: an event starts when its first day starts
 * and ends when its last day ends, so a one day event is valid and stays ongoing until its
 * midnight.
 */
@Composable
private fun CreateEventDatePicker(
    dateField: DateField,
    state: CreateEventUIState,
    onDatePicked: (DateField, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
  val current = if (dateField == DateField.START) state.startDate else state.endDate
  FestivarDatePickerDialog(
      initial = current?.toLocalDate(),
      onConfirm = { selectedDate -> onDatePicked(dateField, selectedDate) },
      onDismiss = onDismiss,
  )
}

private fun ZonedDateTime?.toDisplayDate(
    locale: Locale = Locale.getDefault(),
): String =
    this?.let {
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(it)
    } ?: ""

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
  val addDurationMillis = 3000
  class SlowEventsRepository : EventsRepository by EventsRepositoryLocal() {
    override suspend fun addEvent(event: Event): Unit = delay(addDurationMillis.milliseconds)
  }

  val creator = User("creator-123", "Alice", "AliceInWonderland")
  AppTheme {
    CreateEventScreen(
        viewModel =
            CreateEventViewModel(
                SlowEventsRepository(),
                UsersRepositoryLocal(listOf(creator)),
                creator.uid,
            )
    )
  }
}
