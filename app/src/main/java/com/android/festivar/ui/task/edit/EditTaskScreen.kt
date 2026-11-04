// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.task.DiscardTaskDialog
import com.android.festivar.ui.task.TaskFormBackHandling
import com.android.festivar.ui.task.TaskFormLabels
import com.android.festivar.ui.task.TaskFormTopBar
import com.android.festivar.ui.task.create.CreateTaskActions
import com.android.festivar.ui.task.create.CreateTaskContent
import com.android.festivar.ui.task.create.CreateTaskUiState
import com.android.festivar.ui.task.taskFormActions
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Test tags of the Edit task screen. The new ones are the screen, the spinner and the load error
 * with its retry; everything else keeps its tag from
 * [com.android.festivar.ui.task.create.CreateTaskScreenTestTags], as the Figma legend says.
 */
object EditTaskScreenTestTags {
  const val SCREEN = "editTaskScreen"
  const val LOADING = "editTaskLoading"
  const val LOAD_ERROR = "editTaskLoadError"
  const val LOAD_RETRY = "editTaskLoadRetry"
  const val NOT_OWNER = "editTaskNotOwner"
}

/**
 * Edit task screen, connected to its [viewModel], which knows the task to edit and who is signed
 * in.
 *
 * @param onBack Called when the user leaves without saving.
 * @param onSaved Called once the edit has been saved.
 */
@Composable
fun EditTaskScreen(
    viewModel: EditTaskViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()
  LaunchedEffect(Unit) { viewModel.load() }

  // The latest callback is called, even if the caller passed a new one during the save.
  val currentOnSaved by rememberUpdatedState(onSaved)
  LaunchedEffect(state.isSaved) { if (state.isSaved) currentOnSaved() }

  TaskFormBackHandling(
      needsConfirmation = state.needsDiscardConfirmation,
      onBack = onBack,
      dialogTitle = R.string.edit_task_discard_title,
  ) { requestBack ->
    EditTaskContent(
        state = state,
        actions = taskFormActions(viewModel, onBack = requestBack, onSubmit = viewModel::save),
        onRetry = viewModel::load,
        modifier = modifier,
    )
  }
}

/**
 * Stateless Edit task screen: the Create task form filled with the task once it is loaded, the load
 * error with a retry when a load failed, a short notice instead of the form for a user who does not
 * own the task, and otherwise a spinner.
 *
 * @param actions The form's input; `onCreate` saves the edit.
 * @param onRetry Loads the task again after a failed load.
 */
@Composable
fun EditTaskContent(
    state: EditTaskUiState,
    actions: CreateTaskActions,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.testTag(EditTaskScreenTestTags.SCREEN)) {
    val loadError = state.errorMsg
    when {
      // Reached through a link to the edit route: the details only show the edit button to the
      // owner. A form that looks editable but can never be saved would not say why.
      state.task != null && !state.isOwner() ->
          NoForm(actions.onBack) {
            Text(
                text = stringResource(R.string.edit_task_not_owner),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.padding(horizontal = 32.dp).testTag(EditTaskScreenTestTags.NOT_OWNER),
            )
          }
      state.task != null ->
          CreateTaskContent(
              state = state.form.copy(errorMsg = state.errorMsg),
              actions = actions,
              labels = TaskFormLabels(R.string.edit_task, R.string.edit_task_save),
              canSubmit = state.canSave,
              maxAssignError = state.maxAssignError,
          )
      loadError != null && !state.isLoading ->
          NoForm(actions.onBack) { LoadError(loadError, onRetry) }
      // The spinner also covers the first frames, before load() has run.
      else ->
          NoForm(actions.onBack) {
            CircularProgressIndicator(Modifier.testTag(EditTaskScreenTestTags.LOADING))
          }
    }
  }
}

/** The form's top bar over [content] centered, for the states without a form. */
@Composable
private fun NoForm(onBack: () -> Unit, content: @Composable () -> Unit) {
  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { TaskFormTopBar(R.string.edit_task, onBack) },
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
        modifier = Modifier.testTag(EditTaskScreenTestTags.LOAD_ERROR),
    )
    TextButton(
        onClick = onRetry,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
        modifier = Modifier.testTag(EditTaskScreenTestTags.LOAD_RETRY),
    ) {
      Text(stringResource(R.string.action_retry))
    }
  }
}

// A fixed zone and dates keep the previews the same on every machine.
private val previewStart = ZonedDateTime.of(2026, 10, 17, 9, 0, 0, 0, ZoneOffset.UTC)

private val previewTask =
    Task(
        taskId = "t1",
        eventId = "e1",
        title = "Run power to stage",
        description = "Extension reel is in the blue crate by the van.",
        location = "Stage, north corner",
        startTime = previewStart,
        endTime = previewStart.plusHours(2),
        maxAssign = 3,
        assignees = listOf(User("u1"), User("u2")),
        ownerId = "owner-1",
    )

private val previewForm =
    CreateTaskUiState(
        title = previewTask.title,
        description = previewTask.description,
        location = previewTask.location,
        startDate = LocalDate.of(2026, 10, 17),
        startTime = LocalTime.of(9, 0),
        endDate = LocalDate.of(2026, 10, 17),
        endTime = LocalTime.of(11, 0),
        maxAssign = previewTask.maxAssign,
        zone = ZoneOffset.UTC,
    )

private val previewLoaded =
    EditTaskUiState(task = previewTask, form = previewForm, isOwner = { true })

@Composable
private fun EditTaskPreview(state: EditTaskUiState) {
  AppTheme { EditTaskContent(state = state, actions = CreateTaskActions(), onRetry = {}) }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun EditTaskLoadingPreview() {
  EditTaskPreview(EditTaskUiState())
}

/** The task could not be loaded. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun EditTaskLoadErrorPreview() {
  EditTaskPreview(
      EditTaskUiState(isLoading = false, errorMsg = R.string.edit_task_error_load_failed)
  )
}

/** The form filled with the task, with the title changed so "Save changes" is enabled. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun EditTaskLoadedPreview() {
  EditTaskPreview(previewLoaded.copy(form = previewForm.copy(title = "Run power to the stage")))
}

/** A blank title and fewer people needed than the two already assigned. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun EditTaskErrorsPreview() {
  EditTaskPreview(
      previewLoaded.copy(form = previewForm.copy(title = "", titleEdited = true, maxAssign = 1))
  )
}

/** The dialog shown when going back from an edited form. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
internal fun EditTaskDiscardDialogPreview() {
  AppTheme {
    EditTaskContent(
        state = previewLoaded.copy(form = previewForm.copy(title = "Run power to the stage")),
        actions = CreateTaskActions(),
        onRetry = {},
    )
    DiscardTaskDialog(onConfirm = {}, onDismiss = {}, title = R.string.edit_task_discard_title)
  }
}
