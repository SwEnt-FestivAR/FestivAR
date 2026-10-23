// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import com.android.festivar.model.voicenote.PlayerState
import com.android.festivar.model.voicenote.RecorderState
import com.android.festivar.model.voicenote.VocalNote
import java.time.Duration

/**
 * Everything the voice note section of a task shows.
 *
 * @property note The voice note of the task, or `null` if it has none.
 * @property canManage Whether the current user is the task owner, who can record and delete. The
 *   other users can only listen.
 * @property recorder What the recorder is doing, to show the timer.
 * @property player What the player is doing.
 * @property sendFailed Whether the last recording could not be uploaded.
 * @property micDenied Whether the microphone permission is refused, so the user cannot record.
 * @property showDeleteConfirm Whether the dialog asking to confirm the deletion is open.
 */
data class VoiceNoteUiState(
    val note: VocalNote? = null,
    val canManage: Boolean = false,
    val recorder: RecorderState = RecorderState.Idle,
    val player: PlayerState = PlayerState.Idle,
    val sendFailed: Boolean = false,
    val micDenied: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
  /**
   * Whether the section has something to show: a note to everybody, or the empty state to the
   * owner.
   */
  val isVisible: Boolean
    get() = note != null || canManage

  /** Whether the owner can record. The bar is hidden while a note exists. */
  val showRecordBar: Boolean
    get() = canManage && note == null

  /** Whether the user is recording right now. */
  val isRecording: Boolean
    get() = recorder is RecorderState.Recording

  /** The time recorded so far, zero when not recording. */
  val elapsed: Duration
    get() = (recorder as? RecorderState.Recording)?.elapsed ?: Duration.ZERO

  /** Whether the record button works: it does not without the microphone permission. */
  val recordEnabled: Boolean
    get() = !micDenied

  /** Whether the note of the task is playing. */
  val isPlaying: Boolean
    get() = note != null && (player as? PlayerState.Playing)?.audioUrl == note.audioUrl

  /** Whether the note of the task could not be played. */
  val playbackFailed: Boolean
    get() = note != null && (player as? PlayerState.Failed)?.audioUrl == note.audioUrl

  /** Whether the section shows the owner the empty state. */
  val showEmpty: Boolean
    get() = canManage && note == null

  /** Whether a notice (error or information) has to be shown above the record bar. */
  val hasNotices: Boolean
    get() = playbackFailed || (showRecordBar && (sendFailed || micDenied))
}

/** What the user can do in the voice note section. All do nothing by default. */
data class VoiceNoteCallbacks(
    /** Plays the note, or stops it if it is playing. */
    val onTogglePlay: () -> Unit = {},
    /** The owner taps the trash: asks to confirm the deletion. */
    val onDelete: () -> Unit = {},
    /** The owner confirms the deletion. */
    val onConfirmDelete: () -> Unit = {},
    /** The owner keeps the note. */
    val onDismissDelete: () -> Unit = {},
    /** The owner presses the record button. */
    val onRecordStart: () -> Unit = {},
    /** The owner releases the button: the recording is sent. */
    val onRecordRelease: () -> Unit = {},
    /** The owner slides left and releases: the recording is dropped. */
    val onRecordCancel: () -> Unit = {},
    /** Tries again the last failed action (sending or playing). */
    val onRetry: () -> Unit = {},
    /** Opens the settings of the app to turn the microphone on. */
    val onOpenSettings: () -> Unit = {},
)
