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
 * - [duration] is positive (a recording of zero length is refused, like by [Audio]);
 * - every bar of [waveform] is between 0 and 1. Its length is not checked: the app records
 *   [com.android.festivar.model.audio.WAVEFORM_BAR_COUNT] bars, but a note stored with another
 *   number of bars, or none, is still valid, so whoever draws it must cope with any length.
 *
 * @property id Unique identifier of the [VoiceNote].
 * @property audioUrl Where the audio file can be played from.
 * @property duration Length of the recording.
 * @property createdAt When the note was recorded.
 * @property waveform The loudness of the recording as bars between 0 and 1, taken while recording.
 *   Empty for a note stored before waveforms existed.
 */
data class VoiceNote(
    val id: String,
    val audioUrl: String,
    val duration: Duration,
    val createdAt: ZonedDateTime,
    val waveform: List<Float> = emptyList(),
) {

  init {
    require(id.isNotBlank()) { "The id cannot be empty." }
    require(audioUrl.isNotBlank()) { "The audioUrl cannot be empty." }
    require(duration.isPositive()) { "The duration must be positive." }
    require(waveform.all { it in 0f..1f }) { "The waveform bars must be between 0 and 1." }
  }

  /**
   * The note as the map stored in the database. [duration] is kept in nanoseconds and [createdAt]
   * as ISO text, the same way a task stores its estimated time and its dates. The [waveform] is a
   * list of numbers.
   */
  fun toMap(): Map<String, Any> =
      mapOf(
          "id" to id,
          "audioUrl" to audioUrl,
          "duration" to duration.toNanos(),
          "createdAt" to createdAt.toString(),
          // Firestore keeps its numbers as Double, so the bars are written as such.
          "waveform" to waveform.map { it.toDouble() },
      )

  companion object {
    /**
     * Rebuilds a note from a [map] written by [toMap].
     *
     * A map without a `waveform`, as stored before waveforms existed, gives a note without bars.
     *
     * @throws IllegalArgumentException if a field is missing or has the wrong type, or if the
     *   values break a rule of [VoiceNote].
     */
    fun fromMap(map: Map<*, *>): VoiceNote {
      fun <T> field(name: String, value: T?): T =
          requireNotNull(value) { "Invalid voice note data: '$name' is missing or invalid." }

      // Firestore returns whole numbers as Long. Any other number type is cut to a whole number of
      // nanoseconds, which is accepted because the app never writes one.
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
          waveform = readWaveform(map["waveform"]),
      )
    }

    private fun readWaveform(value: Any?): List<Float> {
      if (value == null) return emptyList()
      val bars = requireNotNull(value as? List<*>) { "Invalid voice note data: bad waveform." }
      return bars.map {
        requireNotNull((it as? Number)?.toFloat()) { "Invalid voice note data: bad waveform." }
      }
    }
  }
}
