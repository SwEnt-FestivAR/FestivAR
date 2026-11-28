package com.android.festivar.ui.event.edit

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.images.ImageStorage
import com.android.festivar.model.images.StoredImage
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.datetime.DateTimeRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.ByteArrayInputStream
import java.io.File
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditEventViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val initialEvent = testEvent()
  private var signedInUserId = initialEvent.ownerId
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var viewModel: EditEventViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    signedInUserId = initialEvent.ownerId
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
    assertEquals(initialEvent.startDate.toLocalDate(), state.range.startDate)
    assertEquals(initialEvent.endDate.toLocalDate(), state.range.endDate)
    assertEquals(initialEvent.startDate.toLocalTime(), state.range.startTime)
    assertEquals(initialEvent.endDate.toLocalTime(), state.range.endTime)
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

  /** Breaks if updating the range does not replace it whole, or does not count as a change. */
  @Test
  fun updateRangeReplacesTheRangeAndMarksTheFormChanged() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    val loaded = viewModel.uiState.value.range
    val newRange =
        loaded.copy(
            startDate = loaded.startDate!!.plusDays(1),
            endDate = loaded.endDate!!.plusDays(2),
        )

    viewModel.updateRange(newRange)

    assertEquals(newRange, viewModel.uiState.value.range)
    assertTrue(viewModel.uiState.value.hasChanges)
  }

  @Test
  fun initialStateValidationAndEventConversionAreSafe() {
    val state = EditEventUIState()

    assertNull(state.startDateTime)
    assertNull(state.endDateTime)
    assertNull(state.nameError)
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

  /** Breaks if changing any one of the four parts of the range is not counted as a change. */
  @Test
  fun changingEachDateOrTimeFieldMarksDraftAsChanged() {
    val state = EditEventUIState().load(initialEvent)

    assertTrue(
        state
            .copy(range = state.range.copy(startDate = state.range.startDate!!.plusDays(1)))
            .hasChanges
    )
    assertTrue(
        state.copy(range = state.range.copy(endDate = state.range.endDate!!.plusDays(1))).hasChanges
    )
    assertTrue(
        state
            .copy(range = state.range.copy(startTime = state.range.startTime!!.plusHours(1)))
            .hasChanges
    )
    assertTrue(
        state
            .copy(range = state.range.copy(endTime = state.range.endTime!!.plusHours(1)))
            .hasChanges
    )
  }

  /** Breaks if an end time with no end date is not reported as a missing date. */
  @Test
  fun endDateErrorRequiresDateWhenEndTimeIsSet() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.value.let { it.copy(range = it.range.copy(endDate = null)) }

    assertEquals(FieldError.MISSING_DATE, state.endDateError)
  }

  /** Breaks if an event that already ended is reported as an error, so it could not be edited. */
  @Test
  fun endDateErrorAcceptsAnEndInThePast() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val lastDay = LocalDate.of(2020, 3, 10)
    val state =
        viewModel.uiState.value.copy(
            range =
                DateTimeRange(
                    startDate = lastDay.minusDays(1),
                    startTime = LocalTime.NOON,
                    endDate = lastDay,
                    endTime = LocalTime.NOON,
                ),
        )

    assertNull(state.endDateError)
  }

  /** Breaks if an event that already ended cannot be saved after its title is changed. */
  @Test
  fun anEventThatEndedCanBeSavedAfterAChange() {
    val ended =
        initialEvent.copy(
            startDate = ZonedDateTime.of(2020, 3, 8, 12, 0, 0, 0, ZoneId.of("Europe/Zurich")),
            endDate = ZonedDateTime.of(2020, 3, 9, 12, 0, 0, 0, ZoneId.of("Europe/Zurich")),
        )
    val state = EditEventUIState().load(ended)

    assertFalse(state.canSave)
    assertTrue(state.copy(name = "Renamed").canSave)
  }

  /** Breaks if the end is read in the zone of the start, which would put it before the start. */
  @Test
  fun endDateErrorUsesTheEndZone() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    val startDate = LocalDate.of(2026, 11, 6)
    val state =
        viewModel.uiState.value.copy(
            range =
                DateTimeRange(
                    startDate = startDate,
                    startTime = LocalTime.of(23, 0),
                    endDate = startDate,
                    endTime = LocalTime.of(17, 0),
                ),
            endZone = ZoneId.of("America/Los_Angeles"),
        )

    assertNull(state.endDateError)
  }

  /** Breaks if an end before the start, on the same day, is not reported as an error. */
  @Test
  fun endDateErrorRejectsEndBeforeStart() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val tomorrow = LocalDate.now().plusDays(1)
    val state =
        viewModel.uiState.value.copy(
            range =
                DateTimeRange(
                    startDate = tomorrow,
                    startTime = LocalTime.of(15, 0),
                    endDate = tomorrow,
                    endTime = LocalTime.of(14, 0),
                ),
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

  /**
   * Breaks if saving the form drops the members, roles or tasks added elsewhere while it was open.
   */
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
    val changedElsewhere = initialEvent.addMember(joinedMember).copy(tasks = listOf(addedTask))
    eventsRepository.editEvent(initialEvent.eventId, changedElsewhere)

    viewModel.saveChanges()
    advanceUntilIdle()

    val saved = eventsRepository.getEvent(initialEvent.eventId)
    assertEquals("Updated festival", saved.title)
    assertEquals(listOf(User("member-1"), joinedMember), saved.members)
    assertEquals(changedElsewhere.roles, saved.roles)
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

  /** Breaks if a member whose role cannot edit the event still gets the form loaded. */
  @Test
  fun fetchEventByMemberWithoutEditRightDoesNotLoadTheForm() = runTest {
    useEventWithSignedInMember(Role.TEAM_LEAD)

    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.canEditEvent)
    assertNull(state.event)
    assertFalse(state.isLoading)
    assertNull(state.errorFetchMsg)
  }

  /** Breaks if a user who is not a member of the event crashes the load or gets a fetch error. */
  @Test
  fun fetchEventByNonMemberIsRefusedWithoutFetchError() = runTest {
    signedInUserId = "stranger"

    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.canEditEvent)
    assertNull(state.event)
    assertFalse(state.isLoading)
    assertNull(state.errorFetchMsg)
  }

  /** Breaks if a member whose role has the edit right, without owning the event, cannot edit it. */
  @Test
  fun staffManagerCanLoadAndSaveTheEvent() = runTest {
    useEventWithSignedInMember(Role.STAFF_MANAGER)

    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")
    viewModel.saveChanges()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.canEditEvent)
    assertEquals(initialEvent.eventId, viewModel.uiState.value.savedEventId)
    assertEquals("Updated festival", eventsRepository.getEvent(initialEvent.eventId).title)
  }

  /** Breaks if a member who lost the edit right while the form was open can still save it. */
  @Test
  fun saveChangesAfterLosingTheEditRightWritesNothing() = runTest {
    val event = useEventWithSignedInMember(Role.STAFF_MANAGER)
    viewModel.fetchEvent(event.eventId)
    advanceUntilIdle()
    viewModel.updateName("Updated festival")
    val demoted = event.setRole(event.ownerId, signedInUserId, Role.TEAM_LEAD)
    eventsRepository.editEvent(event.eventId, demoted)

    viewModel.saveChanges()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(demoted, eventsRepository.getEvent(event.eventId))
    assertFalse(state.canEditEvent)
    assertFalse(state.isSaving)
    assertNull(state.savedEventId)
  }

  /** Breaks if fetching an event with an image URL does not prefill it into the state. */
  @Test
  fun fetchEventPrefillsImageUrl() = runTest {
    val eventWithImage = initialEvent.copy(imageUrl = "https://example.com/event.jpg")
    eventsRepository = EventsRepositoryLocal(listOf(eventWithImage))
    viewModel = createViewModel(eventsRepository)

    viewModel.fetchEvent(eventWithImage.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("https://example.com/event.jpg", state.imageUrl)
    assertTrue(state.hasImage)
    assertFalse(state.hasChanges)
  }

  /** Breaks if changing or clearing the image URL is not recognized as a draft change. */
  @Test
  fun changingImageMarksDraftAsChanged() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.hasChanges)

    viewModel.setImageUrlForTesting("https://example.com/new.jpg")
    assertTrue(viewModel.uiState.value.hasChanges)

    viewModel.setImageUrlForTesting(null)
    assertFalse(viewModel.uiState.value.hasChanges)
  }

  /** Breaks if requesting delete image when no image is present opens the delete dialog. */
  @Test
  fun requestDeleteImageIgnoredWhenNoImage() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    viewModel.requestDeleteImage()
    assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)
  }

  /** Breaks if deleting the image cannot be requested, cancelled, or confirmed. */
  @Test
  fun deleteImageConfirmationDialogFlow() = runTest {
    val eventWithImage = initialEvent.copy(imageUrl = "https://example.com/event.jpg")
    eventsRepository = EventsRepositoryLocal(listOf(eventWithImage))
    viewModel = createViewModel(eventsRepository)
    viewModel.fetchEvent(eventWithImage.eventId)
    advanceUntilIdle()

    viewModel.requestDeleteImage()
    assertTrue(viewModel.uiState.value.showDeleteImageConfirmation)

    viewModel.cancelDeleteImage()
    assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)
    assertEquals("https://example.com/event.jpg", viewModel.uiState.value.imageUrl)

    viewModel.requestDeleteImage()
    viewModel.confirmDeleteImage()
    assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)
    assertNull(viewModel.uiState.value.imageUrl)
    assertTrue(viewModel.uiState.value.hasChanges)
  }

  /**
   * Breaks if saving changes with a modified image URL does not persist the image URL to the
   * repository.
   */
  @Test
  fun saveChangesPersistsModifiedImageUrl() = runTest {
    viewModel.fetchEvent(initialEvent.eventId)
    advanceUntilIdle()

    viewModel.setImageUrlForTesting("https://example.com/updated.jpg")
    viewModel.saveChanges()
    advanceUntilIdle()

    val saved = eventsRepository.getEvent(initialEvent.eventId)
    assertEquals("https://example.com/updated.jpg", saved.imageUrl)
  }

  /**
   * Breaks if updateImage fails to upload selected image and set error state when storage fails.
   */
  @Test
  fun updateImageHandlesUploadError() = runTest {
    withMockedExtension {
      val imageStorage = RecordingImageStorage(uploadError = RuntimeException("upload error"))
      val dataRepo =
          DataRepositoryLocal(
              FakeAuthRepository(),
              eventsRepository = eventsRepository,
              imageStorage = imageStorage,
          )
      val testDispatcher = StandardTestDispatcher(testScheduler)
      viewModel = EditEventViewModel(dataRepo, { signedInUserId }, ioDispatcher = testDispatcher)
      viewModel.fetchEvent(initialEvent.eventId)
      advanceUntilIdle()

      val (resolver, uri) = createMockResolver()
      viewModel.updateImage(uri, resolver, createTempTestDir())
      advanceUntilIdle()

      assertEquals(R.string.edit_event_error_save_failed, viewModel.uiState.value.errorMsg)
      assertFalse(viewModel.uiState.value.isSaving)
    }
  }

  /** Breaks if updateImage updates image URL and deletes previous image on replacement. */
  @Test
  fun updateImageUpdatesImageUrlAndDeletesPrevious() = runTest {
    withMockedExtension {
      val imageStorage = RecordingImageStorage()
      val dataRepo =
          DataRepositoryLocal(
              FakeAuthRepository(),
              eventsRepository = eventsRepository,
              imageStorage = imageStorage,
          )
      val testDispatcher = StandardTestDispatcher(testScheduler)
      viewModel = EditEventViewModel(dataRepo, { signedInUserId }, ioDispatcher = testDispatcher)
      viewModel.fetchEvent(initialEvent.eventId)
      advanceUntilIdle()
      viewModel.setImageUrlForTesting("https://example.com/old.jpg")

      val (resolver, uri) = createMockResolver()
      viewModel.updateImage(uri, resolver, createTempTestDir())
      advanceUntilIdle()

      assertNotNull(viewModel.uiState.value.imageUrl)
      assertEquals(listOf("https://example.com/old.jpg"), imageStorage.deletedUrls)
      assertFalse(viewModel.uiState.value.isSaving)
    }
  }

  /**
   * Breaks if updateImage sets profile picture too large error when image file exceeds size limit.
   */
  @Test
  fun updateImageRejectsImageWhenTooLarge() = runTest {
    withMockedExtension {
      val largeData = ByteArray(11 * 1024 * 1024)
      val (resolver, uri) = createMockResolver(bytes = largeData)
      val testDispatcher = StandardTestDispatcher(testScheduler)
      val dataRepo =
          DataRepositoryLocal(
              FakeAuthRepository(),
              eventsRepository = eventsRepository,
          )
      viewModel = EditEventViewModel(dataRepo, { signedInUserId }, ioDispatcher = testDispatcher)
      viewModel.fetchEvent(initialEvent.eventId)
      advanceUntilIdle()

      viewModel.updateImage(uri, resolver, createTempTestDir())
      advanceUntilIdle()

      assertEquals(R.string.profile_picture_too_large, viewModel.uiState.value.errorMsg)
      assertFalse(viewModel.uiState.value.isSaving)
    }
  }

  /** Breaks if updateImage sets save error when input stream cannot be opened. */
  @Test
  fun updateImageHandlesInputStreamError() = runTest {
    withMockedExtension {
      val (resolver, uri) = createMockResolver(readException = RuntimeException("read error"))
      val testDispatcher = StandardTestDispatcher(testScheduler)
      val dataRepo =
          DataRepositoryLocal(
              FakeAuthRepository(),
              eventsRepository = eventsRepository,
          )
      viewModel = EditEventViewModel(dataRepo, { signedInUserId }, ioDispatcher = testDispatcher)
      viewModel.fetchEvent(initialEvent.eventId)
      advanceUntilIdle()

      viewModel.updateImage(uri, resolver, createTempTestDir())
      advanceUntilIdle()

      assertEquals(R.string.edit_event_error_save_failed, viewModel.uiState.value.errorMsg)
      assertFalse(viewModel.uiState.value.isSaving)
    }
  }

  private inline fun <T> withMockedExtension(
      extension: String = "png",
      block: () -> T,
  ): T {
    mockkStatic(MimeTypeMap::class)
    val mimeTypeMap = mockk<MimeTypeMap>()
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType(any()) } returns extension
    try {
      return block()
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  private fun createMockResolver(
      mimeType: String? = "image/png",
      bytes: ByteArray = "test image content".toByteArray(),
      readException: Throwable? = null,
  ): Pair<ContentResolver, Uri> {
    val resolver = mockk<ContentResolver>()
    val uri = mockk<Uri>()
    every { resolver.getType(uri) } returns mimeType
    if (readException != null) {
      every { resolver.openInputStream(uri) } throws readException
    } else {
      every { resolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)
    }
    return resolver to uri
  }

  private fun createTempTestDir(): File {
    val tempDir = File.createTempFile("test", "dir")
    tempDir.delete()
    tempDir.mkdir()
    tempDir.deleteOnExit()
    return tempDir
  }

  /**
   * Stores [initialEvent] with the signed-in user as a member of [role], given by the owner, and
   * recreates [viewModel] on it.
   *
   * @return The stored event.
   */
  private fun useEventWithSignedInMember(role: Role): Event {
    signedInUserId = "member-2"
    val event =
        initialEvent.addMember(User(uid = signedInUserId)).let {
          it.setRole(it.ownerId, signedInUserId, role)
        }
    eventsRepository = EventsRepositoryLocal(listOf(event))
    viewModel = createViewModel(eventsRepository)
    return event
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

private class RecordingImageStorage(
    var uploadError: Throwable? = null,
    var deleteError: Throwable? = null,
) : ImageStorage {
  val uploadedImages = mutableListOf<Pair<StoredImage, String>>()
  val deletedUrls = mutableListOf<String>()

  override fun getNewUid(): String = "image-uid"

  override suspend fun upload(storedImage: StoredImage, subFolder: String): String {
    uploadError?.let { throw it }
    val url = "https://example.com/${storedImage.file.name}"
    uploadedImages.add(storedImage to subFolder)
    return url
  }

  override suspend fun delete(imageUrl: String) {
    deleteError?.let { throw it }
    deletedUrls.add(imageUrl)
  }
}
