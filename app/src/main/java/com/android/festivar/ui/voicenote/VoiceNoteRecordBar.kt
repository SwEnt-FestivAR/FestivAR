// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.components.InfoNote
import com.android.festivar.ui.components.NoticeSnackbar

/** How far to the left (in dp) the finger has to slide before releasing to cancel. */
private const val CANCEL_DISTANCE_DP = 80

/** How far up (in dp) the finger has to slide to lock the recording. */
private const val LOCK_DISTANCE_DP = 80

/**
 * The record bar, fixed above the bottom of the screen. Only the owner of a task without a note
 * sees it: it is hidden otherwise. The owner holds the round button to record, releases it to send
 * and slides left before releasing to cancel. Sliding up while holding locks the recording: the bar
 * then shows the timer with its controls (cancel on the left, pause or resume and send on the
 * right), which a paused recording shows too. Without the microphone permission it is greyed out.
 */
@Composable
fun VoiceNoteRecordBar(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.showRecordBar) return
  FixedBottomBar(modifier) {
    Row(
        Modifier.fillMaxWidth()
            .alpha(if (state.recordEnabled) 1f else 0.5f)
            // The tag is before the padding so that the node is the whole 56dp bar of the design
            // (a 48dp button with 4dp above and below), not only its content.
            .testTag(VoiceNoteTestTags.RECORDING_BAR)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(
                start = if (state.showRecordControls) 4.dp else 20.dp,
                top = 4.dp,
                end = if (state.showRecordControls) 4.dp else 8.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (state.showRecordControls) 8.dp else 12.dp),
    ) {
      if (state.showRecordControls) {
        IconButton(
            onClick = callbacks.onRecordCancel,
            modifier = Modifier.size(48.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_CANCEL),
        ) {
          Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.voice_note_cancel))
        }
      }
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
      if (state.showRecordControls) {
        RecordControls(state, callbacks)
      } else {
        RecordButton(state.recordEnabled, callbacks)
      }
    }
  }
}

/**
 * The red dot (not while paused) and the timer shown while recording. While the finger holds the
 * button there is a hint under the timer; the locked and the paused recording have the timer alone,
 * as in the S5 design.
 */
@Composable
private fun RecordingIndicator(state: VoiceNoteUiState) {
  val time = formatVoiceDuration(state.elapsed)
  val title =
      when {
        state.isPaused -> stringResource(R.string.voice_note_paused, time)
        state.showRecordControls -> stringResource(R.string.voice_note_recording_time, time)
        else -> stringResource(R.string.voice_note_recording, time)
      }
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (!state.isPaused) {
      Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
    }
    Column {
      Text(title, style = MaterialTheme.typography.labelLarge)
      if (!state.showRecordControls) {
        Text(
            stringResource(R.string.voice_note_recording_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(VoiceNoteTestTags.RECORDING_HINT),
        )
      }
    }
  }
}

/**
 * The controls of a locked or paused recording, as in the S5 design (like WhatsApp): the cancel
 * button is on the left of the bar, and here are a plain pause or resume icon and one round send
 * button, so only the send button is a filled circle.
 */
@Composable
private fun RecordControls(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    if (state.isPaused) {
      IconButton(
          onClick = callbacks.onRecordResume,
          modifier = Modifier.size(48.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_RESUME),
      ) {
        Icon(Icons.Filled.PlayArrow, stringResource(R.string.voice_note_resume_recording))
      }
    } else {
      IconButton(
          onClick = callbacks.onRecordPause,
          modifier = Modifier.size(48.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_PAUSE),
      ) {
        Icon(Icons.Filled.Pause, stringResource(R.string.voice_note_pause_recording))
      }
    }
    FilledIconButton(
        onClick = callbacks.onRecordRelease,
        modifier = Modifier.size(48.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_SEND),
    ) {
      Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.voice_note_send))
    }
  }
}

/**
 * The round lime microphone button. It reacts to the press itself: pressing starts, releasing sends
 * and releasing after a slide to the left cancels. Sliding up locks the recording instead: it goes
 * on without the finger, and releasing then does nothing. The same node stays in place while
 * recording, so the gesture is not interrupted when the bar changes (until the lock replaces the
 * button with the controls).
 */
@Composable
private fun RecordButton(enabled: Boolean, callbacks: VoiceNoteCallbacks) {
  val onStart by rememberUpdatedState(callbacks.onRecordStart)
  val onRelease by rememberUpdatedState(callbacks.onRecordRelease)
  val onCancel by rememberUpdatedState(callbacks.onRecordCancel)
  val onLock by rememberUpdatedState(callbacks.onRecordLock)
  val cancelDistance = with(LocalDensity.current) { CANCEL_DISTANCE_DP.dp.toPx() }
  val lockDistance = with(LocalDensity.current) { LOCK_DISTANCE_DP.dp.toPx() }
  val label = stringResource(R.string.voice_note_record)
  val colors = MaterialTheme.colorScheme
  Box(
      Modifier.size(48.dp)
          .background(colors.primary, CircleShape)
          .semantics { contentDescription = label }
          .testTag(VoiceNoteTestTags.VOICE_NOTE_RECORD)
          .pointerInput(enabled, cancelDistance, lockDistance) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
              val down = awaitFirstDown()
              onStart()
              var cancelled = false
              var locked = false
              while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                cancelled = change.position.x - down.position.x < -cancelDistance
                if (!locked && !cancelled && change.position.y - down.position.y < -lockDistance) {
                  locked = true
                  onLock()
                }
                if (!change.pressed) break
              }
              // A locked recording goes on after the release: its controls end it.
              if (locked) return@awaitEachGesture
              if (cancelled) onCancel() else onRelease()
            }
          },
      contentAlignment = Alignment.Center,
  ) {
    Icon(Icons.Filled.Mic, contentDescription = null, tint = colors.onPrimary)
  }
}

/**
 * The notices fixed just above the record bar: the failed send or playback (with a Retry), a
 * recording that failed by itself, and the information that the microphone is off (with a button to
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
      NoticeSnackbar(
          message = stringResource(R.string.voice_note_playback_failed),
          actionLabel = stringResource(R.string.voice_note_retry),
          onAction = callbacks.onRetry,
          modifier = Modifier.testTag(VoiceNoteTestTags.PLAYBACK_ERROR),
          actionModifier = Modifier.testTag(VoiceNoteTestTags.PLAYBACK_RETRY),
      )
    }
    if (state.showRecordBar && state.sendFailed) {
      NoticeSnackbar(
          message = stringResource(R.string.voice_note_send_failed),
          actionLabel = stringResource(R.string.voice_note_retry),
          onAction = callbacks.onRetry,
          modifier = Modifier.testTag(VoiceNoteTestTags.SEND_ERROR),
          actionModifier = Modifier.testTag(VoiceNoteTestTags.SEND_RETRY),
      )
    }
    if (state.showRecordBar && state.recordingFailed) {
      // No action: the owner tries again by holding the record button, which clears the notice.
      InfoNote(
          message = stringResource(R.string.voice_note_recording_failed),
          modifier = Modifier.testTag(VoiceNoteTestTags.RECORDING_ERROR),
      )
    }
    if (state.showRecordBar && state.micDenied) {
      InfoNote(
          message = stringResource(R.string.voice_note_mic_denied),
          modifier = Modifier.testTag(VoiceNoteTestTags.MIC_DENIED),
          action = {
            OutlinedButton(
                onClick = callbacks.onOpenSettings,
                modifier =
                    Modifier.fillMaxWidth().height(52.dp).testTag(VoiceNoteTestTags.OPEN_SETTINGS),
                shape = MaterialTheme.shapes.medium,
            ) {
              Text(stringResource(R.string.voice_note_open_settings))
            }
          },
      )
    }
  }
}
