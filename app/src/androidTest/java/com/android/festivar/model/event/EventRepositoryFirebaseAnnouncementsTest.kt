// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Announcements are stored inside the event document, as the `announcements` array of maps. These
 * tests run against the Firestore emulator and read every change back through [getEvent], so they
 * also check that the other fields of the event are kept.
 */
@RunWith(AndroidJUnit4::class)
class EventRepositoryFirebaseAnnouncementsTest {
  private lateinit var repository: EventRepositoryFirebase
  private lateinit var event: Event

  private val start = ZonedDateTime.parse("2026-10-17T10:00:00+02:00")
  private val events
    get() = Firebase.firestore.collection("events")

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

  @Before
  fun setUp() {
    runBlocking { Firebase.auth.signInAnonymously().await() }
    // No members, so reading the event back needs no user document.
    event =
        Event(
            eventId = "event-${UUID.randomUUID()}",
            title = "Fête de l'Asso",
            description = "Description",
            startDate = start,
            endDate = start.plusHours(5),
            location = "EPFL",
        )
    repository = EventRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      events.document(event.eventId).delete().await()
      Firebase.auth.signOut()
    }
  }

  // ----------------------------------- add ------------------------------------------------ //

  /** Red if an added announcement loses a field or the announcements come back in another order. */
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

  /** Red if a second announcement with a used id replaces the first one or is stored twice. */
  @Test
  fun addAnnouncement_withAnExistingIdThrows_andKeepsTheStoredOne() = runBlocking {
    val stored = announcement("a1")
    repository.addEvent(event.copy(announcements = listOf(stored)))

    assertThrowsOfType<IllegalArgumentException> {
      repository.addAnnouncement(event.eventId, announcement("a1", title = "Other"))
    }

    assertEquals(listOf(stored), repository.getEvent(event.eventId).announcements)
  }

  /** Red if the cap is not checked against the stored announcements, so the event outgrows it. */
  @Test
  fun addAnnouncement_whenTheEventIsFullThrows_andAddsNothing() = runBlocking {
    val full = (1..MAX_ANNOUNCEMENTS_PER_EVENT).map { announcement("a$it") }
    repository.addEvent(event.copy(announcements = full))

    assertThrowsOfType<IllegalArgumentException> {
      repository.addAnnouncement(event.eventId, announcement("one-too-many"))
    }

    assertEquals(full, repository.getEvent(event.eventId).announcements)
  }

  /** Red if adding to a missing event fails with another error or creates the event document. */
  @Test
  fun addAnnouncement_toAMissingEventThrows_andCreatesNothing() = runBlocking {
    assertThrowsOfType<NoSuchElementException> {
      repository.addAnnouncement(event.eventId, announcement("a1"))
    }

    assertFalse(events.document(event.eventId).get().await().exists())
  }

  /** Red if adds read and then overwrite the array outside a transaction, losing one of them. */
  @Test
  fun addAnnouncement_concurrentAddsKeepBoth() = runBlocking {
    repository.addEvent(event)
    val added = listOf(announcement("a1"), announcement("a2"))

    added.map { async { repository.addAnnouncement(event.eventId, it) } }.awaitAll()

    assertEquals(added.toSet(), repository.getEvent(event.eventId).announcements.toSet())
  }

  // ----------------------------------- edit ----------------------------------------------- //

  /** Red if the edited announcement moves, or if the others change. */
  @Test
  fun editAnnouncement_replacesItInPlace_andKeepsTheOthers() = runBlocking {
    val (a1, a2, a3) = listOf(announcement("a1"), announcement("a2"), announcement("a3"))
    repository.addEvent(event.copy(announcements = listOf(a1, a2, a3)))
    val edited = fullAnnouncement.copy(announcementId = "a2")

    repository.editAnnouncement(event.eventId, edited)

    assertEquals(
        event.copy(announcements = listOf(a1, edited, a3)),
        repository.getEvent(event.eventId),
    )
  }

  /** Red if editing an unknown id adds it instead of refusing. */
  @Test
  fun editAnnouncement_withAnUnknownIdThrows_andChangesNothing() = runBlocking {
    val stored = announcement("a1")
    repository.addEvent(event.copy(announcements = listOf(stored)))

    assertThrowsOfType<IllegalArgumentException> {
      repository.editAnnouncement(event.eventId, announcement("unknown"))
    }

    assertEquals(listOf(stored), repository.getEvent(event.eventId).announcements)
  }

  /** Red if editing in a missing event fails with another error or creates the event document. */
  @Test
  fun editAnnouncement_inAMissingEventThrows_andCreatesNothing() = runBlocking {
    assertThrowsOfType<NoSuchElementException> {
      repository.editAnnouncement(event.eventId, announcement("a1"))
    }

    assertFalse(events.document(event.eventId).get().await().exists())
  }

  // ----------------------------------- delete --------------------------------------------- //

  /** Red if delete removes another announcement, reorders the rest or touches the event. */
  @Test
  fun deleteAnnouncement_removesOnlyIt() = runBlocking {
    val (a1, a2, a3) = listOf(announcement("a1"), announcement("a2"), announcement("a3"))
    repository.addEvent(event.copy(announcements = listOf(a1, a2, a3)))

    repository.deleteAnnouncement(event.eventId, "a2")

    assertEquals(event.copy(announcements = listOf(a1, a3)), repository.getEvent(event.eventId))
  }

  /** Red if deleting an unknown id succeeds silently. */
  @Test
  fun deleteAnnouncement_withAnUnknownIdThrows_andChangesNothing() = runBlocking {
    val stored = announcement("a1")
    repository.addEvent(event.copy(announcements = listOf(stored)))

    assertThrowsOfType<IllegalArgumentException> {
      repository.deleteAnnouncement(event.eventId, "unknown")
    }

    assertEquals(listOf(stored), repository.getEvent(event.eventId).announcements)
  }

  /** Red if deleting in a missing event fails with another error or creates the event document. */
  @Test
  fun deleteAnnouncement_inAMissingEventThrows_andCreatesNothing() = runBlocking {
    assertThrowsOfType<NoSuchElementException> {
      repository.deleteAnnouncement(event.eventId, "a1")
    }

    assertFalse(events.document(event.eventId).get().await().exists())
  }

  // ----------------------------------- whole event ---------------------------------------- //

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

  /** Red if a null field is refused, though only a field of another type is malformed. */
  @Test
  fun getEvent_withANullAnnouncementsFieldReadsAsEmpty() = runBlocking {
    writeEventDocument(announcements = null)

    assertTrue(repository.getEvent(event.eventId).announcements.isEmpty())
  }

  /** Red if an `announcements` field that is not a list reads as no announcements. */
  @Test
  fun getEvent_withAnAnnouncementsFieldThatIsNotAListThrows() = runBlocking {
    writeEventDocument(announcements = "not a list")

    val thrown = assertThrowsOfType<IllegalArgumentException> { repository.getEvent(event.eventId) }
    assertEquals("Invalid announcement data for event ${event.eventId}.", thrown.message)
  }

  /** Red if adding over a malformed `announcements` field overwrites it instead of refusing. */
  @Test
  fun addAnnouncement_onAnAnnouncementsFieldThatIsNotAListThrows_andKeepsIt() = runBlocking {
    writeEventDocument(announcements = "not a list")

    val thrown =
        assertThrowsOfType<IllegalArgumentException> {
          repository.addAnnouncement(event.eventId, announcement("a1"))
        }

    assertEquals("Invalid announcement data for event ${event.eventId}.", thrown.message)
    assertEquals(
        "not a list",
        events.document(event.eventId).get().await().get("announcements"),
    )
  }

  /** Red if a stored announcement that is not a map is skipped instead of refused. */
  @Test
  fun getEvent_withAnAnnouncementThatIsNotAMapThrows() = runBlocking {
    writeEventDocument(announcements = listOf("not an announcement"))

    val thrown = assertThrowsOfType<IllegalArgumentException> { repository.getEvent(event.eventId) }
    assertEquals("Invalid announcement data for event ${event.eventId}.", thrown.message)
  }

  /** Red if a stored announcement missing a required field is read with a made-up value. */
  @Test
  fun getEvent_withAnAnnouncementMissingARequiredFieldThrows_namingTheEvent() = runBlocking {
    writeEventDocument(announcements = listOf(fullAnnouncement.toMap() - "title"))

    val thrown = assertThrowsOfType<IllegalArgumentException> { repository.getEvent(event.eventId) }
    assertEquals(
        "Invalid announcement data for event ${event.eventId}: " +
            "Invalid announcement data: 'title' is missing or invalid.",
        thrown.message,
    )
  }

  /** Writes the event document by hand, with the given raw `announcements` field. */
  private suspend fun writeEventDocument(announcements: Any?) {
    events
        .document(event.eventId)
        .set(
            mapOf(
                "eventId" to event.eventId,
                "title" to event.title,
                "startDate" to event.startDate.toString(),
                "endDate" to event.endDate.toString(),
                "announcements" to announcements,
            )
        )
        .await()
  }

  /** Runs [block], checks that it throws a [T] and returns it. */
  private suspend inline fun <reified T : Throwable> assertThrowsOfType(
      crossinline block: suspend () -> Unit,
  ): T {
    var thrown: Throwable? = null
    try {
      block()
    } catch (exception: Exception) {
      thrown = exception
    }
    assertTrue("Expected ${T::class.java.simpleName} but got $thrown", thrown is T)
    return thrown as T
  }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirestoreEmulator() {
      FirebaseEmulatorSetup.configure()
    }
  }
}
