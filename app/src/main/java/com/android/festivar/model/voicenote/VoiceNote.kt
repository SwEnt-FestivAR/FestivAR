// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import com.android.festivar.model.audio.Audio
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * The voice note of a task, so that its assignees can listen to the explanations instead of reading
 * them. It only holds the data: the audio file itself lives at [audioUrl]. How it is stored in the
 * database is up to the repository.
 *
 * A [VoiceNote] is immutable. Every new version goes through the `init` block, so the rules below
 * always hold:
 * - [id] is not blank;
 * - [audioUrl] is not blank;
 * - [duration] is positive (a recording of zero length is refused, like by [Audio]).
 *
 * @property id Unique identifier of the [VoiceNote].
 * @property audioUrl Where the audio file can be played from.
 * @property duration Length of the recording.
 * @property createdAt When the note was recorded.
 */
data class VoiceNote(
    val id: String,
    val audioUrl: String,
    val duration: Duration,
    val createdAt: ZonedDateTime,
) {

  init {
    require(id.isNotBlank()) { "The id cannot be empty." }
    require(audioUrl.isNotBlank()) { "The audioUrl cannot be empty." }
    require(duration.isPositive()) { "The duration must be positive." }
  }

  /**
   * The note as the map stored in the database. [duration] is kept in nanoseconds and [createdAt]
   * as ISO text, the same way a task stores its estimated time and its dates.
   */
  fun toMap(): Map<String, Any> =
      mapOf(
          "id" to id,
          "audioUrl" to audioUrl,
          "duration" to duration.toNanos(),
          "createdAt" to createdAt.toString(),
      )

  companion object {
    /**
     * Rebuilds a note from a [map] written by [toMap].
     *
     * @throws IllegalArgumentException if a field is missing or has the wrong type, or if the
     *   values break a rule of [VoiceNote].
     */
    fun fromMap(map: Map<*, *>): VoiceNote {
      fun <T> field(name: String, value: T?): T =
          requireNotNull(value) { "Invalid voice note data: '$name' is missing or invalid." }

      return VoiceNote(
          id = field("id", map["id"] as? String),
          audioUrl = field("audioUrl", map["audioUrl"] as? String),
          duration =
              field("duration", (map["duration"] as? Number)?.toLong()).let(Duration::ofNanos),
          createdAt =
              field("createdAt", map["createdAt"] as? String).let {
                try {
                  ZonedDateTime.parse(it)
                } catch (e: DateTimeParseException) {
                  throw IllegalArgumentException("Invalid voice note data: bad createdAt '$it'.", e)
                }
              },
      )
    }
  }
}
