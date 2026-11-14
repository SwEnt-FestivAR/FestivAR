package com.android.festivar.ui.event.edit

// Co-authored-by: Copilot App

import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.FieldError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
  private var signedInUserId = "organizer"
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var viewModel: EditEventViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    signedInUserId = "organizer"
    eventsRepository = EventsRepositoryLocal(listOf(initialEvent))
    viewModel = createViewModel(eventsRepository)
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
  fun fetchEventIgnoresDuplicateCallsWhileLoading() = runTest {
    val pendingEvent = CompletableDeferred<Event>()
    var fetchCount = 0
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun getEvent(eventId: String): Event {
            fetchCount++
            return pendingEvent.await()
          }
        }
    viewModel = createViewModel(repository)

    viewModel.fetchEvent(initialEvent.eventId)
    viewModel.fetchEvent(initialEvent.eventId)
    runCurrent()

    assertEquals(1, fetchCount)
    assertTrue(viewModel.uiState.value.isLoading)
    pendingEvent.complete(initialEvent)
    advanceUntilIdle()
    assertEquals(initialEvent.title, viewModel.uiState.value.name)
  }

  @Test
  fun fetchEventDoesNotResetLoadedDraft() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Draft name")

    viewModel.fetchEvent(initialEvent.eventId)

    assertEquals("Draft name", viewModel.uiState.value.name)
    assertTrue(viewModel.uiState.value.hasChanges)
  }

  @Test
  fun fetchEventRejectsBlankIdWithoutRepositoryRead() = runTest {
    var fetchCount = 0
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun getEvent(eventId: String): Event {
            fetchCount++
            return eventsRepository.getEvent(eventId)
          }
        }
    viewModel = createViewModel(repository)

    viewModel.fetchEvent(" ")
    advanceUntilIdle()

    assertEquals(0, fetchCount)
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        R.string.edit_event_error_fetch_failed,
        viewModel.uiState.value.errorFetchMsg,
    )
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
  fun changingEachDateOrTimeFieldMarksDraftAsChanged() {
    val state = EditEventUIState().load(initialEvent)

    assertTrue(state.copy(startDate = state.startDate!!.plusDays(1)).hasChanges)
    assertTrue(state.copy(endDate = state.endDate!!.plusDays(1)).hasChanges)
    assertTrue(state.copy(startTime = state.startTime!!.plusHours(1)).hasChanges)
    assertTrue(state.copy(endTime = state.endTime!!.plusHours(1)).hasChanges)
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

    val now = ZonedDateTime.of(2026, 11, 5, 12, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    val yesterday = now.toLocalDate().minusDays(1)
    val state =
        viewModel.uiState.value.copy(
            startDate = yesterday.minusDays(1),
            startTime = LocalTime.NOON,
            endDate = yesterday,
            endTime = LocalTime.NOON,
        )

    assertEquals(FieldError.OUTDATED, state.endDateErrorAt(now))
  }

  @Test
  fun endDateErrorUsesEndZoneAndProvidedCurrentTime() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    val startDate = LocalDate.of(2026, 11, 6)
    val state =
        viewModel.uiState.value.copy(
            startDate = startDate,
            startTime = LocalTime.of(23, 0),
            endDate = startDate,
            endTime = LocalTime.of(17, 0),
            endZone = ZoneId.of("America/Los_Angeles"),
        )
    val now = ZonedDateTime.of(2026, 11, 5, 12, 0, 0, 0, ZoneId.of("Europe/Zurich"))

    assertNull(state.endDateErrorAt(now))
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
    assertTrue(saved.closed)
    assertEquals(initialEvent.eventId, viewModel.uiState.value.savedEventId)
    assertFalse(viewModel.uiState.value.isSaving)
    assertFalse(viewModel.uiState.value.hasChanges)
  }

  @Test
  fun doubleTapWhileSavingWritesOnlyOnce() = runTest {
    val writeStarted = CompletableDeferred<Unit>()
    val finishWrite = CompletableDeferred<Unit>()
    var writeCount = 0
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            writeCount++
            writeStarted.complete(Unit)
            finishWrite.await()
            eventsRepository.editEvent(eventId, newValue)
          }
        }
    viewModel = createViewModel(repository)
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")

    viewModel.saveChanges()
    viewModel.saveChanges()
    runCurrent()
    writeStarted.await()
    assertEquals(1, writeCount)
    finishWrite.complete(Unit)
    advanceUntilIdle()

    assertEquals(1, writeCount)
    assertEquals("Updated festival", eventsRepository.getEvent(initialEvent.eventId).title)
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
        initialEvent.copy(
            members = listOf(User("member-1"), joinedMember),
            tasks = listOf(addedTask),
        ),
    )

    viewModel.saveChanges()
    advanceUntilIdle()

    val saved = eventsRepository.getEvent(initialEvent.eventId)
    assertEquals("Updated festival", saved.title)
    assertEquals(listOf(User("member-1"), joinedMember), saved.members)
    assertEquals(listOf(addedTask), saved.tasks)
  }

  @Test
  fun savingAnotherFieldPreservesEndInstantAndZoneWhenZonesDiffer() = runTest {
    val differentZoneEnd =
        initialEvent.endDate.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
    val eventWithDifferentZones = initialEvent.copy(endDate = differentZoneEnd)
    eventsRepository = EventsRepositoryLocal(listOf(eventWithDifferentZones))
    viewModel = createViewModel(eventsRepository)
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
    viewModel = createViewModel(failingRepository)
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
  fun saveCancellationDoesNotBecomeSaveError() = runTest {
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            throw CancellationException("save cancelled")
          }
        }
    viewModel = createViewModel(repository)
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")

    viewModel.saveChanges()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isSaving)
    assertNull(viewModel.uiState.value.errorMsg)
    assertEquals("Updated festival", viewModel.uiState.value.name)
    assertEquals(initialEvent, eventsRepository.getEvent(initialEvent.eventId))
  }

  @Test
  fun saveAfterSignOutIsRejected() = runTest {
    var editCount = 0
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            editCount++
            eventsRepository.editEvent(eventId, newValue)
          }
        }
    viewModel = createViewModel(repository)
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")
    signedInUserId = ""

    viewModel.saveChanges()
    advanceUntilIdle()

    assertEquals(0, editCount)
    assertEquals(initialEvent, eventsRepository.getEvent(initialEvent.eventId))
    assertEquals(R.string.edit_event_error_save_failed, viewModel.uiState.value.errorMsg)
    assertEquals("Updated festival", viewModel.uiState.value.name)
    assertFalse(viewModel.uiState.value.isSaving)
  }

  @Test
  fun timedOutSaveReadsBackAndRecognizesPersistedEvent() = runTest {
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            eventsRepository.editEvent(eventId, newValue)
            delay(100)
          }
        }
    viewModel =
        EditEventViewModel(
            DataRepositoryLocal(FakeAuthRepository(), eventsRepository = repository),
            { signedInUserId },
            saveTimeoutMs = 10,
        )
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")

    viewModel.saveChanges()
    advanceUntilIdle()

    assertEquals("Updated festival", eventsRepository.getEvent(initialEvent.eventId).title)
    assertEquals(initialEvent.eventId, viewModel.uiState.value.savedEventId)
    assertFalse(viewModel.uiState.value.isSaving)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun fetchEventDoesNotResetFormWhileSaveIsInProgress() = runTest {
    val writeStarted = CompletableDeferred<Unit>()
    val finishWrite = CompletableDeferred<Unit>()
    val repository =
        object : EventsRepository by eventsRepository {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            writeStarted.complete(Unit)
            finishWrite.await()
            eventsRepository.editEvent(eventId, newValue)
          }
        }
    viewModel = createViewModel(repository)
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Draft name")

    viewModel.saveChanges()
    runCurrent()
    writeStarted.await()
    viewModel.fetchEvent(initialEvent.eventId)

    assertTrue(viewModel.uiState.value.isSaving)
    assertEquals("Draft name", viewModel.uiState.value.name)
    finishWrite.complete(Unit)
    advanceUntilIdle()
    assertEquals("Draft name", eventsRepository.getEvent(initialEvent.eventId).title)
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

    val freshViewModel = createViewModel(eventsRepository)
    freshViewModel.confirmDiscard()
    assertTrue(freshViewModel.uiState.value.isDiscarded)
    assertFalse(freshViewModel.uiState.value.showDiscardConfirmation)
  }

  @Test
  fun fetchFailureSetsErrorAndClearsLoading() = runTest {
    var shouldFail = true
    val repository =
        object : EventsRepository by EventsRepositoryLocal(listOf(initialEvent)) {
          override suspend fun getEvent(eventId: String): Event {
            if (shouldFail) error("offline")
            return initialEvent
          }
        }
    viewModel = createViewModel(repository)

    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    assertEquals(
        R.string.edit_event_error_fetch_failed,
        viewModel.uiState.value.errorFetchMsg,
    )
    assertFalse(viewModel.uiState.value.isLoading)
    shouldFail = false
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    assertNull(viewModel.uiState.value.errorFetchMsg)
    assertEquals(initialEvent.title, viewModel.uiState.value.name)
  }

  private fun testEvent(): Event {
    val start = ZonedDateTime.now(ZoneId.of("Europe/Zurich")).plusDays(2)
    return Event(
        eventId = "event-1",
        ownerId = "member-1",
        title = "Festival",
        description = "Bring supplies",
        members = listOf(User(uid = "member-1")),
        startDate = start,
        endDate = start.plusHours(3),
        location = "Main square",
        tasks =
            listOf(
                Task(
                    taskId = "task-1",
                    eventId = "event-1",
                    title = "Set up stage",
                    ownerId = "organizer",
                )
            ),
        closed = true,
        announcements =
            listOf(
                Announcement(
                    announcementId = "announcement-1",
                    title = "Welcome",
                    authorId = "organizer",
                    updatedAt = start,
                )
            ),
    )
  }

  private fun createViewModel(repository: EventsRepository) =
      EditEventViewModel(
          DataRepositoryLocal(FakeAuthRepository(), eventsRepository = repository),
          { signedInUserId },
      )
}
