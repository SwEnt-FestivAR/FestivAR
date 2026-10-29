package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.FieldError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
}
