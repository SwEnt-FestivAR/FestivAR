// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import java.time.ZoneId
import java.time.ZonedDateTime
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinEventViewModelTest {
  private val start = ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  private val ada = User(uid = "uid-ada", name = "Ada", surname = "Lovelace")
  private val sami = User(uid = "uid-sami", name = "Sami", surname = "Kabbaj")

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

  private val events = EventsRepositoryLocal(listOf(fete, noel))
  private val users = UsersRepositoryLocal(listOf(ada, sami))

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(
      user: User = ada,
      eventsRepository: EventsRepository = events,
      usersRepository: UsersRepository = users,
  ) = JoinEventViewModel(user, eventsRepository, usersRepository)

  private fun memberIds(eventId: String) = runBlocking {
    events.getEvent(eventId)
  }
      .members
      .map { it.uid }

  private fun joinedEvents(uid: String) = runBlocking { users.getUser(uid) }.joinedEvents

  /** An events repository whose lookup of all events fails with `offline`. */
  private fun offlineEvents() =
      object : EventsRepository by events {
        override suspend fun getAllEvents(): List<Event> = throw IllegalStateException("offline")
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

    assertNull(viewModel.uiState.value.found)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun sixMatchingCharactersFindTheEventWithItsNameDateAndMemberCount() {
    val viewModel = viewModel()

    viewModel.setCode("fete20")

    assertEquals(
        FoundEventUi(
            id = "fete2026",
            name = "Fete de l'Asso 2026",
            dateLine = "Sat 17 Oct, 10:00",
            members = 1,
        ),
        viewModel.uiState.value.found,
    )
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun sixCharactersThatMatchNothingSayNotFoundAndJoinChangesNothing() {
    val viewModel = viewModel()

    viewModel.setCode("ZZZZZZ")
    viewModel.join()

    assertTrue(viewModel.uiState.value.notFound)
    assertNull(viewModel.uiState.value.found)
    assertFalse(viewModel.uiState.value.joined)
    assertEquals(emptyList<String>(), joinedEvents(ada.uid))
  }

  @Test
  fun editingAFoundCodeClearsTheResultAndTheNotFoundMark() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")

    viewModel.setCode("FETE2")

    assertNull(viewModel.uiState.value.found)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun joiningPutsTheEventInTheUsersJoinedEventsAndTheUserAmongTheMembers() {
    val viewModel = viewModel()
    viewModel.setCode("FETE20")

    viewModel.join()

    assertTrue(viewModel.uiState.value.joined)
    assertFalse(viewModel.uiState.value.isJoining)
    assertEquals(listOf("fete2026"), joinedEvents(ada.uid))
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
  }

  @Test
  fun joiningAnEventThePersonAlreadyBelongsToAddsNoSecondMembership() {
    val viewModel = viewModel(user = sami)
    viewModel.setCode("FETE20")

    viewModel.join()

    assertTrue(viewModel.uiState.value.joined)
    assertNull(viewModel.uiState.value.errorMsg)
    assertEquals(listOf(sami.uid), memberIds("fete2026"))
    assertEquals(emptyList<String>(), joinedEvents(sami.uid))
  }

  @Test
  fun joinDoesNothingBeforeAnEventIsFound() {
    val viewModel = viewModel()

    viewModel.join()

    assertFalse(viewModel.uiState.value.joined)
    assertFalse(viewModel.uiState.value.isJoining)
  }

  @Test
  fun aFailingLookupIsReported() {
    val viewModel = viewModel(eventsRepository = offlineEvents())

    viewModel.setCode("FETE20")

    assertEquals("offline", viewModel.uiState.value.errorMsg)
    assertNull(viewModel.uiState.value.found)
    assertFalse(viewModel.uiState.value.notFound)
  }

  @Test
  fun aFailingJoinIsReportedAndStopsJoining() {
    val failing =
        object : UsersRepository by users {
          override suspend fun editUser(uId: String, newValue: User) =
              throw IllegalStateException("denied")
        }
    val viewModel = viewModel(usersRepository = failing)
    viewModel.setCode("FETE20")

    viewModel.join()

    assertEquals("denied", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isJoining)
    assertFalse(viewModel.uiState.value.joined)
    assertEquals(emptyList<String>(), joinedEvents(ada.uid))
  }

  @Test
  fun aJoinFailingWithoutAMessageFallsBackToAGenericOne() {
    val failing =
        object : EventsRepository by events {
          override suspend fun editEvent(eventId: String, newValue: Event) =
              throw RuntimeException()
        }
    val viewModel = viewModel(eventsRepository = failing)
    viewModel.setCode("FETE20")

    viewModel.join()

    assertEquals("Could not join the event", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isJoining)
  }

  @Test
  fun clearErrorMsgRemovesTheError() {
    val viewModel = viewModel(eventsRepository = offlineEvents())
    viewModel.setCode("FETE20")

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
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

    viewModel.join()
    viewModel.join()

    assertTrue(viewModel.uiState.value.isJoining)
    assertEquals(1, writes)
    gate.complete(Unit)
    assertTrue(viewModel.uiState.value.joined)
    assertEquals(listOf(sami.uid, ada.uid), memberIds("fete2026"))
  }

  @Test
  fun aLookupFinishingAfterTheCodeWasEditedIsDropped() {
    val gate = CompletableDeferred<Unit>()
    val slow =
        object : EventsRepository by events {
          override suspend fun getAllEvents(): List<Event> {
            gate.await()
            return events.getAllEvents()
          }
        }
    val viewModel = viewModel(eventsRepository = slow)
    viewModel.setCode("FETE20")

    viewModel.setCode("FETE2")
    gate.complete(Unit)

    assertEquals("FETE2", viewModel.uiState.value.code)
    assertNull(viewModel.uiState.value.found)
    assertFalse(viewModel.uiState.value.notFound)
  }
}
