// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FestivarDateTimePickerDialog
import com.android.festivar.ui.components.FestivarDateTimeRow
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarTextField
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    val onShowPicker: (DateTimeField?) -> Unit = {},
    val onDatePicked: (LocalDate) -> Unit = {},
    val onTimePicked: (LocalTime) -> Unit = {},
    val onCreate: () -> Unit = {},
)

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

  // The latest callback is called, even if the caller passed a new one during the save.
  val currentOnTaskCreated by rememberUpdatedState(onTaskCreated)
  LaunchedEffect(state.isCreated) { if (state.isCreated) currentOnTaskCreated() }

  CreateTaskContent(
      state = state,
      actions =
          CreateTaskActions(
              onBack = onBack,
              onTitleChange = viewModel::updateTitle,
              onDescriptionChange = viewModel::updateDescription,
              onLocationChange = viewModel::updateLocation,
              onShowPicker = viewModel::showPicker,
              onDatePicked = {
                  date ->
                    viewModel.pickDate(date.atStartOfDay(ZoneId.systemDefault()))
                 },
              onTimePicked = viewModel::pickTime,
              onCreate = { viewModel.createTask(eventId) },
          ),
      modifier = modifier,
  )
}

/**
 * Stateless Create task screen: draws [state] and reports user input through [actions]. The open
 * picker dialog is part of [state] too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskContent(
    state: CreateTaskUiState,
    actions: CreateTaskActions,
    modifier: Modifier = Modifier,
) {
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
      FestivarFieldWithError(state.titleError, CreateTaskScreenTestTags.TITLE_ERROR) {
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
      FestivarDateTimeRow(
          dateLabel = stringResource(R.string.create_task_field_start_date),
          timeLabel = stringResource(R.string.create_task_field_start_time),
          date = state.startDate?.toLocalDate(),
          time = state.startTime,
          dateError = state.startDateError,
          onDateClick = { actions.onShowPicker(DateTimeField.START_DATE) },
          onTimeClick = { actions.onShowPicker(DateTimeField.START_TIME) },
          dateTag = CreateTaskScreenTestTags.START_DATE_FIELD,
          dateErrorTag = CreateTaskScreenTestTags.START_DATE_ERROR,
          timeTag = CreateTaskScreenTestTags.START_TIME_FIELD,
      )
      FestivarDateTimeRow(
          dateLabel = stringResource(R.string.create_task_field_end_date),
          timeLabel = stringResource(R.string.create_task_field_end_time),
          date = state.endDate?.toLocalDate(),
          time = state.endTime,
          dateError = state.endDateError,
          onDateClick = { actions.onShowPicker(DateTimeField.END_DATE) },
          onTimeClick = { actions.onShowPicker(DateTimeField.END_TIME) },
          dateTag = CreateTaskScreenTestTags.END_DATE_FIELD,
          dateErrorTag = CreateTaskScreenTestTags.END_DATE_ERROR,
          timeTag = CreateTaskScreenTestTags.END_TIME_FIELD,
      )
    }
  }

  FestivarDateTimePickerDialog(
      field = state.openPicker,
      range = DateTimeRange(
          state.startDate?.toLocalDate(),
          state.startTime,
          state.endDate?.toLocalDate(),
          state.endTime
      ),
      onDatePicked = actions.onDatePicked,
      onTimePicked = actions.onTimePicked,
      onDismiss = { actions.onShowPicker(null) },
  )
}
