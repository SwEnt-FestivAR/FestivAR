// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring only: the screen shows the state it is given and calls back. The filter, count and
 * grouping rules are unit tested in [MyTasksViewModelTest], and the screen on its real ViewModel in
 * the My tasks route of [com.android.festivar.navigation.TaskNavGraphTest].
 */
@RunWith(AndroidJUnit4::class)
class MyTasksScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val today = LocalDate.now()

  /** Two sections, today and "any time", with no filter selected. */
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
              listOf(
                  MyTasksDaySection(
                      today,
                      listOf(
                          row("desk", today, Priority.MEDIUM, done = true),
                          row("power", today, Priority.URGENT, minutes = 28),
                      ),
                  ),
                  MyTasksDaySection(null, listOf(row("gear", null, Priority.NONE, event = null))),
              ),
          totalTasks = 3,
          isLoading = false,
          hasLoaded = true,
      )

  /** What the screen shows; a test changes it to move the screen to another state. */
  private val state = mutableStateOf(listed)

  /** Everything the screen called back, so a test can check what a click did. */
  private val calls = mutableListOf<String>()

  private fun show(initial: MyTasksUiState = listed) {
    state.value = initial
    composeTestRule.setContent {
      AppTheme {
        MyTasksContent(
            state = state.value,
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

  /**
   * Turns red when a section, a row or the words of a row stop showing, or when a tapped row stops
   * asking to open its task.
   */
  @Test
  fun rowsShowUnderTheirDayWithTheirWordsAndOpenTheirTask() {
    show()

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
   * Turns red when an unfolded chip loses its count or its call back, when "Clear" can be used with
   * nothing to clear or stops calling back, or when the folded zone stops recalling the active
   * filters.
   */
  @Test
  fun theFiltersZoneShowsTheChipsAndClearsThem() {
    show()
    tag(MyTasksScreenTestTags.FILTER_CHIPS).assertDoesNotExist()
    tag(MyTasksScreenTestTags.FILTERS_TOGGLE).performClick()
    tag(MyTasksScreenTestTags.FILTERS_CLEAR).assertIsNotEnabled()
    tag(MyTasksScreenTestTags.statusChip(MyTaskStatus.TODO)).assertTextContains("2").performClick()
    tag(MyTasksScreenTestTags.priorityChip(Priority.URGENT)).performClick()

    state.value = listed.copy(statusesSelected = setOf(MyTaskStatus.TODO))
    tag(MyTasksScreenTestTags.FILTERS_CLEAR).assertIsEnabled().performClick()
    assertEquals(listOf("status:TODO", "priority:URGENT", "clearChips"), calls)

    tag(MyTasksScreenTestTags.FILTERS_TOGGLE)
        .assertTextContains(str(R.string.mytasks_filters_count, 1))
        .performClick()
    tag(MyTasksScreenTestTags.FILTERS_SUMMARY).assertTextEquals(str(R.string.mytasks_status_todo))
  }

  /**
   * Turns red when the folded selector stops saying "All", the checked event or how many are
   * checked, when an unfolded line stops showing its title and count, naming a missing event,
   * telling whether it is checked, or calling back, or when the last of many events can no longer
   * be scrolled to.
   */
  @Test
  fun theEventsSelectorShowsTheEventsAndChecksThem() {
    show()
    val header = tag(MyTasksScreenTestTags.EVENTS_TOGGLE)
    header.assertTextContains(str(R.string.mytasks_events_all))
    state.value = listed.withSelected(FETE, GONE)
    header.assertTextContains(plural(R.plurals.mytasks_events_selected, 2))
    state.value = listed.withSelected(FETE)
    header.assertTextContains("Fête de l'Asso")

    header.performClick()
    composeTestRule.onNodeWithText("Fête de l'Asso · 2").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(str(R.string.mytasks_event_option, str(R.string.mytasks_unknown_event), 1))
        .assertIsDisplayed()
    tag(MyTasksScreenTestTags.eventOption(FETE)).assertIsOn()
    tag(MyTasksScreenTestTags.eventOption(GONE)).assertIsOff().performClick()
    tag(MyTasksScreenTestTags.EVENTS_CLEAR).assertIsEnabled().performClick()
    assertEquals(listOf("event:$GONE", "clearEvents"), calls)

    state.value =
        listed.copy(events = (1..20).map { EventFilterOption("e$it", "Event $it", 1, false) })
    tag(MyTasksScreenTestTags.eventOption("e20")).performScrollTo().assertIsDisplayed()
  }

  /**
   * Turns red when one of the bodies under the filters stops showing in its state: the spinner of
   * the first load, "No tasks yet", "No match" with its clear, or the error of a failed first load
   * with its Retry, which must not read as "No tasks yet".
   */
  @Test
  fun eachStateShowsItsBody() {
    show(MyTasksUiState())
    tag(MyTasksScreenTestTags.LOADING).assertIsDisplayed()
    tag(MyTasksScreenTestTags.LIST).assertDoesNotExist()

    state.value = MyTasksUiState(isLoading = false, hasLoaded = true)
    tag(MyTasksScreenTestTags.EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText(str(R.string.mytasks_empty_title)).assertIsDisplayed()

    state.value = listed.copy(sections = emptyList())
    tag(MyTasksScreenTestTags.NO_MATCH).assertIsDisplayed()
    tag(MyTasksScreenTestTags.CLEAR_FILTERS).performClick()

    state.value = MyTasksUiState(isLoading = false, errorMsg = R.string.mytasks_load_failed)
    // The snackbar is not clickable, so its text stays on its own node instead of merging into it
    tag(MyTasksScreenTestTags.ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText(str(R.string.mytasks_load_failed)).assertIsDisplayed()
    tag(MyTasksScreenTestTags.EMPTY).assertDoesNotExist()
    tag(MyTasksScreenTestTags.RETRY).performClick()

    assertEquals(listOf("clearFilters", "retry"), calls)
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

    /** A row at 14:30 on [day], or with no start when [day] is null. */
    fun row(
        id: String,
        day: LocalDate?,
        priority: Priority,
        done: Boolean = false,
        minutes: Int? = null,
        event: String? = "Fête de l'Asso",
    ): MyTaskRowUi {
      val time = LocalTime.of(14, 30)
      val start = day?.atTime(time)?.atZone(ZoneId.systemDefault())
      val task =
          Task(
              id,
              FETE,
              id,
              startTime = start,
              location = "Stage",
              priority = priority,
              completed = done,
              ownerId = "owner",
          )
      return MyTaskRowUi(task, event, time.takeIf { day != null }, minutes)
    }
  }
}
