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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
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
  private lateinit var viewModel: CreateEventViewModel

  private val zone = ZoneId.of("UTC")
  private val startDate = LocalDate.of(2026, 11, 1)
  private val endDate = startDate.plusDays(1)

  @Before
  fun setUp() {
    viewModel = CreateEventViewModel(EventsRepositoryLocal())
  }

  @Test
  fun updateFieldsWorks() {
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateStartTime(LocalTime.of(10, 0))
    viewModel.updateEndDate(endDate)
    viewModel.updateEndTime(LocalTime.of(18, 0))
    viewModel.updateVenue("Main square")
    viewModel.updateNotes("Bring the stage equipment")

    val state = viewModel.uiState.value
    assertEquals("Summer festival", state.name)
    assertEquals(startDate, state.startDate)
    assertEquals(LocalTime.of(10, 0), state.startTime)
    assertEquals(endDate, state.endDate)
    assertEquals(LocalTime.of(18, 0), state.endTime)
    assertEquals("Main square", state.venue)
    assertEquals("Bring the stage equipment", state.notes)
  }

  @Test
  fun pickerActionsUpdateAndCloseThePicker() {
    viewModel.showPicker(DateTimeField.START_DATE)
    viewModel.pickDate(startDate)
    assertEquals(startDate, viewModel.uiState.value.startDate)
    assertNull(viewModel.uiState.value.openPicker)

    viewModel.showPicker(DateTimeField.START_TIME)
    viewModel.pickTime(LocalTime.of(10, 0))
    assertEquals(LocalTime.of(10, 0), viewModel.uiState.value.startTime)
    assertNull(viewModel.uiState.value.openPicker)
  }

  @Test
  fun missingDateForPickedTimeIsReported() {
    viewModel.updateName("Festival")
    viewModel.updateStartTime(LocalTime.of(10, 0))
    viewModel.updateEndDate(endDate)
    viewModel.setErrors()

    assertEquals(FieldError.MISSING_DATE, viewModel.uiState.value.startDateError)
    assertFalse(viewModel.uiState.value.isValid)
  }

  @Test
  fun equalOrReversedDateTimeRangeIsReported() {
    viewModel.updateName("Festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateStartTime(LocalTime.of(10, 0))
    viewModel.updateEndDate(startDate)
    viewModel.updateEndTime(LocalTime.of(10, 0))
    viewModel.setErrors()

    assertEquals(FieldError.END_BEFORE_START, viewModel.uiState.value.endDateError)
    assertFalse(viewModel.uiState.value.isValid)
  }

  @Test
  fun pastDateTimeRangeIsReported() {
    val past = LocalDate.of(2020, 1, 1)
    viewModel.updateName("Festival")
    viewModel.updateStartDate(past)
    viewModel.updateStartTime(LocalTime.NOON)
    viewModel.updateEndDate(past)
    viewModel.updateEndTime(LocalTime.of(13, 0))
    viewModel.setErrors()

    assertEquals(FieldError.OUTDATED, viewModel.uiState.value.endDateError)
    assertFalse(viewModel.uiState.value.isValid)
  }

  @Test
  fun validDateTimeRangeIsAccepted() {
    viewModel.updateName("Festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateStartTime(LocalTime.of(10, 0))
    viewModel.updateEndDate(endDate)
    viewModel.updateEndTime(LocalTime.of(18, 0))
    viewModel.updateVenue("Main square")

    assertTrue(viewModel.uiState.value.isValid)
    assertTrue(viewModel.validEvent())
  }

  @Test
  fun missingRequiredFieldsAreReportedAfterCreateAttempt() {
    viewModel.createEvent()

    val state = viewModel.uiState.value
    assertTrue(state.validationRequested)
    assertEquals(FieldError.EMPTY_EVENT_NAME, state.nameError)
    assertTrue(state.venueError)
    assertFalse(viewModel.validEvent())
  }

  @Test
  fun dateTimesDefaultToWholeDayWhenTimesAreNotSet() {
    val state =
        CreateEventUIState(
            name = "Festival",
            startDate = startDate,
            endDate = endDate,
            venue = "Main square",
            zone = zone,
        )

    assertEquals(LocalTime.MIDNIGHT, state.startDateTime?.toLocalTime())
    assertEquals(LocalTime.of(23, 59), state.endDateTime?.toLocalTime())
  }

  @Test
  fun createEventAddsTheCreatorToMembersAndJoinedEvents() = runBlocking {
    val eventsRepository = EventsRepositoryLocal()
    val creator = User(uid = "creator-123")
    val usersRepository = UsersRepositoryLocal(listOf(creator))
    val creatorViewModel =
        CreateEventViewModel(eventsRepository, usersRepository, creator.uid).apply {
          fillRequiredFields()
        }

    creatorViewModel.createEvent()
    waitUntil { creatorViewModel.uiState.value.createdEventId != null }

    val event = eventsRepository.getAllEvents().single()
    assertEquals(listOf(creator), event.members)
    assertEquals(listOf(event.eventId), usersRepository.getUser(creator.uid).joinedEvents)
  }

  @Test
  fun createEventWithMissingCreatorProfileFailsWithoutAddingAnEvent() = runBlocking {
    val eventsRepository = EventsRepositoryLocal()
    val creatorViewModel =
        CreateEventViewModel(
                eventsRepository,
                UsersRepositoryLocal(),
                userId = "missing-creator",
            )
            .apply { fillRequiredFields() }

    creatorViewModel.createEvent()
    waitUntil { creatorViewModel.uiState.value.errorMsg != null }

    assertEquals(R.string.create_event_error_save_failed, creatorViewModel.uiState.value.errorMsg)
    assertTrue(eventsRepository.getAllEvents().isEmpty())
  }

  @Test
  fun createEventWithBlankUserIdShowsAnErrorAndDoesNotAddAnEvent() = runBlocking {
    val eventsRepository = EventsRepositoryLocal()
    val creatorViewModel =
        CreateEventViewModel(
                eventsRepository,
                UsersRepositoryLocal(),
                userId = "",
            )
            .apply { fillRequiredFields() }

    creatorViewModel.createEvent()
    waitUntil { creatorViewModel.uiState.value.errorMsg != null }

    assertEquals(R.string.create_event_error_save_failed, creatorViewModel.uiState.value.errorMsg)
    assertFalse(creatorViewModel.uiState.value.isSaving)
    assertTrue(eventsRepository.getAllEvents().isEmpty())
  }

  @Test
  fun createEventPropagatesCancellationException() = runBlocking {
    val cancellationStarted = CompletableDeferred<Unit>()
    val cancelledRepository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun addEvent(event: Event) {
            cancellationStarted.complete(Unit)
            throw CancellationException("cancelled")
          }
        }
    val creator = User(uid = "creator-123")
    val creatorViewModel =
        CreateEventViewModel(
                cancelledRepository,
                UsersRepositoryLocal(listOf(creator)),
                creator.uid,
            )
            .apply { fillRequiredFields() }

    creatorViewModel.createEvent()
    withTimeout(5_000) { cancellationStarted.await() }
    delay(50)

    assertNull(creatorViewModel.uiState.value.errorMsg)
  }

  private fun CreateEventViewModel.fillRequiredFields() {
    updateName("Summer festival")
    updateStartDate(startDate)
    updateEndDate(endDate)
    updateVenue("Main square")
  }

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(5_000) {
      while (!condition()) {
        delay(10)
      }
    }
  }
}
