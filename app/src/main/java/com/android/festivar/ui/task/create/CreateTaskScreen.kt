// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.ui.components.FestivarDatePickerDialog
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarPickerField
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.components.FestivarTimePickerDialog
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime

object CreateTaskScreenTestTags {
  const val BACK_BUTTON = "createTaskBackButton"
  const val TITLE = "createTaskTitle"
  const val TITLE_FIELD = "createTaskTitleField"
  const val DESCRIPTION_FIELD = "createTaskDescriptionField"
  const val LOCATION_FIELD = "createTaskLocationField"
  const val START_DATE_FIELD = "createTaskStartDateField"
  const val START_TIME_FIELD = "createTaskStartTimeField"
  const val END_DATE_FIELD = "createTaskEndDateField"
  const val END_TIME_FIELD = "createTaskEndTimeField"
  const val TITLE_ERROR = "createTaskTitleError"
  const val START_DATE_ERROR = "createTaskStartDateError"
  const val END_DATE_ERROR = "createTaskEndDateError"
  const val SAVE_ERROR = "createTaskSaveError"
  const val CREATE_BUTTON = "createTaskCreateButton"
}

/** What the user can do on the Create task screen. Lets [CreateTaskContent] stay stateless. */
class CreateTaskActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onLocationChange: (String) -> Unit = {},
    val onStartDateChange: (LocalDate) -> Unit = {},
    val onStartTimeChange: (LocalTime) -> Unit = {},
    val onEndDateChange: (LocalDate) -> Unit = {},
    val onEndTimeChange: (LocalTime) -> Unit = {},
    val onCreate: () -> Unit = {},
)

/** The picker dialog currently open, if any. */
private enum class Picker {
  START_DATE,
  START_TIME,
  END_DATE,
  END_TIME,
}

/**
 * Create task screen, connected to its [viewModel].
 *
 * @param eventId The event the created task belongs to.
 * @param onBack Called when the user leaves without creating a task.
 * @param onTaskCreated Called once the task has been saved.
 */
@Composable
fun CreateTaskScreen(
    eventId: String,
    onBack: () -> Unit,
    onTaskCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTaskViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsState()

  LaunchedEffect(state.isCreated) { if (state.isCreated) onTaskCreated() }

  CreateTaskContent(
      state = state,
      actions =
          CreateTaskActions(
              onBack = onBack,
              onTitleChange = viewModel::updateTitle,
              onDescriptionChange = viewModel::updateDescription,
              onLocationChange = viewModel::updateLocation,
              onStartDateChange = viewModel::updateStartDate,
              onStartTimeChange = viewModel::updateStartTime,
              onEndDateChange = viewModel::updateEndDate,
              onEndTimeChange = viewModel::updateEndTime,
              onCreate = { viewModel.createTask(eventId) },
          ),
      modifier = modifier,
  )
}

/** Stateless Create task screen: draws [state] and reports user input through [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskContent(
    state: CreateTaskUiState,
    actions: CreateTaskActions,
    modifier: Modifier = Modifier,
) {
  var openPicker by rememberSaveable { mutableStateOf<Picker?>(null) }

  Scaffold(
      modifier = modifier.imePadding(),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.create_task),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(CreateTaskScreenTestTags.TITLE),
              )
            },
            navigationIcon = {
              IconButton(
                  onClick = actions.onBack,
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
      },
      bottomBar = {
        Column {
          state.errorMsg?.let {
            FestivarFieldError(
                message = stringResource(it),
                modifier = Modifier.testTag(CreateTaskScreenTestTags.SAVE_ERROR),
            )
          }
          Button(
              onClick = actions.onCreate,
              enabled = state.canCreate,
              shape = RoundedCornerShape(12.dp),
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 12.dp)
                      .height(52.dp)
                      .testTag(CreateTaskScreenTestTags.CREATE_BUTTON),
          ) {
            Text(stringResource(R.string.create_task), style = MaterialTheme.typography.labelLarge)
          }
        }
      },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      FieldWithError(state.titleError, CreateTaskScreenTestTags.TITLE_ERROR) {
        FestivarTextField(
            value = state.title,
            onValueChange = actions.onTitleChange,
            label = stringResource(R.string.create_task_field_title),
            isError = state.titleError != null,
            modifier = Modifier.testTag(CreateTaskScreenTestTags.TITLE_FIELD),
        )
      }
      FestivarTextField(
          value = state.description,
          onValueChange = actions.onDescriptionChange,
          label = stringResource(R.string.create_task_field_description),
          singleLine = false,
          modifier = Modifier.testTag(CreateTaskScreenTestTags.DESCRIPTION_FIELD),
      )
      FestivarTextField(
          value = state.location,
          onValueChange = actions.onLocationChange,
          label = stringResource(R.string.create_task_field_location),
          modifier = Modifier.testTag(CreateTaskScreenTestTags.LOCATION_FIELD),
      )
      DateTimeRow(
          dateLabel = stringResource(R.string.create_task_field_start_date),
          timeLabel = stringResource(R.string.create_task_field_start_time),
          date = state.startDate,
          time = state.startTime,
          dateError = state.startDateError,
          onDateClick = { openPicker = Picker.START_DATE },
          onTimeClick = { openPicker = Picker.START_TIME },
          dateTag = CreateTaskScreenTestTags.START_DATE_FIELD,
          dateErrorTag = CreateTaskScreenTestTags.START_DATE_ERROR,
          timeTag = CreateTaskScreenTestTags.START_TIME_FIELD,
      )
      DateTimeRow(
          dateLabel = stringResource(R.string.create_task_field_end_date),
          timeLabel = stringResource(R.string.create_task_field_end_time),
          date = state.endDate,
          time = state.endTime,
          dateError = state.endDateError,
          onDateClick = { openPicker = Picker.END_DATE },
          onTimeClick = { openPicker = Picker.END_TIME },
          dateTag = CreateTaskScreenTestTags.END_DATE_FIELD,
          dateErrorTag = CreateTaskScreenTestTags.END_DATE_ERROR,
          timeTag = CreateTaskScreenTestTags.END_TIME_FIELD,
      )
    }
  }

  PickerDialog(
      picker = openPicker,
      state = state,
      actions = actions,
      onClose = { openPicker = null },
  )
}

@Composable
private fun DateTimeRow(
    dateLabel: String,
    timeLabel: String,
    date: LocalDate?,
    time: LocalTime?,
    dateError: FieldError?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    dateTag: String,
    dateErrorTag: String,
    timeTag: String,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    FieldWithError(dateError, dateErrorTag, Modifier.weight(1f)) {
      FestivarPickerField(
          value = date?.let(::formatDate).orEmpty(),
          label = dateLabel,
          onClick = onDateClick,
          isError = dateError != null,
          modifier = Modifier.testTag(dateTag),
      )
    }
    FestivarPickerField(
        value = time?.let(::formatTime).orEmpty(),
        label = timeLabel,
        onClick = onTimeClick,
        modifier = Modifier.weight(1f).testTag(timeTag),
    )
  }
}

/** Shows [field] with the message for [error] under it, when there is one. */
@Composable
private fun FieldWithError(
    error: FieldError?,
    errorTag: String,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
  Column(modifier = modifier) {
    field()
    error?.let {
      FestivarFieldError(
          message = stringResource(fieldErrorMessageRes(it)),
          modifier = Modifier.testTag(errorTag),
      )
    }
  }
}

@Composable
private fun PickerDialog(
    picker: Picker?,
    state: CreateTaskUiState,
    actions: CreateTaskActions,
    onClose: () -> Unit,
) {
  when (picker) {
    Picker.START_DATE ->
        FestivarDatePickerDialog(
            initial = state.startDate,
            onConfirm = {
              actions.onStartDateChange(it)
              onClose()
            },
            onDismiss = onClose,
        )
    Picker.END_DATE ->
        FestivarDatePickerDialog(
            initial = state.endDate ?: state.startDate,
            onConfirm = {
              actions.onEndDateChange(it)
              onClose()
            },
            onDismiss = onClose,
        )
    Picker.START_TIME ->
        FestivarTimePickerDialog(
            initial = state.startTime,
            onConfirm = {
              actions.onStartTimeChange(it)
              onClose()
            },
            onDismiss = onClose,
        )
    Picker.END_TIME ->
        FestivarTimePickerDialog(
            initial = state.endTime ?: state.startTime,
            onConfirm = {
              actions.onEndTimeChange(it)
              onClose()
            },
            onDismiss = onClose,
        )
    null -> Unit
  }
}

/** The string resource of the message shown under a field for [error]. */
@StringRes
fun fieldErrorMessageRes(error: FieldError): Int =
    when (error) {
      FieldError.EMPTY_TITLE -> R.string.create_task_error_empty_title
      FieldError.MISSING_DATE -> R.string.create_task_error_missing_date
      FieldError.END_BEFORE_START -> R.string.create_task_error_end_before_start
    }

@Preview(showBackground = true)
@Composable
private fun CreateTaskContentPreview() {
  AppTheme {
    CreateTaskContent(
        state =
            CreateTaskUiState(
                title = "Run power to stage",
                description = "Extension reel is in the blue crate by the van.",
                location = "Stage, north corner",
                startDate = LocalDate.of(2026, 10, 17),
                startTime = LocalTime.of(9, 30),
                endDate = LocalDate.of(2026, 10, 17),
                endTime = LocalTime.of(9, 50),
            ),
        actions = CreateTaskActions(),
    )
  }
}
