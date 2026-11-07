// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.components.InfoNote
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Test tags of the record bar and of the notices above it, named after the tags of the "Voice
 * notes" frames in Figma.
 */
object VoiceNoteRecordTestTags {
  const val RECORDING_HINT = "voiceNoteRecordingHint"
  const val RECORDING_BAR = "voiceNoteRecordingBar"
  const val VOICE_NOTE_SENDING = "voiceNoteSending"
  const val VOICE_NOTE_RECORD = "voiceNoteRecord"
  const val SEND_ERROR = "voiceNoteSendError"
  const val SEND_RETRY = "voiceNoteSendRetry"
  const val RECORDING_ERROR = "voiceNoteRecordingError"
  const val RECORDING_ERROR_DISMISS = "voiceNoteRecordingErrorDismiss"
  const val MIC_DENIED = "voiceNoteMicDenied"
  const val OPEN_SETTINGS = "voiceNoteOpenSettings"
  const val PLAYBACK_ERROR = "voiceNotePlaybackError"
  const val PLAYBACK_RETRY = "voiceNotePlaybackRetry"
}

/** How far to the left (in dp) the finger has to slide before releasing to cancel. */
private const val CANCEL_DISTANCE_DP = 80

/**
 * The record bar, fixed above the bottom of the screen. Only the owner of a task without a note
 * sees it: it is hidden otherwise. The owner holds the round button to record, releases it to stop
 * and slides left before releasing to cancel. Without the microphone permission, and while the
 * recording is sent, it is greyed out. While the recording waits for the owner to send it, the bar
 * shows it as a note that cannot be played yet.
 */
@Composable
fun VoiceNoteRecordBar(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.showRecordBar) return
  FixedBottomBar(modifier) {
    val waiting = state.pending?.takeUnless { it.isUploading }
    if (waiting != null) {
      // Nothing to play yet: the recording is not uploaded, so the player is disabled.
      VoiceNotePlayer(waiting.duration, false, null, Modifier.fillMaxWidth())
    } else {
      RecordPill(state, callbacks)
    }
  }
}

/**
 * The bar of the design: a white box with a border and rounded corners, with the label (hint, timer
 * or sending) and the record button.
 */
@Composable
private fun RecordPill(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  val colors = MaterialTheme.colorScheme
  val shape = MaterialTheme.shapes.medium
  Row(
      Modifier.fillMaxWidth()
          .alpha(if (state.recordEnabled) 1f else 0.5f)
          // The tag is before the padding so that the node is the whole 56dp bar of the design
          // (a 48dp button with 4dp above and below), not only its content.
          .testTag(VoiceNoteRecordTestTags.RECORDING_BAR)
          .background(colors.surfaceContainerLowest, shape)
          .border(1.dp, colors.outline, shape)
          .padding(start = 16.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(Modifier.weight(1f)) {
      if (state.isRecording) RecordingIndicator(state) else IdleLabel(state)
    }
    RecordButton(state.recordEnabled, callbacks)
  }
}

/** The label of the idle bar: "Hold to record", or the length of the recording being sent. */
@Composable
private fun IdleLabel(state: VoiceNoteUiState) {
  val sending = state.pending?.takeIf { it.isUploading }
  Text(
      if (sending != null) {
        stringResource(R.string.voice_note_sending, formatVoiceDuration(sending.duration))
      } else {
        stringResource(R.string.voice_note_hold_to_record)
      },
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
          if (sending != null) Modifier.testTag(VoiceNoteRecordTestTags.VOICE_NOTE_SENDING)
          else Modifier,
  )
}

/** The red dot, the timer and the hint shown while recording. */
@Composable
private fun RecordingIndicator(state: VoiceNoteUiState) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
    Column {
      Text(
          stringResource(R.string.voice_note_recording, formatVoiceDuration(state.elapsed)),
          style = MaterialTheme.typography.labelLarge,
      )
      Text(
          stringResource(R.string.voice_note_recording_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(VoiceNoteRecordTestTags.RECORDING_HINT),
      )
    }
  }
}

/**
 * The round lime microphone button. It reacts to the press itself: pressing starts, releasing stops
 * and releasing after a slide to the left cancels. The same node stays in place while recording, so
 * the gesture is not interrupted when the bar changes.
 */
@Composable
private fun RecordButton(enabled: Boolean, callbacks: VoiceNoteCallbacks) {
  val onStart by rememberUpdatedState(callbacks.onRecordStart)
  val onRelease by rememberUpdatedState(callbacks.onRecordRelease)
  val onCancel by rememberUpdatedState(callbacks.onRecordCancel)
  val cancelDistance = with(LocalDensity.current) { CANCEL_DISTANCE_DP.dp.toPx() }
  val label = stringResource(R.string.voice_note_record)
  val colors = MaterialTheme.colorScheme
  Box(
      Modifier.size(48.dp)
          .background(colors.primary, CircleShape)
          .semantics { contentDescription = label }
          .testTag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD)
          .pointerInput(enabled, cancelDistance) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
              val down = awaitFirstDown()
              onStart()
              var cancelled = false
              while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                cancelled = change.position.x - down.position.x < -cancelDistance
                if (!change.pressed) break
              }
              if (cancelled) onCancel() else onRelease()
            }
          },
      contentAlignment = Alignment.Center,
  ) {
    Icon(Icons.Filled.Mic, contentDescription = null, tint = colors.onPrimary)
  }
}

/**
 * The notices fixed just above the record bar: the failed playback, send or recording (each with an
 * action: Retry, Retry, Dismiss), and the information that the microphone is off (with a button to
 * the settings).
 */
@Composable
fun VoiceNoteNotices(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.hasNotices) return
  Column(
      modifier.fillMaxWidth().padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (state.playbackFailed) {
      ErrorNotice(
          R.string.voice_note_playback_failed,
          R.string.voice_note_retry,
          callbacks.onRetry,
          VoiceNoteRecordTestTags.PLAYBACK_ERROR,
          VoiceNoteRecordTestTags.PLAYBACK_RETRY,
      )
    }
    if (state.showRecordBar && state.sendFailed) {
      ErrorNotice(
          R.string.voice_note_send_failed,
          R.string.voice_note_retry,
          callbacks.onRetry,
          VoiceNoteRecordTestTags.SEND_ERROR,
          VoiceNoteRecordTestTags.SEND_RETRY,
      )
    }
    if (state.showRecordBar && state.recordingFailed) {
      ErrorNotice(
          R.string.voice_note_recording_failed,
          R.string.voice_note_dismiss,
          callbacks.onDismissRecordingFailed,
          VoiceNoteRecordTestTags.RECORDING_ERROR,
          VoiceNoteRecordTestTags.RECORDING_ERROR_DISMISS,
      )
    }
    if (state.showRecordBar && state.micDenied) {
      InfoNote(
          message = stringResource(R.string.voice_note_mic_denied),
          modifier = Modifier.testTag(VoiceNoteRecordTestTags.MIC_DENIED),
          action = {
            OutlinedButton(
                onClick = callbacks.onOpenSettings,
                modifier =
                    Modifier.fillMaxWidth()
                        .height(52.dp)
                        .testTag(VoiceNoteRecordTestTags.OPEN_SETTINGS),
                shape = MaterialTheme.shapes.medium,
            ) {
              Text(stringResource(R.string.voice_note_open_settings))
            }
          },
      )
    }
  }
}

/** An error of the voice note, shown with the shared snackbar and the one [action] that fits it. */
@Composable
private fun ErrorNotice(
    @StringRes message: Int,
    @StringRes action: Int,
    onAction: () -> Unit,
    tag: String,
    actionTag: String,
) {
  NoticeSnackbar(
      message = stringResource(message),
      actionLabel = stringResource(action),
      onAction = onAction,
      modifier = Modifier.testTag(tag),
      actionModifier = Modifier.testTag(actionTag),
  )
}

private val previewNote =
    VoiceNote(
        id = "n1",
        audioUrl = "https://example.com/n1.m4a",
        duration = Duration.ofSeconds(14),
        createdAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC),
    )

private val callbacks = VoiceNoteCallbacks.NoOp

/** The notices and the record bar fixed at the bottom of the screen, as on the screen. */
@Composable
private fun VoiceNoteBarPreviewScreen(state: VoiceNoteUiState) {
  AppTheme {
    Column(Modifier.fillMaxWidth()) {
      VoiceNoteNotices(state, callbacks)
      VoiceNoteRecordBar(state, callbacks)
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarIdlePreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarRecordingPreview() {
  VoiceNoteBarPreviewScreen(
      VoiceNoteUiState(canManage = true, recorder = RecorderState.Recording(Duration.ofSeconds(3)))
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarWaitingPreview() {
  VoiceNoteBarPreviewScreen(
      VoiceNoteUiState(canManage = true, pending = PendingRecording(Duration.ofSeconds(7)))
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarSendingPreview() {
  VoiceNoteBarPreviewScreen(
      VoiceNoteUiState(
          canManage = true,
          pending = PendingRecording(Duration.ofSeconds(7), isUploading = true),
      )
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarRecordingFailedPreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true, recorder = RecorderState.Failed))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarSendFailedPreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true, sendFailed = true))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarMicDeniedPreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true, micDenied = true))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNotePlaybackFailedPreview() {
  VoiceNoteBarPreviewScreen(
      VoiceNoteUiState(note = previewNote, player = PlayerState.Failed(previewNote.audioUrl))
  )
}
