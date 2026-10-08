package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds
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
}
