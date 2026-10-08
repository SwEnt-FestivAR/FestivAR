// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction

/**
 * Matches the clickable cell of day [dayOfMonth] in a date picker.
 *
 * The Material date picker does not expose the day number as text: a day cell is described by its
 * full date, e.g. "Thursday, October 15, 2026". So the day is looked for as a whole number in that
 * description (or in the text, should a version expose it), which does not depend on the locale and
 * does not match the year or another day.
 */
fun hasDayOfMonth(dayOfMonth: Int): SemanticsMatcher {
  val whole = Regex("""(^|\D)$dayOfMonth(\D|$)""")
  val isTheDay =
      SemanticsMatcher("is the cell of day $dayOfMonth") { node ->
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        (descriptions + texts).any { whole.containsMatchIn(it) }
      }
  return isTheDay and hasClickAction()
}
