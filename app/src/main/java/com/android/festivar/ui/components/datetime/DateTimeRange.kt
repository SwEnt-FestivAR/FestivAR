// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components.datetime

import java.time.LocalDate
import java.time.LocalTime

/** The start and end picked in a form. Any part may still be unset. */
data class DateTimeRange(
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
)

/** Default start time when a date is selected without an explicit time. */
val START_OF_DAY: LocalTime = LocalTime.MIDNIGHT

/** Default end time when a date is selected without an explicit time. */
val END_OF_DAY: LocalTime = LocalTime.of(23, 59)
