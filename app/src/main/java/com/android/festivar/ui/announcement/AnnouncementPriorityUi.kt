// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import androidx.annotation.StringRes
import com.android.festivar.R
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.PillStyle

/** The string resource that names [priority], e.g. on the chips and the pills. */
@StringRes
fun announcementPriorityLabelRes(priority: AnnouncementPriority): Int =
    when (priority) {
      AnnouncementPriority.INFO -> R.string.create_announcement_priority_info
      AnnouncementPriority.WARNING -> R.string.create_announcement_priority_warning
      AnnouncementPriority.URGENT -> R.string.create_announcement_priority_urgent
    }

/** The look of the pill showing [priority]: red when urgent, amber for a warning, grey for info. */
fun announcementPriorityPillStyle(priority: AnnouncementPriority): PillStyle =
    when (priority) {
      AnnouncementPriority.INFO -> PillStyle.MUTED
      AnnouncementPriority.WARNING -> PillStyle.AMBER
      AnnouncementPriority.URGENT -> PillStyle.ERROR
    }
