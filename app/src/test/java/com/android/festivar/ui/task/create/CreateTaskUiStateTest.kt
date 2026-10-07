// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTaskUiStateTest {
  private val day = LocalDate.of(2026, 10, 17)
  private val valid = CreateTaskUiState(title = "Run power to stage")

  @Test
  fun emptyForm_cannotBeCreated() {
    assertFalse(CreateTaskUiState().canCreate)
  }

  @Test
  fun blankTitle_cannotBeCreated() {
    assertFalse(valid.copy(title = "   ").canCreate)
  }

  @Test
  fun titleOnly_canBeCreated() {
    assertTrue(valid.canCreate)
    assertNull(valid.scheduleError)
  }

  @Test
  fun whileSaving_cannotBeCreatedAgain() {
    assertFalse(valid.copy(isSaving = true).canCreate)
  }

  @Test
  fun dateWithoutTime_startsAtMidnight() {
    assertEquals(day.atStartOfDay(), valid.copy(startDate = day).startDateTime)
  }

  @Test
  fun timeWithoutDate_isAnError() {
    assertEquals(
        ScheduleError.TIME_WITHOUT_DATE,
        valid.copy(startTime = LocalTime.of(9, 30)).scheduleError,
    )
    assertEquals(
        ScheduleError.TIME_WITHOUT_DATE,
        valid.copy(endTime = LocalTime.of(9, 50)).scheduleError,
    )
    assertFalse(valid.copy(startTime = LocalTime.of(9, 30)).canCreate)
  }

  @Test
  fun endBeforeStart_isAnError() {
    val state =
        valid.copy(
            startDate = day,
            startTime = LocalTime.of(9, 30),
            endDate = day,
            endTime = LocalTime.of(9, 29),
        )
    assertEquals(ScheduleError.END_BEFORE_START, state.scheduleError)
    assertFalse(state.canCreate)
  }

  @Test
  fun endEqualToStart_isValid() {
    val state =
        valid.copy(
            startDate = day,
            startTime = LocalTime.of(9, 30),
            endDate = day,
            endTime = LocalTime.of(9, 30),
        )
    assertNull(state.scheduleError)
  }

  @Test
  fun onlyOneBoundSet_isValid() {
    assertNull(valid.copy(endDate = day).scheduleError)
  }

  @Test
  fun toTask_copiesAndTrimsFields() {
    val task =
        valid
            .copy(
                title = "  Run power to stage ",
                description = " Extension reel is in the blue crate. ",
                location = " Stage, north corner ",
                startDate = day,
                startTime = LocalTime.of(9, 30),
                endDate = day,
                endTime = LocalTime.of(9, 50),
            )
            .toTask(taskId = "t1", eventId = "e1")

    assertEquals("t1", task.taskId)
    assertEquals("e1", task.eventId)
    assertEquals("Run power to stage", task.title)
    assertEquals("Extension reel is in the blue crate.", task.description)
    assertEquals("Stage, north corner", task.location)
    assertEquals(LocalDateTime.of(2026, 10, 17, 9, 30), task.startTime)
    assertEquals(LocalDateTime.of(2026, 10, 17, 9, 50), task.endTime)
  }

  @Test
  fun toTask_withoutSchedule_leavesTimesNull() {
    val task = valid.toTask("t1", "e1")
    assertNull(task.startTime)
    assertNull(task.endTime)
  }

  @Test
  fun toTask_withBlankTitle_isRejected() {
    assertThrows(IllegalArgumentException::class.java) { CreateTaskUiState().toTask("t1", "e1") }
  }

  @Test
  fun formatDate_usesShortWeekdayDayAndMonth() {
    assertEquals("Sat 17 Oct", formatDate(day))
  }

  @Test
  fun formatTime_usesTwentyFourHourClockWithPadding() {
    assertEquals("09:30", formatTime(LocalTime.of(9, 30)))
    assertEquals("00:05", formatTime(LocalTime.of(0, 5)))
    assertEquals("21:00", formatTime(LocalTime.of(21, 0)))
  }
}
