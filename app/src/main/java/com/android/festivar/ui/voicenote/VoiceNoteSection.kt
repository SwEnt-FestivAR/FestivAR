// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Test tags of the voice note section and of its player, named after the tags of the "Voice notes"
 * frames in Figma.
 */
object VoiceNoteTestTags {
  const val VOICE_NOTE_AUTHOR = "voiceNoteAuthor"
  const val VOICE_NOTE_PLAY = "voiceNotePlay"
  const val VOICE_NOTE_DURATION = "voiceNoteDuration"
  const val VOICE_NOTE_DELETE = "voiceNoteDelete"
  const val VOICE_NOTE_EMPTY = "voiceNoteEmpty"
  const val CONFIRM_DELETE = "voiceNoteConfirmDelete"
  const val KEEP_BUTTON = "voiceNoteKeep"
  const val CONFIRM_DELETE_BUTTON = "voiceNoteConfirmDeleteButton"
}

/**
 * The voice note section of a task, for the owner and for the volunteers: the label, then the note,
 * and the dialog to confirm the deletion of the note. It shows nothing to a volunteer when the task
 * has no note.
 */
@Composable
fun VoiceNoteSection(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.isVisible) return
  Column(modifier.fillMaxWidth()) {
    SectionLabel(stringResource(R.string.voice_note_label))
    val note = state.note
    if (note != null) {
      VoiceNoteRow(note, state, callbacks)
    }
  }
  if (state.showDeleteConfirm) {
    FestivarConfirmDialog(
        title = stringResource(R.string.voice_note_delete_title),
        message = stringResource(R.string.voice_note_delete_body),
        confirmText = stringResource(R.string.voice_note_delete_confirm),
        dismissText = stringResource(R.string.voice_note_keep),
        onConfirm = callbacks.onConfirmDelete,
        onDismiss = callbacks.onDismissDelete,
        modifier = Modifier.testTag(VoiceNoteTestTags.CONFIRM_DELETE),
        confirmModifier = Modifier.testTag(VoiceNoteTestTags.CONFIRM_DELETE_BUTTON),
        dismissModifier = Modifier.testTag(VoiceNoteTestTags.KEEP_BUTTON),
    )
  }
}

/** The note: who and when, then the player and, for the owner, the trash. */
@Composable
private fun VoiceNoteRow(note: VoiceNote, state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    val time = formatTime(note.createdAt.toLocalTime())
    Text(
        stringResource(
            if (state.canManage) R.string.voice_note_author_you
            else R.string.voice_note_author_organizer,
            time,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR),
    )
    VoiceNotePlayer(
        note.duration,
        state.isPlaying,
        callbacks.onTogglePlay,
        Modifier.fillMaxWidth(),
        trailing = if (state.canManage) ({ VoiceNoteDeleteButton(callbacks.onDelete) }) else null,
    )
  }
}

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
