@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.images.ImageStorage
import com.android.festivar.model.images.StoredImage
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.datetime.DateTimeField
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
    assertEquals(startDate, state.startDate)
    assertEquals(endDate, state.endDate)
    assertEquals(startTime, state.startTime)
    assertEquals(endTime, state.endTime)
    assertEquals("Main square", state.venue)
    assertEquals("Bring the stage equipment", state.notes)
  }

  /** Breaks if the created event does not include the uploaded image URL. */
  @Test
  fun toEventIncludesImageUrl() {
    fillAllFields()
    val event =
        viewModel.uiState.value
            .copy(imageUrl = "https://example.com/image.png")
            .toEvent("event-1", creator)
    assertEquals("https://example.com/image.png", event.imageUrl)
  }

  /** Breaks if confirming discard does not delete an uploaded image from storage. */
  @Test
  fun confirmDiscardDeletesUploadedImage() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()

      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      val uploadedUrl = viewModel.uiState.value.imageUrl
      assertNotNull(uploadedUrl)

      viewModel.confirmDiscard()

      withTimeout(5_000.milliseconds) {
        while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
      }

      assertEquals(listOf(uploadedUrl), imageStorage.deletedUrls)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if an event draft with an uploaded image is not considered modified. */
  @Test
  fun needsDiscardConfirmationReturnsTrueWhenImageIsUploaded() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()

      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      assertFalse(viewModel.needsDiscardConfirmation)

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      assertTrue(viewModel.needsDiscardConfirmation)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if selecting an image exceeding the size limit fails to report a file-too-large error.
   */
  @Test
  fun updateImageWithTooLargeFileSetsError() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()

      val largeData = ByteArray(11 * 1024 * 1024)
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns ByteArrayInputStream(largeData)

      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
      }

      assertEquals(R.string.profile_picture_too_large, viewModel.uiState.value.errorMsg)
      assertNull(viewModel.uiState.value.imageUrl)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if requesting image deletion does not open the confirmation dialog. */
  @Test
  fun requestDeleteImageOpensConfirmationDialog() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      viewModel.requestDeleteImage()
      assertTrue(viewModel.uiState.value.showDeleteImageConfirmation)

      viewModel.cancelDeleteImage()
      assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if confirming image deletion does not clear the image URL from state and storage. */
  @Test
  fun confirmDeleteImageClearsImageUrlAndDeletesFromStorage() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      val uploadedUrl = viewModel.uiState.value.imageUrl
      assertNotNull(uploadedUrl)

      viewModel.requestDeleteImage()
      viewModel.confirmDeleteImage()

      assertNull(viewModel.uiState.value.imageUrl)
      assertFalse(viewModel.uiState.value.showDeleteImageConfirmation)

      withTimeout(5_000.milliseconds) {
        while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
      }

      assertEquals(listOf(uploadedUrl), imageStorage.deletedUrls)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
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

  /** Red if the ViewModel misses a form field or asks before the user enters anything. */
  @Test
  fun needsDiscardConfirmationTracksEnteredDetails() {
    assertFalse(viewModel.needsDiscardConfirmation)
    viewModel.showPicker(DateTimeField.START_DATE)
    assertFalse(viewModel.needsDiscardConfirmation)

    val edits: List<(CreateEventViewModel) -> Unit> =
        listOf(
            { it.updateName("Festival") },
            { it.updateStartDate(startDate) },
            { it.updateEndDate(endDate) },
            { it.updateStartTime(startTime) },
            { it.updateEndTime(endTime) },
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
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = gatedRepository,
            usersRepository = usersRepository,
        )
    val gatedViewModel = CreateEventViewModel(dataRepository, creator.uid)
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
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = failingRepository,
            usersRepository = usersRepository,
        )
    val failingViewModel = CreateEventViewModel(dataRepository, creator.uid)
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

  /**
   * Breaks if an exception during image reading or processing is not caught and reported as an
   * error.
   */
  @Test
  fun updateImageHandlesExceptionGracefully() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } throws RuntimeException("read error")

      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
      }

      assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if a CancellationException during image upload is treated as a save error instead of
   * being propagated.
   */
  @Test
  fun updateImagePropagatesCancellationException() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage(uploadError = CancellationException("cancelled"))
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
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      assertNull(viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if an error during image upload fails to trigger cleanup and report a save error. */
  @Test
  fun saveImageHandlesUploadExceptionAndCleansUp() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage(uploadError = RuntimeException("upload error"))
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
      }

      assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if an error deleting the old image during replacement fails to set an error message. */
  @Test
  fun saveImageHandlesOldImageDeletionError() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      imageStorage.deleteError = RuntimeException("delete error")
      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
      }

      assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if an exception during online image deletion on confirmation throws instead of being
   * caught.
   */
  @Test
  fun confirmDeleteImageHandlesExceptionGracefully() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      imageStorage.deleteError = RuntimeException("delete error")
      viewModel.requestDeleteImage()
      viewModel.confirmDeleteImage()

      assertNull(viewModel.uiState.value.imageUrl)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if an exception during image deletion on discard throws instead of being caught. */
  @Test
  fun confirmDiscardHandlesExceptionGracefully() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      imageStorage.deleteError = RuntimeException("delete error")
      viewModel.confirmDiscard()

      assertTrue(viewModel.uiState.value.isDiscarded)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if an exception during image deletion on ViewModel clearance throws instead of being
   * caught.
   */
  @Test
  fun onClearedHandlesExceptionGracefully() {
    runBlocking {
      val mimeTypeMap = mockk<MimeTypeMap>()
      mockkStatic(MimeTypeMap::class)
      every { MimeTypeMap.getSingleton() } returns mimeTypeMap
      every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

      try {
        val imageStorage = RecordingImageStorage()
        dataRepository =
            DataRepositoryLocal(
                mockk(),
                eventsRepository = eventsRepository,
                usersRepository = usersRepository,
                imageStorage = imageStorage,
            )
        viewModel =
            CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
        val resolver = mockk<ContentResolver>()
        val tempDir = File.createTempFile("test", "dir")
        tempDir.delete()
        tempDir.mkdir()
        tempDir.deleteOnExit()
        val uri = mockk<Uri>()

        every { resolver.getType(uri) } returns "image/png"
        every { resolver.openInputStream(uri) } returns
            ByteArrayInputStream("test image content".toByteArray())

        viewModel.updateImage(uri, resolver, tempDir)
        withTimeout(5_000.milliseconds) {
          while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
        }

        imageStorage.deleteError = RuntimeException("delete error")
        val method = viewModel.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)
      } finally {
        unmockkStatic(MimeTypeMap::class)
      }
    }
  }

  /** Breaks if setting an image URL for testing fails to update the view model state. */
  @Test
  fun setImageUrlForTestingUpdatesState() {
    viewModel.setImageUrlForTesting("https://example.com/custom.jpg")
    assertEquals("https://example.com/custom.jpg", viewModel.uiState.value.imageUrl)
  }

  /** Breaks if updating an image with a null mime type fails to default to the jpg extension. */
  @Test
  fun updateImageWithNullMimeTypeUsesDefaultJpgExtension() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType(any()) } returns null

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns null
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      assertTrue(viewModel.uiState.value.imageUrl!!.endsWith(".jpg"))
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if a CancellationException during image update reading sets a save error instead of
   * propagating.
   */
  @Test
  fun updateImagePropagatesCancellationExceptionDuringRead() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } throws CancellationException("cancelled")

      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

      viewModel.updateImage(uri, resolver, tempDir)

      assertNull(viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if a CancellationException during old image deletion sets a save error instead of
   * propagating.
   */
  @Test
  fun saveImagePropagatesCancellationDuringOldImageDeletion() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      imageStorage.deleteError = CancellationException("cancel delete")

      viewModel.updateImage(uri, resolver, tempDir)

      assertNull(viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if attempting to delete a non-existent temporary file throws an exception. */
  @Test
  fun deleteTemporaryImageWithNonExistentFile() {
    val nonExistentFile = File("non_existent_file_12345.tmp")
    viewModel.deleteTemporaryImageForTesting(nonExistentFile)
    assertFalse(nonExistentFile.exists())
  }

  /** Breaks if deleting an existing temporary file fails to remove it. */
  @Test
  fun deleteTemporaryImageWithSuccessfullyDeletableFile() {
    val tempFile = File.createTempFile("temp", ".tmp")
    assertTrue(tempFile.exists())
    viewModel.deleteTemporaryImageForTesting(tempFile)
    assertFalse(tempFile.exists())
  }

  /**
   * Breaks if a file deletion failure during temporary cleanup fails to log a warning gracefully.
   */
  @Test
  fun deleteTemporaryImageWithUndeletableFile() {
    val mockFile = mockk<File>()
    every { mockFile.exists() } returns true
    every { mockFile.delete() } returns false
    every { mockFile.path } returns "mock/path"

    viewModel.deleteTemporaryImageForTesting(mockFile)
    verify { mockFile.exists() }
    verify { mockFile.delete() }
  }

  /**
   * Breaks if clearing the ViewModel when an image is uploaded and event is not created does not
   * delete the image.
   */
  @Test
  fun onClearedDeletesUploadedImageWhenEventNotCreated() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      val uploadedUrl = viewModel.uiState.value.imageUrl
      assertNotNull(uploadedUrl)

      val method = viewModel.javaClass.getDeclaredMethod("onCleared")
      method.isAccessible = true
      method.invoke(viewModel)

      withTimeout(5_000.milliseconds) {
        while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
      }

      assertEquals(listOf(uploadedUrl), imageStorage.deletedUrls)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if updating an image when a previous image exists fails to delete the old image from
   * storage.
   */
  @Test
  fun updateImageReplacesOldImageAndDeletesPreviousFromStorage() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)
      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.imageUrl == null) delay(10.milliseconds)
      }

      val firstUploadedUrl = viewModel.uiState.value.imageUrl
      assertNotNull(firstUploadedUrl)

      viewModel.updateImage(uri, resolver, tempDir)
      withTimeout(5_000.milliseconds) {
        while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
      }

      assertTrue(imageStorage.deletedUrls.contains(firstUploadedUrl))
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if image storage cleanup fails during a save error and does not log or set an error
   * message gracefully.
   */
  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun saveImageCleanupHandlesExceptionWhenDeleteFails() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      imageStorage.deleteError = RuntimeException("cleanup delete error")
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

      val field = EventFormViewModel::class.java.getDeclaredField("_uiState")
      field.isAccessible = true
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
              if (updateCount == 2) {
                throw RuntimeException("Simulated updateState failure")
              }
              return originalStateFlow.compareAndSet(expect, update)
            }
          }
      field.set(viewModel, failingStateFlow)

      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.errorMsg == null) delay(10.milliseconds)
      }

      assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /** Breaks if hasImage returns true when the image URL is null. */
  @Test
  fun hasImageReturnsFalseWhenImageUrlIsNull() {
    val state = CreateEventUIState(imageUrl = null)
    assertFalse(state.hasImage)
  }

  /** Breaks if hasImage returns true when the image URL is empty. */
  @Test
  fun hasImageReturnsFalseWhenImageUrlIsEmpty() {
    val state = CreateEventUIState(imageUrl = "")
    assertFalse(state.hasImage)
  }

  /** Breaks if hasImage returns false when a valid image URL is set. */
  @Test
  fun hasImageReturnsTrueWhenImageUrlIsPresent() {
    val state = CreateEventUIState(imageUrl = "https://example.com/image.png")
    assertTrue(state.hasImage)
  }

  /**
   * Breaks if confirming discard attempts to delete an image when the image URL is null or blank.
   */
  @Test
  fun confirmDiscardDoesNotDeleteImageWhenUrlIsNullOrBlank() = runBlocking {
    val imageStorage = RecordingImageStorage()
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
            imageStorage = imageStorage,
        )
    viewModel =
        CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

    viewModel.setImageUrlForTesting(null)
    viewModel.confirmDiscard()

    viewModel.setImageUrlForTesting("   ")
    viewModel.confirmDiscard()

    assertTrue(imageStorage.deletedUrls.isEmpty())
  }

  /**
   * Breaks if confirming discard attempts to delete an image when the event has already been
   * created.
   */
  @Test
  fun confirmDiscardDoesNotDeleteImageWhenEventAlreadyCreated() = runBlocking {
    val imageStorage = RecordingImageStorage()
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
            imageStorage = imageStorage,
        )
    viewModel =
        CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

    viewModel.setImageUrlForTesting("https://example.com/image.png")
    val stateField = EventFormViewModel::class.java.getDeclaredField("_uiState")
    stateField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = stateField.get(viewModel) as MutableStateFlow<CreateEventUIState>
    stateFlow.value = stateFlow.value.copy(createdEventId = "event-123")

    viewModel.confirmDiscard()

    assertTrue(imageStorage.deletedUrls.isEmpty())
  }

  /**
   * Breaks if clearing the ViewModel attempts to delete an image when the image URL is null or
   * blank.
   */
  @Test
  fun onClearedDoesNotDeleteImageWhenUrlIsNullOrBlank() = runBlocking {
    val imageStorage = RecordingImageStorage()
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
            imageStorage = imageStorage,
        )
    viewModel =
        CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

    viewModel.setImageUrlForTesting(null)
    val method = viewModel.javaClass.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(viewModel)

    viewModel.setImageUrlForTesting("   ")
    method.invoke(viewModel)

    assertTrue(imageStorage.deletedUrls.isEmpty())
  }

  /**
   * Breaks if clearing the ViewModel attempts to delete an image when the event has already been
   * created.
   */
  @Test
  fun onClearedDoesNotDeleteImageWhenEventIsCreated() = runBlocking {
    val imageStorage = RecordingImageStorage()
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
            imageStorage = imageStorage,
        )
    viewModel =
        CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

    viewModel.setImageUrlForTesting("https://example.com/image.png")
    val stateField = EventFormViewModel::class.java.getDeclaredField("_uiState")
    stateField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = stateField.get(viewModel) as MutableStateFlow<CreateEventUIState>
    stateFlow.value = stateFlow.value.copy(createdEventId = "event-123")

    val method = viewModel.javaClass.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(viewModel)

    assertTrue(imageStorage.deletedUrls.isEmpty())
  }

  /**
   * Breaks if confirming image deletion attempts to delete from storage when the image URL is null
   * or empty.
   */
  @Test
  fun confirmDeleteImageDoesNotDeleteFromStorageWhenUrlIsNullOrEmpty() = runBlocking {
    val imageStorage = RecordingImageStorage()
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = eventsRepository,
            usersRepository = usersRepository,
            imageStorage = imageStorage,
        )
    viewModel =
        CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

    viewModel.setImageUrlForTesting(null)
    viewModel.confirmDeleteImage()

    viewModel.setImageUrlForTesting("")
    viewModel.confirmDeleteImage()

    assertTrue(imageStorage.deletedUrls.isEmpty())
  }

  /**
   * Breaks if updating an image attempts to delete the old image when old and new image URLs are
   * equal.
   */
  @Test
  fun saveImageDoesNotDeleteOldImageWhenOldAndNewUrlsAreEqual() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage =
          object : ImageStorage {
            val deleted = mutableListOf<String>()

            override fun getNewUid(): String = "uid"

            override suspend fun upload(storedImage: StoredImage, subFolder: String): String =
                "https://example.com/same_image.png"

            override suspend fun delete(imageUrl: String) {
              deleted.add(imageUrl)
            }
          }
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

      viewModel.setImageUrlForTesting("https://example.com/same_image.png")

      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (viewModel.uiState.value.isSaving) delay(10.milliseconds)
      }

      assertTrue(imageStorage.deleted.isEmpty())
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
  }

  /**
   * Breaks if an exception during image save fails to clean up the newly uploaded image from
   * storage.
   */
  @Test
  fun saveImageDeletesNewlyUploadedImageWhenExceptionOccursAfterUpload() = runBlocking {
    val mimeTypeMap = mockk<MimeTypeMap>()
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"

    try {
      val imageStorage = RecordingImageStorage()
      dataRepository =
          DataRepositoryLocal(
              mockk(),
              eventsRepository = eventsRepository,
              usersRepository = usersRepository,
              imageStorage = imageStorage,
          )
      viewModel =
          CreateEventViewModel(dataRepository, creator.uid, ioDispatcher = Dispatchers.Unconfined)

      val field = EventFormViewModel::class.java.getDeclaredField("_uiState")
      field.isAccessible = true
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
              if (updateCount == 2) {
                throw RuntimeException("Simulated updateState failure after upload")
              }
              return originalStateFlow.compareAndSet(expect, update)
            }
          }
      field.set(viewModel, failingStateFlow)

      val resolver = mockk<ContentResolver>()
      val tempDir = File.createTempFile("test", "dir")
      tempDir.delete()
      tempDir.mkdir()
      tempDir.deleteOnExit()
      val uri = mockk<Uri>()

      every { resolver.getType(uri) } returns "image/png"
      every { resolver.openInputStream(uri) } returns
          ByteArrayInputStream("test image content".toByteArray())

      viewModel.updateImage(uri, resolver, tempDir)

      withTimeout(5_000.milliseconds) {
        while (imageStorage.deletedUrls.isEmpty()) delay(10.milliseconds)
      }

      assertTrue(imageStorage.deletedUrls.isNotEmpty())
      assertEquals(R.string.create_event_error_save_failed, viewModel.uiState.value.errorMsg)
    } finally {
      unmockkStatic(MimeTypeMap::class)
    }
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
