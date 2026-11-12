// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    FirebaseEmulatorSetup.configure()
    runBlocking {
      Firebase.auth.signInAnonymously().await()
      val userRepo = UsersRepositoryFirebase(Firebase.firestore)
      userRepo.addUser(User("user-1"))
      userRepo.addUser(User("user-2", "Alex", "Lee"))
    }
    event =
        Event(
            eventId = "event-${UUID.randomUUID()}",
            ownerId = "user-1",
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
      val userRepo = UsersRepositoryFirebase(Firebase.firestore)
      runCatching { userRepo.deleteUser("user-1") }
      runCatching { userRepo.deleteUser("user-2") }
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
    assertEquals(joinCodeOf(event), data["joinCode"])
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

  /**
   * Breaks if an event without `ownerId` is accepted or the exception does not identify the event.
   */
  @Test
  fun getEvent_withoutOwnerIdIsRefused() = runBlocking {
    eventDocument()
        .set(
            mapOf(
                "eventId" to event.eventId,
                "title" to event.title,
                "startDate" to event.startDate.toString(),
                "endDate" to event.endDate.toString(),
            )
        )
        .await()

    assertThrowsOfType<IllegalArgumentException>("Event '${event.eventId}' has no owner") {
      repository.getEvent(event.eventId)
    }
  }

  @Test
  fun getEventsForUser_returnsOnlyMatchingEvents() = runBlocking {
    val otherEvent =
        event.copy(
            eventId = "event-${UUID.randomUUID()}",
            ownerId = "other-owner",
            members = listOf(User("other-owner")),
        )
    repository.addEvent(event)
    repository.addEvent(otherEvent)

    try {
      assertEquals(listOf(event), repository.getEventsForUser("user-1"))
      assertTrue(repository.getEventsForUser("unknown").isEmpty())
    } finally {
      Firebase.firestore.collection("events").document(otherEvent.eventId).delete().await()
    }
  }

  /**
   * Red if the document lacks the `joinCode` field or the query reads another one. The looked up
   * event has a bare UUID id, so its code is six random hex digits and a document another test left
   * in the emulator cannot share it; the class fixture's `event-` ids have only sixteen codes.
   */
  @Test
  fun getEventsByJoinCode_returnsOnlyTheEventsWithThatCode() = runBlocking {
    val coded = event.copy(eventId = UUID.randomUUID().toString())
    repository.addEvent(coded)
    repository.addEvent(event)

    try {
      assertEquals(listOf(coded), repository.getEventsByJoinCode(joinCodeOf(coded)))
      assertTrue(repository.getEventsByJoinCode("ZZZZZZ").isEmpty())
    } finally {
      Firebase.firestore.collection("events").document(coded.eventId).delete().await()
    }
  }

  /** Red if one malformed document carrying the code makes the whole lookup fail. */
  @Test
  fun getEventsByJoinCode_skipsAMalformedDocumentAndReturnsTheGoodEventAlone() = runBlocking {
    val coded = event.copy(eventId = UUID.randomUUID().toString())
    repository.addEvent(coded)
    val events = Firebase.firestore.collection("events")
    val broken = events.document("${coded.eventId}-broken")
    // No title, so the mapper cannot build an event from it.
    broken.set(mapOf("eventId" to broken.id, "joinCode" to joinCodeOf(coded))).await()

    try {
      assertEquals(listOf(coded), repository.getEventsByJoinCode(joinCodeOf(coded)))
    } finally {
      events.document(coded.eventId).delete().await()
      broken.delete().await()
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
    assertThrowsOfType<NoSuchElementException> { repository.editEvent(event.eventId, event) }
  }

  @Test
  fun editEvent_doesNotCreateEventWhenEventDoesNotExist() = runBlocking {
    assertThrowsOfType<NoSuchElementException> { repository.editEvent(event.eventId, event) }

    assertThrowsOfType<NoSuchElementException> { repository.getEvent(event.eventId) }
  }

  @Test
  fun deleteEvent_removesEvent() = runBlocking {
    repository.addEvent(event)

    repository.deleteEvent(event.eventId)

    assertThrows { repository.getEvent(event.eventId) }
  }

  @Test
  fun deleteEvent_unknownIdThrows() = runBlocking {
    assertThrowsOfType<NoSuchElementException> { repository.deleteEvent(event.eventId) }
  }

  @Test
  fun deleteEvent_throwsWhenEventWasAlreadyDeleted() = runBlocking {
    repository.addEvent(event)
    repository.deleteEvent(event.eventId)

    assertThrowsOfType<NoSuchElementException> { repository.deleteEvent(event.eventId) }
  }

  @Test
  fun deleteEvent_concurrentDeletesOnlySucceedOnce() = runBlocking {
    repository.addEvent(event)

    val results =
        listOf(
                async { runCatching { repository.deleteEvent(event.eventId) } },
                async { runCatching { repository.deleteEvent(event.eventId) } },
            )
            .awaitAll()

    assertEquals(1, results.count { it.isSuccess })
    assertTrue(results.single { it.isFailure }.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun getEvent_deserializesMemberIdsToUsers() = runBlocking {
    val document = Firebase.firestore.collection("events").document(event.eventId)
    document
        .set(
            mapOf(
                "eventId" to event.eventId,
                "ownerId" to event.ownerId,
                "title" to event.title,
                "members" to listOf("user-1", "user-2"),
                "startDate" to event.startDate.toString(),
                "endDate" to event.endDate.toString(),
            )
        )
        .await()

    assertEquals(
        listOf(User("user-1"), User("user-2", "Alex", "Lee")),
        repository.getEvent(event.eventId).members,
    )
  }

  @Test
  fun getEvent_usesDefaultsForMissingOptionalFields() = runBlocking {
    val document = Firebase.firestore.collection("events").document(event.eventId)
    document
        .set(
            mapOf(
                "ownerId" to event.ownerId,
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
    assertEquals(listOf(User("user-1")), result.members)
    assertTrue(result.tasks.isEmpty())
    assertFalse(result.closed)
    assertTrue(result.announcements.isEmpty())

    // A null field reads as absent, while another type is malformed (see the test below).
    document.update("announcements", null).await()
    assertTrue(repository.getEvent(event.eventId).announcements.isEmpty())
  }

  // ----------------------------------- announcements -------------------------------------- //

  /** Every field away from its default, with a named zone, so a lossy mapping shows. */
  private val fullAnnouncement =
      Announcement(
          announcementId = "a-full",
          title = "Gates open",
          authorId = "author-1",
          content = "Main gate at 18:00.",
          priority = AnnouncementPriority.WARNING,
          updatedAt = ZonedDateTime.of(2026, 10, 17, 9, 30, 15, 0, ZoneId.of("Europe/Zurich")),
          links = listOf("https://example.com/map", "https://example.com/faq"),
          pinned = true,
      )

  private fun announcement(id: String, title: String = "Announcement $id") =
      Announcement(announcementId = id, title = title, authorId = "author-1", updatedAt = start)

  /** Red if an added announcement loses a field, comes back in another order or edits the event. */
  @Test
  fun addAnnouncement_storesItAfterTheExistingOnes_andKeepsTheEvent() = runBlocking {
    val first = announcement("a1")
    repository.addEvent(event.copy(announcements = listOf(first)))

    repository.addAnnouncement(event.eventId, fullAnnouncement)

    assertEquals(
        event.copy(announcements = listOf(first, fullAnnouncement)),
        repository.getEvent(event.eventId),
    )
  }

  /** Red if a used id replaces the stored announcement, or if a full event outgrows the cap. */
  @Test
  fun addAnnouncement_refusesAUsedIdAndAFullEvent_andAddsNothing() = runBlocking {
    val full = (1..MAX_ANNOUNCEMENTS_PER_EVENT).map { announcement("a$it") }
    repository.addEvent(event.copy(announcements = full))

    assertThrowsOfType<IllegalArgumentException>("This announcement is already in the event.") {
      repository.addAnnouncement(event.eventId, announcement("a1", title = "Other"))
    }
    assertThrowsOfType<IllegalArgumentException>(
        "An event cannot have more than $MAX_ANNOUNCEMENTS_PER_EVENT announcements."
    ) {
      repository.addAnnouncement(event.eventId, announcement("one-too-many"))
    }

    assertEquals(full, repository.getEvent(event.eventId).announcements)
  }

  /** Red if adds read and then overwrite the array outside a transaction, losing one of them. */
  @Test
  fun addAnnouncement_concurrentAddsKeepBoth() = runBlocking {
    repository.addEvent(event)
    val added = listOf(announcement("a1"), announcement("a2"))

    added.map { async { repository.addAnnouncement(event.eventId, it) } }.awaitAll()

    assertEquals(added.toSet(), repository.getEvent(event.eventId).announcements.toSet())
  }

  /** Red if an edited announcement moves, or if a delete removes or reorders the others. */
  @Test
  fun editAnnouncement_replacesItInPlace_andDeleteAnnouncementRemovesOnlyIt() = runBlocking {
    val (a1, a2, a3) = listOf(announcement("a1"), announcement("a2"), announcement("a3"))
    repository.addEvent(event.copy(announcements = listOf(a1, a2, a3)))
    val edited = fullAnnouncement.copy(announcementId = "a2")

    repository.editAnnouncement(event.eventId, edited)
    assertEquals(
        event.copy(announcements = listOf(a1, edited, a3)),
        repository.getEvent(event.eventId),
    )

    repository.deleteAnnouncement(event.eventId, "a2")
    assertEquals(event.copy(announcements = listOf(a1, a3)), repository.getEvent(event.eventId))
  }

  @Test
  fun setAnnouncementPinned_preservesTheLatestAnnouncementFields() = runBlocking {
    val original = fullAnnouncement.copy(pinned = false)
    repository.addEvent(event.copy(announcements = listOf(original)))
    val editedElsewhere = original.copy(title = "Updated elsewhere", content = "New details")
    repository.editAnnouncement(event.eventId, editedElsewhere)

    val pinned = repository.setAnnouncementPinned(event.eventId, original.announcementId, true)

    assertEquals(editedElsewhere.copy(pinned = true), pinned)
    assertEquals(listOf(pinned), repository.getEvent(event.eventId).announcements)
  }

  /** Red if editing or deleting an unknown id succeeds, adds it or changes the stored ones. */
  @Test
  fun editAndDeleteAnnouncement_withAnUnknownIdThrow_andChangeNothing() = runBlocking {
    val stored = announcement("a1")
    repository.addEvent(event.copy(announcements = listOf(stored)))

    assertThrowsOfType<IllegalArgumentException> {
      repository.editAnnouncement(event.eventId, announcement("unknown"))
    }
    assertThrowsOfType<IllegalArgumentException> {
      repository.deleteAnnouncement(event.eventId, "unknown")
    }

    assertEquals(listOf(stored), repository.getEvent(event.eventId).announcements)
  }

  /** Red if a change in a missing event fails with another error or creates the event document. */
  @Test
  fun announcementChanges_inAMissingEventThrow_andCreateNothing() = runBlocking {
    assertThrowsOfType<NoSuchElementException> {
      repository.addAnnouncement(event.eventId, announcement("a1"))
    }
    assertThrowsOfType<NoSuchElementException> {
      repository.editAnnouncement(event.eventId, announcement("a1"))
    }
    assertThrowsOfType<NoSuchElementException> {
      repository.deleteAnnouncement(event.eventId, "a1")
    }

    assertFalse(eventDocument().get().await().exists())
  }

  /** Red if addEvent or editEvent do not write the announcements, so they vanish on save. */
  @Test
  fun addEventAndEditEvent_saveTheAnnouncements() = runBlocking {
    val created = event.copy(announcements = listOf(fullAnnouncement, announcement("a1")))
    repository.addEvent(created)
    assertEquals(created, repository.getEvent(event.eventId))

    val edited = created.copy(announcements = listOf(announcement("a2")))
    repository.editEvent(event.eventId, edited)
    assertEquals(edited, repository.getEvent(event.eventId))
  }

  /**
   * Red if malformed stored announcements are skipped, read with made-up values, or overwritten by
   * an add, or if the error does not name the event.
   */
  @Test
  fun malformedStoredAnnouncements_areRefused_namingTheEvent() = runBlocking {
    val invalid = "Invalid announcement data for event ${event.eventId}"

    writeEventDocument(announcements = "not a list")
    assertThrowsOfType<IllegalArgumentException>("$invalid.") { repository.getEvent(event.eventId) }
    assertThrowsOfType<IllegalArgumentException>("$invalid.") {
      repository.addAnnouncement(event.eventId, announcement("a1"))
    }
    assertEquals("not a list", eventDocument().get().await().get("announcements"))

    writeEventDocument(announcements = listOf("not an announcement"))
    assertThrowsOfType<IllegalArgumentException>("$invalid.") { repository.getEvent(event.eventId) }

    writeEventDocument(announcements = listOf(fullAnnouncement.toMap() - "title"))
    assertThrowsOfType<IllegalArgumentException>(
        "$invalid: Invalid announcement data: 'title' is missing or invalid."
    ) {
      repository.getEvent(event.eventId)
    }
  }

  private fun eventDocument() = Firebase.firestore.collection("events").document(event.eventId)

  /** Writes the event document by hand, with the given raw `announcements` field. */
  private suspend fun writeEventDocument(announcements: Any) {
    eventDocument()
        .set(
            mapOf(
                "eventId" to event.eventId,
                "ownerId" to event.ownerId,
                "title" to event.title,
                "startDate" to event.startDate.toString(),
                "endDate" to event.endDate.toString(),
                "announcements" to announcements,
            )
        )
        .await()
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

  /** Checks that [block] throws a [T], with [message] when it is given. */
  private suspend inline fun <reified T : Throwable> assertThrowsOfType(
      message: String? = null,
      crossinline block: suspend () -> Unit,
  ) {
    var thrown: Throwable? = null
    try {
      block()
    } catch (exception: Exception) {
      thrown = exception
    }
    assertTrue("Expected ${T::class.java.simpleName} but got $thrown", thrown is T)
    if (message != null) assertEquals(message, thrown?.message)
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
      FirebaseEmulatorSetup.configure()
    }
  }
}
