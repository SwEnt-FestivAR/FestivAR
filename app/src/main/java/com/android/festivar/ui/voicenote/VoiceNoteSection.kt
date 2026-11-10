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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.android.festivar.ui.theme.AppTheme
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * The voice note section of a task, for the owner and for the volunteers: the label, then the note
 * or the empty state, and the dialog to confirm the deletion of the note. It shows nothing to a
 * volunteer when the task has no note.
 *
 * It only draws the note and the empty state. The record bar and the notices about the recording or
 * the playback (errors, missing microphone permission) are other composables, so they are not shown
 * here.
 *
 * @param ownerName Who is credited with the note when the current user is not its owner. The owner
 *   of a task is not always the organizer of the event, since every member can create a task, so
 *   the caller must give the name of the owner.
 * @param clock Tells the current year, which the date of the note only shows when it is another.
 */
@Composable
fun VoiceNoteSection(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    ownerName: String,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemDefaultZone(),
) {
  if (!state.isVisible) return
  Column(modifier.fillMaxWidth()) {
    SectionLabel(stringResource(R.string.voice_note_label))
    val note = state.note
    if (note != null) {
      VoiceNoteRow(note, state, callbacks, ownerName, clock)
    } else {
      EmptyVoiceNote()
    }
  }
  if (state.showDeleteDialog) {
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

/**
 * The note: who and when (date, with the year if it is not this one, and time), then the player
 * and, for the owner, the trash.
 */
@Composable
private fun VoiceNoteRow(
    note: VoiceNote,
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    ownerName: String,
    clock: Clock,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    // The date is there because a task can last several days
    val recordedAt =
        formatRecordedAt(note.createdAt, clock, stringResource(R.string.voice_note_today))
    Text(
        if (state.canManage) stringResource(R.string.voice_note_author_you, recordedAt)
        else stringResource(R.string.voice_note_author_other, ownerName, recordedAt),
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

/**
 * What the owner sees when the task has no voice note: the compact "empty voice note" row of the S5
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

internal val previewNote =
    VoiceNote(
        id = "n1",
        audioUrl = "https://example.com/n1.m4a",
        duration = Duration.ofSeconds(14),
        createdAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC),
    )

internal val previewCallbacks = VoiceNoteCallbacks.NoOp

/** The section as on the screen, in its scrolling column. */
@Composable
private fun VoiceNotePreviewScreen(state: VoiceNoteUiState) {
  AppTheme {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
      VoiceNoteSection(state, previewCallbacks, stringResource(R.string.voice_note_organizer))
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
