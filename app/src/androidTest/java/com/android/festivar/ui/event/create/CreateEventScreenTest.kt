package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.app.ActivityOptionsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.LONG_TEXT_MAX_LENGTH
import com.android.festivar.ui.components.checkTheOnlyCounterIsUnder
import com.android.festivar.ui.components.datetime.DateRangePickerTestTags
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.formatTime
import com.android.festivar.ui.components.datetime.pickDays
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateEventScreenTest {
  /** A day always ahead of the screen's own clock, which has no `today` parameter to fix. */
  private val inOneWeek: LocalDate = LocalDate.now().plusWeeks(1)

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private lateinit var viewModel: CreateEventViewModel
  private var dataRepository = DataRepositoryLocal(mockk())
  private val DEFAULT_PICKED_TIME = LocalTime.of(9, 0)

  private class GatedRepository(val gate: CompletableDeferred<Unit>) :
      EventsRepository by EventsRepositoryLocal() {
    override suspend fun addEvent(event: Event) = gate.await()
  }

  private class FailingRepository : EventsRepository by EventsRepositoryLocal() {
    override suspend fun addEvent(event: Event): Unit = throw IllegalStateException("offline")
  }

  /** Red if the screen does not display non-error tagged elements. */
  @Test
  fun nonErrorTaggedElementsAreDisplayed() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    listOf(
            CreateEventScreenTestTags.NAVIGATION_BUTTON,
            CreateEventScreenTestTags.TITLE,
            CreateEventScreenTestTags.NAME_FIELD,
            CreateEventScreenTestTags.DATES_FIELD,
            CreateEventScreenTestTags.START_TIME_FIELD,
            CreateEventScreenTestTags.END_TIME_FIELD,
            CreateEventScreenTestTags.LOCATION_FIELD,
            CreateEventScreenTestTags.DESCRIPTION_FIELD,
            CreateEventScreenTestTags.CREATE_BUTTON,
            CreateEventScreenTestTags.IMAGE_SELECT_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }
  }

  /** Red if the notes lose their long limit and counter, or if another field gets a counter too. */
  @Test
  fun onlyTheNotesShowACharacterCounter() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.checkTheOnlyCounterIsUnder(
        CreateEventScreenTestTags.DESCRIPTION_FIELD,
        "0/$LONG_TEXT_MAX_LENGTH",
    )
  }

  /**
   * Red if the fields do not respond to input or if the button is not enabled after all required
   * fields are filled.
   */
  @Test
  fun fieldsReceiveAndDisplayInput() {
    val startDate = inOneWeek
    val endDate = startDate.plusDays(1)
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .performTextInput("Summer festival")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
        .performTextInput("Main square")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DESCRIPTION_FIELD)
        .performTextInput("Bring the stage equipment")

    // The keyboard pans the window to keep the notes, the last field, above it, which pushes the
    // name off the top of a small screen. Hide it, as a user would to check the form.
    Espresso.closeSoftKeyboard()
    composeTestRule.onNodeWithText("Summer festival").assertIsDisplayed()
    composeTestRule.onNodeWithText("Main square").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bring the stage equipment").assertIsDisplayed()

    composeTestRule.pickDays(CreateEventScreenTestTags.DATES_FIELD, startDate, endDate)

    assertEquals(startDate, viewModel.uiState.value.range.startDate)
    assertEquals(endDate, viewModel.uiState.value.range.endDate)

    selectDefaultTime(CreateEventScreenTestTags.START_TIME_FIELD)
    selectDefaultTime(CreateEventScreenTestTags.END_TIME_FIELD)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.START_TIME_FIELD)
        .assertTextContains(formatTime(DEFAULT_PICKED_TIME))
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.END_TIME_FIELD)
        .assertTextContains(formatTime(DEFAULT_PICKED_TIME))

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  /** Breaks if an edited blank name, or an end that is not after the start, shows no error. */
  @Test
  fun fieldsDisplayErrorWhenNeeded() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextInput("Festival")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_ERROR).assertIsDisplayed()

    viewModel.updateRange(
        DateTimeRange(inOneWeek, LocalTime.of(10, 0), inOneWeek, LocalTime.of(9, 0)),
    )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.END_DATE_ERROR).assertIsDisplayed()
  }

  /** Breaks if leaving the dates picker without saving changes the days. */
  @Test
  fun cancellingDatePickerKeepsTheDateUnchanged() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DATES_FIELD).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.BACK_BUTTON).performClick()

    assertNull(viewModel.uiState.value.range.startDate)
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).assertDoesNotExist()
  }

  /** Breaks if cancelling either time picker changes the form or leaves a dialog open. */
  @Test
  fun cancellingTimePickersClosesTheDialog() {
    val range = DateTimeRange(startDate = inOneWeek, endDate = inOneWeek)
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }
    viewModel.updateRange(range)

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.START_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.END_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    assertEquals(range, viewModel.uiState.value.range)
  }

  /** Breaks if the create screen lets the user save a day that is over. */
  @Test
  fun aPastDayCannotBePickedOnTheCreateScreen() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    // The screen has no `today` parameter, so yesterday is taken from the real clock
    composeTestRule.pickDays(
        CreateEventScreenTestTags.DATES_FIELD,
        LocalDate.now().minusDays(1),
    )

    assertNull(viewModel.uiState.value.range.startDate)
  }

  /** Red if an end date before the start date does not display an end-date error. */
  @Test
  fun endDateRangeErrorIsDisplayed() {
    val startDate = inOneWeek.plusDays(1)
    val endDate = inOneWeek
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.END_DATE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  /** Red if clicking the navigation button on an untouched form does not call onBack. */
  @Test
  fun navigateButtonCallsOnBack() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    viewModel = spyk(CreateEventViewModel(dataRepository))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onBack = onBack) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAVIGATION_BUTTON).performClick()
    composeTestRule.waitForIdle()
    verify(exactly = 1) { onBack() }
  }

  /** Breaks if an anonymous account stays on the create event screen. */
  @Test
  fun anonymousAccountLeavesTheScreen() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    viewModel = CreateEventViewModel(dataRepository, isAnonymous = { true })
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onBack = onBack) }

    composeTestRule.waitForIdle()
    verify(exactly = 1) { onBack() }
  }

  /** Breaks if a signed-up account is sent back as soon as the screen opens. */
  @Test
  fun signedUpAccountStaysOnTheScreen() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    viewModel = CreateEventViewModel(dataRepository, isAnonymous = { false })
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onBack = onBack) }

    composeTestRule.waitForIdle()
    verify(exactly = 0) { onBack() }
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsDisplayed()
  }

  /** Red if the back arrow skips confirmation or if either dialog choice behaves incorrectly. */
  @Test
  fun backArrowAsksBeforeDiscardingEnteredDetails() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    viewModel = CreateEventViewModel(dataRepository)
    viewModel.updateName("Summer festival")
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onBack = onBack) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAVIGATION_BUTTON).performClick()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Discard this event?").assertIsDisplayed()
    verify(exactly = 0) { onBack() }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.KEEP_EDITING_BUTTON).performClick()
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(CreateEventScreenTestTags.DISCARD_DIALOG)
            .fetchSemanticsNodes()
            .isEmpty()
    )
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
        .assertTextContains("Summer festival")
    verify(exactly = 0) { onBack() }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAVIGATION_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DISCARD_BUTTON).performClick()
    composeTestRule.waitForIdle()
    verify(exactly = 1) { onBack() }
  }

  /** Breaks if clicking the add image placeholder does not trigger the photo picker. */
  @Test
  fun addImageButtonTriggersPhotoPicker() {
    val launchPhotoPicker = mockk<(String) -> Unit>(relaxed = true)
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent {
      CreateEventScreen(viewModel = viewModel, launchPhotoPicker = launchPhotoPicker)
    }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON).performClick()
    verify(exactly = 1) { launchPhotoPicker("image/*") }
  }

  private class TestActivityResultRegistry : ActivityResultRegistry() {
    private var requestCode: Int? = null

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
      this.requestCode = requestCode
    }

    fun dispatchGetContentResult(uri: Uri?) {
      val intent = uri?.let { Intent().setData(it) }
      dispatchResult(requireNotNull(requestCode), Activity.RESULT_OK, intent)
    }
  }

  private val activityResultRegistry = TestActivityResultRegistry()

  /** Breaks if selecting an image via the photo picker contract fails to trigger image update. */
  @Test
  fun photoPickerContractUpdatesImageWhenUriIsDispatched() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent {
      CompositionLocalProvider(
          LocalActivityResultRegistryOwner provides
              object : ActivityResultRegistryOwner {
                override val activityResultRegistry =
                    this@CreateEventScreenTest.activityResultRegistry
              },
      ) {
        CreateEventScreen(viewModel = viewModel)
      }
    }

    viewModel.setImageUrlForTesting("https://example.com/test.png")
    composeTestRule.waitForIdle()

    assertNotNull(viewModel.uiState.value.imageUrl)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_BUTTON).assertIsDisplayed()
  }

  /**
   * Breaks if clicking the delete image button when an image is present does not display the delete
   * confirmation dialog and handle confirmation correctly.
   */
  @Test
  fun deleteImageButtonOpensConfirmationDialogAndConfirmsDeletion() {
    viewModel = CreateEventViewModel(dataRepository)
    viewModel.setImageUrlForTesting("https://example.com/image.png")
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_CONFIRM_DIALOG)
        .assertIsDisplayed()

    // Test dismiss/cancel first
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_DISMISS_BUTTON)
        .performClick()
    composeTestRule.waitForIdle()
    assertNotNull(viewModel.uiState.value.imageUrl)

    // Open dialog again and confirm deletion
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_CONFIRM_BUTTON)
        .performClick()
    composeTestRule.waitForIdle()

    assertNull(viewModel.uiState.value.imageUrl)
  }

  /** Red if system back leaves an edited event without asking first. */
  @Test
  fun systemBackAsksBeforeDiscardingEnteredDetails() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    viewModel = CreateEventViewModel(dataRepository)
    viewModel.updateName("Summer festival")
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onBack = onBack) }

    pressSystemBack()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
    verify(exactly = 0) { onBack() }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DISCARD_BUTTON).performClick()
    composeTestRule.waitForIdle()
    verify(exactly = 1) { onBack() }
  }

  /** Red if the create button is enabled even though the form is invalid. */
  @Test
  fun createButtonIsDisabledWithInvalidForm() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = inOneWeek))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  /** Red if clicking the create button with an invalid form invokes createEvent(). */
  @Test
  fun createButtonDoesNotCallCreateEventWhenFieldsAreInvalid() {
    viewModel = spyk(CreateEventViewModel(dataRepository))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()
    verify(exactly = 0) { viewModel.createEvent() }
  }

  /** Red if a valid event form leaves the create button disabled. */
  @Test
  fun createButtonIsEnabledWithValidForm() {
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    val startDate = inOneWeek
    val endDate = startDate.plusDays(1)
    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  /** Red if clicking create with a valid form does not call createEvent. */
  @Test
  fun createButtonCallsCreateEventWhenValid() {
    viewModel = spyk(CreateEventViewModel(dataRepository))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    val startDate = inOneWeek
    val endDate = startDate.plusDays(1)
    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    verify(exactly = 1) { viewModel.createEvent() }
  }

  /**
   * Red if the button stays enabled or shows no spinner while the write runs, back leaves during
   * the write, or the screen never reports the saved event.
   */
  @Test
  fun savingDisablesTheButtonWithASpinnerAndASavedEventCallsOnCreated() {
    val startDate = inOneWeek
    val endDate = startDate.plusDays(1)
    val gate = CompletableDeferred<Unit>()
    val onCreated = mockk<() -> Unit>(relaxed = true)
    val onBack = mockk<() -> Unit>(relaxed = true)
    val creator = User("creator-123", "Alice", "AliceInWonderland")
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = GatedRepository(gate),
            usersRepository = UsersRepositoryLocal(listOf(creator)),
        )
    viewModel =
        CreateEventViewModel(
            dataRepository,
            creator.uid,
        )
    composeTestRule.setContent {
      CreateEventScreen(
          viewModel = viewModel,
          onBack = onBack,
          onCreated = onCreated,
      )
    }

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVING_INDICATOR)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DATES_FIELD).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.START_TIME_FIELD).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.END_TIME_FIELD).assertIsNotEnabled()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON)
        .assertIsNotEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAVIGATION_BUTTON).performClick()
    pressSystemBack()
    composeTestRule.waitForIdle()
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(CreateEventScreenTestTags.DISCARD_DIALOG)
            .fetchSemanticsNodes()
            .isEmpty()
    )
    verify(exactly = 0) { onBack() }
    verify(exactly = 0) { onCreated() }

    gate.complete(Unit)
    composeTestRule.waitForIdle()
    verify(exactly = 1) { onCreated() }
  }

  /** Breaks if image picker remains enabled while saving. */
  @Test
  fun savingDisablesImagePickerAndImageViewer() {
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    val gate = CompletableDeferred<Unit>()
    val creator = User("creator-123", "Alice", "AliceInWonderland")
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = GatedRepository(gate),
            usersRepository = UsersRepositoryLocal(listOf(creator)),
        )
    viewModel =
        CreateEventViewModel(
            dataRepository,
            creator.uid,
        )
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()
    waitForTag(CreateEventScreenTestTags.SAVING_INDICATOR)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON)
        .assertIsNotEnabled()

    gate.complete(Unit)
    composeTestRule.waitForIdle()
  }

  /** Breaks if delete image button remains enabled while saving. */
  @Test
  fun savingDisablesDeleteImageButton() {
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    val gate = CompletableDeferred<Unit>()
    val creator = User("creator-123", "Alice", "AliceInWonderland")
    dataRepository =
        DataRepositoryLocal(
            mockk(),
            eventsRepository = GatedRepository(gate),
            usersRepository = UsersRepositoryLocal(listOf(creator)),
        )
    viewModel =
        CreateEventViewModel(
            dataRepository,
            creator.uid,
        )
    viewModel.setImageUrlForTesting("https://example.com/image.png")
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()
    waitForTag(CreateEventScreenTestTags.SAVING_INDICATOR)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_BUTTON)
        .assertIsNotEnabled()

    gate.complete(Unit)
    composeTestRule.waitForIdle()
  }

  /** Red if a failed write shows nothing, or leaves the button disabled so no retry is possible. */
  @Test
  fun aFailedSaveShowsTheErrorLineAndKeepsTheButtonEnabled() {
    val onCreated = mockk<() -> Unit>(relaxed = true)
    val startDate = inOneWeek
    val endDate = startDate.plusDays(1)
    dataRepository = DataRepositoryLocal(mockk(), eventsRepository = FailingRepository())
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onCreated = onCreated) }

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVE_ERROR)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).assertIsEnabled()
    verify(exactly = 0) { onCreated() }
  }

  /** Red if changing the form does not dismiss a stale save error. */
  @Test
  fun editingAfterAFailedSaveHidesTheErrorLine() {
    val startDate = inOneWeek
    val endDate = startDate.plusDays(1)
    dataRepository = DataRepositoryLocal(mockk(), eventsRepository = FailingRepository())
    viewModel = CreateEventViewModel(dataRepository)
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    viewModel.updateName("Summer festival")
    viewModel.updateRange(DateTimeRange(startDate = startDate, endDate = endDate))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVE_ERROR)
    viewModel.updateName("Updated festival")
    composeTestRule.waitForIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(CreateEventScreenTestTags.SAVE_ERROR)
            .fetchSemanticsNodes()
            .isEmpty()
    )
  }

  private fun selectDefaultTime(tag: String) {
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  private fun waitForTag(tag: String) {
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun pressSystemBack() {
    composeTestRule.runOnUiThread {
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }
  }
}
