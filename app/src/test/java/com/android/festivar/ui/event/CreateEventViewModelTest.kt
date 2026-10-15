package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateEventViewModelTest {
  private lateinit var repository: EventsRepositoryLocal
  private lateinit var viewModel: CreateEventViewModel

  private val startDate = ZonedDateTime.parse("2026-10-08T10:00:00+02:00")
  private val endDate = ZonedDateTime.parse("2026-10-08T18:00:00+02:00")

  @Before
  fun setUp() {
    repository = EventsRepositoryLocal()
    viewModel = CreateEventViewModel(repository)
  }

  @Test
  fun updateNameWorks() {
    viewModel.updateName("Summer festival")
    assertEquals("Summer festival", viewModel.uiState.value.name)
  }

  @Test
  fun updateStartDateWorks() {
    viewModel.updateStartDate(startDate)
    assertEquals(startDate, viewModel.uiState.value.startDate)
  }

  @Test
  fun updateEndDateWorks() {
    viewModel.updateEndDate(endDate)
    assertEquals(endDate, viewModel.uiState.value.endDate)
  }

  @Test
  fun updateStartDateMillisWorks() {
    viewModel.updateStartDateMillis(1_000L)
    assertEquals(1_000L, viewModel.uiState.value.startDateMillis)
  }

  @Test
  fun updateEndDateMillisWorks() {
    viewModel.updateEndDateMillis(2_000L)
    assertEquals(2_000L, viewModel.uiState.value.endDateMillis)
  }

  @Test
  fun updateVenueWorks() {
    viewModel.updateVenue("Main square")
    assertEquals("Main square", viewModel.uiState.value.venue)
  }

  @Test
  fun updateNotesWorks() {
    viewModel.updateNotes("Bring the stage equipment")
    assertEquals("Bring the stage equipment", viewModel.uiState.value.notes)
  }

  @Test
  fun updateActiveDatePickerWorks() {
    viewModel.updateActiveDatePicker(DateField.START)
    assertEquals(DateField.START, viewModel.uiState.value.activeDatePicker)

    viewModel.updateActiveDatePicker(null)
    assertNull(viewModel.uiState.value.activeDatePicker)
  }

  @Test
  fun removeErrorNameWorks() {
    viewModel.setErrors()
    viewModel.removeErrorName()

    assertFalse(viewModel.uiState.value.errorName)
  }

  @Test
  fun removeErrorDateWorks() {
    viewModel.setErrors()
    viewModel.removeErrorDate()

    assertFalse(viewModel.uiState.value.errorDate)
  }

  @Test
  fun removeErrorVenueWorks() {
    viewModel.setErrors()
    viewModel.removeErrorVenue()

    assertFalse(viewModel.uiState.value.errorVenue)
  }

  @Test
  fun setErrorsSetsErrorsForBlankFields() {
    viewModel.setErrors()

    val state = viewModel.uiState.value
    assertTrue(state.errorName)
    assertTrue(state.errorDate)
    assertTrue(state.errorVenue)
  }

  @Test
  fun setErrorsSetsDateErrorForInvalidDateRange() {
    viewModel.updateStartDate(endDate)
    viewModel.updateEndDate(startDate)

    viewModel.setErrors()

    assertTrue(viewModel.uiState.value.errorDate)
  }

  @Test
  fun endDateComesAfterStartDateWorksForValidDates() {
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)

    assertTrue(viewModel.endDateComesAfterStartDate())
  }

  @Test
  fun endDateComesAfterStartDateIsFalseForInvalidDates() {
    viewModel.updateStartDate(endDate)
    viewModel.updateEndDate(startDate)

    assertFalse(viewModel.endDateComesAfterStartDate())
  }

  @Test
  fun validEventIsTrueWhenAllFieldsAreValid() {
    fillRequiredFields()

    assertTrue(viewModel.validEvent())
  }

  @Test
  fun validEventIsFalseWhenDatesAreInvalid() {
    fillRequiredFields()
    viewModel.updateStartDate(endDate)
    viewModel.updateEndDate(startDate)

    assertFalse(viewModel.validEvent())
  }

  @Test
  fun createEvent_addsTheFormEventToTheRepository() = runBlocking {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateVenue("Main square")
    viewModel.updateNotes("Bring the stage equipment")

    viewModel.createEvent()

    var events: List<Event> = emptyList()
    withTimeout(5_000.milliseconds) {
      while (events.isEmpty()) {
        events = repository.getAllEvents()
        if (events.isEmpty()) delay(10.milliseconds)
      }
    }

    assertEquals(1, events.size)
    assertEquals(
        Event(
            eventId = events.single().eventId,
            title = "Summer festival",
            description = "Bring the stage equipment",
            startDate = startDate,
            endDate = endDate,
            location = "Main square",
        ),
        events.single(),
    )
  }

  @Test
  fun createEvent_automaticallyAddsCreatorToEventMembersAndJoinedEvents() = runBlocking {
    val creator = User(uid = "creator-123", name = "Alice", surname = "Smith")
    val usersRepo = UsersRepositoryLocal(listOf(creator))
    val creatorViewModel =
        CreateEventViewModel(
            eventsRepository = repository,
            usersRepository = usersRepo,
            userId = creator.uid,
        )

    creatorViewModel.updateName("Summer festival")
    creatorViewModel.updateStartDate(startDate)
    creatorViewModel.updateEndDate(endDate)
    creatorViewModel.updateVenue("Main square")

    creatorViewModel.createEvent()

    var events: List<Event> = emptyList()
    withTimeout(5_000.milliseconds) {
      while (events.isEmpty()) {
        events = repository.getAllEvents()
        if (events.isEmpty()) delay(10.milliseconds)
      }
    }

    assertEquals(1, events.size)
    val createdEvent = events.single()
    assertEquals(listOf(creator), createdEvent.members)

    val updatedUser = usersRepo.getUser(creator.uid)
    assertTrue(updatedUser.joinedEvents.contains(createdEvent.eventId))
  }

  @Test
  fun createEvent_whenUserIdIsBlank_doesNotAddMembers() = runBlocking {
    val creatorViewModel =
        CreateEventViewModel(
            eventsRepository = repository,
            userId = "",
        )

    creatorViewModel.updateName("Summer festival")
    creatorViewModel.updateStartDate(startDate)
    creatorViewModel.updateEndDate(endDate)
    creatorViewModel.updateVenue("Main square")

    creatorViewModel.createEvent()

    var events: List<Event> = emptyList()
    withTimeout(5_000.milliseconds) {
      while (events.isEmpty()) {
        events = repository.getAllEvents()
        if (events.isEmpty()) delay(10.milliseconds)
      }
    }

    assertEquals(1, events.size)
    val createdEvent = events.single()
    assertTrue(createdEvent.members.isEmpty())
  }

  @Test
  fun createEvent_whenGetUserFails_addsFallbackMemberWithoutEditingUser() = runBlocking {
    val usersRepo = UsersRepositoryLocal(emptyList())
    val creatorViewModel =
        CreateEventViewModel(
            eventsRepository = repository,
            usersRepository = usersRepo,
            userId = "non-existent-user",
        )

    creatorViewModel.updateName("Summer festival")
    creatorViewModel.updateStartDate(startDate)
    creatorViewModel.updateEndDate(endDate)
    creatorViewModel.updateVenue("Main square")

    creatorViewModel.createEvent()

    var events: List<Event> = emptyList()
    withTimeout(5_000.milliseconds) {
      while (events.isEmpty()) {
        events = repository.getAllEvents()
        if (events.isEmpty()) delay(10.milliseconds)
      }
    }

    assertEquals(1, events.size)
    val createdEvent = events.single()
    assertEquals(listOf(User(uid = "non-existent-user")), createdEvent.members)

    var userFetchFailed = false
    try {
      usersRepo.getUser("non-existent-user")
    } catch (_: Exception) {
      userFetchFailed = true
    }
    assertTrue(userFetchFailed)
  }

  @Test
  fun formIsComplete_isFalseForTheInitialState() {
    assertFalse(viewModel.formIsComplete())
  }

  @Test
  fun formIsComplete_isTrueWhenAllRequiredFieldsAreFilled() {
    fillRequiredFields()

    assertTrue(viewModel.formIsComplete())
  }

  @Test
  fun formIsComplete_isFalseWhenAnyRequiredFieldIsMissing() {
    listOf("name", "startDate", "endDate", "venue").forEach { missingField ->
      val testViewModel = CreateEventViewModel(EventsRepositoryLocal())
      if (missingField != "name") testViewModel.updateName("Summer festival")
      if (missingField != "startDate") testViewModel.updateStartDate(startDate)
      if (missingField != "endDate") testViewModel.updateEndDate(endDate)
      if (missingField != "venue") testViewModel.updateVenue("Main square")

      assertFalse(testViewModel.formIsComplete())
    }
  }

  private fun fillRequiredFields() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateVenue("Main square")
  }

  /** A repository whose addEvent waits on [gate], so the saving state can be observed. */
  private class GatedRepository(val gate: CompletableDeferred<Unit>) :
      EventsRepository by EventsRepositoryLocal() {
    var adds = 0

    override suspend fun addEvent(event: Event) {
      adds++
      gate.await()
    }
  }

  private class FailingRepository : EventsRepository by EventsRepositoryLocal() {
    override suspend fun addEvent(event: Event): Unit = throw IllegalStateException("offline")
  }

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(5_000.milliseconds) { while (!condition()) delay(10.milliseconds) }
  }

  /** Red if the saving flag is never raised, or if a saved event does not report its id. */
  @Test
  fun createEvent_isSavingUntilTheEventIsWrittenThenReportsItsId() = runBlocking {
    val repository = GatedRepository(CompletableDeferred())
    val viewModel = CreateEventViewModel(repository)
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateVenue("Main square")

    viewModel.createEvent()
    waitUntil { viewModel.uiState.value.isSaving }
    assertNull(viewModel.uiState.value.createdEventId)

    repository.gate.complete(Unit)
    waitUntil { viewModel.uiState.value.createdEventId != null }
    assertFalse(viewModel.uiState.value.isSaving)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Red if a second tap during the save writes a second event. */
  @Test
  fun createEvent_ignoresASecondTapWhileSaving() = runBlocking {
    val repository = GatedRepository(CompletableDeferred())
    val viewModel = CreateEventViewModel(repository)
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateVenue("Main square")

    viewModel.createEvent()
    waitUntil { viewModel.uiState.value.isSaving }
    viewModel.createEvent()
    // Asserted inside the gated window, so an unguarded second launch has had time to count
    delay(200.milliseconds)
    assertEquals(1, repository.adds)

    repository.gate.complete(Unit)
    waitUntil { viewModel.uiState.value.createdEventId != null }
    assertEquals(1, repository.adds)
  }

  /** Red if a failed write crashes, wipes the form, or leaves the button disabled. */
  @Test
  fun createEvent_failureShowsAnErrorAndKeepsTheForm() = runBlocking {
    val viewModel = CreateEventViewModel(FailingRepository())
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateVenue("Main square")

    viewModel.createEvent()
    waitUntil { viewModel.uiState.value.errorMsg != null }

    val state = viewModel.uiState.value
    assertEquals(R.string.create_event_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertNull(state.createdEventId)
    assertEquals("Summer festival", state.name)

    // Editing the form clears the stale error
    viewModel.updateName("Summer festival 2")
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Red if equal dates reach Event, whose init throws, instead of the date error. */
  @Test
  fun createEvent_withEqualDatesSetsTheDateErrorInsteadOfCrashing() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(startDate)
    viewModel.updateVenue("Main square")

    viewModel.createEvent()

    assertTrue(viewModel.uiState.value.errorDate)
    assertFalse(viewModel.uiState.value.isSaving)
  }

  /** Red if a one day event, ending at the end of its day, is refused. */
  @Test
  fun aOneDayEventEndingAtTheEndOfItsDayIsValid() {
    val day = ZonedDateTime.parse("2026-10-08T00:00:00+02:00")
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(day)
    viewModel.updateEndDate(day.plusDays(1).minusSeconds(1))
    viewModel.updateVenue("Main square")

    assertTrue(viewModel.validEvent())
  }
}
