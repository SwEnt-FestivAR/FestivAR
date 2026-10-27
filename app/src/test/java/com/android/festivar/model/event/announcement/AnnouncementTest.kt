// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event.announcement

import com.android.festivar.model.task.Priority
import java.time.ZoneId
import java.time.ZonedDateTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AnnouncementTest {

  // Messages of the `init` block of Announcement, in the order in which the rules are checked.
  private val announcementIdMessage = "The announcementId cannot be empty."
  private val titleMessage = "The title cannot be empty."
  private val authorIdMessage = "The authorId cannot be empty."
  private val endTimeMessage = "endTime must not be before or equal to createdAt."
  private val blankLinkMessage = "A link cannot be empty."
  private val duplicateLinkMessage = "A link is duplicated."

  // Messages of the functions of Announcement.
  private val newEndTimeMessage = "newEndTime must not be before createdAt or equal to it."
  private val linkAlreadyExistsMessage = "The link already exists."
  private val linkNotPresentMessage = "This link is not present in this Announcement."

  private val createdAt = ZonedDateTime.of(2026, 7, 1, 14, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private val link1 = "https://example.com/1"
  private val link2 = "https://example.com/2"
  private val link3 = "https://example.com/3"

  /** Builds an announcement with valid default values, so that only the given fields matter. */
  private fun announcement(
      announcementId: String = "a1",
      title: String = "Announcement",
      authorId: String = "author-1",
      endTime: ZonedDateTime? = null,
      links: List<String> = emptyList(),
  ): Announcement =
      Announcement(
          announcementId = announcementId,
          title = title,
          authorId = authorId,
          createdAt = createdAt,
          endTime = endTime,
          links = links,
      )

  /** Builds a valid announcement with every field set, to check that the functions keep them. */
  private fun announcementWith(
      endTime: ZonedDateTime? = createdAt.plusDays(1),
      links: List<String> = listOf(link1, link2),
  ): Announcement =
      Announcement(
          announcementId = "a1",
          title = "Announcement",
          authorId = "author-1",
          description = "Description",
          priority = Priority.HIGH,
          createdAt = createdAt,
          endTime = endTime,
          links = links,
      )

  /** Checks that [block] throws an [IllegalArgumentException] with [expectedMessage]. */
  private fun assertInvalid(expectedMessage: String, block: () -> Unit) {
    val exception = assertThrows(IllegalArgumentException::class.java) { block() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------- //
  // init rules
  // ---------------------------------------------------------------------------------------- //

  /** An announcement with only the required fields is valid and gets the default values. */
  @Test
  fun init_succeeds_withOnlyRequiredFields() {
    val announcement =
        Announcement(
            announcementId = "a1",
            title = "Announcement",
            authorId = "author-1",
            createdAt = createdAt,
        )

    assertEquals("", announcement.description)
    assertEquals(Priority.NONE, announcement.priority)
    assertNull(announcement.endTime)
    assertTrue(announcement.links.isEmpty())
    assertFalse(announcement.pinned)
  }

  /** A blank announcementId is refused. */
  @Test
  fun init_fails_whenAnnouncementIdIsBlank() {
    assertInvalid(announcementIdMessage) { announcement(announcementId = "") }
    assertInvalid(announcementIdMessage) { announcement(announcementId = " \t\n") }
  }

  /** A blank title is refused. */
  @Test
  fun init_fails_whenTitleIsBlank() {
    assertInvalid(titleMessage) { announcement(title = "") }
    assertInvalid(titleMessage) { announcement(title = " \t\n") }
  }

  /** A blank authorId is refused. */
  @Test
  fun init_fails_whenAuthorIdIsBlank() {
    assertInvalid(authorIdMessage) { announcement(authorId = "") }
    assertInvalid(authorIdMessage) { announcement(authorId = " \t\n") }
  }

  /** An endTime strictly after createdAt is accepted, even by a single nanosecond. */
  @Test
  fun init_succeeds_whenEndTimeIsAfterCreatedAt() {
    assertEquals(createdAt.plusNanos(1), announcement(endTime = createdAt.plusNanos(1)).endTime)
  }

  /** An endTime equal to or before createdAt is refused, even in another time zone. */
  @Test
  fun init_fails_whenEndTimeIsNotAfterCreatedAt() {
    val sameInstantInLondon = createdAt.withZoneSameInstant(ZoneId.of("Europe/London"))

    assertInvalid(endTimeMessage) { announcement(endTime = createdAt) }
    assertInvalid(endTimeMessage) { announcement(endTime = sameInstantInLondon) }
    assertInvalid(endTimeMessage) { announcement(endTime = createdAt.minusNanos(1)) }
  }

  /** A blank link is refused. */
  @Test
  fun init_fails_whenALinkIsBlank() {
    assertInvalid(blankLinkMessage) { announcement(links = listOf(link1, " ")) }
  }

  /** A link present twice is refused, even when the duplicates are not next to each other. */
  @Test
  fun init_fails_whenALinkIsDuplicated() {
    assertInvalid(duplicateLinkMessage) { announcement(links = listOf(link1, link2, link1)) }
  }

  // ---------------------------------------------------------------------------------------- //
  // editTitle, editDescription, editEndTime and editLinks
  // ---------------------------------------------------------------------------------------- //

  /** editTitle replaces only the title and does not change the original. */
  @Test
  fun editTitle_replacesOnlyTheTitle() {
    val announcement = announcementWith()

    assertEquals(announcement.copy(title = "New title"), announcement.editTitle("New title"))
    assertEquals("Announcement", announcement.title)
  }

  /** editTitle refuses a blank title. */
  @Test
  fun editTitle_fails_whenTitleIsBlank() {
    assertInvalid(titleMessage) { announcementWith().editTitle(" ") }
  }

  /** editDescription replaces only the description. */
  @Test
  fun editDescription_replacesOnlyTheDescription() {
    val announcement = announcementWith()

    assertEquals(announcement.copy(description = "New"), announcement.editDescription("New"))
  }

  /** editEndTime replaces only the endTime, by a later time or by null. */
  @Test
  fun editEndTime_replacesOnlyTheEndTime() {
    val announcement = announcementWith()

    assertEquals(announcement.copy(endTime = null), announcement.editEndTime(null))
    assertEquals(
        announcement.copy(endTime = createdAt.plusNanos(1)),
        announcement.editEndTime(createdAt.plusNanos(1)),
    )
  }

  /** editEndTime refuses an endTime equal to or before createdAt. */
  @Test
  fun editEndTime_fails_whenEndTimeIsNotAfterCreatedAt() {
    val announcement = announcementWith()

    assertInvalid(newEndTimeMessage) { announcement.editEndTime(createdAt) }
    assertInvalid(newEndTimeMessage) { announcement.editEndTime(createdAt.minusNanos(1)) }
  }

  /** editLinks replaces only the links. */
  @Test
  fun editLinks_replacesOnlyTheLinks() {
    val announcement = announcementWith()

    assertEquals(announcement.copy(links = listOf(link3)), announcement.editLinks(listOf(link3)))
  }

  /** editLinks refuses a list with a blank or a duplicated link. */
  @Test
  fun editLinks_fails_whenALinkIsBlankOrDuplicated() {
    val announcement = announcementWith()

    assertInvalid(blankLinkMessage) { announcement.editLinks(listOf(link1, "")) }
    assertInvalid(duplicateLinkMessage) { announcement.editLinks(listOf(link1, link1)) }
  }

  // ---------------------------------------------------------------------------------------- //
  // addLink and removeLink
  // ---------------------------------------------------------------------------------------- //

  /** addLink adds the link at the end, keeps every other field and the original. */
  @Test
  fun addLink_addsLinkAtTheEnd() {
    val announcement = announcementWith(links = listOf(link1, link2))

    val result = announcement.addLink(link3)

    assertEquals(announcement.copy(links = listOf(link1, link2, link3)), result)
    assertEquals(listOf(link1, link2), announcement.links)
  }

  /** addLink refuses a blank link. */
  @Test
  fun addLink_fails_whenLinkIsBlank() {
    assertInvalid(blankLinkMessage) { announcementWith().addLink(" ") }
  }

  /** addLink refuses a link that is already in the announcement. */
  @Test
  fun addLink_fails_whenLinkAlreadyExists() {
    assertInvalid(linkAlreadyExistsMessage) {
      announcementWith(links = listOf(link1)).addLink(link1)
    }
  }

  /** removeLink removes only the given link, keeps the order of the others and the original. */
  @Test
  fun removeLink_removesOnlyTheGivenLink() {
    val announcement = announcementWith(links = listOf(link1, link2, link3))

    val result = announcement.removeLink(link2)

    assertEquals(announcement.copy(links = listOf(link1, link3)), result)
    assertEquals(listOf(link1, link2, link3), announcement.links)
  }

  /** removeLink refuses a link that is not in the announcement. */
  @Test
  fun removeLink_fails_whenLinkIsNotPresent() {
    assertInvalid(linkNotPresentMessage) {
      announcementWith(links = listOf(link1)).removeLink(link2)
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // isObsolete and togglePin
  // ---------------------------------------------------------------------------------------- //

  /** An announcement without endTime is never obsolete. */
  @Test
  fun isObsolete_isFalse_whenThereIsNoEndTime() {
    assertFalse(announcementWith(endTime = null).isObsolete(createdAt.plusYears(100)))
  }

  /** An announcement is obsolete from its endTime on, including exactly at its endTime. */
  @Test
  fun isObsolete_isTrue_onlyFromTheEndTime() {
    val end = createdAt.plusDays(1)
    val announcement = announcementWith(endTime = end)

    assertFalse(announcement.isObsolete(end.minusNanos(1)))
    assertTrue(announcement.isObsolete(end))
  }

  /** togglePin pins then unpins, keeps every other field and does not change the original. */
  @Test
  fun togglePin_pinsThenUnpins() {
    val announcement = announcementWith()

    val pinned = announcement.togglePin()

    assertEquals(announcement.copy(pinned = true), pinned)
    assertEquals(announcement, pinned.togglePin())
    assertFalse(announcement.pinned)
  }
}
