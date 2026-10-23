// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.voicenote.VocalNote
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.formatTime

/** Relative heights of the bars of the static waveform. */
private val waveform =
    listOf(0.3f, 0.5f, 0.8f, 0.6f, 0.9f, 0.4f, 0.7f, 1f, 0.55f, 0.35f, 0.65f, 0.85f, 0.45f, 0.3f)

/**
 * The voice note section of a task, for the owner and for the volunteers: the label, then the note
 * or the empty state, and the dialog to confirm the deletion. It shows nothing to a volunteer when
 * the task has no note. The record bar and the notices are separate composables
 * ([VoiceNoteRecordBar], [VoiceNoteNotices]) because the screen fixes them at the bottom.
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
    ConfirmDialog(
        title = stringResource(R.string.voice_note_delete_title),
        text = stringResource(R.string.voice_note_delete_body),
        confirmLabel = stringResource(R.string.voice_note_delete_confirm),
        dismissLabel = stringResource(R.string.voice_note_keep),
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
private fun VoiceNoteRow(note: VocalNote, state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
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
      Row(
          Modifier.weight(1f)
              .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
              .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        PlayButton(state.isPlaying, callbacks.onTogglePlay)
        Waveform(state.isPlaying, Modifier.weight(1f))
        Text(
            formatVoiceDuration(note.duration),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag(VoiceNoteTestTags.VOICE_NOTE_DURATION),
        )
      }
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

/** The lime round button that plays the note, or stops it while it plays. */
@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
  FilledIconButton(
      onClick = onClick,
      modifier = Modifier.size(40.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_PLAY),
  ) {
    Icon(
        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        contentDescription =
            stringResource(if (isPlaying) R.string.voice_note_pause else R.string.voice_note_play),
    )
  }
}

/** A static waveform: the bars only decorate, they do not follow the audio. */
@Composable
private fun Waveform(isPlaying: Boolean, modifier: Modifier = Modifier) {
  val color =
      if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
      else MaterialTheme.colorScheme.outline
  Row(
      modifier.height(28.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    waveform.forEach { level ->
      Box(Modifier.width(3.dp).height(28.dp * level).background(color, RoundedCornerShape(2.dp)))
    }
  }
}

/** What the owner sees when the task has no voice note. */
@Composable
private fun EmptyVoiceNote() {
  Surface(
      Modifier.fillMaxWidth().testTag(VoiceNoteTestTags.VOICE_NOTE_EMPTY),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
        Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
          stringResource(R.string.voice_note_empty_title),
          style = MaterialTheme.typography.titleMedium,
      )
      Text(
          stringResource(R.string.voice_note_empty_body),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
      )
    }
  }
}
