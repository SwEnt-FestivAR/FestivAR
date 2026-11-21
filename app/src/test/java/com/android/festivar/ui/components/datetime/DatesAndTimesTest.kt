// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The shared row of a form: the Dates field and the Start and End times. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS-w412dp-h915dp")
class DatesAndTimesTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val saturday = LocalDate.of(2026, 10, 17)
  private val tags = DatesAndTimesTags()
  private var range by mutableStateOf(DateTimeRange())

  private fun show(
      initial: DateTimeRange,
      timesError: FieldError? = null,
      allowPast: Boolean = false,
      today: LocalDate = saturday,
      rowTags: DatesAndTimesTags = tags,
      enabled: Boolean = true,
  ) {
    range = initial
    composeTestRule.setContent {
      AppTheme {
        FestivarDatesAndTimes(
            range = range,
            onRangeChange = { range = it },
            enabled = enabled,
            timesError = timesError,
            allowPast = allowPast,
            today = today,
            tags = rowTags,
        )
      }
    }
  }

  private fun assertTimesMarkedAsError(marked: Boolean) {
    listOf(tags.startTime, tags.endTime).forEach {
      val node = composeTestRule.onNodeWithTag(it)
      val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
      if (marked) node.assert(hasError) else node.assert(!hasError)
    }
  }

  private fun assertField(tag: String, text: String) =
      composeTestRule.onNodeWithTag(tag).assertTextContains(text)

  private fun tapDay(date: LocalDate) {
    composeTestRule.onNode(cellOf(date)).performScrollTo().performClick()
  }

  /** Breaks if the row fills in any day or time by itself: a new form starts empty. */
  @Test
  fun anEmptyRangeShowsEmptyFieldsAndNoError() {
    show(DateTimeRange())
    listOf(tags.dates, tags.startTime, tags.endTime).forEach {
      val text =
          composeTestRule
              .onNodeWithTag(it)
              .fetchSemanticsNode()
              .config[SemanticsProperties.EditableText]
      assertEquals("", text.text)
    }
    composeTestRule.onNodeWithTag(tags.timesError).assertDoesNotExist()
  }

  /** Breaks if a one-day range shows a second day in the Dates field. */
  @Test
  fun aOneDayRangeShowsASingleDay() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    assertField(tags.dates, "Sat 17 Oct")
  }

  /** Breaks if a range shows only its first day, or repeats the month on both days. */
  @Test
  fun aRangeShowsBothDaysWithTheMonthOnce() {
    show(DateTimeRange(LocalDate.of(2026, 11, 20), LocalTime.of(18, 0), LocalDate.of(2026, 11, 22)))
    assertField(tags.dates, "Fri 20 to Sun 22 Nov")
  }

  /** Breaks if the Dates field does not open the range picker on the current days. */
  @Test
  fun theDatesFieldOpensTheRangePickerOnTheCurrentDays() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday.plusDays(1), LocalTime.of(2, 0)))
    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertIsDisplayed()
    composeTestRule
        .onNode(
            hasText("Sat 17 to Sun 18 Oct", substring = true) and
                hasAnyAncestor(hasTestTag(DateRangePickerTestTags.SUMMARY))
        )
        .assertIsDisplayed()
  }

  /** Breaks if choosing days in an empty row fills in a time, or loses a day. */
  @Test
  fun choosingDaysInAnEmptyRowSetsTheDaysAndLeavesTheTimesEmpty() {
    show(DateTimeRange())
    composeTestRule.onNodeWithTag(tags.dates).performClick()
    tapDay(saturday)
    tapDay(saturday.plusDays(2))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()

    assertEquals(DateTimeRange(saturday, null, saturday.plusDays(2), null), range)
  }

  /** Breaks if choosing other days in a filled row drops the times the user already set. */
  @Test
  fun choosingOtherDaysKeepsTheTimes() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    composeTestRule.onNodeWithTag(tags.dates).performClick()
    tapDay(saturday.plusDays(3))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()

    val chosen = saturday.plusDays(3)
    assertEquals(DateTimeRange(chosen, LocalTime.of(18, 0), chosen, LocalTime.of(20, 0)), range)
  }

  /**
   * Breaks if a row that is not enabled, for instance while its form is saving, can still open a
   * picker. The row has a day, so its times would be enabled if the row were.
   */
  @Test
  fun aDisabledRowCannotBeEdited() {
    show(
        DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)),
        enabled = false,
    )
    listOf(tags.dates, tags.startTime, tags.endTime).forEach {
      composeTestRule.onNodeWithTag(it).assertIsNotEnabled().performClick()
    }
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertDoesNotExist()
    composeTestRule.onNodeWithText("OK").assertDoesNotExist()
  }

  /** Breaks if an open picker is lost when the screen is recreated, for instance on rotation. */
  @Test
  fun anOpenPickerSurvivesTheScreenBeingRecreated() {
    val restoration = StateRestorationTester(composeTestRule)
    restoration.setContent {
      AppTheme {
        FestivarDatesAndTimes(
            range = DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)),
            onRangeChange = {},
            today = saturday,
        )
      }
    }
    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertIsDisplayed()

    restoration.emulateSavedInstanceStateRestore()

    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertIsDisplayed()
  }

  /** Breaks if saving the dates drops the times, or going back changes the days. */
  @Test
  fun savingTheDatesKeepsTheTimesAndGoingBackKeepsTheDates() {
    val initial = DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0))
    show(initial)

    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.BACK_BUTTON).performClick()
    assertEquals(initial, range)

    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(initial, range)
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.TITLE).assertDoesNotExist()
  }

  /** Breaks if saving the picker with no days keeps a time that has no day. */
  @Test
  fun savingNoDaysClearsTheTimesToo() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))

    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()

    assertEquals(DateTimeRange(), range)
  }

  /** Breaks if a time can be picked before any day, which would leave a time with no day. */
  @Test
  fun theTimesWaitForADay() {
    show(DateTimeRange())
    composeTestRule.onNodeWithTag(tags.startTime).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(tags.endTime).assertIsNotEnabled()

    range = DateTimeRange(saturday, endDate = saturday)
    composeTestRule.onNodeWithTag(tags.startTime).assertIsEnabled()
    composeTestRule.onNodeWithTag(tags.endTime).assertIsEnabled()
  }

  /** Breaks if the error given to the row is not shown under the times with both fields marked. */
  @Test
  fun anErrorGivenToTheRowIsShownUnderBothTimes() {
    show(
        DateTimeRange(saturday, LocalTime.of(20, 0), saturday, LocalTime.of(18, 0)),
        timesError = FieldError.END_BEFORE_START,
    )
    composeTestRule.onNodeWithTag(tags.timesError).assertIsDisplayed()
    composeTestRule.onNodeWithText("Ends before it starts").assertIsDisplayed()
    assertTimesMarkedAsError(true)
  }

  /** Breaks if the row finds an error of its own: the form decides which errors are shown. */
  @Test
  fun aRowWithoutAnErrorShowsNoneEvenWhenTheEndIsBeforeTheStart() {
    show(DateTimeRange(saturday, LocalTime.of(20, 0), saturday, LocalTime.of(18, 0)))
    composeTestRule.onNodeWithTag(tags.timesError).assertDoesNotExist()
    assertTimesMarkedAsError(false)
  }

  /** Breaks if picking the start time moves the end time: the row adds no rule on the times. */
  @Test
  fun pickingTheStartTimeLeavesTheEndTime() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    composeTestRule.onNodeWithTag(tags.startTime).performClick()
    composeTestRule.onNodeWithContentDescription("17 hours").performClick()
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(
        DateTimeRange(saturday, LocalTime.of(17, 0), saturday, LocalTime.of(20, 0)),
        range,
    )
  }

  /** Breaks if picking the end time changes the start time or the days. */
  @Test
  fun pickingTheEndTimeLeavesTheRestOfTheRange() {
    show(DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)))
    composeTestRule.onNodeWithTag(tags.endTime).performClick()
    composeTestRule.onNodeWithContentDescription("21 hours").performClick()
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(
        DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(21, 0)),
        range,
    )
  }

  /** Breaks if days already past are kept selected when the screen does not allow past days. */
  @Test
  fun pastDaysOpenEmptyWhenTheyAreNotAllowed() {
    show(
        DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)),
        today = saturday.plusDays(10),
    )
    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
  }

  /** Breaks if an edit screen cannot keep the days of an event that is already past. */
  @Test
  fun pastDaysStaySelectedWhenTheyAreAllowed() {
    show(
        DateTimeRange(saturday, LocalTime.of(18, 0), saturday, LocalTime.of(20, 0)),
        allowPast = true,
        today = saturday.plusDays(10),
    )
    composeTestRule.onNodeWithTag(tags.dates).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertIsDisplayed()
  }

  /** Breaks if the row ignores the tags of the screen, so its tests cannot find the fields. */
  @Test
  fun theFieldsUseTheTagsOfTheScreen() {
    val own = DatesAndTimesTags("a", "b", "c", "d")
    show(
        DateTimeRange(saturday, LocalTime.of(20, 0), saturday, LocalTime.of(18, 0)),
        timesError = FieldError.END_BEFORE_START,
        rowTags = own,
    )
    listOf(own.dates, own.startTime, own.endTime, own.timesError).forEach {
      composeTestRule.onNodeWithTag(it).assertIsDisplayed()
    }
    composeTestRule.onNodeWithTag(tags.dates).assertDoesNotExist()
  }

  /**
   * Breaks if a preview of the fields stops rendering, so a state that is shown for design review
   * is lost: each of the seven shows one Dates field, and the two error previews show their
   * message.
   */
  @Test
  fun everyPreviewShowsItsStateOfTheFields() {
    composeTestRule.setContent {
      Column {
        FestivarDatesAndTimesNothingChosenPreview()
        FestivarDatesAndTimesDayChosenPreview()
        FestivarDatesAndTimesOneDayPreview()
        FestivarDatesAndTimesRangePreview()
        FestivarDatesAndTimesEndBeforeStartPreview()
        FestivarDatesAndTimesOutdatedPreview()
        FestivarDatesAndTimesDisabledPreview()
      }
    }
    composeTestRule.onAllNodesWithTag(tags.dates).assertCountEquals(7)
    composeTestRule.onNodeWithText("Ends before it starts").assertExists()
    composeTestRule.onNodeWithText("Ends in the past").assertExists()
  }
}
