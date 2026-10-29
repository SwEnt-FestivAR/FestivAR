// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.FestivarDateTimePickerDialog
import com.android.festivar.ui.components.FestivarDateTimeRow
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarFieldWithError
import com.android.festivar.ui.components.FestivarPrioritySelector
import com.android.festivar.ui.components.FestivarStepper
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

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
  const val PRIORITY_SELECTOR = "createTaskPrioritySelector"
  const val MAX_ASSIGN_FIELD = "createTaskMaxAssignField"
  const val MAX_ASSIGN_DECREASE_BUTTON = "createTaskMaxAssignDecreaseButton"
  const val MAX_ASSIGN_INCREASE_BUTTON = "createTaskMaxAssignIncreaseButton"
  const val ESTIMATED_TIME_FIELD = "createTaskEstimatedTimeField"
  const val ESTIMATED_TIME_ERROR = "createTaskEstimatedTimeError"
  const val TITLE_ERROR = "createTaskTitleError"
  const val START_DATE_ERROR = "createTaskStartDateError"
  const val END_DATE_ERROR = "createTaskEndDateError"
  const val SAVE_ERROR = "createTaskSaveError"
  const val CREATE_BUTTON = "createTaskCreateButton"
  const val DISCARD_DIALOG = "createTaskDiscardDialog"
  const val KEEP_EDITING_BUTTON = "createTaskKeepEditingButton"
  const val DISCARD_BUTTON = "createTaskDiscardButton"
}

/** What the user can do on the Create task screen. Lets [CreateTaskContent] stay stateless. */
class CreateTaskActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onLocationChange: (String) -> Unit = {},
    val onPriorityChange: (Priority) -> Unit = {},
    val onMaxAssignDecrease: () -> Unit = {},
    val onMaxAssignIncrease: () -> Unit = {},
    val onEstimatedMinutesChange: (String) -> Unit = {},
    val onShowPicker: (DateTimeField?) -> Unit = {},
    val onDatePicked: (LocalDate) -> Unit = {},
    val onTimePicked: (LocalTime) -> Unit = {},
    val onCreate: () -> Unit = {},
)

/**
 * Create task screen, connected to its [viewModel], which knows the event the task belongs to and
 * its owner.
 *
 * @param onBack Called when the user leaves without creating a task.
 * @param onTaskCreated Called once the task has been saved.
 */
@Composable
fun CreateTaskScreen(
    viewModel: CreateTaskViewModel,
    onBack: () -> Unit,
    onTaskCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()

  // Leaving a form that was edited asks first; a created task has nothing left to lose.
  var askToDiscard by rememberSaveable { mutableStateOf(false) }
  val requestBack = { if (state.hasChanges && !state.isCreated) askToDiscard = true else onBack() }
  BackHandler(onBack = requestBack)

  // The latest callback is called, even if the caller passed a new one during the save.
  val currentOnTaskCreated by rememberUpdatedState(onTaskCreated)
  LaunchedEffect(state.isCreated) { if (state.isCreated) currentOnTaskCreated() }

  CreateTaskContent(
      state = state,
      actions =
          CreateTaskActions(
              onBack = requestBack,
              onTitleChange = viewModel::updateTitle,
              onDescriptionChange = viewModel::updateDescription,
              onLocationChange = viewModel::updateLocation,
              onPriorityChange = viewModel::updatePriority,
              onMaxAssignDecrease = viewModel::decrementMaxAssign,
              onMaxAssignIncrease = viewModel::incrementMaxAssign,
              onEstimatedMinutesChange = viewModel::updateEstimatedMinutes,
              onShowPicker = viewModel::showPicker,
              onDatePicked = viewModel::pickDate,
              onTimePicked = viewModel::pickTime,
              onCreate = viewModel::createTask,
          ),
      modifier = modifier,
  )

  if (askToDiscard) {
    FestivarConfirmDialog(
        title = stringResource(R.string.create_task_discard_title),
        message = stringResource(R.string.create_task_discard_message),
        confirmText = stringResource(R.string.create_task_discard_confirm),
        dismissText = stringResource(R.string.create_task_discard_keep),
        onConfirm = {
          askToDiscard = false
          onBack()
        },
        onDismiss = { askToDiscard = false },
        modifier = Modifier.testTag(CreateTaskScreenTestTags.DISCARD_DIALOG),
        confirmModifier = Modifier.testTag(CreateTaskScreenTestTags.DISCARD_BUTTON),
        dismissModifier = Modifier.testTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON),
    )
  }
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
              // As in Figma, the disabled button is the beige of the hairlines with dark text;
              // Material would draw it grey with faded text.
              colors =
                  ButtonDefaults.buttonColors(
                      disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                      disabledContentColor = MaterialTheme.colorScheme.onSurface,
                  ),
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp)
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
                .padding(start = 20.dp, top = 12.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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
          date = state.startDate,
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
          date = state.endDate,
          time = state.endTime,
          dateError = state.endDateError,
          onDateClick = { actions.onShowPicker(DateTimeField.END_DATE) },
          onTimeClick = { actions.onShowPicker(DateTimeField.END_TIME) },
          dateTag = CreateTaskScreenTestTags.END_DATE_FIELD,
          dateErrorTag = CreateTaskScreenTestTags.END_DATE_ERROR,
          timeTag = CreateTaskScreenTestTags.END_TIME_FIELD,
      )
      FestivarPrioritySelector(
          selected = state.priority,
          onSelect = actions.onPriorityChange,
          label = stringResource(R.string.create_task_field_priority),
          modifier = Modifier.testTag(CreateTaskScreenTestTags.PRIORITY_SELECTOR),
      )
      FestivarStepper(
          value = state.maxAssign,
          label = stringResource(R.string.create_task_field_max_assign),
          onDecrease = actions.onMaxAssignDecrease,
          onIncrease = actions.onMaxAssignIncrease,
          decreaseDescription = stringResource(R.string.create_task_max_assign_decrease),
          increaseDescription = stringResource(R.string.create_task_max_assign_increase),
          minValue = MIN_MAX_ASSIGN,
          modifier = Modifier.testTag(CreateTaskScreenTestTags.MAX_ASSIGN_FIELD),
          decreaseModifier = Modifier.testTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON),
          increaseModifier = Modifier.testTag(CreateTaskScreenTestTags.MAX_ASSIGN_INCREASE_BUTTON),
      )
      FestivarFieldWithError(
          state.estimatedTimeError,
          CreateTaskScreenTestTags.ESTIMATED_TIME_ERROR,
      ) {
        FestivarTextField(
            value = state.estimatedMinutes,
            onValueChange = actions.onEstimatedMinutesChange,
            label = stringResource(R.string.create_task_field_estimated_time),
            isError = state.estimatedTimeError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.testTag(CreateTaskScreenTestTags.ESTIMATED_TIME_FIELD),
        )
      }
    }
  }

  FestivarDateTimePickerDialog(
      field = state.openPicker,
      range = DateTimeRange(state.startDate, state.startTime, state.endDate, state.endTime),
      onDatePicked = actions.onDatePicked,
      onTimePicked = actions.onTimePicked,
      onDismiss = { actions.onShowPicker(null) },
  )
}

// A fixed zone and dates keep the previews the same on every machine.
private val previewForm =
    CreateTaskUiState(
        title = "Run power to stage",
        description = "Extension reel is in the blue crate by the van.",
        location = "Stage, north corner",
        startDate = LocalDate.of(2026, 10, 17),
        startTime = LocalTime.of(9, 0),
        endDate = LocalDate.of(2026, 10, 17),
        endTime = LocalTime.of(11, 0),
        zone = ZoneOffset.UTC,
    )

@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun CreateTaskEmptyPreview() {
  AppTheme { CreateTaskContent(state = CreateTaskUiState(), actions = CreateTaskActions()) }
}

/** The priority, people needed and estimated time set. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun CreateTaskFilledPreview() {
  val state = previewForm.copy(priority = Priority.URGENT, maxAssign = 3, estimatedMinutes = "90")
  AppTheme { CreateTaskContent(state = state, actions = CreateTaskActions()) }
}

/** A blank title and an estimated time of zero: two errors. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun CreateTaskErrorsPreview() {
  val state = CreateTaskUiState(titleEdited = true, estimatedMinutes = "0")
  AppTheme { CreateTaskContent(state = state, actions = CreateTaskActions()) }
}

/** The dialog shown when going back from an edited form. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun CreateTaskDiscardDialogPreview() {
  AppTheme {
    CreateTaskContent(state = previewForm, actions = CreateTaskActions())
    FestivarConfirmDialog(
        title = stringResource(R.string.create_task_discard_title),
        message = stringResource(R.string.create_task_discard_message),
        confirmText = stringResource(R.string.create_task_discard_confirm),
        dismissText = stringResource(R.string.create_task_discard_keep),
        onConfirm = {},
        onDismiss = {},
        modifier = Modifier.testTag(CreateTaskScreenTestTags.DISCARD_DIALOG),
        confirmModifier = Modifier.testTag(CreateTaskScreenTestTags.DISCARD_BUTTON),
        dismissModifier = Modifier.testTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON),
    )
  }
}
