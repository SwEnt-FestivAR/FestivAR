// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinEventViewModelTest {
  private val start = ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  private val ada = User(uid = "uid-ada", name = "Ada", surname = "Lovelace")
  private val sami =
      User(uid = "uid-sami", name = "Sami", surname = "Kabbaj", joinedEvents = listOf("fete2026"))

  // The code of fete2026 is FETE20.
  private val fete =
      Event(
          eventId = "fete2026",
          title = "Fete de l'Asso 2026",
          members = listOf(sami),
          startDate = start,
          endDate = start.plusHours(8),
      )
  private val noel =
      Event(
          eventId = "noel-12",
          title = "Noel",
          startDate = start.plusDays(60),
          endDate = start.plusDays(60).plusHours(3),
      )

  /** Delegates to [local] and counts the calls to [editEvent]. */
  private class CountingEvents(private val local: EventsRepository) : EventsRepository by local {
    var writes = 0

    override suspend fun editEvent(eventId: String, newValue: Event) {
      writes++
      local.editEvent(eventId, newValue)
    }
  }

  /** Delegates to [local] and counts the calls to [editUser]. */
  private class CountingUsers(private val local: UsersRepository) : UsersRepository by local {
    var writes = 0

    override suspend fun editUser(uId: String, newValue: User) {
      writes++
      local.editUser(uId, newValue)
    }
  }

  private val events = CountingEvents(EventsRepositoryLocal(listOf(fete, noel)))
  private val users = CountingUsers(UsersRepositoryLocal(listOf(ada, sami)))

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(
      userId: String = ada.uid,
      eventsRepository: EventsRepository = events,
      usersRepository: UsersRepository = users,
  ) = JoinEventViewModel(userId, eventsRepository, usersRepository)

  private fun memberIds(eventId: String) = runBlocking {
    events.getEvent(eventId)
  }
      .members
      .map { it.uid }

  private fun joinedEvents(uid: String) = runBlocking { users.getUser(uid) }.joinedEvents

  /** An events repository whose lookup by join code fails with `offline`. */
  private fun offlineEvents() =
      object : EventsRepository by events {
        override suspend fun getEventsByJoinCode(code: String): List<Event> =
            throw IllegalStateException("offline")
      }

  @Test
  fun aTypedCodeIsUppercasedAndStrippedToLettersAndDigits() {
    val viewModel = viewModel()

    viewModel.setCode("fe-te 2")

    assertEquals("FETE2", viewModel.uiState.value.code)
  }

  @Test
  fun theCodeStopsAtSixCharacters() {
    val viewModel = viewModel()

    viewModel.setCode("FETE2026")

    assertEquals("FETE20", viewModel.uiState.value.code)
  }

  @Test
  fun anIncompleteCodeIsNeitherFoundNorNotFound() {
    val viewModel = viewModel()

    viewModel.setCode("FETE2")

    assertEquals(emptyList<FoundEventUi>(), viewModel.uiState.value.matches)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun aLookupAsksTheRepositoryForTheCodeOnceAndNeverDownloadsEveryEvent() {
    var byCode = 0
    var all = 0
    val counting =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> {
            byCode++
            return events.getEventsByJoinCode(code)
          }

          override suspend fun getAllEvents(): List<Event> {
            all++
            return events.getAllEvents()
          }
        }
    val viewModel = viewModel(eventsRepository = counting)

    viewModel.setCode("FETE20")

    assertEquals(1, byCode)
    assertEquals(0, all)
    assertEquals("fete2026", viewModel.uiState.value.matches.single().id)
  }

  @Test
  fun sixMatchingCharactersFindTheOneEventWithItsNameDateAndMemberCount() {
    val viewModel = viewModel()

    viewModel.setCode("fete20")

    assertEquals(
        FoundEventUi(
            id = "fete2026",
            name = "Fete de l'Asso 2026",
            start = start,
            members = 1,
        ),
        viewModel.uiState.value.matches.single(),
    )
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun theCardOfAnEventTheUserBelongsToIsFlaggedAsMemberAndAStrangersCardIsNot() {
    val member = viewModel(userId = sami.uid)
    val stranger = viewModel(userId = ada.uid)

    member.setCode("FETE20")
    stranger.setCode("FETE20")

    assertTrue(member.uiState.value.matches.single().isMember)
    assertFalse(stranger.uiState.value.matches.single().isMember)
  }

  @Test
  fun sixCharactersThatMatchNothingSayNotFoundAndJoinChangesNothing() {
    val viewModel = viewModel()

    viewModel.setCode("ZZZZZZ")
    viewModel.join("fete2026")

    assertTrue(viewModel.uiState.value.notFound)
    assertEquals(emptyList<FoundEventUi>(), viewModel.uiState.value.matches)
    assertNull(viewModel.uiState.value.joinedEventId)
    assertEquals(emptyList<String>(), joinedEvents(ada.uid))
  }

  @Test
  fun editingAFoundCodeClearsTheResultAndTheNotFoundMark() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")

    viewModel.setCode("FETE2")

    assertEquals(emptyList<FoundEventUi>(), viewModel.uiState.value.matches)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun joiningPutsTheEventInTheUsersJoinedEventsAndTheUserAmongTheMembers() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)
    assertFalse(viewModel.uiState.value.isJoining)
    assertEquals(listOf("fete2026"), joinedEvents(ada.uid))
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
  }

  @Test
  fun joiningAnEventThePersonAlreadyBelongsToAddsNoSecondMembership() {
    val viewModel = viewModel(userId = sami.uid)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)
    assertNull(viewModel.uiState.value.errorRes)
    assertEquals(listOf(sami.uid), memberIds("fete2026"))
    assertEquals(listOf("fete2026"), joinedEvents(sami.uid))
    assertEquals(0, events.writes)
    assertEquals(0, users.writes)
  }

  @Test
  fun joinDoesNothingForAnIdTheCodeDidNotMatch() {
    val viewModel = viewModel()

    viewModel.join("fete2026")

    assertNull(viewModel.uiState.value.joinedEventId)
    assertFalse(viewModel.uiState.value.isJoining)
    assertNull(viewModel.uiState.value.errorRes)
    assertEquals(0, events.writes)
    assertEquals(0, users.writes)
  }

  @Test
  fun aFailingLookupIsReported() {
    val viewModel = viewModel(eventsRepository = offlineEvents())

    viewModel.setCode("FETE20")

    assertEquals(R.string.join_event_lookup_failed, viewModel.uiState.value.errorRes)
    assertEquals(emptyList<FoundEventUi>(), viewModel.uiState.value.matches)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun aFailingUserWriteLeavesTheEventSideWrittenAndReportsIt() {
    val failing =
        object : UsersRepository by users {
          override suspend fun editUser(uId: String, newValue: User) =
              throw IllegalStateException("denied")
        }
    val viewModel = viewModel(usersRepository = failing)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals(R.string.join_event_join_failed, viewModel.uiState.value.errorRes)
    assertFalse(viewModel.uiState.value.isJoining)
    assertNull(viewModel.uiState.value.joinedEventId)
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
  }

  @Test
  fun aJoinFailingWithoutAMessageShowsTheJoinFailedMessage() {
    val failing =
        object : EventsRepository by events {
          override suspend fun editEvent(eventId: String, newValue: Event) =
              throw RuntimeException()
        }
    val viewModel = viewModel(eventsRepository = failing)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals(R.string.join_event_join_failed, viewModel.uiState.value.errorRes)
    assertFalse(viewModel.uiState.value.isJoining)
    assertEquals(emptyList<String>(), joinedEvents(ada.uid))
  }

  @Test
  fun aMemberWhoseOpenFailsIsToldTheEventCouldNotBeOpenedNotJoined() {
    val failing =
        object : UsersRepository by users {
          override suspend fun getUser(uId: String): User = throw IllegalStateException("offline")
        }
    val viewModel = viewModel(userId = sami.uid, usersRepository = failing)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals(R.string.join_event_open_failed, viewModel.uiState.value.errorRes)
    assertFalse(viewModel.uiState.value.isJoining)
    assertNull(viewModel.uiState.value.joinedEventId)
  }

  @Test
  fun aUserWhoseRecordHoldsTheEventButIsMissingFromItsMembersEndsWithOneMembershipOnEachSide() {
    val halfJoined = ada.addJoinedEvent("fete2026")
    val halfUsers = UsersRepositoryLocal(listOf(halfJoined, sami))
    val viewModel = viewModel(userId = halfJoined.uid, usersRepository = halfUsers)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)
    assertNull(viewModel.uiState.value.errorRes)
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
    assertEquals(listOf("fete2026"), runBlocking { halfUsers.getUser(ada.uid) }.joinedEvents)
  }

  @Test
  fun aJoinThatFailedOnTheEventSideLeavesTheUserUnrecordedAndTheRetrySucceeds() {
    var failures = 1
    val flaky =
        object : EventsRepository by events {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            if (failures-- > 0) throw IllegalStateException("flaky")
            events.editEvent(eventId, newValue)
          }
        }
    val viewModel = viewModel(eventsRepository = flaky)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    assertEquals(R.string.join_event_join_failed, viewModel.uiState.value.errorRes)
    assertFalse(viewModel.uiState.value.isJoining)
    assertEquals(emptyList<String>(), joinedEvents(ada.uid))

    viewModel.join("fete2026")

    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
    assertEquals(listOf("fete2026"), joinedEvents(ada.uid))
  }

  @Test
  fun joiningKeepsTheOtherJoinedEventsAndTheAssignedTasksOfTheStoredUser() {
    val stored = ada.addJoinedEvent("noel-12").addAssignedTask("task-1")
    val storedUsers = UsersRepositoryLocal(listOf(stored, sami))
    val viewModel = viewModel(userId = ada.uid, usersRepository = storedUsers)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")

    val after = runBlocking { storedUsers.getUser(ada.uid) }
    assertEquals(listOf("noel-12", "fete2026"), after.joinedEvents)
    assertEquals(listOf("task-1"), after.tasksAssigned)
  }

  @Test
  fun aKeystrokeWhileJoiningDoesNotLoseTheJoinedEvent() {
    val gate = CompletableDeferred<Unit>()
    val slow =
        object : EventsRepository by events {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            gate.await()
            events.editEvent(eventId, newValue)
          }
        }
    val viewModel = viewModel(eventsRepository = slow)
    viewModel.setCode("FETE20")
    viewModel.join("fete2026")

    viewModel.setCode("FETE2")
    gate.complete(Unit)

    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)
    assertEquals("FETE20", viewModel.uiState.value.code)
  }

  @Test
  fun aCancelledLookupIsNotReportedAsAFailure() {
    // Only the first lookup suspends; cancelling it throws a CancellationException out of await.
    val gate = CompletableDeferred<Unit>()
    var calls = 0
    val slow =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> {
            if (calls++ == 0) gate.await()
            return events.getEventsByJoinCode(code)
          }
        }
    val viewModel = viewModel(eventsRepository = slow)
    viewModel.setCode("FETE20")
    viewModel.setCode("NOEL12")

    gate.complete(Unit)

    assertNull(viewModel.uiState.value.errorRes)
    assertEquals("noel-12", viewModel.uiState.value.matches.single().id)
  }

  @Test
  fun aCodeUnchangedByFilteringDoesNotLookTheEventUpAgain() {
    var lookups = 0
    val counting =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> {
            lookups++
            return events.getEventsByJoinCode(code)
          }
        }
    val viewModel = viewModel(eventsRepository = counting)

    viewModel.setCode("FETE20")
    viewModel.setCode("FETE20-")

    assertEquals(1, lookups)
    assertEquals("fete2026", viewModel.uiState.value.matches.single().id)
  }

  @Test
  fun aSecondJoinWhileJoiningWritesNothingTwice() {
    val gate = CompletableDeferred<Unit>()
    var writes = 0
    val slow =
        object : UsersRepository by users {
          override suspend fun editUser(uId: String, newValue: User) {
            writes++
            gate.await()
            users.editUser(uId, newValue)
          }
        }
    val viewModel = viewModel(usersRepository = slow)
    viewModel.setCode("FETE20")

    viewModel.join("fete2026")
    viewModel.join("fete2026")

    assertTrue(viewModel.uiState.value.isJoining)
    assertEquals(1, writes)
    gate.complete(Unit)
    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
  }

  @Test
  fun shorteningTheCodeCancelsTheRunningLookupSoItsMatchesNeverShow() {
    val gate = CompletableDeferred<Unit>()
    val slow =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> {
            gate.await()
            return events.getEventsByJoinCode(code)
          }
        }
    val viewModel = viewModel(eventsRepository = slow)
    viewModel.setCode("FETE20")

    viewModel.setCode("FETE2")
    gate.complete(Unit)

    assertEquals("FETE2", viewModel.uiState.value.code)
    assertEquals(emptyList<FoundEventUi>(), viewModel.uiState.value.matches)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun changingTheCodeCancelsTheRunningLookupSoItsResultNeverShows() {
    val gate = CompletableDeferred<Unit>()
    var cancelled: CancellationException? = null
    var calls = 0
    val slow =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> {
            if (calls++ == 0) {
              try {
                gate.await()
              } catch (e: CancellationException) {
                cancelled = e
                throw e
              }
            }
            return events.getEventsByJoinCode(code)
          }
        }
    val viewModel = viewModel(eventsRepository = slow)
    viewModel.setCode("FETE20")

    viewModel.setCode("NOEL12")
    gate.complete(Unit)

    assertNotNull(cancelled)
    assertEquals("NOEL12", viewModel.uiState.value.code)
    assertEquals(listOf("noel-12"), viewModel.uiState.value.matches.map { it.id })
  }

  @Test
  fun onceTheScreenOpenedTheJoinedEventTheJoinIsConsumedAndTypingWorksAgain() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")
    viewModel.join("fete2026")

    viewModel.onEventOpened()

    assertNull(viewModel.uiState.value.joinedEventId)
    viewModel.setCode("NOEL12")
    assertEquals("noel-12", viewModel.uiState.value.matches.single().id)
  }

  @Test
  fun retypingTheSameCodeAfterAFailedLookupLooksTheEventUpAgain() {
    var lookups = 0
    val flaky =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> {
            if (lookups++ == 0) throw IllegalStateException("offline")
            return events.getEventsByJoinCode(code)
          }
        }
    val viewModel = viewModel(eventsRepository = flaky)
    viewModel.setCode("FETE20")
    assertEquals(R.string.join_event_lookup_failed, viewModel.uiState.value.errorRes)

    viewModel.setCode("FETE20")

    assertEquals(2, lookups)
    assertNull(viewModel.uiState.value.errorRes)
    assertEquals("fete2026", viewModel.uiState.value.matches.single().id)
  }

  @Test
  fun aLookupFailureShowsTheSameMessageWhetherOrNotTheExceptionCarriesText() {
    fun failingWith(error: Exception) =
        object : EventsRepository by events {
          override suspend fun getEventsByJoinCode(code: String): List<Event> = throw error
        }
    val withText = viewModel(eventsRepository = failingWith(IllegalStateException("raw text")))
    val withoutText = viewModel(eventsRepository = failingWith(RuntimeException()))

    withText.setCode("FETE20")
    withoutText.setCode("FETE20")

    assertEquals(R.string.join_event_lookup_failed, withText.uiState.value.errorRes)
    assertEquals(R.string.join_event_lookup_failed, withoutText.uiState.value.errorRes)
  }

  @Test
  fun typingAfterAJoinIsIgnoredUntilTheScreenConsumedTheId() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")
    viewModel.join("fete2026")
    assertEquals("fete2026", viewModel.uiState.value.joinedEventId)

    viewModel.setCode("X")

    assertEquals("FETE20", viewModel.uiState.value.code)
    viewModel.onEventOpened()
    viewModel.setCode("X")
    assertEquals("X", viewModel.uiState.value.code)
  }

  @Test
  fun twoEventsSharingACodeAreBothListedAndJoiningOneLeavesTheOtherAlone() {
    // fete2026 and fete2026-bis both give FETE20.
    val bis = fete.copy(eventId = "fete2026-bis", title = "Fete bis", members = emptyList())
    val colliding = EventsRepositoryLocal(listOf(fete, bis, noel))
    val viewModel = viewModel(eventsRepository = colliding)

    viewModel.setCode("FETE20")

    assertEquals(listOf("fete2026", "fete2026-bis"), viewModel.uiState.value.matches.map { it.id })
    assertFalse(viewModel.uiState.value.notFound)

    viewModel.join("fete2026-bis")

    assertEquals("fete2026-bis", viewModel.uiState.value.joinedEventId)
    assertEquals(
        listOf(ada.uid),
        runBlocking { colliding.getEvent("fete2026-bis") }.members.map { it.uid },
    )
    assertEquals(
        listOf(sami.uid),
        runBlocking { colliding.getEvent("fete2026") }.members.map { it.uid },
    )
    assertEquals(listOf("fete2026-bis"), joinedEvents(ada.uid))
  }

  @Test
  fun joiningAnEventTheCodeDidNotMatchWritesNothing() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")

    viewModel.join("noel-12")

    assertFalse(viewModel.uiState.value.isJoining)
    assertNull(viewModel.uiState.value.joinedEventId)
    assertEquals(0, events.writes)
    assertEquals(0, users.writes)
  }
}
