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
 * Checks that a calendar day is recognised in an English and in a French device. The language is
 * given to `labelsMatch`, so the result does not depend on the language of the emulator.
 */
class DateRangeCellLabelTest {

  private val english = Locale.ENGLISH
  private val french = Locale.FRENCH

  private val firstOfNovember = LocalDate.of(2026, 11, 1)
  private val elevenOfNovember = LocalDate.of(2026, 11, 11)

  private fun label(date: LocalDate, locale: Locale) =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)

  @Test
  fun aDayIsFoundByItsOwnLabelInEnglish() {
    assertTrue(labelsMatch(firstOfNovember, listOf(label(firstOfNovember, english)), english))
  }

  @Test
  fun aDayIsFoundByItsOwnLabelInFrench() {
    assertTrue(labelsMatch(firstOfNovember, listOf(label(firstOfNovember, french)), french))
  }

  @Test
  fun the1stIsNotMixedUpWithThe11thInEnglish() {
    assertFalse(labelsMatch(firstOfNovember, listOf(label(elevenOfNovember, english)), english))
  }

  @Test
  fun the1stIsNotMixedUpWithThe11thInFrench() {
    assertFalse(labelsMatch(firstOfNovember, listOf(label(elevenOfNovember, french)), french))
  }
}
