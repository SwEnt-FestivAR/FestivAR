// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Matches the calendar cell of [date] in the range picker. A cell is described by its full date,
 * e.g. "November 21, 2026", not by its day number, so the month and year cannot be confused. The
 * label is the long date of the device language, looked for in the text and in the content
 * description, and not as a plain substring so that day 1 never matches day 11 or 21.
 */
fun cellOf(date: LocalDate) =
    SemanticsMatcher("is the cell of $date") { node ->
      val labels =
          node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
              node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
      labelsMatch(date, labels, Locale.getDefault())
    }

/**
 * True when one of [labels] contains the long date of [date] in [locale].
 *
 * Finding the date inside the text is not enough: "1 novembre 2026" is also found inside "11
 * novembre 2026". So no digit may come right before the date (`(?<!\d)`, "not preceded by a digit")
 * or right after it (`(?!\d)`, "not followed by a digit").
 */
internal fun labelsMatch(date: LocalDate, labels: List<String>, locale: Locale): Boolean {
  val longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
  val whole = Regex("(?<!\\d)" + Regex.escape(longDate) + "(?!\\d)")
  return labels.any { whole.containsMatchIn(it) }
}

/**
 * Opens the range picker from the Dates field [datesTag], scrolling to the field first, taps
 * [start] then [end] (a single tap when they are the same day) and saves.
 *
 * The calendar lists its months lazily, so the calendar is first scrolled to the month of each day,
 * whatever that month is.
 */
fun ComposeTestRule.pickDays(datesTag: String, start: LocalDate, end: LocalDate = start) {
  require(!end.isBefore(start)) { "The range must end on or after its start: $start to $end." }
  onNodeWithTag(datesTag).performScrollTo().performClick()
  tapDay(start)
  if (end != start) tapDay(end)
  onNodeWithTag(DateRangePickerTestTags.SAVE_BUTTON).performClick()
}

/**
 * Taps the cell of [date] after scrolling the calendar to its month. The calendar is a lazy list
 * with one row per month, from the first year Material offers, so the row of a month is known from
 * its date: scrolling to the cell itself cannot work, as a cell is inside a month row and not a row
 * of the list. The scroller is the calendar or one of its children, as the tag is on the picker
 * that Material draws.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun ComposeTestRule.tapDay(date: LocalDate) {
  val inTheCalendar = hasTestTag(DateRangePickerTestTags.CALENDAR)
  val monthRow = (date.year - DatePickerDefaults.YearRange.first) * 12 + date.monthValue - 1
  onAllNodes(hasScrollToIndexAction() and (inTheCalendar or hasAnyAncestor(inTheCalendar)))
      .onFirst()
      .performScrollToIndex(monthRow)
  onNode(cellOf(date)).performClick()
}
