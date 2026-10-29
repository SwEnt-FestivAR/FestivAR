// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.user.User
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventsRepositoryLocalTest {
  private lateinit var repository: EventsRepositoryLocal

  private val start = ZonedDateTime.parse("2026-10-17T10:00:00+02:00")
  private val sara = User("u1")

  private val event =
      Event(
          eventId = "1",
          title = "Fête de l'Asso",
          members = listOf(sara),
          startDate = start,
          endDate = start.plusHours(13),
      )
  private val otherEvent =
      Event(eventId = "2", title = "Soirée Jazz", startDate = start, endDate = start.plusHours(5))
  private val announcement =
      Announcement(announcementId = "a1", title = "Gates open", authorId = "u1", updatedAt = start)
  private val eventWithAnnouncement = event.copy(announcements = listOf(announcement))

  @Before
  fun setUp() {
    repository = EventsRepositoryLocal()
  }

  @Test
  fun getNewUid_generatesDifferentNonEmptyIdentifiers() {
    val uid = repository.getNewUid()
    val otherUid = repository.getNewUid()
    assertTrue(uid.isNotEmpty())
    assertTrue(otherUid.isNotEmpty())
    assertNotEquals(uid, otherUid)
  }

  @Test fun startsEmptyByDefault() = runTest { assertTrue(repository.getAllEvents().isEmpty()) }

  @Test
  fun startsWithInitialEvents() = runTest {
    repository = EventsRepositoryLocal(listOf(event, otherEvent))
    assertEquals(listOf(event, otherEvent), repository.getAllEvents())
  }

  @Test
  fun initialEventsWithDuplicatedIdThrows() {
    assertThrows(IllegalArgumentException::class.java) {
      EventsRepositoryLocal(listOf(event, otherEvent.copy(eventId = event.eventId)))
    }
  }

  @Test
  fun addEvent_thenGetEventAndGetAllEventsReturnIt() = runTest {
    repository.addEvent(event)
    assertEquals(event, repository.getEvent(event.eventId))
    assertEquals(listOf(event), repository.getAllEvents())
  }

  @Test
  fun addEvent_withExistingIdThrows() = runTest {
    repository.addEvent(event)
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.addEvent(event.copy(title = "Other")) }
    }
  }

  @Test
  fun getAllEvents_returnsACopy() = runTest {
    repository.addEvent(event)
    val events = repository.getAllEvents()
    repository.addEvent(otherEvent)
    assertEquals(listOf(event), events)
  }

  @Test
  fun getEvent_findsTheEventAmongOthers() = runTest {
    repository = EventsRepositoryLocal(listOf(event, otherEvent))
    assertEquals(otherEvent, repository.getEvent(otherEvent.eventId))
  }

  @Test
  fun getEvent_unknownIdThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getEvent("unknown") }
    }
  }

  @Test
  fun getEventsForUser_returnsOnlyEventsTheUserIsAMemberOf() = runTest {
    repository = EventsRepositoryLocal(listOf(event, otherEvent))
    assertEquals(listOf(event), repository.getEventsForUser(sara.uid))
    assertTrue(repository.getEventsForUser("unknown").isEmpty())
  }

  /** Red if the lookup compares the raw id or matches on a prefix: "fete2026" has code FETE20. */
  @Test
  fun getEventsByJoinCode_returnsEveryEventWithThatCodeAndNothingElse() = runTest {
    val fete = event.copy(eventId = "fete2026")
    val feteBis = otherEvent.copy(eventId = "fete-2026-bis")
    val noel = otherEvent.copy(eventId = "noel-12")
    repository = EventsRepositoryLocal(listOf(fete, feteBis, noel))

    assertEquals(listOf(fete, feteBis), repository.getEventsByJoinCode("FETE20"))
    assertTrue(repository.getEventsByJoinCode("NOEL1").isEmpty())
    assertTrue(repository.getEventsByJoinCode("ZZZZZZ").isEmpty())
  }

  @Test
  fun editEvent_replacesTheEvent() = runTest {
    repository.addEvent(event)
    val edited = event.copy(title = "Fête de l'Asso 2026")
    repository.editEvent(event.eventId, edited)
    assertEquals(edited, repository.getEvent(event.eventId))
    assertEquals(1, repository.getAllEvents().size)
  }

  @Test
  fun editEvent_withDifferentIdThrows() = runTest {
    repository.addEvent(event)
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.editEvent(event.eventId, otherEvent) }
    }
  }

  @Test
  fun editEvent_unknownIdThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.editEvent(event.eventId, event) }
    }
  }

  @Test
  fun deleteEvent_removesTheEvent() = runTest {
    repository = EventsRepositoryLocal(listOf(event, otherEvent))
    repository.deleteEvent(event.eventId)
    assertEquals(listOf(otherEvent), repository.getAllEvents())
  }

  @Test
  fun deleteEvent_unknownIdThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteEvent("unknown") }
    }
  }

  @Test
  fun addAnnouncement_addsItToTheEventOnly() = runTest {
    repository = EventsRepositoryLocal(listOf(event, otherEvent))
    repository.addAnnouncement(event.eventId, announcement)
    assertEquals(eventWithAnnouncement, repository.getEvent(event.eventId))
    assertEquals(otherEvent, repository.getEvent(otherEvent.eventId))
  }

  @Test
  fun addAnnouncement_withExistingIdThrowsAndKeepsTheEvent() = runTest {
    repository = EventsRepositoryLocal(listOf(eventWithAnnouncement))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking {
        repository.addAnnouncement(
            event.eventId,
            announcement.editTitle("Gates open later", at = start.plusMinutes(1)),
        )
      }
    }
    assertEquals(eventWithAnnouncement, repository.getEvent(event.eventId))
  }

  @Test
  fun addAnnouncement_toFullEventThrowsAndKeepsTheEvent() = runTest {
    val fullEvent =
        event.copy(
            announcements =
                (1..MAX_ANNOUNCEMENTS_PER_EVENT).map { i ->
                  announcement.copy(announcementId = "a$i")
                }
        )
    repository = EventsRepositoryLocal(listOf(fullEvent))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking {
        repository.addAnnouncement(
            event.eventId,
            announcement.copy(announcementId = "a${MAX_ANNOUNCEMENTS_PER_EVENT + 1}"),
        )
      }
    }
    assertEquals(fullEvent, repository.getEvent(event.eventId))
  }

  @Test
  fun addAnnouncement_unknownEventThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.addAnnouncement("unknown", announcement) }
    }
  }

  @Test
  fun editAnnouncement_replacesTheAnnouncement() = runTest {
    val other = announcement.copy(announcementId = "a2", title = "Bar opens")
    repository =
        EventsRepositoryLocal(listOf(event.copy(announcements = listOf(announcement, other))))
    val edited = announcement.editTitle("Gates open later", at = start.plusMinutes(1)).togglePin()
    repository.editAnnouncement(event.eventId, edited)
    assertEquals(listOf(edited, other), repository.getEvent(event.eventId).announcements)
  }

  @Test
  fun editAnnouncement_missingAnnouncementThrowsAndKeepsTheEvent() = runTest {
    repository = EventsRepositoryLocal(listOf(eventWithAnnouncement))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking {
        repository.editAnnouncement(event.eventId, announcement.copy(announcementId = "a2"))
      }
    }
    assertEquals(eventWithAnnouncement, repository.getEvent(event.eventId))
  }

  @Test
  fun editAnnouncement_unknownEventThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.editAnnouncement("unknown", announcement) }
    }
  }

  @Test
  fun deleteAnnouncement_removesTheAnnouncement() = runTest {
    repository = EventsRepositoryLocal(listOf(eventWithAnnouncement))
    repository.deleteAnnouncement(event.eventId, announcement.announcementId)
    assertEquals(event, repository.getEvent(event.eventId))
  }

  @Test
  fun deleteAnnouncement_missingAnnouncementThrowsAndKeepsTheEvent() = runTest {
    repository = EventsRepositoryLocal(listOf(eventWithAnnouncement))
    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.deleteAnnouncement(event.eventId, "unknown") }
    }
    assertEquals(eventWithAnnouncement, repository.getEvent(event.eventId))
  }

  @Test
  fun deleteAnnouncement_unknownEventThrows() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteAnnouncement("unknown", announcement.announcementId) }
    }
  }
}
