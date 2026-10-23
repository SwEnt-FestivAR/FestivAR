// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.time.Duration
import java.time.LocalDateTime

/**
 * The voice note of a task, so that its assignees can listen to the explanations instead of reading
 * them. It only holds the data: the audio file itself lives at [audioUrl]. How it is stored in the
 * database is up to the repository.
 *
 * A [VocalNote] is immutable. Every new version goes through the `init` block, so the rules below
 * always hold:
 * - [id] is not blank;
 * - [audioUrl] is not blank;
 * - [duration] is not negative.
 *
 * @property id Unique identifier of the [VocalNote].
 * @property audioUrl Where the audio file can be played from.
 * @property duration Length of the recording.
 * @property createdAt When the note was recorded.
 */
data class VocalNote(
    val id: String,
    val audioUrl: String,
    val duration: Duration,
    val createdAt: LocalDateTime,
) {

  init {
    require(id.isNotBlank()) { "The id cannot be empty." }
    require(audioUrl.isNotBlank()) { "The audioUrl cannot be empty." }
    require(!duration.isNegative) { "The duration cannot be negative." }
  }
}
