// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event.announcement

import java.time.ZoneId
import java.time.ZonedDateTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AnnouncementTest {

  // Messages of the `init` block of Announcement, in the order in which the rules are checked.
  private val announcementIdMessage = "The announcementId cannot be empty."
  private val titleMessage = "The title cannot be empty."
  private val authorIdMessage = "The authorId cannot be empty."
  private val blankLinkMessage = "A link cannot be empty."
  private val duplicateLinkMessage = "A link is duplicated."

  // Messages of the functions of Announcement.
  private val editTimeMessage = "The modification cannot be before the previous one."
  private val linkAlreadyExistsMessage = "The link already exists."
  private val linkNotPresentMessage = "This link is not present in this Announcement."

  private val updatedAt = ZonedDateTime.of(2026, 7, 1, 14, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  /** A valid time for a modification, after [updatedAt]. */
  private val later = updatedAt.plusHours(1)

  /** An invalid time for a modification, just before [updatedAt]. */
  private val tooEarly = updatedAt.minusNanos(1)

  private val link1 = "https://example.com/1"
  private val link2 = "https://example.com/2"
  private val link3 = "https://example.com/3"

  /** Builds an announcement with valid default values, so that only the given fields matter. */
  private fun announcement(
      announcementId: String = "a1",
      title: String = "Announcement",
      authorId: String = "author-1",
      links: List<String> = emptyList(),
  ): Announcement =
      Announcement(
          announcementId = announcementId,
          title = title,
          authorId = authorId,
          updatedAt = updatedAt,
          links = links,
      )

  /** Builds a valid announcement with every field set, to check that the functions keep them. */
  private fun announcementWith(links: List<String> = listOf(link1, link2)): Announcement =
      Announcement(
          announcementId = "a1",
          title = "Announcement",
          authorId = "author-1",
          content = "Content",
          priority = AnnouncementPriority.URGENT,
          updatedAt = updatedAt,
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
  fun anAnnouncementWithOnlyTheRequiredFieldsGetsTheDefaultValues() {
    val announcement =
        Announcement(
            announcementId = "a1",
            title = "Announcement",
            authorId = "author-1",
            updatedAt = updatedAt,
        )

    assertEquals("", announcement.content)
    assertEquals(AnnouncementPriority.INFO, announcement.priority)
    assertTrue(announcement.links.isEmpty())
    assertFalse(announcement.pinned)
  }

  /** A blank announcementId is refused. */
  @Test
  fun aBlankAnnouncementIdIsRefused() {
    assertInvalid(announcementIdMessage) { announcement(announcementId = "") }
    assertInvalid(announcementIdMessage) { announcement(announcementId = " \t\n") }
  }

  /** A blank title is refused. */
  @Test
  fun aBlankTitleIsRefused() {
    assertInvalid(titleMessage) { announcement(title = "") }
    assertInvalid(titleMessage) { announcement(title = " \t\n") }
  }

  /** A blank authorId is refused. */
  @Test
  fun aBlankAuthorIdIsRefused() {
    assertInvalid(authorIdMessage) { announcement(authorId = "") }
    assertInvalid(authorIdMessage) { announcement(authorId = " \t\n") }
  }

  /** A blank link is refused. */
  @Test
  fun aBlankLinkIsRefused() {
    assertInvalid(blankLinkMessage) { announcement(links = listOf(link1, " ")) }
  }

  /** A link present twice is refused, even when the duplicates are not next to each other. */
  @Test
  fun aLinkPresentTwiceIsRefused() {
    assertInvalid(duplicateLinkMessage) { announcement(links = listOf(link1, link2, link1)) }
  }

  // ---------------------------------------------------------------------------------------- //
  // Time of the modification, the same rule for every edit function
  // ---------------------------------------------------------------------------------------- //

  /** The six edit functions, called on [announcementWith] with valid values at a given time. */
  private val edits: List<(ZonedDateTime) -> Announcement> =
      listOf(
          { at -> announcementWith().editTitle("New title", at) },
          { at -> announcementWith().editContent("New", at) },
          { at -> announcementWith().editPriority(AnnouncementPriority.INFO, at) },
          { at -> announcementWith().addLink(link3, at) },
          { at -> announcementWith().removeLink(link1, at) },
          { at -> announcementWith().editLinks(listOf(link3), at) },
      )

  /** Every edit function refuses a time before updatedAt, even by a single nanosecond. */
  @Test
  fun everyEditFunctionRefusesATimeBeforeUpdatedAt() {
    for (edit in edits) {
      assertInvalid(editTimeMessage) { edit(tooEarly) }
    }
  }

  /**
   * Every edit function accepts a modification at the same instant as updatedAt, even in another
   * time zone.
   */
  @Test
  fun everyEditFunctionAcceptsTheSameInstantAsUpdatedAt() {
    val sameInstantInLondon = updatedAt.withZoneSameInstant(ZoneId.of("Europe/London"))

    for (edit in edits) {
      assertEquals(updatedAt, edit(updatedAt).updatedAt)
      assertEquals(sameInstantInLondon, edit(sameInstantInLondon).updatedAt)
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // editTitle, editContent, editPriority and editLinks
  // ---------------------------------------------------------------------------------------- //

  /** editTitle replaces only the title and updatedAt, and does not change the original. */
  @Test
  fun editTitleReplacesOnlyTheTitleAndUpdatedAt() {
    val announcement = announcementWith()

    val result = announcement.editTitle("New title", later)

    assertEquals(announcement.copy(title = "New title", updatedAt = later), result)
    assertEquals("Announcement", announcement.title)
    assertEquals(updatedAt, announcement.updatedAt)
  }

  /** editTitle refuses a blank title. */
  @Test
  fun editTitleRefusesABlankTitle() {
    assertInvalid(titleMessage) { announcementWith().editTitle(" ", later) }
  }

  /** editContent replaces only the content and updatedAt, even by an empty one. */
  @Test
  fun editContentReplacesOnlyTheContentAndUpdatedAt() {
    val announcement = announcementWith()

    assertEquals(
        announcement.copy(content = "", updatedAt = later),
        announcement.editContent("", later),
    )
  }

  /** editPriority replaces only the priority and updatedAt, for every priority. */
  @Test
  fun editPriorityReplacesOnlyThePriorityAndUpdatedAt() {
    val announcement = announcementWith()

    for (priority in AnnouncementPriority.entries) {
      assertEquals(
          announcement.copy(priority = priority, updatedAt = later),
          announcement.editPriority(priority, later),
      )
    }
    assertEquals(AnnouncementPriority.URGENT, announcement.priority)
  }

  /** editLinks replaces only the links and updatedAt, even by an empty list. */
  @Test
  fun editLinksReplacesOnlyTheLinksAndUpdatedAt() {
    val announcement = announcementWith()

    assertEquals(
        announcement.copy(links = listOf(link3), updatedAt = later),
        announcement.editLinks(listOf(link3), later),
    )
    assertTrue(announcement.editLinks(emptyList(), later).links.isEmpty())
  }

  /** editLinks refuses a list with a blank or a duplicated link. */
  @Test
  fun editLinksRefusesABlankOrDuplicatedLink() {
    val announcement = announcementWith()

    assertInvalid(blankLinkMessage) { announcement.editLinks(listOf(link1, ""), later) }
    assertInvalid(duplicateLinkMessage) { announcement.editLinks(listOf(link1, link1), later) }
  }

  // ---------------------------------------------------------------------------------------- //
  // addLink and removeLink
  // ---------------------------------------------------------------------------------------- //

  /** addLink adds the link at the end, updates updatedAt and does not change the original. */
  @Test
  fun addLinkAddsTheLinkAtTheEnd() {
    val announcement = announcementWith(links = listOf(link1, link2))

    val result = announcement.addLink(link3, later)

    assertEquals(announcement.copy(links = listOf(link1, link2, link3), updatedAt = later), result)
    assertEquals(listOf(link1, link2), announcement.links)
  }

  /** addLink works on an announcement without any link. */
  @Test
  fun addLinkWorksOnAnAnnouncementWithoutAnyLink() {
    assertEquals(listOf(link1), announcementWith(links = emptyList()).addLink(link1, later).links)
  }

  /** addLink refuses a blank link. */
  @Test
  fun addLinkRefusesABlankLink() {
    assertInvalid(blankLinkMessage) { announcementWith().addLink(" ", later) }
  }

  /** addLink refuses a link that is already in the announcement. */
  @Test
  fun addLinkRefusesALinkAlreadyInTheAnnouncement() {
    assertInvalid(linkAlreadyExistsMessage) {
      announcementWith(links = listOf(link1)).addLink(link1, later)
    }
  }

  /** removeLink removes only the given link, keeps the order of the others and the original. */
  @Test
  fun removeLinkRemovesOnlyTheGivenLink() {
    val announcement = announcementWith(links = listOf(link1, link2, link3))

    val result = announcement.removeLink(link2, later)

    assertEquals(announcement.copy(links = listOf(link1, link3), updatedAt = later), result)
    assertEquals(listOf(link1, link2, link3), announcement.links)
  }

  /** removeLink of the last link gives an announcement without any link. */
  @Test
  fun removingTheLastLinkGivesAnAnnouncementWithoutAnyLink() {
    assertTrue(announcementWith(links = listOf(link1)).removeLink(link1, later).links.isEmpty())
  }

  /** removeLink refuses a link that is not in the announcement. */
  @Test
  fun removeLinkRefusesALinkNotInTheAnnouncement() {
    assertInvalid(linkNotPresentMessage) {
      announcementWith(links = listOf(link1)).removeLink(link2, later)
    }
  }

  // ---------------------------------------------------------------------------------------- //
  // togglePin
  // ---------------------------------------------------------------------------------------- //

  /** togglePin pins then unpins and keeps every other field, updatedAt included. */
  @Test
  fun togglePinPinsThenUnpinsAndKeepsUpdatedAt() {
    val announcement = announcementWith()

    val pinned = announcement.togglePin()

    assertEquals(announcement.copy(pinned = true), pinned)
    assertEquals(announcement, pinned.togglePin())
    assertFalse(announcement.pinned)
  }
}
