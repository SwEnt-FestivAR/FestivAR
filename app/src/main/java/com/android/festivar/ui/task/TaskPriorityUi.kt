// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import androidx.annotation.StringRes
import com.android.festivar.R
import com.android.festivar.model.task.Priority

/** The string resource that names [priority], e.g. on the chips and in the task cards. */
@StringRes
fun priorityLabelRes(priority: Priority): Int =
    when (priority) {
      Priority.NONE -> R.string.priority_none
      Priority.LOW -> R.string.priority_low
      Priority.MEDIUM -> R.string.priority_medium
      Priority.HIGH -> R.string.priority_high
      Priority.URGENT -> R.string.priority_urgent
    }
