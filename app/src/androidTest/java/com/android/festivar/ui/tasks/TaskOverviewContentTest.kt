package com.android.festivar.ui.tasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The stateless overview: what it renders for a given state, with no ViewModel. */
@RunWith(AndroidJUnit4::class)
class TaskOverviewContentTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun row(
      id: String,
      minutes: Int? = null,
      start: LocalTime? = null,
      completed: Boolean = false,
      isMine: Boolean = false,
      isFull: Boolean = false,
  ) =
      OverviewRowUi(
          task = Task(id, EVENT, "Task $id", completed = completed),
          location = "",
          peopleNeeded = 1,
          estimatedMinutes = minutes,
          startTime = start,
          assigned = if (isFull) 1 else 0,
          isMine = isMine,
          isFull = isFull,
      )

  private val rows =
      listOf(
          OverviewSectionUi(
              OverviewGroup.NOW,
              listOf(row("mine", 60, LocalTime.of(10, 0), isMine = true)),
          ),
          OverviewSectionUi(
              OverviewGroup.MORNING,
              listOf(row("done", 20, LocalTime.of(9, 0), completed = true)),
          ),
          OverviewSectionUi(
              OverviewGroup.AFTERNOON,
              listOf(row("full", 90, LocalTime.of(14, 0), isFull = true)),
          ),
          OverviewSectionUi(
              OverviewGroup.EVENING,
              listOf(row("late", start = LocalTime.of(19, 0))),
          ),
          OverviewSectionUi(OverviewGroup.ANYTIME, listOf(row("open"))),
      )

  private fun setContent(state: TaskOverviewUiState) {
    composeTestRule.setContent { TaskOverviewContent(state) }
  }

  /** Turns red when the length stops reading "<m> min", "<h> h" or "<h> h <m> min". */
  @Test
  fun lengthsAreWordedInMinutesHoursOrBoth() {
    setContent(TaskOverviewUiState(isLoading = false, sections = rows))

    composeTestRule.onNodeWithText("20 min").assertIsDisplayed()
    composeTestRule.onNodeWithText("1 h").assertIsDisplayed()
    composeTestRule.onNodeWithText("1 h 30 min").assertIsDisplayed()
  }

  /** Turns red when a done, mine, full or open row, or a section, stops rendering. */
  @Test
  fun everyKindOfRowAndEverySectionRenders() {
    setContent(TaskOverviewUiState(isLoading = false, sections = rows))

    rows.forEach { section ->
      composeTestRule
          .onNodeWithTag(TaskOverviewScreenTestTags.section(section.group))
          .assertIsDisplayed()
      section.rows.forEach {
        composeTestRule
            .onNodeWithTag(TaskOverviewScreenTestTags.row(it.task.taskId))
            .assertIsDisplayed()
      }
    }
  }

  /** Turns red when the error message stops showing under its tag. */
  @Test
  fun anErrorShowsItsMessage() {
    setContent(TaskOverviewUiState(isLoading = false, errorMsg = "Network down"))

    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Network down")
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.EMPTY).assertDoesNotExist()
  }

  /** Turns red when the empty line shows before the first load ends. */
  @Test
  fun loadingShowsNeitherTheEmptyLineNorRows() {
    setContent(TaskOverviewUiState(isLoading = true))

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.EMPTY).assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.section(OverviewGroup.NOW))
        .assertDoesNotExist()
  }

  /** Turns red when the open search field stops showing the state's query. */
  @Test
  fun anOpenSearchShowsTheQuery() {
    setContent(TaskOverviewUiState(isLoading = false, searchOpen = true, query = "bunt"))

    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.SEARCH_FIELD)
        .assertIsDisplayed()
        .assertTextContains("bunt")
  }

  private companion object {
    const val EVENT = "fete"
  }
}
