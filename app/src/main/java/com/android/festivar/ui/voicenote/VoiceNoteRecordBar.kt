// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.components.FixedBottomBarHorizontalPadding
import com.android.festivar.ui.components.InfoNote
import com.android.festivar.ui.components.InfoNoteButton
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration

/**
 * Test tags of the record bar and of the notices above it, named after the tags of the "Voice
 * notes" frames in Figma.
 */
object VoiceNoteRecordTestTags {
  const val RECORDING_BAR = "voiceNoteRecordingBar"
  const val VOICE_NOTE_RECORD = "voiceNoteRecord"
  const val RECORDING_DOT = "voiceNoteRecordingDot"
  const val SEND_ERROR = "voiceNoteSendError"
  const val SEND_RETRY = "voiceNoteSendRetry"
  const val MIC_DENIED = "voiceNoteMicDenied"
  const val OPEN_SETTINGS = "voiceNoteOpenSettings"
  const val PLAYBACK_ERROR = "voiceNotePlaybackError"
  const val PLAYBACK_RETRY = "voiceNotePlaybackRetry"
}

/** The opacity of the record bar when the microphone is off. */
private const val DISABLED_ALPHA = 0.5f

/**
 * The bar fixed above the bottom of the screen: the notices of the owner (failed send, microphone
 * off) and the record bar under them, in the same [FixedBottomBar]. Only the owner of a task
 * without a note sees it: it is hidden otherwise. The owner holds the round button to record,
 * releases it to stop. Without the microphone permission the record bar is greyed out.
 */
@Composable
fun VoiceNoteRecordBar(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.showRecordBar) return
  FixedBottomBar(modifier) {
    VoiceNoteOwnerNotices(state, callbacks)
    RecordPill(state, callbacks)
  }
}

/**
 * The notices above the record bar: the failed send (with a Retry), and the information that the
 * microphone is off (with a button to the settings).
 */
@Composable
private fun VoiceNoteOwnerNotices(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  if (state.sendFailed) {
    ErrorNotice(
        message = R.string.voice_note_send_failed,
        onRetry = callbacks.onRetry,
        tag = VoiceNoteRecordTestTags.SEND_ERROR,
        actionTag = VoiceNoteRecordTestTags.SEND_RETRY,
    )
  }
  if (state.micDenied) {
    InfoNote(
        message = stringResource(R.string.voice_note_mic_denied),
        modifier = Modifier.testTag(VoiceNoteRecordTestTags.MIC_DENIED),
        action = {
          InfoNoteButton(
              stringResource(R.string.action_open_settings),
              onClick = callbacks.onOpenSettings,
              modifier = Modifier.testTag(VoiceNoteRecordTestTags.OPEN_SETTINGS),
          )
        },
    )
  }
}

/**
 * An error of the voice note, shown with the shared snackbar and a Retry. The [modifier] is applied
 * before the tag, so the tagged node is the snackbar alone, without the space around it.
 */
@Composable
private fun ErrorNotice(
    @StringRes message: Int,
    onRetry: () -> Unit,
    tag: String,
    actionTag: String,
    modifier: Modifier = Modifier,
) {
  NoticeSnackbar(
      message = stringResource(message),
      actionLabel = stringResource(R.string.action_retry),
      onAction = onRetry,
      modifier = modifier.testTag(tag),
      actionModifier = Modifier.testTag(actionTag),
  )
}

/**
 * The notice of a failed playback. It is not part of the record bar: a note exists, so the bar is
 * hidden, and everybody can listen, so everybody must see it. The design ("notice (fixed)") puts it
 * just above the buttons of the task.
 */
@Composable
fun VoiceNotePlaybackNotice(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.playbackFailed) return
  ErrorNotice(
      message = R.string.voice_note_playback_failed,
      onRetry = callbacks.onRetry,
      tag = VoiceNoteRecordTestTags.PLAYBACK_ERROR,
      actionTag = VoiceNoteRecordTestTags.PLAYBACK_RETRY,
      modifier =
          modifier.padding(
              start = FixedBottomBarHorizontalPadding,
              top = 8.dp,
              end = FixedBottomBarHorizontalPadding,
              bottom = 4.dp,
          ),
  )
}

/**
 * The bar of the design: a white box with a border and rounded corners, with the hint or the timer
 * and the record button.
 */
@Composable
private fun RecordPill(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  val colors = MaterialTheme.colorScheme
  val shape = MaterialTheme.shapes.medium
  Row(
      Modifier.fillMaxWidth()
          .alpha(if (state.recordEnabled) 1f else DISABLED_ALPHA)
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
      if (state.isRecording) {
        RecordingIndicator(state)
      } else {
        Text(
            stringResource(R.string.voice_note_hold_to_record),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    RecordButton(state.recordEnabled, callbacks)
  }
}

/** The red dot and the timer shown while recording. */
@Composable
private fun RecordingIndicator(state: VoiceNoteUiState) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
        Modifier.size(10.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .testTag(VoiceNoteRecordTestTags.RECORDING_DOT)
    )
    Text(
        stringResource(R.string.voice_note_recording, formatVoiceDuration(state.elapsed)),
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

/**
 * The round microphone button. Pressing starts the recording and releasing stops it. A press that
 * ends in any other way (the finger slides off the button, the system takes the gesture, or the
 * button stops listening because the microphone is turned off) is released too, so the recorder
 * never stays on.
 */
@Composable
private fun RecordButton(enabled: Boolean, callbacks: VoiceNoteCallbacks) {
  // The gesture below lives as long as `enabled`, so it reads the callbacks of the latest
  // composition instead of the ones it started with.
  val onStart by rememberUpdatedState(callbacks.onRecordStart)
  val onRelease by rememberUpdatedState(callbacks.onRecordRelease)
  val label = stringResource(R.string.voice_note_record)
  val colors = MaterialTheme.colorScheme
  Box(
      Modifier.size(48.dp)
          .background(colors.primary, CircleShape)
          .semantics {
            role = Role.Button
            contentDescription = label
            if (!enabled) disabled()
          }
          .testTag(VoiceNoteRecordTestTags.VOICE_NOTE_RECORD)
          .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                  onStart()
                  try {
                    // Returns when the finger lifts, and also when the press is cancelled.
                    tryAwaitRelease()
                  } finally {
                    onRelease()
                  }
                }
            )
          },
      contentAlignment = Alignment.Center,
  ) {
    Icon(Icons.Filled.Mic, contentDescription = null, tint = colors.onPrimary)
  }
}

/** The bar with its notices fixed at the bottom of the screen, as on the screen. */
@Composable
private fun VoiceNoteBarPreviewScreen(state: VoiceNoteUiState) {
  AppTheme {
    Column {
      VoiceNotePlaybackNotice(state, previewCallbacks)
      VoiceNoteRecordBar(state, previewCallbacks)
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
