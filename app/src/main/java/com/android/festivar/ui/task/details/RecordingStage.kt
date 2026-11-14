// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.Audio
import com.android.festivar.model.audio.PendingRecording

/**
 * What became of the last recording of the owner, from the press of the record button to the saved
 * note. There is one stage at a time, so the screen cannot be, for example, sending a recording
 * that was just dropped.
 */
internal sealed interface RecordingStage {
  /** Nothing to show: there is no recording, or the last one is saved or dropped. */
  data object Idle : RecordingStage

  /** The recording could not be started or finished. */
  data object Failed : RecordingStage

  /** A recording that is made and not saved yet, kept in the temporary file of [audio]. */
  sealed interface Held : RecordingStage {
    val audio: Audio
  }

  /** The owner released the button: the recording waits to be sent or dropped. */
  data class Waiting(override val audio: Audio) : Held

  /** The owner confirmed: the recording is being uploaded. */
  data class Sending(override val audio: Audio) : Held

  /** The recording could not be sent, and is kept for another try. */
  data class SendFailed(override val audio: Audio) : Held
}

/** The recording as the section shows it while it waits or is sent, `null` in any other stage. */
internal val RecordingStage.pending: PendingRecording?
  get() =
      when (this) {
        is RecordingStage.Waiting -> PendingRecording.Waiting(audio.duration)
        is RecordingStage.Sending -> PendingRecording.Sending(audio.duration)
        else -> null
      }
