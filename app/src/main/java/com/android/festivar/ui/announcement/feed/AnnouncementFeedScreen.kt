// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.ui.announcement.announcementPriorityLabelRes
import com.android.festivar.ui.announcement.announcementPriorityPillStyle
import com.android.festivar.ui.components.EmptyState
import com.android.festivar.ui.components.Hairline
import com.android.festivar.ui.components.RefreshableStateList
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.components.formatWeekday
import java.time.LocalDate

object AnnouncementFeedScreenTestTags {
  const val BACK_BUTTON = "announcementFeedBackButton"
  const val TITLE = "announcementFeedTitle"
  const val ANNOUNCEMENT_LIST = "announcementFeedList"
  const val PINNED_HEADER = "announcementFeedPinnedHeader"

  fun dateHeader(date: LocalDate): String = "announcementFeedDateHeader$date"

  fun announcementRow(announcement: Announcement): String =
      "announcementFeedRow${announcement.announcementId}"
}

/**
 * The string resource that starts the header of the announcements updated on [date]: "Today" or
 * "Yesterday" relative to [today], or `null` for an older day, which only shows its date.
 */
@StringRes
internal fun dateHeaderPrefixRes(date: LocalDate, today: LocalDate): Int? =
    when (date) {
      today -> R.string.announcement_feed_today
      today.minusDays(1) -> R.string.announcement_feed_yesterday
      else -> null
    }

/**
 * List of the [Announcement] items of an event, displaying the given [uiState]: the pinned ones
 * first, then the others by day, the most recent first. Only the announcements selected by the
 * state's filter are shown. The user can pull the list down to refresh it.
 *
 * @param uiState The state to display.
 * @param today The day the "Today" and "Yesterday" headers are relative to.
 * @param onRefresh Called when the user pulls the list down or clicks on "Retry".
 * @param onBack Called when the user clicks on the back arrow.
 * @param onSelectAnnouncement Called when the user clicks on an [Announcement].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementFeedContent(
    uiState: AnnouncementFeedUIState,
    today: LocalDate = LocalDate.now(),
    onRefresh: () -> Unit = {},
    onBack: () -> Unit = {},
    onSelectAnnouncement: (Announcement) -> Unit = {},
) {
  // Read once: each one is computed from the whole state on every access
  val shownPinned = uiState.shownPinnedAnnouncements
  val shownSections = uiState.shownSections

  Scaffold(
      topBar = {
        TopAppBar(
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.BACK_BUTTON),
                  colors =
                      IconButtonDefaults.iconButtonColors(
                          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                      ),
              ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.announcement_feed_navigate_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
              }
            },
            title = {
              Text(
                  text = stringResource(R.string.announcement_feed_title),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.TITLE),
              )
            },
        )
      },
  ) { pd ->
    RefreshableStateList(
        isLoading = uiState.isLoading,
        hasNoItems = uiState.hasNoAnnouncements,
        hasNoShownItems = shownPinned.isEmpty() && shownSections.isEmpty(),
        errorMessage = uiState.errorMsg?.let { stringResource(it) },
        onRefresh = onRefresh,
        filterEmptyMessage = stringResource(R.string.announcement_feed_filter_empty),
        emptyState = {
          EmptyState(
              icon = R.drawable.ic_notifications,
              title = stringResource(R.string.announcement_feed_empty_title),
              subtitle = stringResource(R.string.announcement_feed_empty_subtitle),
          )
        },
        modifier = Modifier.fillMaxSize().padding(pd).padding(top = 4.dp),
        // A new list state for each filter, so that switching filter starts back at the top
        listKey = uiState.selectedFilter,
        listTestTag = AnnouncementFeedScreenTestTags.ANNOUNCEMENT_LIST,
    ) {
      if (shownPinned.isNotEmpty()) {
        item(key = "pinnedHeader") {
          SectionLabel(
              text = stringResource(R.string.announcement_feed_pinned),
              modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.PINNED_HEADER),
          )
        }
        announcementRows(shownPinned, onSelectAnnouncement)
      }
      shownSections.forEach { section ->
        item(key = "date${section.date}") {
          SectionLabel(
              text = dateHeader(section.date, today),
              modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.dateHeader(section.date)),
          )
        }
        announcementRows(section.announcements, onSelectAnnouncement)
      }
    }
  }
}

/** One item per announcement of [announcements], keyed by its id. */
private fun LazyListScope.announcementRows(
    announcements: List<Announcement>,
    onSelectAnnouncement: (Announcement) -> Unit,
) {
  items(announcements, key = { "announcement${it.announcementId}" }) { announcement ->
    AnnouncementRow(announcement = announcement, onClick = { onSelectAnnouncement(announcement) })
  }
}

/** The header of the announcements updated on [date], e.g. "Today · Sat 17 Oct" or "Thu 15 Oct". */
@Composable
private fun dateHeader(date: LocalDate, today: LocalDate): String {
  val formatted = formatDate(date)
  return dateHeaderPrefixRes(date, today)?.let { stringResource(it, formatted) } ?: formatted
}

/**
 * Row showing an [announcement]: its priority and time, then its title and content, above a
 * hairline. A pinned announcement has no date header above it, so it shows a pin and its weekday
 * before its time.
 */
@Composable
private fun AnnouncementRow(announcement: Announcement, onClick: () -> Unit) {
  val time = formatTime(announcement.updatedAt.toLocalTime())
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(AnnouncementFeedScreenTestTags.announcementRow(announcement))
              .clickable(onClick = onClick)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      StatusPill(
          label = stringResource(announcementPriorityLabelRes(announcement.priority)),
          style = announcementPriorityPillStyle(announcement.priority),
      )
      Spacer(modifier = Modifier.weight(1f))
      if (announcement.pinned) {
        Icon(
            painter = painterResource(R.drawable.ic_push_pin),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text =
                stringResource(
                    R.string.announcement_feed_pinned_time,
                    formatWeekday(announcement.updatedAt.toLocalDate()),
                    time,
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = announcement.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (announcement.content.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = announcement.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Hairline()
  }
}
