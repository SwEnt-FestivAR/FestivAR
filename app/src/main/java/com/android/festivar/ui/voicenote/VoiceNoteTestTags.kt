// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

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
  const val CONFIRM_SEND = "voiceNoteConfirmSend"
  const val CANCEL_SEND_BUTTON = "voiceNoteCancelSendButton"
  const val CONFIRM_SEND_BUTTON = "voiceNoteConfirmSendButton"
}

/**
 * Test tags of the record bar and of the notices above it, named after the tags of the "Voice
 * notes" frames in Figma. [RECORD_BAR] is the one exception (RECORDING_BAR in Figma): the bar is
 * there while idle too, not only while recording.
 */
object VoiceNoteRecordTestTags {
  const val RECORD_BAR = "voiceNoteRecordBar"
  const val VOICE_NOTE_SENDING = "voiceNoteSending"
  const val VOICE_NOTE_RECORD = "voiceNoteRecord"
  const val RECORDING_DOT = "voiceNoteRecordingDot"
  const val SEND_ERROR = "voiceNoteSendError"
  const val SEND_RETRY = "voiceNoteSendRetry"
  const val RECORDING_ERROR = "voiceNoteRecordingError"
  const val RECORDING_ERROR_DISMISS = "voiceNoteRecordingErrorDismiss"
  const val MIC_DENIED = "voiceNoteMicDenied"
  const val OPEN_SETTINGS = "voiceNoteOpenSettings"
  const val PLAYBACK_ERROR = "voiceNotePlaybackError"
  const val PLAYBACK_RETRY = "voiceNotePlaybackRetry"
}
