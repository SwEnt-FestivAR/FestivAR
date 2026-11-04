package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.FieldError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditEventViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val initialEvent = testEvent()
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var viewModel: EditEventViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    eventsRepository = EventsRepositoryLocal(listOf(initialEvent))
    viewModel = EditEventViewModel(eventsRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun fetchEventPrefillsFieldsAndStartsUnchanged() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(initialEvent.title, state.name)
    assertEquals(initialEvent.startDate.toLocalDate(), state.startDate)
    assertEquals(initialEvent.endDate.toLocalDate(), state.endDate)
    assertEquals(initialEvent.startDate.toLocalTime(), state.startTime)
    assertEquals(initialEvent.endDate.toLocalTime(), state.endTime)
    assertEquals(initialEvent.location, state.venue)
    assertEquals(initialEvent.description, state.notes)
    assertFalse(state.hasChanges)
    assertFalse(state.canSave)
  }

  @Test
  fun pickerUpdatesSelectedFieldAndCloses() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val date = initialEvent.startDate.toLocalDate().plusDays(1)
    viewModel.showPicker(DateTimeField.START_DATE)
    viewModel.pickDate(date)

    assertEquals(date, viewModel.uiState.value.startDate)
    assertNull(viewModel.uiState.value.openPicker)
  }

  @Test
  fun pickerUpdatesEveryDateAndTimeFieldAndIgnoresMismatchedPicker() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    val state = viewModel.uiState.value
    val date = state.startDate!!.plusDays(1)
    val time = state.startTime!!.plusHours(1)

    viewModel.showPicker(DateTimeField.END_DATE)
    viewModel.pickDate(date)
    assertEquals(date, viewModel.uiState.value.endDate)

    viewModel.showPicker(DateTimeField.START_TIME)
    viewModel.pickTime(time)
    assertEquals(time, viewModel.uiState.value.startTime)

    viewModel.showPicker(DateTimeField.END_TIME)
    viewModel.pickTime(time)
    assertEquals(time, viewModel.uiState.value.endTime)

    viewModel.showPicker(DateTimeField.START_DATE)
    viewModel.pickTime(time)
    assertEquals(state.startDate, viewModel.uiState.value.startDate)
    assertNull(viewModel.uiState.value.openPicker)

    viewModel.showPicker(null)
    viewModel.pickDate(date)
    assertNull(viewModel.uiState.value.openPicker)
  }

  @Test
  fun initialStateValidationAndEventConversionAreSafe() {
    val state = EditEventUIState()

    assertNull(state.startDateTime)
    assertNull(state.endDateTime)
    assertNull(state.nameError)
    assertNull(state.startDateError)
    assertNull(state.endDateError)
    assertFalse(state.isValid)
    assertFalse(state.hasChanges)
    assertFalse(state.canSave)
    assertThrows(IllegalStateException::class.java) { state.toEvent() }
  }

  @Test
  fun stateValidationAndEventConversionReflectDraft() {
    val future = ZonedDateTime.now().plusDays(2)
    val event =
        initialEvent.copy(
            startDate = future,
            endDate = future.plusHours(3),
        )
    val loadedState = EditEventUIState().load(event)
    val updatedState = loadedState.copy(name = "Updated festival", notes = "New notes")

    assertTrue(loadedState.isValid)
    assertFalse(loadedState.hasChanges)
    assertEquals(event, loadedState.toEvent())
    assertTrue(updatedState.hasChanges)
    assertEquals("Updated festival", updatedState.toEvent().title)
    assertEquals("New notes", updatedState.toEvent().description)
    assertFalse(updatedState.copy(isLoading = true).canSave)
    assertFalse(updatedState.copy(isSaving = true).canSave)
    assertFalse(updatedState.copy(savedEventId = event.eventId).canSave)
    assertEquals(
        FieldError.EMPTY_EVENT_NAME,
        loadedState.copy(name = " ", nameEdited = true).nameError,
    )
    assertThrows(IllegalArgumentException::class.java) { loadedState.copy(name = "").toEvent() }
  }

  @Test
  fun endDateErrorRequiresDateWhenEndTimeIsSet() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.value.copy(endDate = null)

    assertEquals(FieldError.MISSING_DATE, state.endDateError)
  }

  @Test
  fun endDateErrorRejectsPastEndTime() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val yesterday = LocalDate.now().minusDays(1)
    val state =
        viewModel.uiState.value.copy(
            startDate = yesterday.minusDays(1),
            startTime = LocalTime.NOON,
            endDate = yesterday,
            endTime = LocalTime.NOON,
        )

    assertEquals(FieldError.OUTDATED, state.endDateError)
  }

  @Test
  fun endDateErrorRejectsEndBeforeStart() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val tomorrow = LocalDate.now().plusDays(1)
    val state =
        viewModel.uiState.value.copy(
            startDate = tomorrow,
            startTime = LocalTime.of(15, 0),
            endDate = tomorrow,
            endTime = LocalTime.of(14, 0),
        )

    assertEquals(FieldError.END_BEFORE_START, state.endDateError)
  }

  @Test
  fun saveChangesUpdatesEventAndPreservesUneditedEventData() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")
    viewModel.updateVenue("New venue")

    assertTrue(viewModel.uiState.value.canSave)
    viewModel.saveChanges()
    advanceUntilIdle()

    val saved = eventsRepository.getEvent(initialEvent.eventId)
    assertEquals("Updated festival", saved.title)
    assertEquals("New venue", saved.location)
    assertEquals(initialEvent.members, saved.members)
    assertEquals(initialEvent.tasks, saved.tasks)
    assertEquals(initialEvent.announcements, saved.announcements)
    assertEquals(initialEvent.eventId, viewModel.uiState.value.savedEventId)
    assertFalse(viewModel.uiState.value.isSaving)
    assertFalse(viewModel.uiState.value.hasChanges)
  }

  @Test
  fun saveChangesPreservesMembersAndTasksAddedWhileFormWasOpen() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")
    val joinedMember = User(uid = "new-member")
    val addedTask =
        Task(
            taskId = "new-task",
            eventId = initialEvent.eventId,
            title = "New task",
            ownerId = "organizer",
        )
    eventsRepository.editEvent(
        initialEvent.eventId,
        initialEvent.copy(members = listOf(joinedMember), tasks = listOf(addedTask)),
    )

    viewModel.saveChanges()
    advanceUntilIdle()

    val saved = eventsRepository.getEvent(initialEvent.eventId)
    assertEquals("Updated festival", saved.title)
    assertEquals(listOf(joinedMember), saved.members)
    assertEquals(listOf(addedTask), saved.tasks)
  }

  @Test
  fun savingAnotherFieldPreservesEndInstantAndZoneWhenZonesDiffer() = runTest {
    val differentZoneEnd =
        initialEvent.endDate.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
    val eventWithDifferentZones = initialEvent.copy(endDate = differentZoneEnd)
    eventsRepository = EventsRepositoryLocal(listOf(eventWithDifferentZones))
    viewModel = EditEventViewModel(eventsRepository)
    viewModel.fetchEvent(eventWithDifferentZones.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")

    assertTrue(viewModel.uiState.value.canSave)
    viewModel.saveChanges()
    advanceUntilIdle()

    val saved = eventsRepository.getEvent(eventWithDifferentZones.eventId)
    assertEquals(eventWithDifferentZones.endDate.toInstant(), saved.endDate.toInstant())
    assertEquals(eventWithDifferentZones.endDate.zone, saved.endDate.zone)
  }

  @Test
  fun invalidFormCannotSave() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("")

    viewModel.saveChanges()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.canSave)
    assertEquals(initialEvent, eventsRepository.getEvent(initialEvent.eventId))
  }

  @Test
  fun saveFailureShowsErrorAndKeepsEdits() = runTest {
    val failingRepository =
        object : EventsRepository by eventsRepository {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            error("offline")
          }
        }
    viewModel = EditEventViewModel(failingRepository)
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")

    viewModel.saveChanges()
    advanceUntilIdle()

    assertEquals(R.string.edit_event_error_save_failed, viewModel.uiState.value.errorMsg)
    assertEquals("Updated festival", viewModel.uiState.value.name)
    assertFalse(viewModel.uiState.value.isSaving)
  }

  @Test
  fun discardConfirmationCanBeCancelledOrConfirmed() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")

    viewModel.requestDiscard()
    assertTrue(viewModel.uiState.value.showDiscardConfirmation)
    viewModel.cancelDiscard()
    assertEquals("Updated festival", viewModel.uiState.value.name)

    viewModel.requestDiscard()
    viewModel.confirmDiscard()
    assertEquals(initialEvent.title, viewModel.uiState.value.name)
    assertTrue(viewModel.uiState.value.isDiscarded)
    assertFalse(viewModel.uiState.value.showDiscardConfirmation)
  }

  @Test
  fun discardWithoutChangesAndConfirmBeforeLoadingDiscardImmediately() {
    viewModel.requestDiscard()
    assertTrue(viewModel.uiState.value.isDiscarded)
    assertFalse(viewModel.uiState.value.showDiscardConfirmation)

    val freshViewModel = EditEventViewModel(eventsRepository)
    freshViewModel.confirmDiscard()
    assertTrue(freshViewModel.uiState.value.isDiscarded)
    assertFalse(freshViewModel.uiState.value.showDiscardConfirmation)
  }

  @Test
  fun fetchFailureSetsErrorAndClearsLoading() = runTest {
    val failingRepository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun getEvent(eventId: String): Event {
            error("offline")
          }
        }
    viewModel = EditEventViewModel(failingRepository)

    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.errorFetch)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  private fun testEvent(): Event {
    val start = ZonedDateTime.now(ZoneId.of("Europe/Zurich")).plusDays(2)
    return Event(
        eventId = "event-1",
        title = "Festival",
        description = "Bring supplies",
        startDate = start,
        endDate = start.plusHours(3),
        location = "Main square",
    )
  }
}
