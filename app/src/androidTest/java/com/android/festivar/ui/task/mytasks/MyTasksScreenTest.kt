// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring only: the screen shows the state it is given and calls back. The filter, count and
 * grouping rules are unit tested in [MyTasksViewModelTest].
 */
@RunWith(AndroidJUnit4::class)
class MyTasksScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val today = LocalDate.now()

  private val power = row("power", "Run power to stage", today, Priority.URGENT, minutes = 28)
  private val desk = row("desk", "Set up ticket desk", today, Priority.MEDIUM, done = true)
  private val gear = row("gear", "Return the sound gear", null, Priority.NONE, eventTitle = null)

  /** A list of two sections, today and "any time", with no filter selected. */
  private val listed =
      MyTasksUiState(
          statusCounts = mapOf(MyTaskStatus.TODO to 2, MyTaskStatus.DONE to 1),
          priorityCounts = mapOf(Priority.URGENT to 1, Priority.MEDIUM to 1, Priority.NONE to 1),
          events =
              listOf(
                  EventFilterOption(FETE, "Fête de l'Asso", 2, selected = false),
                  EventFilterOption(GONE, null, 1, selected = false),
              ),
          sections =
              listOf(MyTasksDaySection(today, listOf(desk, power)), MyTasksDaySection(null, listOf(gear))),
          totalTasks = 3,
          isLoading = false,
          hasLoaded = true,
      )

  /** Everything the screen called back, so a test can check what a click did. */
  private val calls = mutableListOf<String>()

  private fun show(state: MyTasksUiState) {
    composeTestRule.setContent {
      AppTheme {
        MyTasksContent(
            state = state,
            onToggleSearch = { calls += "search" },
            onQueryChange = { calls += "query:$it" },
            onToggleStatus = { calls += "status:$it" },
            onTogglePriority = { calls += "priority:$it" },
            onToggleEvent = { calls += "event:$it" },
            onClearChips = { calls += "clearChips" },
            onClearEvents = { calls += "clearEvents" },
            onClearFilters = { calls += "clearFilters" },
            onRetry = { calls += "retry" },
            onOpenTask = { calls += "open:$it" },
        )
      }
    }
  }

  private fun tag(tag: String) = composeTestRule.onNodeWithTag(tag)

  /** Turns red when a section, a row or the words of a row stop showing. */
  @Test
  fun rowsShowUnderTheirDayWithTheirWords() {
    show(listed)

    // Section labels are drawn in capitals
    tag(MyTasksScreenTestTags.section(today))
        .assertTextContains(str(R.string.mytasks_today), substring = true, ignoreCase = true)
    tag(MyTasksScreenTestTags.section(null))
        .assertTextContains(str(R.string.mytasks_anytime), ignoreCase = true)
    tag(MyTasksScreenTestTags.row("power"))
        .assertIsDisplayed()
        .assertTextContains(str(R.string.mytasks_in_minutes, 28))
        .assertTextContains("${str(R.string.priority_urgent)} · Stage · Fête de l'Asso")
    tag(MyTasksScreenTestTags.row("desk")).assertTextContains(str(R.string.mytasks_done))
    tag(MyTasksScreenTestTags.row("gear")).assertTextContains("Stage")
  }

  /** Turns red when a tapped row stops asking to open its task. */
  @Test
  fun tappingARowOpensItsTask() {
    show(listed)

    tag(MyTasksScreenTestTags.row("power")).performClick()

    assertEquals(listOf("open:power"), calls)
  }

  /** Turns red when the search icon or the search field stop calling back. */
  @Test
  fun theSearchIconAndFieldCallBack() {
    show(listed.copy(searchOpen = true))

    tag(MyTasksScreenTestTags.SEARCH).performClick()
    tag(MyTasksScreenTestTags.SEARCH_FIELD).performTextInput("bar")

    assertEquals(listOf("search", "query:bar"), calls)
  }

  /**
   * Turns red when the folded Filters zone stops recalling the active filters, when an unfolded
   * chip loses its count or its call back, or when "Clear" works with nothing to clear.
   */
  @Test
  fun theFiltersZoneShowsTheChipsAndClearsThem() {
    show(listed.copy(statusesSelected = setOf(MyTaskStatus.TODO)))
    tag(MyTasksScreenTestTags.FILTERS_SUMMARY).assertTextEquals(str(R.string.mytasks_status_todo))
    tag(MyTasksScreenTestTags.FILTER_CHIPS).assertDoesNotExist()

    tag(MyTasksScreenTestTags.FILTERS_TOGGLE)
        .assertTextContains(str(R.string.mytasks_filters_count, 1))
        .performClick()
    tag(MyTasksScreenTestTags.statusChip(MyTaskStatus.TODO)).assertTextContains("2").performClick()
    tag(MyTasksScreenTestTags.priorityChip(Priority.URGENT)).performClick()
    tag(MyTasksScreenTestTags.FILTERS_CLEAR).assertIsEnabled().performClick()

    assertEquals(listOf("status:TODO", "priority:URGENT", "clearChips"), calls)
  }

  /** Turns red when "Clear" of the Filters zone can be used with no chip selected. */
  @Test
  fun clearIsDisabledWithNoChipSelected() {
    show(listed)

    tag(MyTasksScreenTestTags.FILTERS_TOGGLE).performClick()

    tag(MyTasksScreenTestTags.FILTERS_CLEAR).assertIsNotEnabled()
  }

  /**
   * Turns red when the events selector stops naming its value, or an unfolded line stops showing
   * its title and count, naming a missing event, or calling back.
   */
  @Test
  fun theEventsSelectorShowsTheEventsAndChecksThem() {
    show(listed.withSelected(FETE))
    tag(MyTasksScreenTestTags.EVENTS_TOGGLE).assertTextContains("Fête de l'Asso", true)

    tag(MyTasksScreenTestTags.EVENTS_TOGGLE).performClick()
    composeTestRule.onNodeWithText("Fête de l'Asso · 2").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(str(R.string.mytasks_event_option, str(R.string.mytasks_unknown_event), 1))
        .assertIsDisplayed()
    tag(MyTasksScreenTestTags.eventOption(GONE)).performClick()
    tag(MyTasksScreenTestTags.EVENTS_CLEAR).assertIsEnabled().performClick()

    assertEquals(listOf("event:$GONE", "clearEvents"), calls)
  }

  /** Turns red when the folded selector stops saying "All" or how many events are checked. */
  @Test
  fun theFoldedSelectorSaysAllOrHowManyEvents() {
    show(listed.withSelected(FETE, GONE))

    tag(MyTasksScreenTestTags.EVENTS_TOGGLE)
        .assertTextContains(plural(R.plurals.mytasks_events_selected, 2), true)
  }

  /** Turns red when the first load shows something else than the spinner. */
  @Test
  fun aFirstLoadShowsTheSpinner() {
    show(MyTasksUiState())

    tag(MyTasksScreenTestTags.LOADING).assertIsDisplayed()
    tag(MyTasksScreenTestTags.LIST).assertDoesNotExist()
  }

  /** Turns red when a user with no task stops seeing the "No tasks yet" state. */
  @Test
  fun noTaskShowsTheEmptyState() {
    show(MyTasksUiState(isLoading = false, hasLoaded = true))

    tag(MyTasksScreenTestTags.EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText(str(R.string.mytasks_empty_title)).assertIsDisplayed()
  }

  /** Turns red when filters hiding every task stop offering to clear them. */
  @Test
  fun filtersHidingEverythingOfferToClearThem() {
    show(listed.copy(sections = emptyList()))

    tag(MyTasksScreenTestTags.NO_MATCH).assertIsDisplayed()
    tag(MyTasksScreenTestTags.CLEAR_FILTERS).performClick()

    assertEquals(listOf("clearFilters"), calls)
  }

  /**
   * Turns red when a failed first load stops showing its message and Retry, or reads as "No tasks
   * yet".
   */
  @Test
  fun aFailedLoadShowsItsMessageAndRetries() {
    show(MyTasksUiState(isLoading = false, errorMsg = R.string.mytasks_load_failed))

    tag(MyTasksScreenTestTags.ERROR).assertTextContains(str(R.string.mytasks_load_failed))
    tag(MyTasksScreenTestTags.EMPTY).assertDoesNotExist()
    tag(MyTasksScreenTestTags.RETRY).performClick()

    assertEquals(listOf("retry"), calls)
  }

  /**
   * Turns red when [MyTasksScreen] stops loading the ViewModel's tasks on open or stops passing a
   * tapped row on.
   */
  @Test
  fun theScreenLoadsTheViewModelAndOpensATask() {
    val me = User("me", tasksAssigned = listOf("power"))
    val start = ZonedDateTime.now().plusDays(1)
    val tasks = TasksRepositoryLocal()
    runBlocking {
      tasks.addTask(
          Task("power", FETE, "Run power to stage", startTime = start, assignees = listOf(me),
              ownerId = me.uid)
      )
    }
    val data =
        DataRepositoryLocal(
            FakeAuthRepository(),
            tasks,
            EventsRepositoryLocal(listOf(Event(FETE, "Fête", startDate = start, endDate = start.plusDays(1)))),
            UsersRepositoryLocal(listOf(me)),
        )
    var opened: String? = null
    composeTestRule.setContent {
      AppTheme { MyTasksScreen(MyTasksViewModel(me.uid, data), onOpenTask = { opened = it }) }
    }
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(MyTasksScreenTestTags.row("power"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    tag(MyTasksScreenTestTags.row("power")).performClick()

    assertEquals("power", opened)
  }

  /** This state with exactly the events [ids] checked, as the ViewModel would build it. */
  private fun MyTasksUiState.withSelected(vararg ids: String) =
      copy(
          eventIdsSelected = ids.toSet(),
          events = events.map { it.copy(selected = it.eventId in ids) },
      )

  private fun str(@StringRes id: Int, vararg args: Any): String =
      InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

  private fun plural(@PluralsRes id: Int, count: Int): String =
      InstrumentationRegistry.getInstrumentation()
          .targetContext
          .resources
          .getQuantityString(id, count, count)

  private companion object {
    const val FETE = "fete"
    const val GONE = "gone"

    /** A row of the Fête de l'Asso at 14:30 on [day], or with no start when [day] is null. */
    fun row(
        id: String,
        title: String,
        day: LocalDate?,
        priority: Priority,
        done: Boolean = false,
        minutes: Int? = null,
        eventTitle: String? = "Fête de l'Asso",
    ): MyTaskRowUi {
      val time = LocalTime.of(14, 30)
      val task =
          Task(id, FETE, title, startTime = day?.atTime(time)?.atZone(ZoneId.systemDefault()),
              location = "Stage", priority = priority, completed = done, ownerId = "owner")
      return MyTaskRowUi(task, eventTitle, time.takeIf { day != null }, minutes)
    }
  }
}
