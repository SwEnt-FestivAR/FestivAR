// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User

/** The members from the highest role to the lowest, keeping their order within a role. */
internal fun Event.membersByRole(): List<User> = members.sortedByDescending {
  roleOf(it.uid).ordinal
}

/** The number of tasks of the event the member [userId] is assigned to. */
internal fun Event.tasksOf(userId: String): Int = tasks.count { task ->
  task.assignees.any { it.uid == userId }
}
