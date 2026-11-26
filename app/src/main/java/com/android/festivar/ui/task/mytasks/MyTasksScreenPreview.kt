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

private const val PREVIEW_OWNER = "owner"

/** The title of each preview event, by its id. */
private val previewEventTitles = mapOf(FESTIVAL_ID to FESTIVAL, JAZZ_ID to JAZZ)

/** [hour]:[minute] on [day], in the time zone of the phone. */
private fun previewAt(day: LocalDate, hour: Int, minute: Int): ZonedDateTime =
    ZonedDateTime.of(day, LocalTime.of(hour, minute), ZoneId.systemDefault())

/** The row of [task], under the title of its event. */
private fun previewRow(task: Task, minutesUntilStart: Int? = null) =
    MyTaskRowUi(
        task = task,
        eventTitle = previewEventTitles[task.eventId],
        startTime = task.startTime?.toLocalTime(),
        minutesUntilStart = minutesUntilStart,
    )

private val previewSections =
    listOf(
        MyTasksDaySection(
            previewToday,
            listOf(
                previewRow(
                    Task(
                        taskId = "t1",
                        eventId = FESTIVAL_ID,
                        title = "Check the tickets",
                        location = "North gate",
                        priority = Priority.URGENT,
                        startTime = previewAt(previewToday, 10, 0),
                        completed = true,
                        ownerId = PREVIEW_OWNER,
                    )
                ),
                previewRow(
                    Task(
                        taskId = "t2",
                        eventId = FESTIVAL_ID,
                        title = "Set up the bar",
                        location = "Main stage",
                        priority = Priority.HIGH,
                        startTime = previewAt(previewToday, 14, 0),
                        ownerId = PREVIEW_OWNER,
                    ),
                    minutesUntilStart = 28,
                ),
            ),
        ),
        MyTasksDaySection(
            previewToday.plusDays(1),
            listOf(
                previewRow(
                    Task(
                        taskId = "t3",
                        eventId = JAZZ_ID,
                        title = "Welcome the musicians",
                        location = "Backstage",
                        priority = Priority.MEDIUM,
                        startTime = previewAt(previewToday.plusDays(1), 18, 30),
                        ownerId = PREVIEW_OWNER,
                    )
                )
            ),
        ),
        MyTasksDaySection(
            null,
            listOf(
                previewRow(
                    Task(
                        taskId = "t4",
                        eventId = FESTIVAL_ID,
                        title = "Print the volunteer badges",
                        priority = Priority.LOW,
                        ownerId = PREVIEW_OWNER,
                    )
                )
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
