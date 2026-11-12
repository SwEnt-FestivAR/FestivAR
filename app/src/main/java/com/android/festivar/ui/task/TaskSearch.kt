// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import com.android.festivar.model.task.Task

/**
 * Whether this task answers the search [query]: its title or its location contains the trimmed
 * query, ignoring case. A blank query matches every task. Meant to be the one search rule of the
 * task lists.
 */
fun Task.matchesQuery(query: String): Boolean {
  val trimmed = query.trim()
  return trimmed.isEmpty() ||
      title.contains(trimmed, ignoreCase = true) ||
      location.contains(trimmed, ignoreCase = true)
}
