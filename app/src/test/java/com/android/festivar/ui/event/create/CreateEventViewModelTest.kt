@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.images.ImageStorage
import com.android.festivar.model.images.StoredImage
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.END_OF_DAY
import com.android.festivar.ui.components.datetime.START_OF_DAY
import com.android.festivar.ui.event.EventFormViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
        DataRepositoryLocal(
            authRepository = mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
        )
    viewModel = CreateEventViewModel(dataRepository, creator.uid)
  }

  /** Red if updating every event field does not persist the entered values. */
  @Test
  fun updateFieldsWorks() {
    fillAllFields()

    val state = viewModel.uiState.value
    assertEquals("Summer festival", state.name)
    assertEquals(startDate, state.range.startDate)
    assertEquals(endDate, state.range.endDate)
    assertEquals(startTime, state.range.startTime)
    assertEquals(endTime, state.range.endTime)
    assertEquals("Main square", state.venue)
    assertEquals("Bring the stage equipment", state.notes)
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

  /** Breaks if an anonymous account is allowed on the create event form. */
  @Test
  fun anonymousAccountCannotCreateEvent() {
    viewModel = CreateEventViewModel(dataRepository, creator.uid, isAnonymous = { true })

    assertFalse(viewModel.uiState.value.canCreateEvent)
  }

  /** Breaks if a signed-up account is sent away from the create event form. */
  @Test
  fun signedUpAccountCanCreateEvent() {
    assertTrue(viewModel.uiState.value.canCreateEvent)
  }

  /** Red if the ViewModel misses a form field or asks before the user enters anything. */
  @Test
  fun needsDiscardConfirmationTracksEnteredDetails() {
    assertFalse(viewModel.needsDiscardConfirmation)

    val edits: List<(CreateEventViewModel) -> Unit> =
        listOf(
            { it.updateName("Festival") },
            { it.updateRange(DateTimeRange(startDate = startDate)) },
            { it.updateRange(DateTimeRange(endDate = endDate)) },
            { it.updateRange(DateTimeRange(startTime = startTime)) },
            { it.updateRange(DateTimeRange(endTime = endTime)) },
            { it.updateVenue("Main square") },
            { it.updateNotes("Bring equipment") },
        )
    edits.forEach { edit ->
      val editedViewModel = CreateEventViewModel(dataRepository, creator.uid)
      edit(editedViewModel)
      assertTrue(editedViewModel.needsDiscardConfirmation)
    }
  }

  /** Red if discard confirmation does not preserve, cancel, or discard the current draft. */
  @Test
  fun discardRequestCanBeCancelledOrConfirmed() {
    val untouchedViewModel = CreateEventViewModel(dataRepository, creator.uid)
    untouchedViewModel.requestDiscard()
    assertTrue(untouchedViewModel.uiState.value.isDiscarded)
    assertFalse(untouchedViewModel.uiState.value.showDiscardConfirmation)

    viewModel.updateName("Festival")

    viewModel.requestDiscard()
    assertTrue(viewModel.uiState.value.showDiscardConfirmation)
    assertFalse(viewModel.uiState.value.isDiscarded)

    viewModel.cancelDiscard()
    assertFalse(viewModel.uiState.value.showDiscardConfirmation)
    assertFalse(viewModel.uiState.value.isDiscarded)
    assertEquals("Festival", viewModel.uiState.value.name)

    viewModel.requestDiscard()
    viewModel.confirmDiscard()
    assertFalse(viewModel.uiState.value.showDiscardConfirmation)
    assertTrue(viewModel.uiState.value.isDiscarded)
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
      viewModel.uiState.value.toEvent("event-123", creator)
    }
  }

  /** Red if converting valid form fields does not create the expected event. */
  @Test
  fun toEventCreatesEventFromFields() {
    fillAllFields()

    val event = viewModel.uiState.value.toEvent("event-123", creator)

    assertEquals("event-123", event.eventId)
    assertEquals(creator.uid, event.ownerId)
    assertEquals("Summer festival", event.title)
    assertEquals("Bring the stage equipment", event.description)
    assertEquals(listOf(creator), event.members)
    assertEquals(viewModel.uiState.value.startDateTime, event.startDate)
    assertEquals(viewModel.uiState.value.endDateTime, event.endDate)
    assertEquals("Main square", event.location)
  }

  /** Red if the untouched event form displays a field error. */
  @Test
  fun initialScreenStateDoesNotShowFieldErrors() {
    val state = viewModel.uiState.value

    assertNull(state.nameError)
    assertNull(state.endDateError)
  }

  /** Red if missing times do not default to the start and end of their dates. */
  @Test
  fun dateTimesUseDefaultTimesWhenTimesAreNotSet() {
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))

    val state = viewModel.uiState.value
    assertEquals(START_OF_DAY, state.startDateTime?.toLocalTime())
    assertEquals(END_OF_DAY, state.endDateTime?.toLocalTime())
  }

  /** Red if explicitly selected start and end times are ignored by the computed date-times. */
  @Test
  fun dateTimesUseSelectedTimesWhenTimesAreSet() {
    val selectedStartTime = LocalTime.of(14, 30)
    val selectedEndTime = LocalTime.of(18, 0)
    viewModel.updateRange(
        DateTimeRange(
            startDate = startDate,
            endDate = endDate,
            startTime = selectedStartTime,
            endTime = selectedEndTime,
        )
    )

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
            range =
                DateTimeRange(
                    startDate = startDate,
                    endDate = endDate,
                ),
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
    viewModel.updateRange(DateTimeRange(startDate = endDate, endDate = startDate))

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
            range =
                DateTimeRange(
                    startDate = oldStartDate,
                    endDate = oldEndDate,
                ),
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
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = gatedRepository,
            usersRepository = usersRepository,
        )
    val gatedViewModel = CreateEventViewModel(dataRepository, creator.uid)
    gatedViewModel.updateName("Summer festival")
    gatedViewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))

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

  /** Red if a timed-out save can create a duplicate when the user retries. */
  @Test
  fun timedOutSaveChecksWhetherEventWasStoredBeforeAllowingRetry() = runBlocking {
    val localRepository = EventsRepositoryLocal()
    val slowRepository =
        object : EventsRepository by localRepository {
          override suspend fun addEvent(event: Event) {
            withContext(NonCancellable) {
              localRepository.addEvent(event)
              delay(50)
            }
          }
        }
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = slowRepository,
            usersRepository = usersRepository,
        )
    viewModel = CreateEventViewModel(dataRepository, creator.uid, saveTimeoutMs = 10)
    fillRequiredFields()

    viewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (viewModel.uiState.value.createdEventId == null) delay(10.milliseconds)
    }
    viewModel.createEvent()
    delay(10.milliseconds)

    assertEquals(1, localRepository.getAllEvents().size)
  }

  /** Red if a timed-out lookup leaves the event save stuck or without an error. */
  @Test
  fun timedOutEventLookupShowsSaveError() = runBlocking {
    val checkingRepository =
        object : EventsRepository by eventsRepository {
          override suspend fun addEvent(event: Event) {
            error("offline")
          }

          override suspend fun getEvent(eventId: String): Event {
            withContext(NonCancellable) { delay(100) }
            error("not found")
          }
        }
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = checkingRepository,
            usersRepository = usersRepository,
        )
    viewModel = CreateEventViewModel(dataRepository, creator.uid, saveTimeoutMs = 10)
    fillRequiredFields()

    viewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }

    assertFalse(viewModel.uiState.value.isSaving)
    assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
  }

  /** Red if cancellation while checking a failed save is converted into a save error. */
  @Test
  fun cancelledEventLookupIsPropagated() = runBlocking {
    val cancelledCheckingRepository =
        object : EventsRepository by eventsRepository {
          override suspend fun addEvent(event: Event) {
            error("offline")
          }

          override suspend fun getEvent(eventId: String): Event {
            throw CancellationException("Cancelled")
          }
        }
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = cancelledCheckingRepository,
            usersRepository = usersRepository,
        )
    viewModel = CreateEventViewModel(dataRepository, creator.uid)
    fillRequiredFields()

    viewModel.createEvent()
    delay(100.milliseconds)

    assertTrue(viewModel.uiState.value.isSaving)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Red if creating an event does not add its creator to membership data. */
  @Test
  fun createEventAddsTheCreatorToMembersAndJoinedEvents() = runBlocking {
    fillAllFields()

    viewModel.createEvent()

    val event = waitForEvents(eventsRepository).single()
    assertEquals(creator.uid, event.ownerId)
    assertEquals(listOf(creator), event.members)
    assertEquals(Role.OWNER, event.roles[creator.uid])
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
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = failingRepository,
            usersRepository = usersRepository,
        )
    val failingViewModel = CreateEventViewModel(dataRepository, creator.uid)
    viewModel = failingViewModel

    fillAllFields()

    failingViewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (failingViewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }

    assertEquals(R.string.create_event_error_save_failed, failingViewModel.uiState.value.errorMsg)
    assertFalse(failingViewModel.uiState.value.isSaving)
    assertEquals("Summer festival", failingViewModel.uiState.value.name)
    assertEquals(startDate, failingViewModel.uiState.value.range.startDate)
    assertEquals(endDate, failingViewModel.uiState.value.range.endDate)
    assertEquals(startTime, failingViewModel.uiState.value.range.startTime)
    assertEquals(endTime, failingViewModel.uiState.value.range.endTime)
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

  /** Breaks if an anonymous account can write an event by calling createEvent. */
  @Test
  fun createEventWithAnonymousAccountShowsAnErrorAndDoesNotAddAnEvent() = runBlocking {
    viewModel = CreateEventViewModel(dataRepository, creator.uid, isAnonymous = { true })
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
    assertTrue(usersRepository.getUser(creator.uid).joinedEvents.isEmpty())
  }

  /** Breaks if save trusts the state computed at creation instead of checking the account. */
  @Test
  fun createEventChecksTheAccountAtSaveTime() = runBlocking {
    var anonymous = false
    viewModel = CreateEventViewModel(dataRepository, creator.uid, isAnonymous = { anonymous })
    assertTrue(viewModel.uiState.value.canCreateEvent)
    fillRequiredFields()

    anonymous = true
    viewModel.createEvent()
    withTimeout(5_000.milliseconds) {
      while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
    }

    assertEquals(
        R.string.create_event_error_save_failed,
        viewModel.uiState.value.errorMsg,
    )
    assertNull(viewModel.uiState.value.createdEventId)
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
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = failingRepository,
            usersRepository = usersRepository,
        )
    val failingViewModel = CreateEventViewModel(dataRepository, creator.uid)
    failingViewModel.updateName("Summer festival")
    failingViewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))

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

  /**
   * Breaks if hasImage, toEvent, or setImageUrlForTesting do not accurately reflect image URL
   * state.
   */
  @Test
  fun uiStateAndViewModelImageHelpersWork() {
    assertFalse(CreateEventUIState(imageUrl = null).hasImage)
    assertFalse(CreateEventUIState(imageUrl = "").hasImage)
    assertTrue(CreateEventUIState(imageUrl = "https://example.com/image.png").hasImage)

    fillAllFields()
    val event =
        viewModel.uiState.value
            .copy(imageUrl = "https://example.com/image.png")
            .toEvent("event-1", creator)
    assertEquals("https://example.com/image.png", event.imageUrl)

    viewModel.setImageUrlForTesting("https://example.com/custom.jpg")
    assertEquals("https://example.com/custom.jpg", viewModel.uiState.value.imageUrl)
  }

  /**
   * Breaks if deleteTemporaryImage fails to handle non-existent, deletable, or undeletable files.
   */
  @Test
  fun deleteTemporaryImageHandlesFileStatus() {
    val nonExistentFile = File("non_existent_file_12345.tmp")
    viewModel.deleteTemporaryImageForTesting(nonExistentFile)
    assertFalse(nonExistentFile.exists())

    val tempFile = File.createTempFile("temp", ".tmp")
    assertTrue(tempFile.exists())
    viewModel.deleteTemporaryImageForTesting(tempFile)
    assertFalse(tempFile.exists())

    val mockFile = mockk<File>()
    every { mockFile.exists() } returns true
    every { mockFile.delete() } returns false
    every { mockFile.path } returns "mock/path"
    viewModel.deleteTemporaryImageForTesting(mockFile)
    verify { mockFile.delete() }
  }

  /**
   * Breaks if an uploaded image is not treated as draft modification or deleted on confirmed
   * discard.
   */
  @Test
  fun uploadedImageRequiresDiscardConfirmationAndDeletesOnConfirm() {
    runBlocking {
      withMimeTypeMap {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()

        assertFalse(viewModel.needsDiscardConfirmation)
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }

        val uploadedUrl = viewModel.uiState.value.imageUrl
        assertNotNull(uploadedUrl)
        assertTrue(viewModel.needsDiscardConfirmation)

        viewModel.confirmDiscard()
        withTimeout(5_000.milliseconds) {
          while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
        }
        assertEquals(listOf(uploadedUrl), imageStorage.deletedUrls)

        imageStorage.deleteError = RuntimeException("delete error")
        viewModel.confirmDiscard()
        assertTrue(viewModel.uiState.value.isDiscarded)
      }
    }
  }

  /**
   * Breaks if confirmDiscard attempts image deletion when URL is null/blank or event is created.
   */
  @Test
  fun confirmDiscardNoOpsImageDeletionWhenNotNeeded() {
    runBlocking {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(
              dataRepository,
              creator.uid,
              ioDispatcher = Dispatchers.Unconfined,
          )

      viewModel.setImageUrlForTesting(null)
      viewModel.confirmDiscard()
      viewModel.setImageUrlForTesting("   ")
      viewModel.confirmDiscard()

      viewModel.setImageUrlForTesting("https://example.com/image.png")
      val stateField =
          EventFormViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
      @Suppress("UNCHECKED_CAST")
      val stateFlow = stateField.get(viewModel) as MutableStateFlow<CreateEventUIState>
      stateFlow.value = stateFlow.value.copy(createdEventId = "event-123")
      viewModel.confirmDiscard()

      assertTrue(imageStorage.deletedUrls.isEmpty())
    }
  }

  /**
   * Breaks if ViewModel clearance fails to delete uploaded images for uncreated events or crash on
   * delete error.
   */
  @Test
  fun onClearedDeletesUploadedImageForUncreatedEvent() {
    runBlocking {
      withMimeTypeMap {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()

        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }
        val uploadedUrl = viewModel.uiState.value.imageUrl!!

        val method =
            viewModel.javaClass.getDeclaredMethod("onCleared").apply { isAccessible = true }
        method.invoke(viewModel)

        withTimeout(5_000.milliseconds) {
          while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
        }
        assertEquals(listOf(uploadedUrl), imageStorage.deletedUrls)

        imageStorage.deleteError = RuntimeException("delete error")
        method.invoke(viewModel)
      }
    }
  }

  /** Breaks if onCleared attempts image deletion when URL is null/blank or event is created. */
  @Test
  fun onClearedNoOpsImageDeletionWhenNotNeeded() {
    runBlocking {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(
              dataRepository,
              creator.uid,
              ioDispatcher = Dispatchers.Unconfined,
          )
      val method = viewModel.javaClass.getDeclaredMethod("onCleared").apply { isAccessible = true }

      viewModel.setImageUrlForTesting(null)
      method.invoke(viewModel)
      viewModel.setImageUrlForTesting("   ")
      method.invoke(viewModel)

      viewModel.setImageUrlForTesting("https://example.com/image.png")
      val stateField =
          EventFormViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
      @Suppress("UNCHECKED_CAST")
      val stateFlow = stateField.get(viewModel) as MutableStateFlow<CreateEventUIState>
      stateFlow.value = stateFlow.value.copy(createdEventId = "event-123")
      method.invoke(viewModel)

      assertTrue(imageStorage.deletedUrls.isEmpty())
    }
  }

  /**
   * Breaks if image deletion dialog, storage removal, or exception handling during delete image
   * fails.
   */
  @Test
  fun deleteImageDialogAndConfirmationFlow() {
    runBlocking {
      withMimeTypeMap {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()

        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }

        val uploadedUrl = viewModel.uiState.value.imageUrl!!
        viewModel.requestDeleteImage()
        assertTrue(viewModel.uiState.value.showDeleteImageConfirmation)
        viewModel.cancelDeleteImage()
        assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)

        viewModel.requestDeleteImage()
        viewModel.confirmDeleteImage()
        assertNull(viewModel.uiState.value.imageUrl)
        assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)

        withTimeout(5_000.milliseconds) {
          while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
        }
        assertEquals(listOf(uploadedUrl), imageStorage.deletedUrls)

        viewModel.setImageUrlForTesting("https://example.com/test.png")
        imageStorage.deleteError = RuntimeException("delete error")
        viewModel.requestDeleteImage()
        viewModel.confirmDeleteImage()
        assertNull(viewModel.uiState.value.imageUrl)
      }
    }
  }

  /**
   * Breaks if requesting or confirming image deletion without an image triggers dialog or storage
   * delete.
   */
  @Test
  fun deleteImageNoOpsWhenImageMissing() {
    runBlocking {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(
              dataRepository,
              creator.uid,
              ioDispatcher = Dispatchers.Unconfined,
          )

      viewModel.requestDeleteImage()
      assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)

      viewModel.setImageUrlForTesting(null)
      viewModel.confirmDeleteImage()
      viewModel.setImageUrlForTesting("")
      viewModel.confirmDeleteImage()

      assertTrue(imageStorage.deletedUrls.isEmpty())
    }
  }

  /**
   * Breaks if updating image fails to enforce file size limits or default to jpg extension when
   * mime type is null.
   */
  @Test
  fun updateImageFileValidationAndExtensionFallback() {
    runBlocking {
      withMimeTypeMap {
        val largeData = ByteArray(11 * 1024 * 1024)
        val (resolver, uri) = createMockResolver(bytes = largeData)
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
        }
        assertEquals(R.string.profile_picture_too_large, viewModel.uiState.value.errorMsg)
        assertNull(viewModel.uiState.value.imageUrl)
      }

      withMimeTypeMap(extension = null) {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver(mimeType = null)
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }
        assertTrue(viewModel.uiState.value.imageUrl!!.endsWith(".jpg"))
      }
    }
  }

  /**
   * Breaks if updating image when a previous image exists fails to delete old image or skips
   * deletion when URLs match.
   */
  @Test
  fun updateImageHandlesImageReplacement() {
    runBlocking {
      withMimeTypeMap {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()

        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }
        val firstUrl = viewModel.uiState.value.imageUrl!!

        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
        }
        assertTrue(imageStorage.deletedUrls.contains(firstUrl))

        val sameStorage =
            object : ImageStorage {
              val deleted = mutableListOf<String>()

              override fun getNewUid() = "uid"

              override suspend fun upload(storedImage: StoredImage, subFolder: String) =
                  "https://example.com/same.png"

              override suspend fun delete(imageUrl: String) {
                deleted.add(imageUrl)
              }
            }
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = sameStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        viewModel.setImageUrlForTesting("https://example.com/same.png")
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.isSaving) delay(10.milliseconds)
        }
        assertTrue(sameStorage.deleted.isEmpty())
      }
    }
  }

  /**
   * Breaks if read or upload exceptions during updateImage fail to report save error or clean up
   * uploaded image.
   */
  @Test
  fun updateImageHandlesReadAndUploadExceptions() {
    runBlocking {
      withMimeTypeMap {
        val (failingResolver, failingUri) =
            createMockResolver(readException = RuntimeException("read error"))
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        viewModel.updateImage(failingUri, failingResolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
        }
        assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)

        val imageStorage = RecordingImageStorage(uploadError = RuntimeException("upload error"))
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
        }
        assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
      }
    }
  }

  /**
   * Breaks if old image deletion failure or post-upload state update failure fails to log error and
   * clean up image.
   */
  @Test
  fun updateImageHandlesPostUploadErrors() {
    runBlocking {
      withMimeTypeMap {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()

        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }
        imageStorage.deleteError = RuntimeException("delete error")
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
        }
        assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)

        val cleanupStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = cleanupStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val field =
            EventFormViewModel::class.java.getDeclaredField("_uiState").apply {
              isAccessible = true
            }
        @Suppress("UNCHECKED_CAST")
        val originalStateFlow = field.get(viewModel) as MutableStateFlow<CreateEventUIState>
        var updateCount = 0
        @Suppress("OPT_IN_USAGE")
        val failingStateFlow =
            object : MutableStateFlow<CreateEventUIState> by originalStateFlow {
              override fun compareAndSet(
                  expect: CreateEventUIState,
                  update: CreateEventUIState,
              ): Boolean {
                updateCount++
                if (updateCount == 2) throw RuntimeException("Simulated updateState failure")
                return originalStateFlow.compareAndSet(expect, update)
              }
            }
        field.set(viewModel, failingStateFlow)
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (cleanupStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
        }
        assertTrue(cleanupStorage.deletedUrls.isNotEmpty())
        assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)

        val failingStorage2 =
            RecordingImageStorage().apply { deleteError = RuntimeException("cleanup fail") }
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = failingStorage2,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        @Suppress("UNCHECKED_CAST")
        val stateFlow2 = field.get(viewModel) as MutableStateFlow<CreateEventUIState>
        var updateCount2 = 0
        @Suppress("OPT_IN_USAGE")
        val failingStateFlow2 =
            object : MutableStateFlow<CreateEventUIState> by stateFlow2 {
              override fun compareAndSet(
                  expect: CreateEventUIState,
                  update: CreateEventUIState,
              ): Boolean {
                updateCount2++
                if (updateCount2 == 2) throw RuntimeException("Simulated updateState failure")
                return stateFlow2.compareAndSet(expect, update)
              }
            }
        field.set(viewModel, failingStateFlow2)
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
        }
        assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
      }
    }
  }

  /**
   * Breaks if CancellationException during image read, upload, or old image deletion is swallowed
   * as save error.
   */
  @Test
  fun updateImagePropagatesCancellationExceptions() {
    runBlocking {
      withMimeTypeMap {
        val (readCancelResolver, readCancelUri) =
            createMockResolver(readException = CancellationException("cancelled"))
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        viewModel.updateImage(readCancelUri, readCancelResolver, createTempTestDir())
        assertNull(viewModel.uiState.value.errorMsg)

        val cancelUploadStorage =
            RecordingImageStorage(uploadError = CancellationException("cancelled"))
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = cancelUploadStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val (resolver, uri) = createMockResolver()
        viewModel.updateImage(uri, resolver, createTempTestDir())
        assertNull(viewModel.uiState.value.errorMsg)

        val cancelDeleteStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = cancelDeleteStorage,
            )
        viewModel =
            CreateEventViewModel(
                dataRepository,
                creator.uid,
                ioDispatcher = Dispatchers.Unconfined,
            )
        viewModel.updateImage(uri, resolver, createTempTestDir())
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }
        cancelDeleteStorage.deleteError = CancellationException("cancel delete")
        viewModel.updateImage(uri, resolver, createTempTestDir())
        assertNull(viewModel.uiState.value.errorMsg)
      }
    }
  }

  private fun fillFormAndCreateEvent(
      eventsRepository: EventsRepository,
      usersRepository: UsersRepository,
  ) {
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
        )
    viewModel = CreateEventViewModel(dataRepository, creator.uid)
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
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
  }

  private fun fillAllFields() {
    viewModel.updateName("Summer festival")
    viewModel.updateRange(
        DateTimeRange(
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
        )
    )
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

  private inline fun <T> withMimeTypeMap(extension: String? = "png", block: () -> T): T {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
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
    uploadedImages += storedImage to subFolder
    return "https://example.com/${subFolder}/${storedImage.file.name}"
  }

  override suspend fun delete(imageUrl: String) {
    deleteError?.let { throw it }
    deletedUrls += imageUrl
  }
}
