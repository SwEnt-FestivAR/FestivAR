// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.PillStyle
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.voicenote.AndroidVoicePlayer
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks
import com.android.festivar.ui.voicenote.VoiceNoteNotices
import com.android.festivar.ui.voicenote.VoiceNoteRecordBar
import com.android.festivar.ui.voicenote.VoiceNoteSection
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import java.time.ZoneId
import java.time.ZonedDateTime

/** Test tags named after the tags of the "Task details" frames in Figma. */
object TaskDetailsScreenTestTags {
  const val BACK_BUTTON = "detailsBackButton"
  const val STATUS_BADGE = "detailsStatusBadge"
  const val TITLE = "detailsTitle"
  const val WHEN = "detailsWhen"
  const val PLACE = "detailsPlace"
  const val DESCRIPTION = "detailsDescription"
  const val TAKE_BUTTON = "detailsTakeButton"
  const val GIVE_BACK_BUTTON = "detailsGiveBackButton"
  const val MARK_DONE_BUTTON = "detailsMarkDoneButton"
  const val LOADING = "detailsLoading"
  const val ERROR_MESSAGE = "detailsError"
  const val ERROR_ACTION = "detailsErrorAction"
}

/**
 * The details of the task [taskId], where [currentUser] can take it, give it back or finish it, and
 * everybody can listen to its voice note. The recording and the storage of the voice note are not
 * connected yet: until they are, only an existing note can be played.
 */
@Composable
fun TaskDetailsScreen(
    taskId: String,
    currentUser: User,
    viewModel: TaskDetailsViewModel = rememberTaskDetailsViewModel(taskId, currentUser),
    onBack: () -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val voiceNote by viewModel.voiceNote.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val micLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onMicPermissionResult(granted)
      }
  // Coming back from the settings, the permission may have been given.
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (voiceNote.micDenied) viewModel.onMicPermissionResult(hasMicPermission(context))
  }
  val voiceCallbacks =
      VoiceNoteCallbacks(
          onTogglePlay = viewModel::togglePlay,
          onDelete = viewModel::requestDeleteVoiceNote,
          onConfirmDelete = viewModel::confirmDeleteVoiceNote,
          onDismissDelete = viewModel::dismissDeleteVoiceNote,
          onRecordStart = {
            if (hasMicPermission(context)) viewModel.startRecording()
            else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
          },
          onRecordRelease = viewModel::finishRecording,
          onRecordCancel = viewModel::cancelRecording,
          onRetry = viewModel::retryVoiceNote,
          onOpenSettings = { openAppSettings(context) },
      )
  TaskDetailsContent(
      state = state,
      onBack = onBack,
      onTake = viewModel::take,
      onGiveBack = viewModel::giveBack,
      onMarkDone = viewModel::markDone,
      onRetry = viewModel::load,
      onDismissError = viewModel::clearError,
      voiceNote = voiceNote,
      voiceNoteCallbacks = voiceCallbacks,
  )
}

/** The view model of the screen, with the real player of the voice note. */
@Composable
private fun rememberTaskDetailsViewModel(taskId: String, currentUser: User): TaskDetailsViewModel {
  val appContext = LocalContext.current.applicationContext
  return viewModel(key = "$taskId:${currentUser.uid}") {
    TaskDetailsViewModel(taskId, currentUser, player = AndroidVoicePlayer(appContext))
  }
}

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
  context.startActivity(
      Intent(
              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
              Uri.fromParts("package", context.packageName, null),
          )
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  )
}

/**
 * The stateless details screen: everything it shows comes from [state] and [voiceNote]. The voice
 * note section is below the description; its notices and record bar are fixed above the actions.
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
    voiceNote: VoiceNoteUiState = VoiceNoteUiState(),
    voiceNoteCallbacks: VoiceNoteCallbacks = VoiceNoteCallbacks(),
) {
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
        )
      },
      bottomBar = {
        Column {
          VoiceNoteNotices(voiceNote, voiceNoteCallbacks)
          VoiceNoteRecordBar(voiceNote, voiceNoteCallbacks)
          if (state.hasActions) Actions(state, onTake, onGiveBack, onMarkDone)
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
      if (task != null && status != null) Body(task, status, voiceNote, voiceNoteCallbacks)
    }
  }
}

@Composable
private fun Body(
    task: Task,
    status: TaskStatus,
    voiceNote: VoiceNoteUiState,
    voiceNoteCallbacks: VoiceNoteCallbacks,
) {
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
  VoiceNoteSection(voiceNote, voiceNoteCallbacks)
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

/** The bar at the bottom: only the actions the current status allows. */
@Composable
private fun Actions(
    state: TaskDetailsUiState,
    onTake: () -> Unit,
    onGiveBack: () -> Unit,
    onMarkDone: () -> Unit,
) {
  Row(
      Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
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
          onClick = onGiveBack,
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

private val previewUser = User("u1", "Sami", "Kabbaj")

private val previewTask =
    Task(
        taskId = "t1",
        eventId = "e1",
        title = "Run power to stage",
        description =
            "Extension reel is in the blue crate by the van. Run it along the fence, not across " +
                "the path. Ask Marie for the gaffer tape.",
        startTime = ZonedDateTime.of(2026, 10, 17, 9, 30, 0, 0, ZoneId.of("Europe/Zurich")),
        endTime = ZonedDateTime.of(2026, 10, 17, 9, 50, 0, 0, ZoneId.of("Europe/Zurich")),
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
              TaskStatus.TAKEN -> previewTask.addAssignee(previewOtherUser)
              TaskStatus.YOURS -> previewTask.addAssignee(previewUser)
              TaskStatus.DONE -> previewTask.addAssignee(previewOtherUser).complete()
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
