package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var viewModel: CreateEventViewModel

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
    viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    listOf(
            CreateEventScreenTestTags.NAVIGATION_BUTTON,
            CreateEventScreenTestTags.TITLE,
            CreateEventScreenTestTags.NAME_FIELD,
            CreateEventScreenTestTags.START_DATE_FIELD,
            CreateEventScreenTestTags.START_TIME_FIELD,
            CreateEventScreenTestTags.END_DATE_FIELD,
            CreateEventScreenTestTags.END_TIME_FIELD,
            CreateEventScreenTestTags.LOCATION_FIELD,
            CreateEventScreenTestTags.DESCRIPTION_FIELD,
            CreateEventScreenTestTags.CREATE_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }
  }

  /**
   * Red if the fields do not respond to input or if the button is not enabled after all required
   * fields are filled.
   */
  @Test
  fun fieldsReceiveAndDisplayInput() {
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    viewModel = CreateEventViewModel(EventsRepositoryLocal())
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

    composeTestRule.onNodeWithText("Summer festival").assertIsDisplayed()
    composeTestRule.onNodeWithText("Main square").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bring the stage equipment").assertIsDisplayed()

    selectDate(CreateEventScreenTestTags.START_DATE_FIELD, startDate)
    selectDate(CreateEventScreenTestTags.END_DATE_FIELD, endDate)

    val expectedStartDate = formatDate(startDate)
    val expectedEndDate = formatDate(endDate)
    composeTestRule.onNodeWithText(expectedStartDate).assertIsDisplayed()
    composeTestRule.onNodeWithText(expectedEndDate).assertIsDisplayed()

    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(9, 0)
    selectTime(CreateEventScreenTestTags.START_TIME_FIELD, startTime)
    selectTime(CreateEventScreenTestTags.END_TIME_FIELD, endTime)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.START_TIME_FIELD)
        .assertTextContains(formatTime(startTime))
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.END_TIME_FIELD)
        .assertTextContains(formatTime(endTime))

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  /** Red if an edited blank name or incomplete date/time field shows no error. */
  @Test
  fun fieldsDisplayErrorWhenNeeded() {
    viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextInput("Festival")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_ERROR).assertIsDisplayed()

    viewModel.updateStartTime(LocalTime.of(9, 0))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.START_DATE_ERROR).assertIsDisplayed()

    viewModel.updateEndTime(LocalTime.of(10, 0))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.END_DATE_ERROR).assertIsDisplayed()
  }

  /** Red if clicking the navigation button does not call onBack. */
  @Test
  fun navigateButtonCallsOnBack() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    viewModel = spyk(CreateEventViewModel(EventsRepositoryLocal()))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onBack = onBack) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAVIGATION_BUTTON).performClick()
    verify(exactly = 1) { onBack() }
  }

  /** Red if the create button is enabled even though the form is invalid. */
  @Test
  fun createButtonIsDisabledWithInvalidForm() {
    viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()

    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(LocalDate.now())
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  /** Red if the create button is enabled even though the form is empty. */
  @Test
  fun createButtonDoesNotCallCreateEventWhenFieldsAreInvalid() {
    viewModel = spyk(CreateEventViewModel(EventsRepositoryLocal()))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()
    verify(exactly = 0) { viewModel.createEvent() }
  }

  /** Red if a valid event form leaves the create button disabled. */
  @Test
  fun createButtonIsEnabledWithValidForm() {
    viewModel = CreateEventViewModel(EventsRepositoryLocal())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  /** Red if clicking create with a valid form does not call createEvent. */
  @Test
  fun createButtonCallsCreateEventWhenValid() {
    viewModel = spyk(CreateEventViewModel(EventsRepositoryLocal()))
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel) }

    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    verify(exactly = 1) { viewModel.createEvent() }
  }

  /**
   * Red if the button stays enabled or shows no spinner while the write runs, or if the screen
   * never reports the saved event.
   */
  @Test
  fun savingDisablesTheButtonWithASpinnerAndASavedEventCallsOnCreated() {
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    val gate = CompletableDeferred<Unit>()
    val onCreated = mockk<() -> Unit>(relaxed = true)
    val creator = User("creator-123", "Alice", "AliceInWonderland")
    viewModel =
        CreateEventViewModel(
            GatedRepository(gate),
            UsersRepositoryLocal(listOf(creator)),
            creator.uid,
        )
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onCreated = onCreated) }

    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVING_INDICATOR)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
    verify(exactly = 0) { onCreated() }

    gate.complete(Unit)
    composeTestRule.waitForIdle()
    verify(exactly = 1) { onCreated() }
  }

  /** Red if a failed write shows nothing, or leaves the button disabled so no retry is possible. */
  @Test
  fun aFailedSaveShowsTheErrorLineAndKeepsTheButtonEnabled() {
    val onCreated = mockk<() -> Unit>(relaxed = true)
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(1)
    viewModel = CreateEventViewModel(FailingRepository())
    composeTestRule.setContent { CreateEventScreen(viewModel = viewModel, onCreated = onCreated) }

    viewModel.updateName("Summer festival")
    viewModel.updateStartDate(startDate)
    viewModel.updateEndDate(endDate)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVE_ERROR)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    verify(exactly = 0) { onCreated() }
  }

  private fun selectDate(tag: String, date: LocalDate) {
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    if (date == LocalDate.now()) {
      composeTestRule.onNodeWithText("Today", substring = true).performClick()
    } else {
      val dateDescription =
          DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
              .withLocale(Locale.getDefault())
              .format(date)
      composeTestRule.onNodeWithText(dateDescription, substring = true).performClick()
    }
    composeTestRule.onNodeWithText("OK").performClick()
  }

  private fun selectTime(tag: String, time: LocalTime) {
    require(time == LocalTime.of(9, 0)) { "Only the proposed time 09:00 is pickable." }
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  private fun waitForTag(tag: String) {
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
