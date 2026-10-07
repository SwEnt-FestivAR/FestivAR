// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.temporary.User
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
}
