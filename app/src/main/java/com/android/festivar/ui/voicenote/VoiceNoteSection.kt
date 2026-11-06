// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.formatTime

/**
 * The voice note section of a task, for the owner and for the volunteers: the label, then the note,
 * and the dialog to confirm the deletion of the note.
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
    Row(verticalAlignment = Alignment.CenterVertically) {
      VoiceNotePlayer(
          note.duration,
          state.isPlaying,
          callbacks.onTogglePlay,
          Modifier.weight(1f),
      )
      if (state.canManage) {
        IconButton(
            onClick = callbacks.onDelete,
            modifier = Modifier.testTag(VoiceNoteTestTags.VOICE_NOTE_DELETE),
        ) {
          Icon(
              Icons.Outlined.DeleteOutline,
              contentDescription = stringResource(R.string.voice_note_delete),
          )
        }
      }
    }
  }
}
