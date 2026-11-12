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
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.event.details.EventDetailsScreen
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.details.EventDetailsViewModel
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneId
import java.time.ZonedDateTime
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
            EventDetailsScreenTestTags.STATUS_CHIP,
            EventDetailsScreenTestTags.SUBTITLE,
            EventDetailsScreenTestTags.SEE_TASKS_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }

    composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
    composeTestRule.onNodeWithText("${tasksListSize / 2}").assertIsDisplayed()
    composeTestRule.onNodeWithText("of $tasksListSize").assertIsDisplayed()
    composeTestRule.onNodeWithText("tasks still open").assertIsDisplayed()
  }

  /** Breaks if the dates and the place are not on one line under the title. */
  @Test
  fun subtitleShowsDatesAndPlace() {
    setContent(eventSameMonth)

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .assertTextEquals("Fri 16 to Sun 18 Oct · Parc de Vidy, Lausanne")
  }

  /**
   * Breaks if a long place is cut short instead of wrapping, since this is the only place it shows.
   */
  @Test
  fun subtitleShowsTheWholeLongPlace() {
    val place =
        "Main entrance, north-western courtyard beside the temporary festival information tent"
    setContent(eventSameMonth.copy(location = place))

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .assertTextEquals("Fri 16 to Sun 18 Oct · $place")

    val layouts = mutableListOf<TextLayoutResult>()
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .fetchSemanticsNode()
        .config[SemanticsActions.GetTextLayoutResult]
        .action
        ?.invoke(layouts)

    assertTrue(layouts.single().lineCount > 1)
    assertTrue((0 until layouts.single().lineCount).none { layouts.single().isLineEllipsized(it) })
  }

  /** Breaks if an event without a place shows a dangling separator or a placeholder. */
  @Test
  fun subtitleShowsOnlyDatesWithoutPlace() {
    setContent(eventSameMonth.copy(location = "  "))

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .assertTextEquals("Fri 16 to Sun 18 Oct")
  }

  /** Breaks if the description is not shown as plain text under the subtitle. */
  @Test
  fun descriptionShowsUnderSubtitle() {
    setContent(eventSameMonth.copy(description = "Bring a jacket."))

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.DESCRIPTION)
        .assertTextEquals("Bring a jacket.")
    val subtitleBottom =
        composeTestRule
            .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
            .getUnclippedBoundsInRoot()
            .bottom
    val descriptionTop =
        composeTestRule
            .onNodeWithTag(EventDetailsScreenTestTags.DESCRIPTION)
            .getUnclippedBoundsInRoot()
            .top
    val statTop =
        composeTestRule
            .onNodeWithTag(EventDetailsScreenTestTags.OPEN_TASKS_STAT)
            .getUnclippedBoundsInRoot()
            .top
    assertTrue(descriptionTop >= subtitleBottom)
    assertTrue(descriptionTop < statTop)
  }

  /** Breaks if an event without a description still shows an empty description block. */
  @Test
  fun descriptionIsAbsentWhenBlank() {
    setContent(eventSameMonth.copy(description = "  "))

    composeTestRule.onAllNodesWithTag(EventDetailsScreenTestTags.DESCRIPTION).assertCountEquals(0)
  }

  /** Breaks if a long description is cut off instead of reachable by scrolling. */
  @Test
  fun longDescriptionIsFullyReadableByScrolling() {
    val text = (1..80).joinToString(" ") { "Line$it of the long description." }
    setContent(eventSameMonth.copy(description = text))

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.DESCRIPTION).assertTextEquals(text)
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_ROW).performScrollTo()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_ROW).assertIsDisplayed()
  }

  /** Breaks if an open event does not show the Open chip. */
  @Test
  fun statusChipShowsOpenForOpenEvent() {
    setContent(eventSameMonth)
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.STATUS_CHIP)
        .onChild()
        .assertTextEquals("Open")
  }

  /** Breaks if a closed event still shows the Open chip. */
  @Test
  fun statusChipShowsClosedForClosedEvent() {
    setContent(eventSameMonth.copy(closed = true))
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.STATUS_CHIP)
        .onChild()
        .assertTextEquals("Closed")
  }

  /** Red if an event ending on its start day is formatted as a multi-day event. */
  @Test
  fun displaysSameDayEventDate() {
    setContent(eventSameDay)
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .assertTextEquals("Fri 16 Oct · Parc de Vidy, Lausanne")
  }

  /** Red if an event spanning one month is formatted without both endpoint dates. */
  @Test
  fun displaysDifferentMonthEventDate() {
    setContent(eventDifferentMonth)
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .assertTextEquals("Fri 16 Oct to Mon 16 Nov · Parc de Vidy, Lausanne")
  }

  /** Red if an event spanning one year omits either year from the displayed date. */
  @Test
  fun displaysDifferentYearEventDate() {
    setContent(eventDifferentYear)
    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.SUBTITLE)
        .assertTextEquals("Fri 16 Oct 2026 to Sat 16 Oct 2027 · Parc de Vidy, Lausanne")
  }

  /** Red if an event with no tasks displays incorrect task statistics. */
  @Test
  fun displaysNoTasksEventStatistics() {
    setContent(eventNoTasks)
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
    composeTestRule.onNodeWithText("of 0").assertIsDisplayed()
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
          onAnnouncementsClicked = {},
          onBack = {},
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.FETCH_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ANNOUNCEMENTS_ROW).assertDoesNotExist()
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

  /**
   * Breaks if the announcements row shows anything but its label, such as the title of an
   * announcement, since Figma shows the label alone.
   */
  @Test
  fun announcementsRowShowsOnlyItsLabel() {
    val announcement =
        Announcement(
            announcementId = "a1",
            title = "Stage B is closed",
            authorId = "user",
            updatedAt = eventStartDate,
        )

    setContent(eventSameMonth.copy(announcements = listOf(announcement)))

    composeTestRule
        .onNodeWithTag(EventDetailsScreenTestTags.ANNOUNCEMENTS_ROW)
        .assertIsDisplayed()
        .assertTextEquals("Announcements")
    composeTestRule.onNodeWithText(announcement.title).assertDoesNotExist()
  }

  /** Breaks if clicking the announcements row does not invoke the callback exactly once. */
  @Test
  fun clickingAnnouncementsRowCallsOnAnnouncementsClicked() {
    val onAnnouncementsClicked = mockk<() -> Unit>(relaxed = true)

    setContent(eventSameMonth, onAnnouncementsClicked = onAnnouncementsClicked)

    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ANNOUNCEMENTS_ROW).performClick()

    verify(exactly = 1) { onAnnouncementsClicked() }
  }

  /**
   * Breaks if the announcements row is not directly under the join code and above the members row,
   * as in Figma, for instance back above the join code.
   */
  @Test
  fun announcementsRowSitsBetweenTheJoinCodeAndTheMembers() {
    setContent(eventSameMonth)

    val joinCode = composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.JOIN_CODE_ROW)
    val announcements = composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ANNOUNCEMENTS_ROW)
    val members = composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.MEMBERS_CARD)
    val joinCodeBottom = joinCode.getUnclippedBoundsInRoot().bottom
    val announcementsBounds = announcements.getUnclippedBoundsInRoot()
    val membersTop = members.getUnclippedBoundsInRoot().top

    assertTrue(announcementsBounds.top >= joinCodeBottom)
    assertTrue(announcementsBounds.bottom <= membersTop)
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
      onAnnouncementsClicked: () -> Unit = {},
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
          onAnnouncementsClicked = onAnnouncementsClicked,
          onBack = onBack,
      )
    }
    composeTestRule.waitForIdle()
  }
}
