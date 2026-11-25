// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Matches the calendar cell of [date] in the range picker. A cell is described by its full date,
 * e.g. "November 21, 2026", not by its day number, so the month and year cannot be confused.
 */
internal fun cellOf(date: LocalDate) =
    SemanticsMatcher("is the cell of $date") { node ->
      val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
      val month = date.month.getDisplayName(TextStyle.FULL, Locale.US)
      texts.any { "$month ${date.dayOfMonth}, ${date.year}" in it }
    }
