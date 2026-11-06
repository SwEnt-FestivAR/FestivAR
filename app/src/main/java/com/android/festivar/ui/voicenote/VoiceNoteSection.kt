// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 * Test tags named after the tags of the "Voice notes" frames in Figma. They tag the voice note
 * section, the record bar and the notices, which are in this package.
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
 * The voice note section of a task, for the owner and for the volunteers: the label, then the note
 * or the empty state, and the dialog to confirm the deletion of the note. It shows nothing to a
 * volunteer when the task has no note.
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
    } else {
      EmptyVoiceNote()
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

/**
 * What the owner sees when the task has no voice note: the compact "empty voice note" row of the S4
 * design, a microphone on a round disc, the title and a one-line hint.
 */
@Composable
private fun EmptyVoiceNote() {
  Surface(
      Modifier.fillMaxWidth().testTag(VoiceNoteTestTags.VOICE_NOTE_EMPTY),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Row(
        Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
          Modifier.size(48.dp)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
          contentAlignment = Alignment.Center,
      ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.voice_note_empty_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.voice_note_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
