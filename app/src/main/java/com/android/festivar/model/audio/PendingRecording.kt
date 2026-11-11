// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import java.time.Duration

/**
 * A recording that is not sent yet. It is not a [VoiceNote] yet, because it has no URL: only its
 * length is known. It is first [Waiting] for the owner to confirm or cancel, then [Sending].
 */
sealed interface PendingRecording {
  /** The length of the recording, shown while it waits and while it is sent. */
  val duration: Duration

  /** The recording waits for the owner to send it or to drop it. */
  data class Waiting(override val duration: Duration) : PendingRecording

  /** The owner confirmed and the recording is being uploaded. */
  data class Sending(override val duration: Duration) : PendingRecording
}
