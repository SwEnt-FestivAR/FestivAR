package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import android.annotation.SuppressLint
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.event.details.EventDetailsScreen
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.details.EventDetailsViewModel
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Like the ids the repository generates; its join code, 3F9A1C, is no task count. */
private const val EVENT_ID = "3f9a1c2e-7b4d-4e8a-9c15-d2a6b0f84e37"

@RunWith(AndroidJUnit4::class)
class EventDetailsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val eventStartDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  private val eventEndDate: ZonedDateTime =
      ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private val tasksListSize = 10

  private val eventSameMonth =
      Event(
          eventId = EVENT_ID,
          ownerId = "user",
          title = "Fête de l'Asso 2026",
          members = listOf(User(uid = "user", name = "Sami K.")),
          startDate = eventStartDate,
          endDate = eventEndDate,
          location = "Parc de Vidy, Lausanne",
          tasks =
              List(
                  size = tasksListSize,
                  init = { index ->
                    Task(
                        taskId = index.toString(),
                        eventId = EVENT_ID,
                        title = "Task $index",
                        ownerId = "user",
                        completed = index % 2 == 0,
                    )
                  },
              ),
      )

  private val eventSameDay =
      eventSameMonth.copy(endDate = eventEndDate.withDayOfMonth(eventStartDate.dayOfMonth))
  private val eventDifferentMonth = eventSameMonth.copy(endDate = eventStartDate.plusMonths(1))
  private val eventDifferentYear = eventSameMonth.copy(endDate = eventStartDate.plusYears(1))
  private val eventNoTasks = eventSameMonth.copy(tasks = emptyList())

  /** Red if the screen does not display the event information and expected controls. */
  @Test
  fun displaysEventDataAndTaggedElements() {
    val event = eventSameMonth

    setContent(event)

    listOf(
            EventDetailsScreenTestTags.BACK_BUTTON,
            EventDetailsScreenTestTags.EVENT_NAME,
            EventDetailsScreenTestTags.OPEN_TASKS_STAT,
            EventDetailsScreenTestTags.OWNER_ROW,
            EventDetailsScreenTestTags.DATE_ROW,
            EventDetailsScreenTestTags.ADDRESS_ROW,
            EventDetailsScreenTestTags.SEE_TASKS_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }

    composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sami K.").assertIsDisplayed()
    composeTestRule.onNodeWithText(event.location).assertIsDisplayed()
    composeTestRule.onNodeWithText("${tasksListSize / 2}").assertIsDisplayed()
    composeTestRule.onNodeWithText("of $tasksListSize").assertIsDisplayed()
    composeTestRule.onNodeWithText("tasks still open").assertIsDisplayed()
    composeTestRule.onNodeWithText("Fri 16 to Sun 18 Oct").assertIsDisplayed()
  }

  /** Red if an event ending on its start day is formatted as a multi-day event. */
  @Test
  fun displaysSameDayEventDate() {
    setContent(eventSameDay)
    composeTestRule.onNodeWithText("Fri 16 Oct").assertIsDisplayed()
  }

  /** Red if an event spanning one month is formatted without both endpoint dates. */
  @Test
  fun displaysDifferentMonthEventDate() {
    setContent(eventDifferentMonth)
    composeTestRule.onNodeWithText("Fri 16 Oct to Mon 16 Nov").assertIsDisplayed()
  }

  /** Red if an event spanning one year omits either year from the displayed date. */
  @Test
  fun displaysDifferentYearEventDate() {
    setContent(eventDifferentYear)
    composeTestRule.onNodeWithText("Fri 16 Oct 2026 to Sat 16 Oct 2027").assertIsDisplayed()
  }

  /** Red if an event with no tasks displays incorrect task statistics. */
  @Test
  fun displaysNoTasksEventStatistics() {
    setContent(eventNoTasks)
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
    composeTestRule.onNodeWithText("of 0").assertIsDisplayed()
  }

  /** Red if an empty location does not display the address fallback text. */
  @Test
  fun displaysFallbackWhenEventHasEmptyLocationString() {
    setContent(eventSameMonth.copy(location = ""))

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ADDRESS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithText("No location specified").assertIsDisplayed()
  }

  /** Red if a whitespace-only location is not treated as missing. */
  @Test
  fun displaysFallbackWhenEventHasWhitespaceOnlyLocationString() {
    setContent(eventSameMonth.copy(location = "     "))

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ADDRESS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithText("No location specified").assertIsDisplayed()
  }

  /** Red if a long location overflows its address row or is not ellipsized. */
  @Test
  fun truncatesLongLocationWithinAddressRow() {
    val longLocation =
        "Main entrance, north-western courtyard beside the temporary festival information tent"
    setContent(eventSameMonth.copy(location = longLocation))

    val rowBounds =
        composeTestRule
            .onNodeWithTag(EventDetailsScreenTestTags.ADDRESS_ROW)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
    val valueBounds =
        composeTestRule.onNodeWithText(longLocation).assertIsDisplayed().getUnclippedBoundsInRoot()

    assertTrue(valueBounds.right <= rowBounds.right)
    assertTrue(valueBounds.bottom <= rowBounds.bottom)

    val layouts = mutableListOf<TextLayoutResult>()
    composeTestRule
        .onNodeWithText(longLocation)
        .fetchSemanticsNode()
        .config[SemanticsActions.GetTextLayoutResult]
        .action
        ?.invoke(layouts)

    assertEquals(2, layouts.single().lineCount)
    assertTrue(layouts.single().isLineEllipsized(1))
  }

  /** Red if a fetch failure leaves event content or actions visible. */
  @SuppressLint("ViewModelConstructorInComposable")
  @Test
  fun displaysErrorWhenEventCannotBeFetched() {
    composeTestRule.setContent {
      EventDetailsScreen(
          viewModel =
              EventDetailsViewModel(
                  dataRepositoryWith(
                      object : EventsRepository by EventsRepositoryLocal() {
                        override suspend fun getEvent(eventId: String): Event {
                          throw IllegalStateException("fetch failed")
                        }
                      }
                  ),
                  eventId = "missing",
              ),
          onTasksButtonClicked = {},
          onBack = {},
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.FETCH_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.EVENT_NAME)
            .fetchSemanticsNodes()
            .isEmpty()
    )
    assertTrue(
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON)
            .fetchSemanticsNodes()
            .isEmpty()
    )
  }

  /** Red if clicking See tasks does not invoke the supplied callback exactly once. */
  @Test
  fun clickingSeeTasksButtonCallsOnTasksButtonClicked() {
    val onTasksButtonClicked = mockk<() -> Unit>(relaxed = true)
    val event = eventSameMonth

    setContent(event, onTasksButtonClicked = onTasksButtonClicked)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()

    verify(exactly = 1) { onTasksButtonClicked() }
  }

  /** Red if clicking Back does not invoke the supplied callback exactly once. */
  @Test
  fun clickingBackButtonCallsOnBack() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    val event = eventSameMonth

    setContent(event, onBack = onBack)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).performClick()

    verify(exactly = 1) { onBack() }
  }

  private fun dataRepositoryWith(eventsRepository: EventsRepository) =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
      )

  /**
   * Breaks if the code is not the first six letters or digits of the id, uppercased, with the
   * dashes of the id skipped, or if its label is missing.
   */
  @Test
  fun displaysJoinCodeWithItsLabel() {
    setContent(eventSameMonth.copy(eventId = "vidy-26-abc"))

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_ROW).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_VALUE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("VIDY26")
    composeTestRule.onNodeWithText(joinCodeLabel(), useUnmergedTree = true).assertIsDisplayed()
  }

  /** Breaks if the label and the code sit on different lines. */
  @Test
  fun joinCodeSitsOnOneLineWithItsLabel() {
    setContent(eventSameMonth)

    val label =
        composeTestRule
            .onNodeWithText(joinCodeLabel(), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    val code =
        composeTestRule
            .onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_VALUE, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    assertTrue(label.top < code.bottom && code.top < label.bottom)
    assertTrue(label.right <= code.left)
  }

  /**
   * Breaks if the join code row, or anything in it, can be clicked, for instance a share button.
   */
  @Test
  fun joinCodeIsPlainText() {
    setContent(eventSameMonth)

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_ROW)
        .assertIsDisplayed()
        .assertHasNoClickAction()
    composeTestRule
        .onAllNodes(
            hasClickAction() and
                hasAnyAncestor(hasTestTag(EventDetailsScreenTestTags.JOIN_CODE_ROW)),
            useUnmergedTree = true,
        )
        .assertCountEquals(0)
  }

  /** Breaks if a screen reader no longer reads the join code row as its label then its code. */
  @Test
  fun joinCodeRowIsReadAsOneItem() {
    setContent(eventSameMonth)

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_ROW)
        .assertTextEquals(joinCodeLabel(), "3F9A1C")
  }

  /** The label of the join code row, in capitals as the screen shows it. */
  private fun joinCodeLabel(): String =
      InstrumentationRegistry.getInstrumentation()
          .targetContext
          .getString(R.string.event_details_join_code)
          .uppercase()

  @SuppressLint("ViewModelConstructorInComposable")
  private fun setContent(
      event: Event,
      onTasksButtonClicked: () -> Unit = {},
      onBack: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      EventDetailsScreen(
          viewModel =
              EventDetailsViewModel(
                  dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
                  event.eventId,
              ),
          onTasksButtonClicked = onTasksButtonClicked,
          onBack = onBack,
      )
    }
    composeTestRule.waitForIdle()
  }
}
