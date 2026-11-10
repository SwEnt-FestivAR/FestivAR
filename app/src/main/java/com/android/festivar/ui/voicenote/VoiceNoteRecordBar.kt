// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration

/** The opacity of the record bar when the microphone is off. */
private const val DISABLED_ALPHA = 0.5f

/**
 * The record bar of the voice note: the notices of the owner (failed send, failed recording,
 * microphone off) and the record bar under them. It draws no bar of its own: the screen puts it in
 * the [FixedBottomBar] it shares with the other things fixed at the bottom (the playback notice,
 * the buttons of the task). Only the owner of a task without a note sees it: it is hidden
 * otherwise. The owner holds the round button to record, releases it to stop. Without the
 * microphone permission, and while the recording is sent, the record bar is greyed out. While the
 * recording waits for the owner to send it, the bar shows it as a note that cannot be played yet.
 */
@Composable
fun ColumnScope.VoiceNoteRecordBar(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  if (!state.showRecordBar) return
  VoiceNoteOwnerNotices(state, callbacks)
  val waiting = state.waiting
  if (waiting != null) {
    WaitingRecording(waiting)
  } else {
    RecordPill(state, callbacks)
  }
}

/**
 * The recording that waits for the owner to send it, shown as a note. Nothing can be played yet,
 * because the recording is not uploaded.
 */
@Composable
private fun WaitingRecording(recording: PendingRecording.Waiting) {
  VoiceNotePlayer(
      duration = recording.duration,
      isPlaying = false,
      onTogglePlay = {},
      modifier = Modifier.fillMaxWidth(),
      enabled = false,
  )
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
          .alpha(if (state.recordEnabled) 1f else DISABLED_ALPHA)
          // The tag is before the padding so that the node is the whole 56dp bar of the design
          // (a 48dp button with 4dp above and below), not only its content.
          .testTag(VoiceNoteRecordTestTags.RECORD_BAR)
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
        IdleLabel(state)
      }
    }
    RecordButton(state.recordEnabled, callbacks)
  }
}

/** The label of the idle pill: "Hold to record", or the length of the recording being sent. */
@Composable
private fun IdleLabel(state: VoiceNoteUiState) {
  val sending = state.sending
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
  AppTheme { FixedBottomBar { VoiceNoteRecordBar(state, previewCallbacks) } }
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
      VoiceNoteUiState(canManage = true, pending = PendingRecording.Waiting(Duration.ofSeconds(7)))
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarSendingPreview() {
  VoiceNoteBarPreviewScreen(
      VoiceNoteUiState(
          canManage = true,
          pending = PendingRecording.Sending(Duration.ofSeconds(7)),
      )
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarSendFailedPreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true, sendFailed = true))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarRecordingFailedPreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true, recorder = RecorderState.Failed))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteRecordBarMicDeniedPreview() {
  VoiceNoteBarPreviewScreen(VoiceNoteUiState(canManage = true, micDenied = true))
}
