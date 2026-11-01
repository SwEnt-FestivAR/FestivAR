// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object AnnouncementFeedScreenTestTags {
  const val TITLE = "announcementFeedTitle"
  const val BACK = "announcementFeedBack"
  const val PULL_TO_REFRESH = "announcementFeedPullToRefresh"
  const val ADD = "announcementFeedAdd"
  const val RETRY = "announcementFeedRetry"
  const val LOADING = "announcementFeedLoading"
  const val EMPTY = "announcementFeedEmpty"

  fun announcement(id: String) = "announcementFeedItem$id"

  fun section(date: LocalDate) = "announcementFeedSection$date"
}

@Composable
fun AnnouncementFeedScreen(
    viewModel: AnnouncementFeedViewModel,
    onBack: () -> Unit,
    onAddAnnouncement: () -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
  AnnouncementFeedContent(
      state = state,
      onBack = onBack,
      onRefresh = viewModel::refresh,
      onAddAnnouncement = onAddAnnouncement,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementFeedContent(
    state: AnnouncementFeedUiState,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onAddAnnouncement: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
) {
  val hasAnnouncements = state.pinnedAnnouncements.isNotEmpty() || state.sections.isNotEmpty()

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.announcement_feed_title),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.TITLE),
              )
            },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.BACK),
              ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.announcement_feed_back),
                )
              }
            },
        )
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = onAddAnnouncement,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.announcement_feed_add)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.ADD),
        )
      },
  ) { contentPadding ->
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
      PullToRefreshBox(
          isRefreshing = state.isLoading && hasAnnouncements,
          onRefresh = onRefresh,
          modifier = Modifier.fillMaxSize().testTag(AnnouncementFeedScreenTestTags.PULL_TO_REFRESH),
      ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
          if (state.isLoading && !hasAnnouncements) {
            item {
              Box(
                  modifier = Modifier.fillParentMaxSize(),
                  contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.LOADING)
                )
              }
            }
          } else if (!hasAnnouncements && state.errorMsg == null) {
            item {
              Text(
                  text = stringResource(R.string.announcement_feed_empty),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier =
                      Modifier.fillParentMaxSize()
                          .wrapContentSize(Alignment.Center)
                          .testTag(AnnouncementFeedScreenTestTags.EMPTY),
              )
            }
          }

          items(
              items = state.pinnedAnnouncements,
              key = { "pinned-${it.announcementId}" },
          ) { announcement ->
            Column {
              AnnouncementFeedItem(
                  announcement = announcement,
                  modifier =
                      Modifier.testTag(
                          AnnouncementFeedScreenTestTags.announcement(announcement.announcementId)
                      ),
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
          }

          state.sections.forEach { section ->
            item(key = "date-${section.date}") {
              AnnouncementDateHeader(
                  date = section.date,
                  today = today,
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(top = 12.dp, bottom = 8.dp)
                          .testTag(AnnouncementFeedScreenTestTags.section(section.date)),
              )
            }
            items(
                items = section.announcements,
                key = { it.announcementId },
            ) { announcement ->
              Column {
                AnnouncementFeedItem(
                    announcement = announcement,
                    modifier =
                        Modifier.testTag(
                            AnnouncementFeedScreenTestTags.announcement(announcement.announcementId)
                        ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              }
            }
          }
        }
      }

      state.errorMsg?.let { error ->
        val message =
            error
                .takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.announcement_feed_error, it) }
                ?: stringResource(R.string.announcement_feed_error_unknown)
        NoticeSnackbar(
            message = message,
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRefresh,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
            actionModifier = Modifier.testTag(AnnouncementFeedScreenTestTags.RETRY),
        )
      }
    }
  }
}

@Composable
private fun AnnouncementDateHeader(
    date: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
  val label =
      when (date) {
        today -> stringResource(R.string.announcement_feed_today)
        today.minusDays(1) -> stringResource(R.string.announcement_feed_yesterday)
        else -> formatDate(date)
      }
  Row(
      modifier =
          modifier
              .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Text(
        text = formatDate(date),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
    )
  }
}

@Composable
private fun AnnouncementFeedItem(
    announcement: Announcement,
    modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      PriorityBadge(announcement.priority, announcement.pinned)
      Spacer(Modifier.weight(1f))
      if (announcement.pinned) {
        Icon(
            imageVector = Icons.Outlined.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
      }
      Text(
          text = formatTime(announcement.updatedAt.toLocalTime()),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(start = 6.dp),
      )
    }
    Text(
        text = announcement.title,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
    if (announcement.content.isNotBlank()) {
      Text(
          text = announcement.content,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun PriorityBadge(
    priority: AnnouncementPriority,
    pinned: Boolean,
) {
  val colors = MaterialTheme.colorScheme
  val badge =
      when (priority) {
        AnnouncementPriority.URGENT ->
            PriorityBadgeStyle(
                stringResource(R.string.priority_urgent),
                colors.errorContainer,
                colors.onErrorContainer,
                false,
            )
        AnnouncementPriority.WARNING ->
            PriorityBadgeStyle(
                stringResource(R.string.priority_high),
                colors.tertiaryContainer,
                colors.onTertiaryContainer,
                false,
            )
        AnnouncementPriority.INFO ->
            if (pinned) {
              PriorityBadgeStyle(
                  stringResource(R.string.priority_medium),
                  colors.surfaceVariant,
                  colors.onSurface,
                  false,
              )
            } else {
              PriorityBadgeStyle(
                  stringResource(R.string.priority_low),
                  colors.surface,
                  colors.onSurfaceVariant,
                  true,
              )
            }
      }
  Surface(
      color = badge.background,
      shape = CircleShape,
      border = if (badge.outlined) BorderStroke(1.dp, colors.outlineVariant) else null,
  ) {
    Text(
        text = badge.label,
        color = badge.foreground,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

private data class PriorityBadgeStyle(
    val label: String,
    val background: Color,
    val foreground: Color,
    val outlined: Boolean,
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AnnouncementFeedPreview() {
  val zone = ZoneId.of("Europe/Zurich")
  val today = LocalDate.of(2026, 10, 17)
  fun at(date: LocalDate, hour: Int, minute: Int = 0) =
      ZonedDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute, 0, 0, zone)
  fun announcement(
      id: String,
      title: String,
      content: String,
      priority: AnnouncementPriority,
      updatedAt: ZonedDateTime,
      pinned: Boolean = false,
  ) =
      Announcement(
          announcementId = id,
          title = title,
          content = content,
          priority = priority,
          updatedAt = updatedAt,
          authorId = "organizer",
          pinned = pinned,
      )
  val pinned =
      announcement(
          "opening-hours",
          "Gates and opening hours",
          "Main gate opens at 10:00, north gate at 12:00. Last entry is 23:00 on both days.",
          AnnouncementPriority.INFO,
          at(today, 9),
          pinned = true,
      )
  val todayAnnouncements =
      listOf(
          announcement(
              "stage-closed",
              "Stage B is closed",
              "Strong wind on the hill. Send everyone to the north gate until 16:00.",
              AnnouncementPriority.URGENT,
              at(today, 14, 2),
          ),
          announcement(
              "headliner",
              "Headliner moved to 21:30",
              "The band's van is late. Doors stay at 19:00, the main set starts half an hour later.",
              AnnouncementPriority.WARNING,
              at(today, 11, 30),
          ),
          announcement(
              "free-water",
              "Free water at the info tent",
              "Refill points are open by the info tent and next to the food trucks all day.",
              AnnouncementPriority.INFO,
              at(today, 9, 15),
          ),
      )
  val yesterday = today.minusDays(1)
  val yesterdayAnnouncements =
      listOf(
          announcement(
              "lost-found",
              "Lost and found",
              "A blue backpack was left at the ticket desk. Ask at the entrance if it's yours.",
              AnnouncementPriority.INFO,
              at(yesterday, 16, 5),
          )
      )

  AppTheme {
    AnnouncementFeedContent(
        state =
            AnnouncementFeedUiState(
                pinnedAnnouncements = listOf(pinned),
                sections =
                    listOf(
                        AnnouncementFeedSection(today, todayAnnouncements),
                        AnnouncementFeedSection(yesterday, yesterdayAnnouncements),
                    ),
            ),
        today = today,
    )
  }
}
