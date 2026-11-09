// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event.announcement

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests of [Announcement.toMap] and [Announcement.fromMap], the format stored in Firestore. */
class AnnouncementMappingTest {

  private val requiredFields =
      listOf("announcementId", "title", "authorId", "priority", "updatedAt")
  private val optionalFields = listOf("content", "links", "pinned")

  /** Every field away from its default, with a named zone, so a lossy mapping shows. */
  private val full =
      Announcement(
          announcementId = "a1",
          title = "Gates open",
          authorId = "author-1",
          content = "Main gate at 18:00.",
          priority = AnnouncementPriority.URGENT,
          updatedAt = ZonedDateTime.of(2026, 10, 17, 9, 30, 15, 0, ZoneId.of("Europe/Zurich")),
          links = listOf("https://example.com/map", "https://example.com/faq"),
          pinned = true,
      )

  private fun missingOrInvalid(name: String) =
      "Invalid announcement data: '$name' is missing or invalid."

  private fun assertInvalid(expectedMessage: String, map: Map<*, *>): IllegalArgumentException {
    val exception = assertThrows(IllegalArgumentException::class.java) { Announcement.fromMap(map) }
    assertEquals(expectedMessage, exception.message)
    return exception
  }

  // ----------------------------------- toMap ---------------------------------------------- //

  /** Red if a field is renamed or stored with another type, so saved announcements break. */
  @Test
  fun toMap_keepsEveryField_withTheStoredTypes() {
    assertEquals(
        mapOf(
            "announcementId" to "a1",
            "title" to "Gates open",
            "authorId" to "author-1",
            "content" to "Main gate at 18:00.",
            "priority" to "URGENT",
            "updatedAt" to "2026-10-17T09:30:15+02:00[Europe/Zurich]",
            "links" to listOf("https://example.com/map", "https://example.com/faq"),
            "pinned" to true,
        ),
        full.toMap(),
    )
  }

  // ----------------------------------- round trip ----------------------------------------- //

  /** Red if an announcement read back differs from the saved one, zone of the date included. */
  @Test
  fun fromMap_rebuildsTheAnnouncement_writtenByToMap() {
    val withDefaults =
        Announcement(
            announcementId = "a2",
            title = "Info",
            authorId = "author-2",
            updatedAt = ZonedDateTime.parse("2026-10-17T10:00:00Z"),
        )

    listOf(full, withDefaults).forEach { assertEquals(it, Announcement.fromMap(it.toMap())) }
  }

  // ----------------------------------- optional fields ------------------------------------ //

  /** Red if an announcement saved without content, links or pin state can no longer be read. */
  @Test
  fun fromMap_usesTheDefaults_whenTheOptionalFieldsAreAbsentOrNull() {
    val expected = full.copy(content = "", links = emptyList(), pinned = false)

    assertEquals(expected, Announcement.fromMap(full.toMap() - optionalFields.toSet()))
    assertEquals(
        expected,
        Announcement.fromMap(full.toMap() + optionalFields.associateWith { null }),
    )
  }

  // ----------------------------------- invalid data --------------------------------------- //

  /** Red if a missing required field is replaced by a made-up value instead of refused. */
  @Test
  fun fromMap_rejects_aMissingOrNullRequiredField() {
    requiredFields.forEach { name ->
      assertInvalid(missingOrInvalid(name), full.toMap() - name)
      assertInvalid(missingOrInvalid(name), full.toMap() + (name to null))
    }
  }

  /** Red if a field of the wrong type is accepted, or an optional one falls back to its default. */
  @Test
  fun fromMap_rejects_aFieldOfTheWrongType() {
    // A number is the wrong type for every field: they are strings, a list or a boolean.
    (requiredFields + optionalFields).forEach { name ->
      assertInvalid(missingOrInvalid(name), full.toMap() + (name to 42))
    }
  }

  /** Red if a link that is not a string is dropped or read instead of refused. */
  @Test
  fun fromMap_rejects_aLinkThatIsNotAString() {
    assertInvalid(
        missingOrInvalid("links"),
        full.toMap() + ("links" to listOf("https://example.com", 3)),
    )
  }

  /** Red if an unknown priority is accepted or fails without a clear message and its cause. */
  @Test
  fun fromMap_rejects_aBadPriority() {
    // Priorities are stored by their exact enum name, so another case is unknown too.
    listOf("LOW", "urgent").forEach { priority ->
      val exception =
          assertInvalid(
              "Invalid announcement data: bad priority '$priority'.",
              full.toMap() + ("priority" to priority),
          )
      assertTrue(exception.cause is IllegalArgumentException)
    }
  }

  /** Red if an unreadable date is accepted or fails without a clear message and its cause. */
  @Test
  fun fromMap_rejects_aBadUpdatedAt() {
    val exception =
        assertInvalid(
            "Invalid announcement data: bad updatedAt 'yesterday'.",
            full.toMap() + ("updatedAt" to "yesterday"),
        )

    assertTrue(exception.cause is DateTimeParseException)
  }

  /** Red if stored values that break the rules of an announcement are accepted when read. */
  @Test
  fun fromMap_rejects_valuesThatBreakTheRules() {
    assertInvalid("The title cannot be empty.", full.toMap() + ("title" to " "))
    assertInvalid(
        "A link is duplicated.",
        full.toMap() + ("links" to listOf("https://example.com", "https://example.com")),
    )
  }
}
