// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// The screen reads "today" from the phone, so the preview days are built from it.
private val previewToday = LocalDate.now()

private const val FESTIVAL_ID = "e1"
private const val FESTIVAL = "Fête de l'Asso 2026"
private const val JAZZ_ID = "e2"
private const val JAZZ = "Soirée Jazz"

/** One row of the preview; it has no start when [day] or [time] is null. */
private fun previewRow(
    id: String,
    title: String,
    eventId: String,
    eventTitle: String,
    priority: Priority = Priority.NONE,
    location: String = "",
    day: LocalDate? = null,
    time: LocalTime? = null,
    completed: Boolean = false,
    minutesUntilStart: Int? = null,
): MyTaskRowUi {
  val start =
      if (day != null && time != null) ZonedDateTime.of(day, time, ZoneId.systemDefault()) else null
  return MyTaskRowUi(
      task =
          Task(
              taskId = id,
              eventId = eventId,
              title = title,
              location = location,
              priority = priority,
              startTime = start,
              completed = completed,
              ownerId = "owner",
          ),
      eventTitle = eventTitle,
      startTime = start?.toLocalTime(),
      minutesUntilStart = minutesUntilStart,
  )
}

private val previewSections =
    listOf(
        MyTasksDaySection(
            previewToday,
            listOf(
                previewRow(
                    "t1",
                    "Check the tickets",
                    FESTIVAL_ID,
                    FESTIVAL,
                    Priority.URGENT,
                    "North gate",
                    previewToday,
                    LocalTime.of(10, 0),
                    completed = true,
                ),
                previewRow(
                    "t2",
                    "Set up the bar",
                    FESTIVAL_ID,
                    FESTIVAL,
                    Priority.HIGH,
                    "Main stage",
                    previewToday,
                    LocalTime.of(14, 0),
                    minutesUntilStart = 28,
                ),
            ),
        ),
        MyTasksDaySection(
            previewToday.plusDays(1),
            listOf(
                previewRow(
                    "t3",
                    "Welcome the musicians",
                    JAZZ_ID,
                    JAZZ,
                    Priority.MEDIUM,
                    "Backstage",
                    previewToday.plusDays(1),
                    LocalTime.of(18, 30),
                )
            ),
        ),
        MyTasksDaySection(
            null,
            listOf(
                previewRow("t4", "Print the volunteer badges", FESTIVAL_ID, FESTIVAL, Priority.LOW)
            ),
        ),
    )

/** The user has four tasks across two events and no filter is on. */
private val previewLoadedState =
    MyTasksUiState(
        statusCounts = mapOf(MyTaskStatus.TODO to 3, MyTaskStatus.DONE to 1),
        priorityCounts =
            mapOf(
                Priority.URGENT to 1,
                Priority.HIGH to 1,
                Priority.MEDIUM to 1,
                Priority.LOW to 1,
                Priority.NONE to 0,
            ),
        events =
            listOf(
                EventFilterOption(FESTIVAL_ID, FESTIVAL, count = 3, selected = false),
                EventFilterOption(JAZZ_ID, JAZZ, count = 1, selected = false),
            ),
        sections = previewSections,
        totalTasks = 4,
        isLoading = false,
        hasLoaded = true,
    )

/** The tasks grouped by day: done, starting soon, tomorrow, and without a start. */
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
internal fun MyTasksListPreview() {
  AppTheme { MyTasksContent(previewLoadedState) }
}

/** The user has taken no task yet. */
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
internal fun MyTasksEmptyPreview() {
  AppTheme { MyTasksContent(MyTasksUiState(isLoading = false, hasLoaded = true)) }
}

/** The search and the Done chip hide every task: "Clear filters" is offered. */
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
internal fun MyTasksNoMatchPreview() {
  val state =
      previewLoadedState.copy(
          searchOpen = true,
          query = "badges",
          statusesSelected = setOf(MyTaskStatus.DONE),
          statusCounts = mapOf(MyTaskStatus.TODO to 1, MyTaskStatus.DONE to 0),
          priorityCounts = Priority.entries.associateWith { 0 },
          events = previewLoadedState.events.map { it.copy(count = 0) },
          sections = emptyList(),
      )
  AppTheme { MyTasksContent(state) }
}

/** A refresh failed: the last loaded tasks stay shown under the snackbar. */
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
internal fun MyTasksRefreshErrorPreview() {
  AppTheme { MyTasksContent(previewLoadedState.copy(errorMsg = R.string.mytasks_refresh_failed)) }
}
