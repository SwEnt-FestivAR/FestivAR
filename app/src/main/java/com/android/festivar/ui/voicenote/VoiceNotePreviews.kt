// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.audio.PlayerState
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

/** The section as on the screen, in its scrolling column. */
@Composable
private fun VoiceNotePreviewScreen(state: VoiceNoteUiState) {
  AppTheme {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
      VoiceNoteSection(state, callbacks)
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteOwnerPreview() {
  VoiceNotePreviewScreen(VoiceNoteUiState(note = previewNote, canManage = true))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteOwnerPlayingPreview() {
  VoiceNotePreviewScreen(
      VoiceNoteUiState(
          note = previewNote,
          canManage = true,
          player = PlayerState.Playing(previewNote.audioUrl),
      )
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteEmptyPreview() {
  VoiceNotePreviewScreen(VoiceNoteUiState(canManage = true))
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteDeleteConfirmPreview() {
  VoiceNotePreviewScreen(
      VoiceNoteUiState(note = previewNote, canManage = true, showDeleteConfirm = true)
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNoteVolunteerPreview() {
  VoiceNotePreviewScreen(VoiceNoteUiState(note = previewNote))
}
