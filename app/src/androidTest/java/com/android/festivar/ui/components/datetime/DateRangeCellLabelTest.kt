// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that a calendar day is recognised on an English and on a French device. The language is
 * given to `labelsMatch`, so the result does not depend on the language of the emulator.
 */
class DateRangeCellLabelTest {

  private val firstOfNovember = LocalDate.of(2026, 11, 1)

  private fun label(date: LocalDate, locale: Locale) =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)

  /**
   * Breaks if a day is no longer found by its own label, in a month-first or a day-first language.
   */
  @Test
  fun aDayIsFoundByItsOwnLabelInEnglishAndInFrench() {
    for (locale in listOf(Locale.ENGLISH, Locale.FRENCH)) {
      val labels = listOf(label(firstOfNovember, locale))
      assertTrue(locale.toString(), labelsMatch(firstOfNovember, labels, locale))
    }
  }

  /**
   * Breaks if day 1 is taken for day 11 or 21: in French "1 novembre 2026" is inside "11 novembre
   * 2026". It cannot happen in English, where "November 1, 2026" is not inside "November 11, 2026".
   */
  @Test
  fun the1stIsNotMixedUpWithThe11thAndThe21stInFrench() {
    for (day in listOf(11, 21)) {
      val labels = listOf(label(LocalDate.of(2026, 11, day), Locale.FRENCH))
      assertFalse("day $day", labelsMatch(firstOfNovember, labels, Locale.FRENCH))
    }
  }
}
