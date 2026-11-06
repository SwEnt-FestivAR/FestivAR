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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration

/** Test tags of the record bar, named after the tags of the "Voice notes" frames in Figma. */
object VoiceNoteRecordTestTags {
  const val RECORDING_HINT = "voiceNoteRecordingHint"
  const val RECORDING_BAR = "voiceNoteRecordingBar"
  const val VOICE_NOTE_RECORD = "voiceNoteRecord"
}

/** How far to the left (in dp) the finger has to slide before releasing to cancel. */
private const val CANCEL_DISTANCE_DP = 80

/**
 * The record bar, fixed above the bottom of the screen. Only the owner of a task without a note
 * sees it: it is hidden otherwise. The owner holds the round button to record, releases it to stop
 * and slides left before releasing to cancel. Without the microphone permission it is greyed out.
 */
@Composable
fun VoiceNoteRecordBar(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.showRecordBar) return
  FixedBottomBar(modifier) { RecordPill(state, callbacks) }
}

/** The pill with the hint or the timer, and the record button. */
@Composable
private fun RecordPill(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  Row(
      Modifier.fillMaxWidth()
          .alpha(if (state.recordEnabled) 1f else 0.5f)
          // The tag is before the padding so that the node is the whole 56dp bar of the design
          // (a 48dp button with 4dp above and below), not only its content.
          .testTag(VoiceNoteRecordTestTags.RECORDING_BAR)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
          .padding(start = 20.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
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

private val callbacks = VoiceNoteCallbacks.NoOp

/** The record bar fixed at the bottom of the screen, as on the screen. */
@Composable
private fun VoiceNoteBarPreviewScreen(state: VoiceNoteUiState) {
  AppTheme { Column(Modifier.fillMaxWidth()) { VoiceNoteRecordBar(state, callbacks) } }
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
