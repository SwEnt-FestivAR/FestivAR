// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.voicenote

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VoiceNoteTest {

  private val created = ZonedDateTime.of(2026, 10, 17, 9, 30, 15, 0, ZoneId.of("UTC"))

  private fun note(
      id: String = "n1",
      audioUrl: String = "https://example.com/n1.m4a",
      duration: Duration = Duration.ofSeconds(14),
      waveform: List<Float> = listOf(0f, 0.25f, 1f),
  ) = VoiceNote(id, audioUrl, duration, created, waveform)

  private fun assertInvalid(expectedMessage: String, build: () -> Any?) {
    val exception = assertThrows(IllegalArgumentException::class.java) { build() }
    assertEquals(expectedMessage, exception.message)
  }

  // ----------------------------------- creation ------------------------------------------- //

  @Test
  fun init_rejects_zeroDuration() {
    assertInvalid("The duration must be positive.") { note(duration = Duration.ZERO) }
  }

  @Test
  fun init_rejects_blankId() {
    listOf("", " ", "\t\n").forEach { assertInvalid("The id cannot be empty.") { note(id = it) } }
  }

  @Test
  fun init_rejects_blankAudioUrl() {
    listOf("", " ", "\t\n").forEach {
      assertInvalid("The audioUrl cannot be empty.") { note(audioUrl = it) }
    }
  }

  @Test
  fun init_rejects_negativeDuration() {
    assertInvalid("The duration must be positive.") { note(duration = Duration.ofMillis(-1)) }
  }

  // ----------------------------------- map conversion ------------------------------------- //

  /**
   * Breaks if the duration is no longer stored in nanoseconds or the date as ISO text, so saved
   * notes become unreadable.
   */
  @Test
  fun toMap_keepsEveryField_withTheStoredTypes() {
    assertEquals(
        mapOf(
            "id" to "n1",
            "audioUrl" to "https://example.com/n1.m4a",
            "duration" to 14_000_000_000L,
            "createdAt" to "2026-10-17T09:30:15Z[UTC]",
            "waveform" to listOf(0.0, 0.25, 1.0),
        ),
        note().toMap(),
    )
  }

  /** Breaks if a note read back from the database differs from the one that was saved. */
  @Test
  fun fromMap_rebuildsTheNote_writtenByToMap() {
    assertEquals(note(), VoiceNote.fromMap(note().toMap()))
  }

  /** Breaks if reading a note back changes the zone of its creation date. */
  @Test
  fun fromMap_keepsTheZoneOfTheDate() {
    val inZurich = note().copy(createdAt = created.withZoneSameInstant(ZoneId.of("Europe/Zurich")))

    assertEquals(inZurich, VoiceNote.fromMap(inZurich.toMap()))
  }

  /** Breaks if a duration stored as another number type than Long is no longer read. */
  @Test
  fun fromMap_readsTheDuration_fromAnyNumberType() {
    // Firestore returns whole numbers as Long, but any Number is read the same way.
    val map = note().toMap() + ("duration" to 5_000)

    assertEquals(Duration.ofNanos(5_000), VoiceNote.fromMap(map).duration)
  }

  /** Breaks if a note with a missing or wrongly typed field is accepted instead of refused. */
  @Test
  fun fromMap_rejects_aMissingOrWrongTypedField() {
    listOf("id", "audioUrl", "duration", "createdAt").forEach { name ->
      assertInvalid("Invalid voice note data: '$name' is missing or invalid.") {
        VoiceNote.fromMap(note().toMap() - name)
      }
      assertInvalid("Invalid voice note data: '$name' is missing or invalid.") {
        VoiceNote.fromMap(note().toMap() + (name to true))
      }
    }
  }

  /** Breaks if an unreadable creation date is accepted or fails without a clear message. */
  @Test
  fun fromMap_rejects_aBadDate() {
    assertInvalid("Invalid voice note data: bad createdAt 'yesterday'.") {
      VoiceNote.fromMap(note().toMap() + ("createdAt" to "yesterday"))
    }
  }

  /** Breaks if stored values that break the rules of a note are accepted when read. */
  @Test
  fun fromMap_rejects_valuesThatBreakTheRules() {
    assertInvalid("The duration must be positive.") {
      VoiceNote.fromMap(note().toMap() + ("duration" to 0L))
    }
    assertInvalid("The id cannot be empty.") { VoiceNote.fromMap(note().toMap() + ("id" to " ")) }
  }

  // ----------------------------------- waveform ------------------------------------------- //

  /** Breaks if a bar outside 0 to 1, or not a number, is accepted in a note. */
  @Test
  fun init_rejects_barsOutsideZeroToOne() {
    listOf(-0.1f, 1.1f, Float.NaN).forEach { bar ->
      assertInvalid("The waveform bars must be between 0 and 1.") { note(waveform = listOf(bar)) }
    }
  }

  /** Breaks if the bars at exactly 0 and 1 are refused. */
  @Test
  fun init_accepts_barsAtTheLimits() {
    assertEquals(listOf(0f, 1f), note(waveform = listOf(0f, 1f)).waveform)
  }

  /** Breaks if the bars are not read back when Firestore returns its numbers as Double. */
  @Test
  fun fromMap_rebuildsTheWaveform_whenFirestoreReturnsDoubles() {
    val stored = note().toMap() + ("waveform" to listOf(0.0, 0.25, 1.0))

    assertEquals(listOf(0f, 0.25f, 1f), VoiceNote.fromMap(stored).waveform)
  }

  /** Breaks if a note stored before waveforms existed can no longer be read. */
  @Test
  fun fromMap_readsAnOldNoteWithoutBars() {
    val old = note().toMap() - "waveform"

    assertEquals(note(waveform = emptyList()), VoiceNote.fromMap(old))
  }

  /** Breaks if a waveform that is not a list of numbers is accepted instead of refused. */
  @Test
  fun fromMap_rejects_aBadWaveform() {
    listOf("text", listOf("a"), listOf(0.5, null)).forEach { bad ->
      assertInvalid("Invalid voice note data: bad waveform.") {
        VoiceNote.fromMap(note().toMap() + ("waveform" to bad))
      }
    }
  }

  /** Breaks if stored bars outside 0 to 1 are accepted when read. */
  @Test
  fun fromMap_rejects_barsOutsideZeroToOne() {
    assertInvalid("The waveform bars must be between 0 and 1.") {
      VoiceNote.fromMap(note().toMap() + ("waveform" to listOf(2.0)))
    }
  }
}
