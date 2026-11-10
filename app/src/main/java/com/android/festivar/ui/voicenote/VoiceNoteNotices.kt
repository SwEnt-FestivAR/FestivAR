// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.ui.components.FixedBottomBar
import com.android.festivar.ui.components.InfoNote
import com.android.festivar.ui.components.InfoNoteButton
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.theme.AppTheme

/**
 * An error of the voice note, shown with the shared snackbar and a Retry. The [modifier] is applied
 * before the tag, so the tagged node is the snackbar alone, without the space around it.
 */
@Composable
internal fun ErrorNotice(
    @StringRes message: Int,
    onRetry: () -> Unit,
    tag: String,
    actionTag: String,
    modifier: Modifier = Modifier,
) {
  NoticeSnackbar(
      message = stringResource(message),
      actionLabel = stringResource(R.string.action_retry),
      onAction = onRetry,
      modifier = modifier.testTag(tag),
      actionModifier = Modifier.testTag(actionTag),
  )
}

/**
 * The notices above the record bar: the failed send (with a Retry), and the information that the
 * microphone is off (with a button to the settings).
 */
@Composable
internal fun VoiceNoteOwnerNotices(state: VoiceNoteUiState, callbacks: VoiceNoteCallbacks) {
  if (state.sendFailed) {
    ErrorNotice(
        message = R.string.voice_note_send_failed,
        onRetry = callbacks.onRetry,
        tag = VoiceNoteRecordTestTags.SEND_ERROR,
        actionTag = VoiceNoteRecordTestTags.SEND_RETRY,
    )
  }
  if (state.micDenied) {
    InfoNote(
        message = stringResource(R.string.voice_note_mic_denied),
        modifier = Modifier.testTag(VoiceNoteRecordTestTags.MIC_DENIED),
        action = {
          InfoNoteButton(
              stringResource(R.string.action_open_settings),
              onClick = callbacks.onOpenSettings,
              modifier = Modifier.testTag(VoiceNoteRecordTestTags.OPEN_SETTINGS),
          )
        },
    )
  }
}

/**
 * The notice of a failed playback. It is not part of the record bar: a note exists, so the bar is
 * hidden, and everybody can listen, so everybody must see it. The design ("notice (fixed)") puts it
 * just above the buttons of the task. Like [VoiceNoteOwnerNotices] it has no bar and no padding of
 * its own: the screen puts it in the [FixedBottomBar] it shares with those buttons.
 */
@Composable
fun VoiceNotePlaybackNotice(
    state: VoiceNoteUiState,
    callbacks: VoiceNoteCallbacks,
    modifier: Modifier = Modifier,
) {
  if (!state.playbackFailed) return
  ErrorNotice(
      message = R.string.voice_note_playback_failed,
      onRetry = callbacks.onRetry,
      tag = VoiceNoteRecordTestTags.PLAYBACK_ERROR,
      actionTag = VoiceNoteRecordTestTags.PLAYBACK_RETRY,
      modifier = modifier,
  )
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNotePlaybackFailedPreview() {
  AppTheme {
    FixedBottomBar {
      VoiceNotePlaybackNotice(
          VoiceNoteUiState(note = previewNote, player = PlayerState.Failed(previewNote.audioUrl)),
          previewCallbacks,
      )
    }
  }
}
