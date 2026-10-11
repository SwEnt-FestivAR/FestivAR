package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.theme.NumeralLarge
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object EventDetailsScreenTestTags {
  const val BACK_BUTTON = "backButton"
  const val EVENT_NAME = "eventName"
  const val OPEN_TASKS_STAT = "openTasksStat"
  const val OWNER_ROW = "ownerRow"
  const val DATE_ROW = "dateRow"
  const val ADDRESS_ROW = "addressRow"
  const val SEE_TASKS_BUTTON = "seeTasksButton"
}

private val EventDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/**
 * A screen for viewing event details.
 *
 * @param event The event whose details will be shown
 * @param onBack A callback to be invoked when the back button is clicked
 * @param onTasksButtonClicked A callback to be invoked when the "See tasks" button is clicked
 */
@Composable
fun EventDetailsScreen(
    event: Event,
    onBack: () -> Unit,
    onTasksButtonClicked: () -> Unit,
) {
  val openTasks = event.tasks.count { !it.completed }
  val date = formatEventDate(event)
  val owner = event.members.firstOrNull()?.uid.orEmpty()

  Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = { EventDetailsTopBar(onBack = onBack) },
      bottomBar = { EventDetailsBottomBar(onTasksButtonClicked = onTasksButtonClicked) },
  ) { paddingValues ->
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = event.title,
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = 8.dp)
                  .testTag(EventDetailsScreenTestTags.EVENT_NAME),
          color = MaterialTheme.colorScheme.onBackground,
          style = MaterialTheme.typography.titleMedium,
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerLow)
                  .padding(horizontal = 12.dp, vertical = 12.dp)
                  .testTag(EventDetailsScreenTestTags.OPEN_TASKS_STAT),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = openTasks.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            style = NumeralLarge,
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
          Text(
              text = "of ${event.tasks.size}",
              color = MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.bodySmall,
          )
          Text(
              text = "tasks still open",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodySmall,
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))

      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .border(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant,
                      RoundedCornerShape(10.dp),
                  ),
      ) {
        EventDetailsRow(
            label = "Owner",
            value = owner,
            tag = EventDetailsScreenTestTags.OWNER_ROW,
            showDivider = true,
        )
        EventDetailsRow(
            label = "Date",
            value = date,
            tag = EventDetailsScreenTestTags.DATE_ROW,
            showDivider = true,
        )
        EventDetailsRow(
            label = "Address",
            value = event.location,
            tag = EventDetailsScreenTestTags.ADDRESS_ROW,
            showDivider = false,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailsTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {},
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(EventDetailsScreenTestTags.BACK_BUTTON),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Navigate back",
              tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
          ),
  )
}

@Composable
private fun EventDetailsBottomBar(onTasksButtonClicked: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 12.dp)
              .height(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primary)
              .clickable(onClick = onTasksButtonClicked)
              .testTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
        imageVector = Icons.Outlined.TaskAlt,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.onPrimary,
    )
    Text(
        text = "See tasks",
        modifier = Modifier.padding(start = 8.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun EventDetailsRow(
    label: String,
    value: String,
    tag: String,
    showDivider: Boolean,
) {
  Column(modifier = Modifier.fillMaxWidth().testTag(tag)) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
          text = label,
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyMedium,
      )
      Text(
          text = value,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
      )
    }
    if (showDivider) {
      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .height(1.dp)
                  .background(MaterialTheme.colorScheme.outlineVariant)
      )
    }
  }
}

private fun formatEventDate(event: Event): String {
  val start = event.startDate.format(EventDateFormatter)
  val end = event.endDate.format(EventDateFormatter)
  return if (
      event.startDate.month == event.endDate.month && event.startDate.year == event.endDate.year
  ) {
    "${event.startDate.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))} to $end"
  } else {
    "$start to $end"
  }
}

@Preview
@Composable
fun EventDetailsScreenPreview() {
  val event =
      Event(
          eventId = "event-id",
          title = "Fête de l'Asso 2026",
          members = listOf(User("Sami K.")),
          startDate = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich")),
          endDate = ZonedDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneId.of("Europe/Zurich")),
          location = "Parc de Vidy, Lausanne",
      )
  AppTheme { EventDetailsScreen(event = event, onTasksButtonClicked = {}, onBack = {}) }
}
