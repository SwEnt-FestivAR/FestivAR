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
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.components.InfoNote
import com.android.festivar.ui.components.NoticeSnackbar

/** How far to the left (in dp) the finger has to slide before releasing to cancel. */
private const val CANCEL_DISTANCE_DP = 80

/**
 * The record bar, fixed above the bottom of the screen. Only the owner of a task without a note
 * sees it: it is hidden otherwise. The owner holds the round button to record, releases it to send
 * and slides left before releasing to cancel. Without the microphone permission it is greyed out.
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(start = 20.dp, top = 6.dp, end = 6.dp, bottom = 6.dp)
            .testTag(VoiceNoteTestTags.RECORDING_BAR),
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
          modifier = Modifier.testTag(VoiceNoteTestTags.RECORDING_HINT),
      )
    }
  }
}

/**
 * The round lime microphone button. It reacts to the press itself: pressing starts, releasing sends
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
          .testTag(VoiceNoteTestTags.VOICE_NOTE_RECORD)
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
 * The notices fixed just above the record bar: the failed send or playback (with a Retry) and the
 * information that the microphone is off (with a button to the settings).
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
