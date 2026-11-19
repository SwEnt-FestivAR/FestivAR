// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The date range dialog, with the calendar tapped like a user would. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS-w412dp-h915dp")
class DateRangePickerTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val today = LocalDate.of(2026, 11, 18)
  /** What the dialog handed to its caller, one entry per Save: `null` is "no dates". */
  private val saved = mutableListOf<Pair<LocalDate, LocalDate>?>()

  private var dismissals = 0

  private fun show(
      start: LocalDate? = null,
      end: LocalDate? = null,
      allowPast: Boolean = false,
  ) {
    composeTestRule.setContent {
      AppTheme {
        FestivarDateRangePickerDialog(
            initialStart = start,
            initialEnd = end,
            onConfirm = { saved.add(it) },
            onDismiss = { dismissals++ },
            allowPast = allowPast,
            today = today,
        )
      }
    }
  }

  /**
   * Matches the cell of [date], whose text the picker writes as e.g. "Friday, November 27, 2026".
   */
  private fun cellOf(date: LocalDate) =
      SemanticsMatcher("is the cell of $date") { node ->
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val month = date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
        texts.any { "$month ${date.dayOfMonth}, ${date.year}" in it }
      }

  private fun tap(date: LocalDate) {
    composeTestRule.onNode(cellOf(date)).performScrollTo().performClick()
  }

  /** Breaks if the dialog hides a part of the design: the bar, the summary or the calendar. */
  @Test
  fun theDialogShowsItsBarItsSummaryItsCalendarAndSave() {
    show()
    listOf(
            DateRangePickerTestTags.BACK_BUTTON,
            DateRangePickerTestTags.TITLE,
            DateRangePickerTestTags.SUMMARY,
            DateRangePickerTestTags.CALENDAR,
            DateRangePickerTestTags.SAVE_BUTTON,
        )
        .forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }
  }

  /** Breaks if Clear is shown, or Save dates cannot remove the dates, while no day is chosen. */
  @Test
  fun withNoDayChosenSaveRemovesTheDatesAndClearIsHidden() {
    show()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithText("Tap a start day, then an end day").assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(listOf(null), saved)
  }

  /** Breaks if one tap does not select a day whose end is itself, or does not enable Save. */
  @Test
  fun oneTapSelectsASingleDayThatCanBeSavedAtOnce() {
    show()
    tap(LocalDate.of(2026, 11, 20))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertExists()
    composeTestRule.onNodeWithText("Fri 20 Nov", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("1 day", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(listOf(LocalDate.of(2026, 11, 20) to LocalDate.of(2026, 11, 20)), saved)
  }

  /** Breaks if a second tap on a later day does not set the end of a range in one week. */
  @Test
  fun aSecondTapOnALaterDaySetsTheEndOfARangeInAWeek() {
    show()
    tap(LocalDate.of(2026, 11, 23))
    tap(LocalDate.of(2026, 11, 27))
    composeTestRule.onNodeWithText("Mon 23 to Fri 27 Nov", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("5 days", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(listOf(LocalDate.of(2026, 11, 23) to LocalDate.of(2026, 11, 27)), saved)
  }

  /** Breaks if a range cannot go over the end of a month, or its length ignores the month. */
  @Test
  fun aRangeCanRunAcrossTwoMonths() {
    show()
    tap(LocalDate.of(2026, 11, 27))
    tap(LocalDate.of(2026, 12, 2))
    composeTestRule.onNodeWithText("Fri 27 Nov to Wed 2 Dec", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("6 days", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(listOf(LocalDate.of(2026, 11, 27) to LocalDate.of(2026, 12, 2)), saved)
  }

  /** Breaks if a tap before the start extends the range backwards instead of restarting it. */
  @Test
  fun aTapBeforeTheStartRestartsTheRange() {
    show()
    tap(LocalDate.of(2026, 11, 25))
    tap(LocalDate.of(2026, 11, 22))
    composeTestRule.onNodeWithText("Sun 22 Nov", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("1 day", substring = true).assertIsDisplayed()
  }

  /** Breaks if a day before today can be selected when past days are not allowed. */
  @Test
  fun aPastDayCannotBeSelected() {
    show()
    tap(LocalDate.of(2026, 11, 17))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithText("Tap a start day, then an end day").assertIsDisplayed()
  }

  /** Breaks if today itself is refused, which would leave nothing to pick for a same-day event. */
  @Test
  fun todayCanBeSelected() {
    show()
    tap(today)
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertExists()
    composeTestRule.onNodeWithText("Wed 18 Nov", substring = true).assertIsDisplayed()
  }

  /** Breaks if the parameter for the edit screens does not open the past days. */
  @Test
  fun aPastDayCanBeSelectedWhenPastDaysAreAllowed() {
    show(allowPast = true)
    tap(LocalDate.of(2026, 11, 17))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertExists()
    composeTestRule.onNodeWithText("Tue 17 Nov", substring = true).assertIsDisplayed()
  }

  /** Breaks if the dialog ignores the dates it is opened with or loses their end. */
  @Test
  fun itOpensWithTheCurrentRangeSelected() {
    show(start = LocalDate.of(2026, 12, 4), end = LocalDate.of(2026, 12, 6))
    composeTestRule.onNodeWithText("Fri 4 to Sun 6 Dec", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("3 days", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertExists()
  }

  /** Breaks if the calendar opens on another month than the one of the current dates. */
  @Test
  fun itOpensOnTheMonthOfTheCurrentDates() {
    show(start = LocalDate.of(2027, 2, 4), end = LocalDate.of(2027, 2, 6))
    composeTestRule.onNode(cellOf(LocalDate.of(2027, 2, 4))).assertIsDisplayed()
  }

  /** Breaks if the calendar opens on another month than today's while nothing is selected. */
  @Test
  fun itOpensOnTodaysMonthWhenEmpty() {
    show()
    composeTestRule.onNode(cellOf(today)).assertIsDisplayed()
  }

  /**
   * Breaks if tapping the summary line selects or changes a day: the calendar is the only input.
   */
  @Test
  fun theSummaryLineIsDisplayOnly() {
    show()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SUMMARY).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithText("Tap a start day, then an end day").assertIsDisplayed()
  }

  /** Breaks if the calendar moves when a first day is chosen, because the summary changes size. */
  @Test
  fun theSummaryLineKeepsItsSizeWhenADayIsChosen() {
    show()
    val summary = composeTestRule.onNodeWithTag(DateRangePickerTestTags.SUMMARY)
    val empty = summary.getBoundsInRoot()
    tap(LocalDate.of(2026, 11, 25))
    assertEquals(empty, summary.getBoundsInRoot())
  }

  /** Breaks if an end before the start crashes the dialog instead of being ignored. */
  @Test
  fun anEndBeforeTheStartIsIgnored() {
    show(start = LocalDate.of(2026, 12, 4), end = LocalDate.of(2026, 12, 1))
    composeTestRule.onNodeWithText("Fri 4 Dec", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("1 day", substring = true).assertIsDisplayed()
  }

  /** Breaks if a range over New Year shows no years, so that 28 Dec to 3 Jan reads as one week. */
  @Test
  fun aRangeOverNewYearShowsItsYears() {
    show(start = LocalDate.of(2026, 12, 28), end = LocalDate.of(2027, 1, 3))
    composeTestRule
        .onNodeWithText("Mon 28 Dec 2026 to Sun 3 Jan 2027", substring = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("7 days", substring = true).assertIsDisplayed()
  }

  /** Breaks if a range that starts in the past opens selected while past days are refused. */
  @Test
  fun aPastRangeOpensEmptyWhenPastDaysAreRefused() {
    show(start = LocalDate.of(2026, 11, 10), end = LocalDate.of(2026, 11, 12))
    composeTestRule.onNodeWithText("Tap a start day, then an end day").assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
  }

  /** Breaks if a past range is dropped when past days are allowed, as on the edit screens. */
  @Test
  fun aPastRangeOpensSelectedWhenPastDaysAreAllowed() {
    show(start = LocalDate.of(2026, 11, 10), end = LocalDate.of(2026, 11, 12), allowPast = true)
    composeTestRule.onNodeWithText("Tue 10 to Thu 12 Nov", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertExists()
  }

  /** Breaks if Clear leaves a day selected or keeps itself on screen. */
  @Test
  fun clearEmptiesTheSelectionAndHidesItself() {
    show(start = LocalDate.of(2026, 11, 23), end = LocalDate.of(2026, 11, 27))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).performClick()
    composeTestRule.onNodeWithText("Tap a start day, then an end day").assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
  }

  /** Breaks if Back saves the choice, or does not tell the caller that the dialog closed. */
  @Test
  fun backDiscardsTheChoice() {
    show()
    tap(LocalDate.of(2026, 11, 20))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.BACK_BUTTON).performClick()
    assertEquals(1, dismissals)
    assertEquals(emptyList<Pair<LocalDate, LocalDate>?>(), saved)
  }

  /** Breaks if Save hands over anything but the first and the last day chosen. */
  @Test
  fun saveGivesTheOpeningRangeBackUnchanged() {
    show(start = LocalDate.of(2026, 11, 23), end = LocalDate.of(2026, 11, 27))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(listOf(LocalDate.of(2026, 11, 23) to LocalDate.of(2026, 11, 27)), saved)
    assertEquals(0, dismissals)
  }
}
