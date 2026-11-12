// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.task.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.components.PeopleCard
import com.android.festivar.ui.components.PillStyle
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.UserListBottomSheet
import com.android.festivar.ui.components.UserTag
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.components.peopleCount
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks
import com.android.festivar.ui.voicenote.VoiceNotePlaybackNotice
import com.android.festivar.ui.voicenote.VoiceNoteRecordBar
import com.android.festivar.ui.voicenote.VoiceNoteSection
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import com.android.festivar.ui.voicenote.previewNote
import java.time.ZoneId
import java.time.ZonedDateTime

/** Test tags named after the tags of the "Task details" frames in Figma. */
object TaskDetailsScreenTestTags {
  const val BACK_BUTTON = "detailsBackButton"
  const val MORE_ACTIONS_BUTTON = "detailsMoreActionsButton"
  const val EDIT_MENU_ITEM = "detailsEditMenuItem"
  const val DELETE_MENU_ITEM = "detailsDeleteMenuItem"
  const val DELETE_DIALOG = "detailsDeleteDialog"
  const val DELETE_CONFIRM_BUTTON = "detailsDeleteConfirmButton"
  const val DELETE_DISMISS_BUTTON = "detailsDeleteDismissButton"
  const val STATUS_BADGE = "detailsStatusBadge"
  const val TITLE = "detailsTitle"
  const val WHEN = "detailsWhen"
  const val PLACE = "detailsPlace"
  const val DESCRIPTION = "detailsDescription"
  const val ON_THIS_TASK_SECTION = "detailsOnThisTask"
  const val ON_THIS_TASK_SHEET = "detailsOnThisTaskSheet"
  const val TAKE_BUTTON = "detailsTakeButton"
  const val GIVE_BACK_BUTTON = "detailsGiveBackButton"
  const val GIVE_BACK_DIALOG = "detailsGiveBackDialog"
  const val GIVE_BACK_CONFIRM_BUTTON = "detailsGiveBackConfirmButton"
  const val GIVE_BACK_KEEP_BUTTON = "detailsGiveBackKeepButton"
  const val MARK_DONE_BUTTON = "detailsMarkDoneButton"
  const val LOADING = "detailsLoading"
  const val ERROR_MESSAGE = "detailsError"
  const val ERROR_ACTION = "detailsErrorAction"
  const val FIXED_BOTTOM_BAR = "detailsFixedBottomBar"
}

/**
 * The details of a task, where the current user can take it, give it back (after a confirmation) or
 * finish it, listen to its voice note (its owner can also record or delete it), and everyone sees
 * who took it in the read only "On this task" section. The task is loaded each time the screen
 * comes to the front, the first time included, so an edit shows on return.
 *
 * @param viewModel holds the task to show and the signed-in user, and applies the actions.
 * @param onBack Called when the user taps the back button.
 * @param onEdit Called with the task id when its owner taps the edit button.
 * @param onTaskDeleted Called after the task is deleted successfully.
 */
@Composable
fun TaskDetailsScreen(
    viewModel: TaskDetailsViewModel,
    onBack: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onTaskDeleted: () -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val voiceNote by viewModel.voiceNote.uiState.collectAsStateWithLifecycle()
  val voiceNoteCallbacks = rememberVoiceNoteCallbacks(viewModel.voiceNote, voiceNote.micDenied)
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
  TaskDetailsContent(
      state = state,
      voiceNote = voiceNote,
      voiceNoteCallbacks = voiceNoteCallbacks,
      onBack = onBack,
      onTake = viewModel::take,
      onGiveBack = viewModel::giveBack,
      onMarkDone = viewModel::markDone,
      onDeleteTask = { viewModel.deleteTask(onTaskDeleted) },
      onRetry = viewModel::load,
      onDismissError = viewModel::clearError,
      onEdit = onEdit,
  )
}

/**
 * The stateless details screen: everything it shows comes from [state] and [voiceNote]. Giving the
 * task back first asks for a confirmation; [onGiveBack] is called only once the user confirms. The
 * voice note is in the page, and its notices and record bar are fixed at the bottom with the
 * buttons of the task. The "On this task" section, shown once someone took the task, opens the list
 * of people with the time each took it in a sheet; nothing in it removes a person.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsContent(
    state: TaskDetailsUiState,
    onBack: () -> Unit = {},
    onTake: () -> Unit = {},
    onGiveBack: () -> Unit = {},
    onMarkDone: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onDeleteTask: () -> Unit = {},
    voiceNote: VoiceNoteUiState = VoiceNoteUiState(),
    voiceNoteCallbacks: VoiceNoteCallbacks = VoiceNoteCallbacks.NoOp,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  var showGiveBackConfirmation by rememberSaveable { mutableStateOf(false) }
  var showPeople by rememberSaveable { mutableStateOf(false) }
  Scaffold(
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  Modifier.testTag(TaskDetailsScreenTestTags.BACK_BUTTON),
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_back))
              }
            },
            actions = {
              state.task
                  ?.takeIf { state.canEdit && !state.isLoading && !state.isUpdating }
                  ?.let { task ->
                    Box {
                      IconButton(
                          onClick = { menuExpanded = true },
                          modifier =
                              Modifier.testTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON),
                      ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.task_details_more_actions),
                        )
                      }
                      DropdownMenu(
                          expanded = menuExpanded,
                          onDismissRequest = { menuExpanded = false },
                      ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_task)) },
                            onClick = {
                              menuExpanded = false
                              onEdit(task.taskId)
                            },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            modifier = Modifier.testTag(TaskDetailsScreenTestTags.EDIT_MENU_ITEM),
                        )
                        DropdownMenuItem(
                            text = {
                              Text(
                                  stringResource(R.string.task_details_delete_task),
                                  color = MaterialTheme.colorScheme.error,
                              )
                            },
                            onClick = {
                              menuExpanded = false
                              showDeleteConfirmation = true
                            },
                            leadingIcon = {
                              Icon(
                                  Icons.Filled.Delete,
                                  contentDescription = null,
                                  tint = MaterialTheme.colorScheme.error,
                              )
                            },
                            colors =
                                MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error,
                                ),
                            modifier = Modifier.testTag(TaskDetailsScreenTestTags.DELETE_MENU_ITEM),
                        )
                      }
                    }
                  }
            },
        )
      },
      bottomBar = {
        if (state.hasActions || voiceNote.hasBottomContent) {
          FixedBottomBar(modifier = Modifier.testTag(TaskDetailsScreenTestTags.FIXED_BOTTOM_BAR)) {
            VoiceNotePlaybackNotice(voiceNote, voiceNoteCallbacks)
            VoiceNoteRecordBar(voiceNote, voiceNoteCallbacks)
            if (state.hasActions)
                Actions(state, onTake, { showGiveBackConfirmation = true }, onMarkDone)
          }
        }
      },
  ) { padding ->
    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      state.errorMsg?.let {
        ErrorLine(
            stringResource(it),
            retry = state.task == null,
            onRetry = onRetry,
            onDismiss = onDismissError,
        )
      }
      if (state.isLoading && state.task == null) {
        CircularProgressIndicator(
            Modifier.align(Alignment.CenterHorizontally)
                .padding(20.dp)
                .size(32.dp)
                .testTag(TaskDetailsScreenTestTags.LOADING)
        )
      }
      val task = state.task
      val status = state.status
      if (task != null && status != null) {
        Body(task, status) { showPeople = true }
        VoiceNoteSection(voiceNote, voiceNoteCallbacks, state.ownerName)
      }
    }
  }
  if (showDeleteConfirmation) {
    state.task?.let { task ->
      FestivarConfirmDialog(
          title = stringResource(R.string.task_details_delete_confirmation_title),
          message = stringResource(R.string.task_details_delete_confirmation_message, task.title),
          confirmText = stringResource(R.string.task_details_delete_task),
          dismissText = stringResource(R.string.task_details_keep_task),
          onConfirm = {
            showDeleteConfirmation = false
            onDeleteTask()
          },
          onDismiss = { showDeleteConfirmation = false },
          modifier = Modifier.testTag(TaskDetailsScreenTestTags.DELETE_DIALOG),
          confirmModifier = Modifier.testTag(TaskDetailsScreenTestTags.DELETE_CONFIRM_BUTTON),
          dismissModifier = Modifier.testTag(TaskDetailsScreenTestTags.DELETE_DISMISS_BUTTON),
      )
    }
  }
  val taskToGiveBack = state.task
  if (showGiveBackConfirmation && taskToGiveBack != null) {
    FestivarConfirmDialog(
        title = stringResource(R.string.details_give_back_title),
        message = stringResource(R.string.details_give_back_message, taskToGiveBack.title),
        confirmText = stringResource(R.string.details_give_back),
        dismissText = stringResource(R.string.details_give_back_keep),
        onConfirm = {
          showGiveBackConfirmation = false
          onGiveBack()
        },
        onDismiss = { showGiveBackConfirmation = false },
        modifier = Modifier.testTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG),
        confirmModifier = Modifier.testTag(TaskDetailsScreenTestTags.GIVE_BACK_CONFIRM_BUTTON),
        dismissModifier = Modifier.testTag(TaskDetailsScreenTestTags.GIVE_BACK_KEEP_BUTTON),
    )
  }
  val taskWithPeople = state.task
  if (showPeople && taskWithPeople != null) {
    PeopleSheet(taskWithPeople, onDismiss = { showPeople = false })
  }
}

/** Whether the voice note fixes something at the bottom: a failed playback or the record bar. */
private val VoiceNoteUiState.hasBottomContent: Boolean
  get() = playbackFailed || showRecordBar

@Composable
private fun Body(task: Task, status: TaskStatus, onOpenPeople: () -> Unit) {
  StatusPill(
      label = stringResource(status.label()),
      style = status.pillStyle(),
      modifier = Modifier.testTag(TaskDetailsScreenTestTags.STATUS_BADGE),
  )
  Text(
      task.title,
      style = MaterialTheme.typography.displaySmall,
      modifier = Modifier.testTag(TaskDetailsScreenTestTags.TITLE),
  )
  val rangeFormat = stringResource(R.string.details_time_range)
  whenLine(task, rangeFormat)?.let { SubtitleText(it, TaskDetailsScreenTestTags.WHEN) }
  if (task.location.isNotBlank()) {
    SubtitleText(
        stringResource(R.string.details_location, task.location),
        TaskDetailsScreenTestTags.PLACE,
    )
  }
  if (task.description.isNotBlank()) {
    SectionLabel(stringResource(R.string.details_description))
    Text(
        task.description,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag(TaskDetailsScreenTestTags.DESCRIPTION),
    )
  }
  if (task.assignees.isNotEmpty()) {
    PeopleCard(
        stringResource(R.string.details_on_this_task),
        task.assignees,
        onOpenPeople,
        Modifier.testTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SECTION),
    )
  }
}

/** Everyone who took [task], read only, each with the time they took it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeopleSheet(task: Task, onDismiss: () -> Unit) {
  val tag =
      if (task.completed) {
        UserTag(
            stringResource(R.string.details_status_done),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
      } else {
        UserTag(
            stringResource(R.string.details_status_taken),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
  val tookIt = stringResource(R.string.details_took_it)
  UserListBottomSheet(
      users = task.assignees,
      onDismissRequest = onDismiss,
      title = stringResource(R.string.details_on_this_task),
      subtitle =
          stringResource(
              R.string.details_people_sheet_subtitle,
              peopleCount(task.assignees.size),
              task.title,
          ),
      supportingText = { user ->
        task.takenAt[user.uid]?.let {
          tookIt.format(formatDate(it.toLocalDate()), formatTime(it.toLocalTime()))
        }
      },
      tags = { listOf(tag) },
      onUserClick = null,
      modifier = Modifier.testTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SHEET),
  )
}

/** A muted line under the title: the time or the place. */
@Composable
private fun SubtitleText(text: String, tag: String) {
  Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag(tag),
  )
}

/** The buttons at the bottom: only the actions the current status allows. */
@Composable
private fun Actions(
    state: TaskDetailsUiState,
    onTake: () -> Unit,
    onGiveBackRequested: () -> Unit,
    onMarkDone: () -> Unit,
) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    val buttonModifier = Modifier.weight(1f).height(52.dp)
    if (state.status == TaskStatus.OPEN) {
      Button(
          onClick = onTake,
          enabled = state.canTake,
          shape = MaterialTheme.shapes.medium,
          modifier = buttonModifier.testTag(TaskDetailsScreenTestTags.TAKE_BUTTON),
      ) {
        Text(stringResource(R.string.details_take), style = MaterialTheme.typography.labelLarge)
      }
    } else {
      OutlinedButton(
          onClick = onGiveBackRequested,
          enabled = state.canGiveBack,
          shape = MaterialTheme.shapes.medium,
          colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = buttonModifier.testTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON),
      ) {
        Text(
            stringResource(R.string.details_give_back),
            style = MaterialTheme.typography.labelLarge,
        )
      }
      Button(
          onClick = onMarkDone,
          enabled = state.canMarkDone,
          shape = MaterialTheme.shapes.medium,
          modifier = buttonModifier.testTag(TaskDetailsScreenTestTags.MARK_DONE_BUTTON),
      ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(
            stringResource(R.string.details_mark_done),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 10.dp),
        )
      }
    }
  }
}

/** The failure in the theme's error colour, with a retry when there is no task to show. */
@Composable
private fun ErrorLine(message: String, retry: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f).testTag(TaskDetailsScreenTestTags.ERROR_MESSAGE),
    )
    TextButton(
        onClick = if (retry) onRetry else onDismiss,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
        modifier = Modifier.testTag(TaskDetailsScreenTestTags.ERROR_ACTION),
    ) {
      Text(stringResource(if (retry) R.string.details_retry else R.string.details_dismiss))
    }
  }
}

private fun TaskStatus.label(): Int =
    when (this) {
      TaskStatus.OPEN -> R.string.details_status_open
      TaskStatus.TAKEN -> R.string.details_status_taken
      TaskStatus.YOURS -> R.string.details_status_yours
      TaskStatus.DONE -> R.string.details_status_done
    }

private fun TaskStatus.pillStyle(): PillStyle =
    when (this) {
      TaskStatus.OPEN -> PillStyle.MUTED
      TaskStatus.TAKEN -> PillStyle.AMBER
      TaskStatus.YOURS,
      TaskStatus.DONE -> PillStyle.LIME
    }

/**
 * "Sat 17 Oct, 09:30 to 09:50" ([rangeFormat] joins the times), or "Sat 17 Oct, 09:30" without an
 * end; null without a start.
 */
internal fun whenLine(task: Task, rangeFormat: String): String? {
  val start = task.startTime ?: return null
  val end = task.endTime
  val startText = formatTime(start.toLocalTime())
  val times =
      if (end != null) rangeFormat.format(startText, formatTime(end.toLocalTime())) else startText
  return "${formatDate(start.toLocalDate())}, $times"
}

private val previewZone = ZoneId.of("Europe/Zurich")

private val previewUser = User("u1", "Sami", "Kabbaj")

private val previewTookItAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, previewZone)

private val previewTask =
    Task(
        taskId = "t1",
        eventId = "e1",
        title = "Run power to stage",
        description =
            "Extension reel is in the blue crate by the van. Run it along the fence, not across " +
                "the path. Ask Marie for the gaffer tape.",
        startTime = ZonedDateTime.of(2026, 10, 17, 9, 30, 0, 0, previewZone),
        endTime = ZonedDateTime.of(2026, 10, 17, 9, 50, 0, 0, previewZone),
        location = "Stage, north corner",
        maxAssign = 1,
        ownerId = previewUser.uid,
    )

private val previewOtherUser = User("u2", "Yasser", "Ameur")

/** The state whose derived status is [status] for [previewUser]. */
private fun previewState(status: TaskStatus) =
    TaskDetailsUiState(
        currentUserId = previewUser.uid,
        task =
            when (status) {
              TaskStatus.OPEN -> previewTask
              TaskStatus.TAKEN -> previewTask.addAssignee(previewOtherUser, previewTookItAt)
              TaskStatus.YOURS -> previewTask.addAssignee(previewUser, previewTookItAt)
              TaskStatus.DONE ->
                  previewTask.addAssignee(previewOtherUser, previewTookItAt).complete()
            },
        isLoading = false,
    )

@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsOpenPreview() {
  AppTheme { TaskDetailsContent(previewState(TaskStatus.OPEN)) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsTakenPreview() {
  AppTheme { TaskDetailsContent(previewState(TaskStatus.TAKEN)) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsYoursPreview() {
  AppTheme { TaskDetailsContent(previewState(TaskStatus.YOURS)) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsDonePreview() {
  AppTheme { TaskDetailsContent(previewState(TaskStatus.DONE)) }
}

/** The owner of a task without a note: the empty state, and the record bar at the bottom. */
@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsVoiceNoteOwnerPreview() {
  AppTheme {
    TaskDetailsContent(
        previewState(TaskStatus.YOURS),
        voiceNote = VoiceNoteUiState(canManage = true),
    )
  }
}

/** A volunteer listens to the note of the owner. */
@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsVoiceNoteVolunteerPreview() {
  AppTheme {
    TaskDetailsContent(
        previewState(TaskStatus.TAKEN).copy(ownerName = "Sami Kabbaj"),
        voiceNote = VoiceNoteUiState(note = previewNote),
    )
  }
}

/** The load failed: nothing to show, so the error offers Retry. */
@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsLoadErrorPreview() {
  val state =
      TaskDetailsUiState(
          currentUserId = previewUser.uid,
          isLoading = false,
          errorMsg = R.string.task_details_load_failed,
      )
  AppTheme { TaskDetailsContent(state) }
}

/** An action failed to save: the task stays shown and the error offers Dismiss. */
@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun TaskDetailsSaveErrorPreview() {
  val state = previewState(TaskStatus.YOURS).copy(errorMsg = R.string.task_details_save_failed)
  AppTheme { TaskDetailsContent(state) }
}
