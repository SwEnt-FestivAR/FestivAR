// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.RecorderState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
