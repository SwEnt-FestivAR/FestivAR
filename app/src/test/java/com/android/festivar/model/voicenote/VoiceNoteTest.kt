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
  ) = VoiceNote(id, audioUrl, duration, created)

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

  @Test
  fun toMap_keepsEveryField_withTheStoredTypes() {
    assertEquals(
        mapOf(
            "id" to "n1",
            "audioUrl" to "https://example.com/n1.m4a",
            "duration" to 14_000_000_000L,
            "createdAt" to "2026-10-17T09:30:15Z[UTC]",
        ),
        note().toMap(),
    )
  }

  @Test
  fun fromMap_rebuildsTheNote_writtenByToMap() {
    assertEquals(note(), VoiceNote.fromMap(note().toMap()))
  }

  @Test
  fun fromMap_keepsTheZoneOfTheDate() {
    val inZurich = note().copy(createdAt = created.withZoneSameInstant(ZoneId.of("Europe/Zurich")))

    assertEquals(inZurich, VoiceNote.fromMap(inZurich.toMap()))
  }

  @Test
  fun fromMap_readsTheDuration_fromAnyNumberType() {
    // Firestore returns whole numbers as Long, but any Number is read the same way.
    val map = note().toMap() + ("duration" to 5_000)

    assertEquals(Duration.ofNanos(5_000), VoiceNote.fromMap(map).duration)
  }

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

  @Test
  fun fromMap_rejects_aBadDate() {
    assertInvalid("Invalid voice note data: bad createdAt 'yesterday'.") {
      VoiceNote.fromMap(note().toMap() + ("createdAt" to "yesterday"))
    }
  }

  @Test
  fun fromMap_rejects_valuesThatBreakTheRules() {
    assertInvalid("The duration must be positive.") {
      VoiceNote.fromMap(note().toMap() + ("duration" to 0L))
    }
    assertInvalid("The id cannot be empty.") { VoiceNote.fromMap(note().toMap() + ("id" to " ")) }
  }
}
