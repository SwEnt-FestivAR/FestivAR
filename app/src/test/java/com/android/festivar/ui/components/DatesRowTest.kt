// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The shared row of a form: the Dates field and the Start and End times. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
class DatesRowTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val saturday = LocalDate.of(2026, 10, 17)
  private var range by mutableStateOf(DateTimeRange())

  private fun show(initial: DateTimeRange) {
    range = initial
    composeTestRule.setContent {
      AppTheme { FestivarDatesRow(range = range, onRangeChange = { range = it }) }
    }
  }

  private fun assertTimesMarkedAsError(marked: Boolean) {
    listOf(DatesRowTestTags.START_TIME_FIELD, DatesRowTestTags.END_TIME_FIELD).forEach {
      val node = composeTestRule.onNodeWithTag(it)
      val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
      if (marked) node.assert(hasError) else node.assert(!hasError)
    }
  }

  private fun assertField(tag: String, text: String) =
      composeTestRule.onNodeWithTag(tag).assertTextContains(text)

  /** Breaks if the row does not show the prefilled day and the hour after now, for two hours. */
  @Test
  fun theRowShowsTheDefaultsOfAForm() {
    show(DateTimeRange.prefilled(LocalDateTime.of(2026, 10, 17, 14, 20)))
    assertField(DatesRowTestTags.DATES_FIELD, "Sat 17 Oct")
    assertField(DatesRowTestTags.START_TIME_FIELD, "15:00")
    assertField(DatesRowTestTags.END_TIME_FIELD, "17:00")
    composeTestRule.onNodeWithTag(DatesRowTestTags.TIMES_ERROR).assertDoesNotExist()
  }

  /** Breaks if a range shows only its first day, or repeats the month on both days. */
  @Test
  fun aRangeShowsBothDaysWithTheMonthOnce() {
    show(DateTimeRange(LocalDate.of(2026, 11, 20), LocalTime.of(18, 0), LocalDate.of(2026, 11, 22)))
    assertField(DatesRowTestTags.DATES_FIELD, "Fri 20 to Sun 22 Nov")
  }

  /** Breaks if the Dates field does not open the range picker on the current days. */
  @Test
  fun theDatesFieldOpensTheRangePickerOnTheCurrentDays() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday.plusDays(1), LocalTime.of(2, 0)))
    composeTestRule.onNodeWithTag(DatesRowTestTags.DATES_FIELD).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sat 17 to Sun 18 Oct", substring = true).assertIsDisplayed()
  }

  /** Breaks if saving the dates drops the times, or going back changes the days. */
  @Test
  fun savingTheDatesKeepsTheTimesAndGoingBackKeepsTheDates() {
    val start = LocalDate.now()
    val initial = DateTimeRange(start, LocalTime.of(18, 0), start, LocalTime.of(20, 0))
    show(initial)

    composeTestRule.onNodeWithTag(DatesRowTestTags.DATES_FIELD).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.BACK_BUTTON).performClick()
    assertEquals(initial, range)

    composeTestRule.onNodeWithTag(DatesRowTestTags.DATES_FIELD).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(initial, range)
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertDoesNotExist()
  }

  /** Breaks if saving the picker with no days keeps the old days, or drops the times. */
  @Test
  fun savingNoDaysClearsTheDatesAndKeepsTheTimes() {
    val start = LocalDate.now()
    show(DateTimeRange(start, LocalTime.of(18, 0), start, LocalTime.of(20, 0)))

    composeTestRule.onNodeWithTag(DatesRowTestTags.DATES_FIELD).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()

    assertEquals(null, range.startDate)
    assertEquals(null, range.endDate)
    assertEquals(LocalTime.of(18, 0), range.startTime)
    assertEquals(LocalTime.of(20, 0), range.endTime)
  }

  /** Breaks if an end that is not after the start on one day is not reported under the times. */
  @Test
  fun aOneDayRangeEndingBeforeItsStartShowsTheErrorUnderTheTimes() {
    show(DateTimeRange(saturday, LocalTime.of(20, 0), saturday, LocalTime.of(18, 0)))
    composeTestRule.onNodeWithTag(DatesRowTestTags.TIMES_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Ends before it starts").assertIsDisplayed()
    assertTimesMarkedAsError(true)
  }

  /** Breaks if the error is shown for an end on a later day, which the picker allows. */
  @Test
  fun anEndOnALaterDayIsNotAnError() {
    show(DateTimeRange(saturday, LocalTime.of(20, 0), saturday.plusDays(1), LocalTime.of(2, 0)))
    composeTestRule.onNodeWithTag(DatesRowTestTags.TIMES_ERROR).assertDoesNotExist()
    assertTimesMarkedAsError(false)
  }

  /** Breaks if the end time stays where it was when the start time is moved. */
  @Test
  fun theEndTimeFollowsTheStartTime() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    pickStartHour(17)
    assertField(DatesRowTestTags.START_TIME_FIELD, "17:00")
    assertField(DatesRowTestTags.END_TIME_FIELD, "19:00")
    composeTestRule.onNodeWithTag(DatesRowTestTags.TIMES_ERROR).assertDoesNotExist()
  }

  /** Breaks if an end time pushed past midnight stays on the first day and shows an error. */
  @Test
  fun anEndTimePushedPastMidnightMovesToTheNextDay() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    pickStartHour(23)
    assertField(DatesRowTestTags.END_TIME_FIELD, "01:00")
    assertField(DatesRowTestTags.DATES_FIELD, "Sat 17 to Sun 18 Oct")
    composeTestRule.onNodeWithTag(DatesRowTestTags.TIMES_ERROR).assertDoesNotExist()
  }

  /** Breaks if picking an end time before the start does not raise the error. */
  @Test
  fun pickingAnEndTimeBeforeTheStartShowsTheError() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    composeTestRule.onNodeWithTag(DatesRowTestTags.END_TIME_FIELD).performClick()
    composeTestRule.onNodeWithContentDescription("17 hours").performClick()
    composeTestRule.onNodeWithText("OK").performClick()
    assertField(DatesRowTestTags.END_TIME_FIELD, "17:00")
    composeTestRule.onNodeWithText("Ends before it starts").assertIsDisplayed()
  }

  /** Opens the start time dialog and confirms [hour] o'clock. */
  private fun pickStartHour(hour: Int) {
    composeTestRule.onNodeWithTag(DatesRowTestTags.START_TIME_FIELD).performClick()
    composeTestRule.onNodeWithContentDescription("$hour hours").performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }
}
