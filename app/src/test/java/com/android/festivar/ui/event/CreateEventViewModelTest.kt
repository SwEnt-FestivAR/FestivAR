package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.event.create.CreateEventUIState
import com.android.festivar.ui.event.create.CreateEventViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateEventViewModelTest {
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var usersRepository: UsersRepositoryLocal
  private lateinit var dataRepository: DataRepositoryLocal
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
    dataRepository =
        DataRepositoryLocal(eventsRepository = eventsRepository, usersRepository = usersRepository)
    viewModel = CreateEventViewModel(dataRepository, creator.uid)
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

  /** Red if picking a date without a date picker leaves an unintended date behind. */
  @Test
  fun pickDateWithoutDatePickerOnlyClosesThePicker() {
    viewModel.showPicker(DateTimeField.START_TIME)

    viewModel.pickDate(startDate)

    assertNull(viewModel.uiState.value.startDate)
    assertNull(viewModel.uiState.value.endDate)
    assertNull(viewModel.uiState.value.openPicker)
  }

  /** Red if picking a time without a time picker writes to an unintended field. */
  @Test
  fun pickTimeWithoutTimePickerOnlyClosesThePicker() {
    viewModel.showPicker(DateTimeField.START_DATE)

    viewModel.pickTime(startTime)

    assertNull(viewModel.uiState.value.startTime)
    assertNull(viewModel.uiState.value.endTime)
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

  /** Red if submitting an invalid form changes the saving state or writes an event. */
  @Test
  fun createEventDoesNothingWhenFormIsInvalid() = runBlocking {
    viewModel.createEvent()

    delay(10.milliseconds)

    assertFalse(viewModel.uiState.value.isSaving)
    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(eventsRepository.getAllEvents().isEmpty())
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

  /** Red if explicitly selected start and end times are ignored by the computed date-times. */
  @Test
  fun dateTimesUseSelectedTimesWhenTimesAreSet() {
    val selectedStartTime = LocalTime.of(14, 30)
    val selectedEndTime = LocalTime.of(18, 0)
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    viewModel.updateStartTime(selectedStartTime)
    viewModel.updateEndTime(selectedEndTime)

    val state = viewModel.uiState.value
    assertEquals(selectedStartTime, state.startDateTime?.toLocalTime())
    assertEquals(selectedEndTime, state.endDateTime?.toLocalTime())
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

  /** Red if an end date in the past is accepted as a valid event date. */
  @Test
  fun outdatedEndDateIsReported() {
    val oldStartDate = LocalDate.now().minusDays(2)
    val oldEndDate = LocalDate.now().minusDays(1)
    val state =
        CreateEventUIState(
            name = "Summer festival",
            startDate = oldStartDate,
            endDate = oldEndDate,
        )

    assertEquals(FieldError.OUTDATED, state.endDateError)
    assertFalse(state.isValid)
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
    assertFalse(viewModel.uiState.value.isSaving)
    assertTrue(viewModel.uiState.value.createdEventId != null)
  }

  /** Red if calling createEvent twice creates more than one event. */
  @Test
  fun createEventDoesNotCreateAnotherEventAfterSuccess() = runBlocking {
    fillRequiredFields()

    viewModel.createEvent()
    waitForEvents(eventsRepository)

    viewModel.createEvent()
    delay(10.milliseconds)

    assertEquals(1, eventsRepository.getAllEvents().size)
  }

  /** Red if a second save starts while the first save is still running. */
  @Test
  fun createEventDoesNotStartAnotherSaveWhileSaving() = runBlocking {
    val gate = CompletableDeferred<Unit>()
    val localRepository = EventsRepositoryLocal()
    val gatedRepository =
        object : EventsRepository by localRepository {
          override suspend fun addEvent(event: Event) {
            gate.await()
            localRepository.addEvent(event)
          }
        }
    val gatedViewModel =
        CreateEventViewModel(
            DataRepositoryLocal(
                eventsRepository = gatedRepository,
                usersRepository = usersRepository,
            ),
            creator.uid,
        )
    gatedViewModel.updateName("Summer festival")
    gatedViewModel.updateStartDate(startDate)
    gatedViewModel.updateEndDate(endDate)

    gatedViewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (!gatedViewModel.uiState.value.isSaving) delay(10.milliseconds)
    }
    gatedViewModel.createEvent()
    gate.complete(Unit)

    withTimeout(5_000.milliseconds) {
      while (gatedViewModel.uiState.value.createdEventId == null) delay(10.milliseconds)
    }

    assertEquals(1, localRepository.getAllEvents().size)
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
    val failingViewModel =
        CreateEventViewModel(
            DataRepositoryLocal(
                eventsRepository = failingRepository,
                usersRepository = usersRepository,
            ),
            creator.uid,
        )
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
    viewModel = CreateEventViewModel(dataRepository, userId = "")
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

  /** Red if editing the form does not clear a stale save error. */
  @Test
  fun editingTheFormClearsTheSaveError() = runBlocking {
    val failingRepository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun addEvent(event: Event) {
            error("offline")
          }
        }
    val failingViewModel =
        CreateEventViewModel(
            DataRepositoryLocal(
                eventsRepository = failingRepository,
                usersRepository = usersRepository,
            ),
            creator.uid,
        )
    failingViewModel.updateName("Summer festival")
    failingViewModel.updateStartDate(startDate)
    failingViewModel.updateEndDate(endDate)

    failingViewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (failingViewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }

    failingViewModel.updateName("Updated festival")

    assertNull(failingViewModel.uiState.value.errorMsg)
  }

  /**
   * Red if a cancellation from `addEvent` is converted into a save error instead of being
   * propagated.
   */
  @Test
  fun createEventPropagatesCancellationExceptionFromAddEvent() = runBlocking {
    val cancelledAddEventRepository =
        object : EventsRepository by eventsRepository {
          override suspend fun addEvent(event: Event) {
            throw CancellationException("Cancelled")
          }
        }
    testCancellationException(cancelledAddEventRepository, usersRepository)
  }

  /**
   * Red if a cancellation from `getUser` is converted into a save error instead of being
   * propagated.
   */
  @Test
  fun createEventPropagatesCancellationExceptionFromGetUser() = runBlocking {
    val cancelledGetUser =
        object : UsersRepository by usersRepository {
          override suspend fun getUser(uId: String): User {
            throw CancellationException("Cancelled")
          }
        }
    testCancellationException(eventsRepository, cancelledGetUser)
  }

  /**
   * Red if a cancellation from `editUser` is converted into a save error instead of being
   * propagated.
   */
  @Test
  fun createEventPropagatesCancellationExceptionFromEditUser() = runBlocking {
    val cancelledEditUser =
        object : UsersRepository by usersRepository {
          override suspend fun editUser(uId: String, newValue: User) {
            throw CancellationException("Cancelled")
          }
        }
    testCancellationException(eventsRepository, cancelledEditUser)
  }

  /** Red if a non-CancellationException from `getUser` is not reported to the user */
  @Test
  fun getUserExceptionIsReportedToUser() = runBlocking {
    val failingGetUser =
        object : UsersRepository by usersRepository {
          override suspend fun getUser(uId: String): User {
            error("offline")
          }
        }
    testNonCancellationException(eventsRepository, failingGetUser)
  }

  /** Red if a non-CancellationException from `editUser` is not reported to the user */
  @Test
  fun editUserExceptionIsReportedToUser() = runBlocking {
    val failingEditUser =
        object : UsersRepository by usersRepository {
          override suspend fun editUser(uId: String, newValue: User) {
            error("offline")
          }
        }
    testNonCancellationException(eventsRepository, failingEditUser)
  }

  /** Red if a failure from `editUser` does not undo the previous `addEvent` operation */
  @Test
  fun failureFromEditUserDeletesEventFromRepository() = runBlocking {
    val started = CompletableDeferred<Unit>()
    val failingEditUser =
        object : UsersRepository by usersRepository {
          override suspend fun editUser(uId: String, newValue: User) {
            started.await()
            error("offline")
          }
        }

    fillFormAndCreateEvent(eventsRepository, failingEditUser)
    withTimeout(5_000.milliseconds) {
      // The event should have been created
      while (eventsRepository.getAllEvents().isEmpty()) delay(10.milliseconds)
    }
    started.complete(Unit)
    withTimeout(5_000.milliseconds) {
      // The event should have been deleted
      while (eventsRepository.getAllEvents().isNotEmpty()) delay(10.milliseconds)
    }
  }

  private fun fillFormAndCreateEvent(
      eventsRepository: EventsRepository,
      usersRepository: UsersRepository,
  ) {
    viewModel =
        CreateEventViewModel(
            DataRepositoryLocal(
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
            ),
            creator.uid,
        )
    fillRequiredFields()
    viewModel.createEvent()
  }

  private suspend fun testCancellationException(
      eventsRepository: EventsRepository,
      usersRepository: UsersRepository,
  ) {
    fillFormAndCreateEvent(eventsRepository, usersRepository)
    delay(10.milliseconds)
    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(eventsRepository.getAllEvents().isEmpty())
  }

  private suspend fun testNonCancellationException(
      eventsRepository: EventsRepository,
      usersRepository: UsersRepository,
  ) {
    fillFormAndCreateEvent(eventsRepository, usersRepository)
    withTimeout(5_000.milliseconds) {
      while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isSaving)
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
