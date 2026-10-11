// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.user.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventRepositoryFirebaseTest {
  private lateinit var repository: EventRepositoryFirebase
  private lateinit var event: Event

  private val start = ZonedDateTime.parse("2026-10-17T10:00:00+02:00")

  @Before
  fun setUp() {
    Firebase.auth.useEmulator("10.0.2.2", 9099)
    runBlocking { Firebase.auth.signInAnonymously().await() }
    event =
        Event(
            eventId = "event-${UUID.randomUUID()}",
            title = "Fête de l'Asso",
            description = "Description",
            members = listOf(User("user-1")),
            startDate = start,
            endDate = start.plusHours(5),
            location = "EPFL",
            closed = false,
        )
    repository = EventRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      if (::event.isInitialized) {
        Firebase.firestore.collection("events").document(event.eventId).delete().await()
      }
      Firebase.auth.signOut()
    }
  }

  @Test
  fun getNewUid_returnsNonEmptyUniqueIds() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    assertTrue(first.isNotEmpty())
    assertNotEquals(first, second)
  }

  @Test
  fun toFirestoreData_serializesEventFields() {
    val data = invokeToFirestoreData(event)

    assertEquals(event.eventId, data["eventId"])
    assertEquals(event.title, data["title"])
    assertEquals(event.description, data["description"])
    assertEquals(event.members.map { it.uid }, data["members"])
    assertEquals(event.startDate.toString(), data["startDate"])
    assertEquals(event.endDate.toString(), data["endDate"])
    assertEquals(event.location, data["location"])
    assertEquals(emptyList<String>(), data["tasks"])
    assertEquals(event.closed, data["closed"])
  }

  @Test
  fun addEvent_thenGetEventAndGetAllEventsReturnIt() = runBlocking {
    repository.addEvent(event)

    assertEquals(event, repository.getEvent(event.eventId))
    assertEquals(listOf(event), repository.getAllEvents())
  }

  @Test
  fun addEvent_withExistingIdThrows() = runBlocking {
    repository.addEvent(event)

    assertThrows { repository.addEvent(event.copy(title = "Other")) }
  }

  @Test
  fun getEvent_unknownIdThrows() = runBlocking {
    assertThrows { repository.getEvent(event.eventId) }
  }

  @Test
  fun getEventsForUser_returnsOnlyMatchingEvents() = runBlocking {
    val otherEvent = event.copy(eventId = "event-${UUID.randomUUID()}", members = emptyList())
    repository.addEvent(event)
    repository.addEvent(otherEvent)

    try {
      assertEquals(listOf(event), repository.getEventsForUser("user-1"))
      assertTrue(repository.getEventsForUser("unknown").isEmpty())
    } finally {
      Firebase.firestore.collection("events").document(otherEvent.eventId).delete().await()
    }
  }

  @Test
  fun editEvent_updatesExistingEvent() = runBlocking {
    repository.addEvent(event)
    val updatedEvent = event.copy(title = "Updated event", closed = true)

    repository.editEvent(event.eventId, updatedEvent)

    assertEquals(updatedEvent, repository.getEvent(event.eventId))
  }

  @Test
  fun editEvent_withDifferentIdThrows() = runBlocking {
    assertThrows { repository.editEvent(event.eventId, event.copy(eventId = "other-event")) }
  }

  @Test
  fun editEvent_unknownIdThrows() = runBlocking {
    assertThrows { repository.editEvent(event.eventId, event) }
  }

  @Test
  fun deleteEvent_removesEvent() = runBlocking {
    repository.addEvent(event)

    repository.deleteEvent(event.eventId)

    assertThrows { repository.getEvent(event.eventId) }
  }

  @Test
  fun deleteEvent_unknownIdThrows() = runBlocking {
    assertThrows { repository.deleteEvent(event.eventId) }
  }

  @Test
  fun getEvent_deserializesMembersStoredAsMaps() = runBlocking {
    val document = Firebase.firestore.collection("events").document(event.eventId)
    document
        .set(
            mapOf(
                "eventId" to event.eventId,
                "title" to event.title,
                "members" to listOf(mapOf("uid" to "user-2", "name" to "Alex", "surname" to "Lee")),
                "startDate" to event.startDate.toString(),
                "endDate" to event.endDate.toString(),
            )
        )
        .await()

    assertEquals(listOf(User("user-2", "Alex", "Lee")), repository.getEvent(event.eventId).members)
  }

  @Test
  fun getEvent_usesDefaultsForMissingOptionalFields() = runBlocking {
    val document = Firebase.firestore.collection("events").document(event.eventId)
    document
        .set(
            mapOf(
                "title" to event.title,
                "startDate" to event.startDate.toString(),
                "endDate" to event.endDate.toString(),
            )
        )
        .await()

    val result = repository.getEvent(event.eventId)

    assertEquals(event.eventId, result.eventId)
    assertEquals("", result.description)
    assertEquals("", result.location)
    assertTrue(result.members.isEmpty())
    assertTrue(result.tasks.isEmpty())
    assertFalse(result.closed)
  }

  private suspend fun assertThrows(block: suspend () -> Unit) {
    var thrown = false
    try {
      block()
    } catch (_: Exception) {
      thrown = true
    }
    assertTrue("Expected an exception", thrown)
  }

  @Suppress("UNCHECKED_CAST")
  private fun invokeToFirestoreData(event: Event): Map<String, Any?> {
    val method =
        EventRepositoryFirebase::class.java.getDeclaredMethod("toFirestoreData", Event::class.java)
    method.isAccessible = true
    return method.invoke(repository, event) as Map<String, Any?>
  }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      Firebase.firestore.useEmulator("10.0.2.2", 8080)
    }
  }
}
