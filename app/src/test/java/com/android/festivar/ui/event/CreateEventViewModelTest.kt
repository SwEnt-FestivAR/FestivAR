package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.task.create.END_OF_DAY
import com.android.festivar.ui.task.create.START_OF_DAY
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateEventViewModelTest {
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var usersRepository: UsersRepositoryLocal
  private val creator = User(uid = "creator-123", name = "Alice", surname = "AliceInWonderland")

  private lateinit var viewModel: CreateEventViewModel

  private val startDate = LocalDate.now()
  private val endDate = LocalDate.now().plusDays(1)
  private val startTime = LocalTime.MIDNIGHT
  private val endTime = LocalTime.of(23, 0)

  @Before
  fun setUp() {
    eventsRepository = EventsRepositoryLocal()
    usersRepository = UsersRepositoryLocal(listOf(creator))
    viewModel = CreateEventViewModel(eventsRepository, usersRepository, creator.uid)
  }

  /** Red if updating every event field does not persist the entered values. */
  @Test
  fun updateFieldsWorks() {
    fillAllFields()

    val state = viewModel.uiState.value
    assertEquals("Summer festival", state.name)
    assertEquals(startDate, state.startDate)
    assertEquals(endDate, state.endDate)
    assertEquals(startTime, state.startTime)
    assertEquals(endTime, state.endTime)
    assertEquals("Main square", state.venue)
    assertEquals("Bring the stage equipment", state.notes)
  }

  /** Red if any date/time picker fails to update its field or close afterward. */
  @Test
  fun pickerActionsUpdateAndCloseThePicker() {
    viewModel.showPicker(DateTimeField.START_DATE)
    assertEquals(DateTimeField.START_DATE, viewModel.uiState.value.openPicker)

    viewModel.pickDate(startDate)
    assertEquals(startDate, viewModel.uiState.value.startDate)
    assertNull(viewModel.uiState.value.openPicker)

    viewModel.showPicker(DateTimeField.END_DATE)
    assertEquals(DateTimeField.END_DATE, viewModel.uiState.value.openPicker)

    viewModel.pickDate(endDate)
    assertEquals(endDate, viewModel.uiState.value.endDate)
    assertNull(viewModel.uiState.value.openPicker)

    viewModel.showPicker(DateTimeField.START_TIME)
    assertEquals(DateTimeField.START_TIME, viewModel.uiState.value.openPicker)

    viewModel.pickTime(startTime)
    assertEquals(startTime, viewModel.uiState.value.startTime)
    assertNull(viewModel.uiState.value.openPicker)

    viewModel.showPicker(DateTimeField.END_TIME)
    assertEquals(DateTimeField.END_TIME, viewModel.uiState.value.openPicker)

    viewModel.pickTime(endTime)
    assertEquals(endTime, viewModel.uiState.value.endTime)
    assertNull(viewModel.uiState.value.openPicker)
  }

  /** Red if the initial event form is considered valid. */
  @Test
  fun initialStateIsInvalid() {
    assertFalse(viewModel.uiState.value.isValid)
  }

  /** Red if the initial event form can be submitted. */
  @Test
  fun initialStateCannotCreateEvent() {
    assertFalse(viewModel.uiState.value.canCreate)
  }

  /** Red if a complete valid event form is considered invalid. */
  @Test
  fun validFormIsValid() {
    fillRequiredFields()
    assertTrue(viewModel.uiState.value.isValid)
  }

  /** Red if a complete valid event form cannot be submitted. */
  @Test
  fun validFormCanCreateEvent() {
    fillRequiredFields()
    assertTrue(viewModel.uiState.value.canCreate)
  }

  /** Red if converting an invalid event form does not throw an IllegalArgumentException. */
  @Test
  fun toEventThrowsWhenFormIsInvalid() {
    assertThrows(IllegalArgumentException::class.java) {
      viewModel.uiState.value.toEvent("event-123", emptyList())
    }
  }

  /** Red if converting valid form fields does not create the expected event. */
  @Test
  fun toEventCreatesEventFromFields() {
    fillAllFields()
    val members = listOf(creator)

    val event = viewModel.uiState.value.toEvent("event-123", members)

    assertEquals("event-123", event.eventId)
    assertEquals("Summer festival", event.title)
    assertEquals("Bring the stage equipment", event.description)
    assertEquals(members, event.members)
    assertEquals(viewModel.uiState.value.startDateTime, event.startDate)
    assertEquals(viewModel.uiState.value.endDateTime, event.endDate)
    assertEquals("Main square", event.location)
  }

  /** Red if the untouched event form displays a field error. */
  @Test
  fun initialScreenStateDoesNotShowFieldErrors() {
    val state = viewModel.uiState.value

    assertNull(state.nameError)
    assertNull(state.startDateError)
    assertNull(state.endDateError)
  }

  /** Red if missing times do not default to the start and end of their dates. */
  @Test
  fun dateTimesUseDefaultTimesWhenTimesAreNotSet() {
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)

    val state = viewModel.uiState.value
    assertEquals(START_OF_DAY, state.startDateTime?.toLocalTime())
    assertEquals(END_OF_DAY, state.endDateTime?.toLocalTime())
  }

  /** Red if an invalid, saving, or already-created event can be submitted. */
  @Test
  fun canCreateIsFalse_withInvalidForm_orSavingEventOrAlreadyCreated() {
    val validState =
        viewModel.uiState.value.copy(
            name = "Summer festival",
            startDate = startDate,
            endDate = endDate,
        )

    assertFalse(validState.copy(name = "").canCreate)
    assertFalse(validState.copy(isSaving = true).canCreate)
    assertFalse(validState.copy(createdEventId = "event-123").canCreate)
  }

  /** Red if clearing an edited event name does not report an empty-name error. */
  @Test
  fun blankNameIsReportedAfterEditing() {
    viewModel.updateName("")
    assertEquals(FieldError.EMPTY_EVENT_NAME, viewModel.uiState.value.nameError)
  }

  /** Red if an end date before the start date is not reported as invalid. */
  @Test
  fun invalidDateRangeIsReported() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(endDate)
    viewModel.updateEndDate(startDate)

    assertEquals(FieldError.END_BEFORE_START, viewModel.uiState.value.endDateError)
    assertFalse(viewModel.uiState.value.isValid)
  }

  /** Red if creating a valid event does not add it to the repository. */
  @Test
  fun createEventAddsTheFormEventToTheRepository() = runBlocking {
    fillRequiredFields()
    viewModel.createEvent()

    val events = waitForEvents(eventsRepository)
    assertEquals(1, events.size)
    assertEquals("Summer festival", events.single().title)
    assertEquals(startDate, events.single().startDate.toLocalDate())
    assertEquals(endDate, events.single().endDate.toLocalDate())
  }

  /** Red if creating an event does not add its creator to membership data. */
  @Test
  fun createEventAddsTheCreatorToMembersAndJoinedEvents() = runBlocking {
    fillAllFields()

    viewModel.createEvent()

    val event = waitForEvents(eventsRepository).single()
    assertEquals(listOf(creator), event.members)
    assertTrue(usersRepository.getUser(creator.uid).joinedEvents.contains(event.eventId))
  }

  /** Red if a failed event save does not show an error and preserve the form. */
  @Test
  fun createEventFailureShowsAnErrorAndKeepsTheForm() = runBlocking {
    val failingRepository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun addEvent(event: Event) {
            error("offline")
          }
        }
    val failingViewModel = CreateEventViewModel(failingRepository, usersRepository, creator.uid)
    viewModel = failingViewModel

    fillAllFields()

    failingViewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (failingViewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }

    assertEquals(R.string.create_event_error_save_failed, failingViewModel.uiState.value.errorMsg)
    assertFalse(failingViewModel.uiState.value.isSaving)
    assertEquals("Summer festival", failingViewModel.uiState.value.name)
    assertEquals(startDate, failingViewModel.uiState.value.startDate)
    assertEquals(endDate, failingViewModel.uiState.value.endDate)
    assertEquals(startTime, failingViewModel.uiState.value.startTime)
    assertEquals(endTime, failingViewModel.uiState.value.endTime)
    assertEquals("Main square", failingViewModel.uiState.value.venue)
    assertEquals("Bring the stage equipment", failingViewModel.uiState.value.notes)
  }

  /** Red if a missing user ID does not fail before writing an event. */
  @Test
  fun createEventWithBlankUserIdShowsAnErrorAndDoesNotAddAnEvent() = runBlocking {
    viewModel = CreateEventViewModel(eventsRepository, usersRepository, userId = "")
    fillRequiredFields()

    viewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }

    assertEquals(
        R.string.create_event_error_save_failed,
        viewModel.uiState.value.errorMsg,
    )
    assertFalse(viewModel.uiState.value.isSaving)
    assertTrue(eventsRepository.getAllEvents().isEmpty())
  }

  /**
   * Red if a cancellation from the repository is converted into a save error instead of being
   * propagated.
   */
  @Test
  fun createEventPropagatesCancellationExceptionIfThrown() = runBlocking {
    val cancellationStarted = CompletableDeferred<Unit>()
    val cancelledRepository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun addEvent(event: Event) {
            cancellationStarted.complete(Unit)
            throw CancellationException("cancelled")
          }
        }
    val cancelledViewModel = CreateEventViewModel(cancelledRepository, usersRepository, creator.uid)
    cancelledViewModel.updateName("Summer festival")
    cancelledViewModel.updateStartDate(startDate)
    cancelledViewModel.updateEndDate(endDate)

    cancelledViewModel.createEvent()
    withTimeout(5_000.milliseconds) { cancellationStarted.await() }
    delay(10.milliseconds)

    assertNull(cancelledViewModel.uiState.value.errorMsg)
  }

  private fun fillRequiredFields() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
  }

  private fun fillAllFields() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateStartTime(startTime)
    viewModel.updateEndTime(endTime)
    viewModel.updateVenue("Main square")
    viewModel.updateNotes("Bring the stage equipment")
  }

  private suspend fun waitForEvents(repository: EventsRepository): List<Event> {
    var events = emptyList<Event>()
    withTimeout(5_000.milliseconds) {
      while (events.isEmpty()) {
        events = repository.getAllEvents()
        if (events.isEmpty()) delay(10.milliseconds)
      }
    }
    return events
  }
}
