package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.components.startDateError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal fun computeStartDateTime(
    date: LocalDate?,
    time: LocalTime?,
    zone: ZoneId,
): ZonedDateTime? = date?.atTime(time ?: START_OF_DAY)?.atZone(zone)

internal fun computeEndDateTime(
    date: LocalDate?,
    time: LocalTime?,
    zone: ZoneId,
): ZonedDateTime? = date?.atTime(time ?: END_OF_DAY)?.atZone(zone)

internal fun validateEventName(name: String, edited: Boolean): FieldError? =
    if (edited && name.isBlank()) FieldError.EMPTY_EVENT_NAME else null

internal fun validateStartDate(date: LocalDate?, time: LocalTime?): FieldError? =
    startDateError(date, time)

internal fun pickEventDate(
    picker: DateTimeField?,
    date: LocalDate,
    updateStartDate: (LocalDate) -> Unit,
    updateEndDate: (LocalDate) -> Unit,
) {
  when (picker) {
    DateTimeField.START_DATE -> updateStartDate(date)
    DateTimeField.END_DATE -> updateEndDate(date)
    else -> Unit
  }
}

internal fun pickEventTime(
    picker: DateTimeField?,
    time: LocalTime,
    updateStartTime: (LocalTime) -> Unit,
    updateEndTime: (LocalTime) -> Unit,
) {
  when (picker) {
    DateTimeField.START_TIME -> updateStartTime(time)
    DateTimeField.END_TIME -> updateEndTime(time)
    else -> Unit
  }
}
