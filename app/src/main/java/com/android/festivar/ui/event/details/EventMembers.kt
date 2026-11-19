// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User

/**
 * The members of the event as one list, from the highest role to the lowest: the owner first, then
 * the admins, and so on down to the visitors. Members with the same role keep the order of
 * [Event.members].
 */
internal fun Event.membersByRole(): List<User> = members.sortedByDescending {
  roleOf(it.uid).ordinal
}

/** The number of tasks of the event the member [userId] is assigned to. */
internal fun Event.tasksOf(userId: String): Int = tasks.count { task ->
  task.assignees.any { it.uid == userId }
}
