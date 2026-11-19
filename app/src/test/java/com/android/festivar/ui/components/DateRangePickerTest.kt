// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
  private var saved: Pair<LocalDate, LocalDate>? = null
  private var dismissals = 0

  private fun show(
      start: LocalDate? = null,
      end: LocalDate? = null,
  ) {
    composeTestRule.setContent {
      AppTheme {
        FestivarDateRangePickerDialog(
            initialStart = start,
            initialEnd = end,
            onConfirm = { from, to -> saved = from to to },
            onDismiss = { dismissals++ },
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

  private fun saveIsEnabled(enabled: Boolean) {
    val save = composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON)
    if (enabled) save.assertIsEnabled() else save.assertIsNotEnabled()
  }

  /** Breaks if the dialog hides a part of the design: the bar, the calendar or Save. */
  @Test
  fun theDialogShowsItsBarItsCalendarAndSave() {
    show()
    listOf(
            DateRangePickerTestTags.BACK_BUTTON,
            DateRangePickerTestTags.TITLE,
            DateRangePickerTestTags.CALENDAR,
            DateRangePickerTestTags.SAVE_BUTTON,
        )
        .forEach { composeTestRule.onNodeWithTag(it).assertIsDisplayed() }
  }

  /** Breaks if Save dates can be pressed, or Clear is shown, while no day is chosen. */
  @Test
  fun withNoDayChosenSaveIsDisabledAndClearIsHidden() {
    show()
    saveIsEnabled(false)
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
  }

  /** Breaks if one tap does not select a day whose end is itself, or does not enable Save. */
  @Test
  fun oneTapSelectsASingleDayThatCanBeSavedAtOnce() {
    show()
    tap(LocalDate.of(2026, 11, 20))
    saveIsEnabled(true)
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(LocalDate.of(2026, 11, 20) to LocalDate.of(2026, 11, 20), saved)
  }

  /** Breaks if a second tap on a later day does not set the end of a range in one week. */
  @Test
  fun aSecondTapOnALaterDaySetsTheEndOfARangeInAWeek() {
    show()
    tap(LocalDate.of(2026, 11, 23))
    tap(LocalDate.of(2026, 11, 27))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(LocalDate.of(2026, 11, 23) to LocalDate.of(2026, 11, 27), saved)
  }

  /** Breaks if a range cannot go over the end of a month, or its length ignores the month. */
  @Test
  fun aRangeCanRunAcrossTwoMonths() {
    show()
    tap(LocalDate.of(2026, 11, 27))
    tap(LocalDate.of(2026, 12, 2))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(LocalDate.of(2026, 11, 27) to LocalDate.of(2026, 12, 2), saved)
  }

  /** Breaks if a tap before the start extends the range backwards instead of restarting it. */
  @Test
  fun aTapBeforeTheStartRestartsTheRange() {
    show()
    tap(LocalDate.of(2026, 11, 25))
    tap(LocalDate.of(2026, 11, 22))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(LocalDate.of(2026, 11, 22) to LocalDate.of(2026, 11, 22), saved)
  }

  /** Breaks if today itself is refused, which would leave nothing to pick for a same-day event. */
  @Test
  fun todayCanBeSelected() {
    show()
    tap(today)
    saveIsEnabled(true)
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

  /** Breaks if Clear leaves a day selected, keeps itself on screen or leaves Save enabled. */
  @Test
  fun clearEmptiesTheSelectionAndHidesItself() {
    show(start = LocalDate.of(2026, 11, 23), end = LocalDate.of(2026, 11, 27))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.CLEAR_BUTTON).assertDoesNotExist()
    saveIsEnabled(false)
  }

  /** Breaks if Back saves the choice, or does not tell the caller that the dialog closed. */
  @Test
  fun backDiscardsTheChoice() {
    show()
    tap(LocalDate.of(2026, 11, 20))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.BACK_BUTTON).performClick()
    assertEquals(1, dismissals)
    assertNull(saved)
  }

  /** Breaks if Save hands over anything but the first and the last day chosen. */
  @Test
  fun saveGivesTheOpeningRangeBackUnchanged() {
    show(start = LocalDate.of(2026, 11, 23), end = LocalDate.of(2026, 11, 27))
    composeTestRule.onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
    assertEquals(LocalDate.of(2026, 11, 23) to LocalDate.of(2026, 11, 27), saved)
    assertEquals(0, dismissals)
  }
}
