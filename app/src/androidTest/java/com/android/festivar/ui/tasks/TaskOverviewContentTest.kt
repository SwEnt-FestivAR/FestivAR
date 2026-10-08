// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.ui.tasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertTrue
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

  /** Turns red when a zero estimate shows as "0 min" or an odd one stops reading "1 h 1 min". */
  @Test
  fun aZeroEstimateIsHiddenAndAnOddOneReadsHoursAndMinutes() {
    val sections =
        listOf(
            OverviewSectionUi(
                OverviewGroup.ANYTIME,
                listOf(row("zero", 0), row("odd", 61)),
            )
        )
    setContent(TaskOverviewUiState(isLoading = false, sections = sections))

    composeTestRule.onNodeWithText("0 min").assertDoesNotExist()
    composeTestRule.onNodeWithText("1 h 1 min").assertIsDisplayed()
  }

  /** Turns red when the pin precedence (done, then mine, then taken, then open) changes. */
  @Test
  fun thePinSpeaksDoneOverMineOverTakenOverOpen() {
    val sections =
        listOf(
            OverviewSectionUi(
                OverviewGroup.ANYTIME,
                listOf(
                    row("done", completed = true, isMine = true, isFull = true),
                    row("mine", isMine = true, isFull = true),
                    row("taken", isFull = true),
                    row("open"),
                ),
            )
        )
    setContent(TaskOverviewUiState(isLoading = false, sections = sections))

    mapOf(
            "done" to "Done task",
            "mine" to "Your task",
            "taken" to "Taken task",
            "open" to "Open task",
        )
        .forEach { (id, spoken) ->
          composeTestRule
              .onNode(
                  hasTestTag(TaskOverviewScreenTestTags.row(id)) and hasContentDescription(spoken)
              )
              .assertIsDisplayed()
        }
  }

  /** Turns red when a dated section stops naming its day beside the part of the day. */
  @Test
  fun aDatedSectionShowsItsDay() {
    val day = LocalDate.of(2026, 10, 11)
    val sections = listOf(OverviewSectionUi(OverviewGroup.MORNING, listOf(row("a")), day))
    setContent(TaskOverviewUiState(isLoading = false, sections = sections))

    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.section(OverviewGroup.MORNING, day))
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("MORNING · SUN 11 OCT").assertIsDisplayed()
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

  /** Turns red when the error message or its retry stops showing, or retry stops calling back. */
  @Test
  fun anErrorShowsItsMessageAndRetryCallsBack() {
    var retried = false
    composeTestRule.setContent {
      TaskOverviewContent(
          TaskOverviewUiState(isLoading = false, errorMsg = "Network down"),
          onRetry = { retried = true },
      )
    }

    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Network down")
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.EMPTY).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.RETRY).performClick()
    assertTrue(retried)
  }

  /** Turns red when the first load shows the empty line or rows instead of the spinner. */
  @Test
  fun loadingShowsTheSpinnerAndNeitherTheEmptyLineNorRows() {
    setContent(TaskOverviewUiState(isLoading = true))

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.EMPTY).assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.section(OverviewGroup.NOW))
        .assertDoesNotExist()
  }

  /** Turns red when a refresh over existing rows hides them behind the spinner. */
  @Test
  fun aRefreshKeepsTheRowsAndHidesTheSpinner() {
    setContent(TaskOverviewUiState(isLoading = true, sections = rows))

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.LOADING).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.row("open")).assertIsDisplayed()
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
