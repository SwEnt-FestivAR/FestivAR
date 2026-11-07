// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.festivar.R
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.announcementPriorityLabelRes
import com.android.festivar.ui.announcement.announcementPriorityPillStyle
import com.android.festivar.ui.components.EmptyState
import com.android.festivar.ui.components.FilterChip
import com.android.festivar.ui.components.Hairline
import com.android.festivar.ui.components.RefreshableStateList
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.floatingShadow
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.components.formatWeekday
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object AnnouncementFeedScreenTestTags {
  const val BACK_BUTTON = "announcementFeedBackButton"
  const val TITLE = "announcementFeedTitle"
  const val ANNOUNCEMENT_LIST = "announcementFeedList"
  const val PINNED_HEADER = "announcementFeedPinnedHeader"
  const val ADD_ANNOUNCEMENT_FAB = "announcementFeedAddFab"

  fun filterChip(filter: AnnouncementFilter): String = "announcementFeedFilterChip${filter.name}"

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
 * Screen showing the [Announcement] items of an event: the pinned ones first, then the others by
 * day, the most recent first. The chips at the top filter them by priority. The [Announcement]
 * items are fetched again every time the screen comes back to the front, e.g. after adding a new
 * one, and when the user pulls the list down.
 *
 * @param viewModel The ViewModel providing the [Announcement] items.
 * @param canAddAnnouncement Whether the user may add an announcement, which shows the add button.
 * @param onBack Called when the user clicks on the back arrow.
 * @param onAddAnnouncement Called when the user clicks on "Add announcement".
 * @param onSelectAnnouncement Called when the user clicks on an [Announcement].
 */
@Composable
fun AnnouncementFeedScreen(
    viewModel: AnnouncementFeedViewModel,
    canAddAnnouncement: Boolean,
    onBack: () -> Unit,
    onAddAnnouncement: () -> Unit,
    onSelectAnnouncement: (Announcement) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshUIState() }

  AnnouncementFeedContent(
      uiState = uiState,
      canAddAnnouncement = canAddAnnouncement,
      onSelectFilter = viewModel::selectFilter,
      onRefresh = viewModel::refreshUIState,
      onBack = onBack,
      onAddAnnouncement = onAddAnnouncement,
      onSelectAnnouncement = onSelectAnnouncement,
  )
}

/**
 * Stateless content of the [AnnouncementFeedScreen], displaying the given [uiState].
 *
 * @param uiState The state to display.
 * @param canAddAnnouncement Whether the user may add an announcement, which shows the add button.
 * @param today The day the "Today" and "Yesterday" headers are relative to.
 * @param onSelectFilter Called when the user clicks on a priority chip.
 * @param onRefresh Called when the user pulls the list down or clicks on "Retry".
 * @param onBack Called when the user clicks on the back arrow.
 * @param onAddAnnouncement Called when the user clicks on "Add announcement".
 * @param onSelectAnnouncement Called when the user clicks on an [Announcement].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementFeedContent(
    uiState: AnnouncementFeedUIState,
    canAddAnnouncement: Boolean,
    today: LocalDate = LocalDate.now(),
    onSelectFilter: (AnnouncementFilter) -> Unit = {},
    onRefresh: () -> Unit = {},
    onBack: () -> Unit = {},
    onAddAnnouncement: () -> Unit = {},
    onSelectAnnouncement: (Announcement) -> Unit = {},
) {
  // Read once: each one is computed from the whole state on every access
  val shownPinned = uiState.shownPinnedAnnouncements
  val shownSections = uiState.shownSections
  val counts = uiState.counts

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
      floatingActionButton = {
        if (canAddAnnouncement) AddAnnouncementButton(onClick = onAddAnnouncement)
      },
  ) { pd ->
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(pd).padding(top = 4.dp),
    ) {
      // Scrolls sideways if the chips don't fit, e.g. with long translations
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      ) {
        chipOrder.forEach { filter ->
          FilterChip(
              label = filterLabel(filter),
              count = counts[filter],
              selected = uiState.selectedFilter == filter,
              onClick = { onSelectFilter(filter) },
              modifier = Modifier.testTag(AnnouncementFeedScreenTestTags.filterChip(filter)),
          )
        }
      }

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
          modifier = Modifier.fillMaxSize(),
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
                modifier =
                    Modifier.testTag(AnnouncementFeedScreenTestTags.dateHeader(section.date)),
            )
          }
          announcementRows(section.announcements, onSelectAnnouncement)
        }
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

/**
 * The order of the filter chips: "All" first, then the priorities from the most urgent. Set here
 * rather than by the order of [AnnouncementFilter], which belongs to the ViewModel.
 */
private val chipOrder =
    listOf(
        AnnouncementFilter.ALL,
        AnnouncementFilter.URGENT,
        AnnouncementFilter.WARNING,
        AnnouncementFilter.INFO,
    )

/**
 * The label of the chip of [filter]. Shorter than the priority names of the pills, so that the four
 * chips fit on one line in every language.
 */
@Composable
private fun filterLabel(filter: AnnouncementFilter): String =
    stringResource(
        when (filter) {
          AnnouncementFilter.ALL -> R.string.announcement_feed_filter_all
          AnnouncementFilter.URGENT -> R.string.announcement_feed_filter_urgent
          AnnouncementFilter.WARNING -> R.string.announcement_feed_filter_warning
          AnnouncementFilter.INFO -> R.string.announcement_feed_filter_info
        }
    )

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

/** Floating "Add announcement" button, with the design's own shadow like the events overview. */
@Composable
private fun AddAnnouncementButton(onClick: () -> Unit) {
  ExtendedFloatingActionButton(
      text = {
        Text(
            text = stringResource(R.string.announcement_feed_add),
            style = MaterialTheme.typography.labelLarge,
        )
      },
      icon = { Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null) },
      onClick = onClick,
      shape = CircleShape,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
      modifier =
          Modifier.floatingShadow(MaterialTheme.colorScheme.onSurface)
              .testTag(AnnouncementFeedScreenTestTags.ADD_ANNOUNCEMENT_FAB),
  )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
internal fun AnnouncementFeedContentPreview() {
  val zone = ZoneId.of("Europe/Zurich")
  fun at(day: Int, hour: Int, minute: Int) =
      ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)
  fun announcement(
      id: String,
      title: String,
      content: String,
      priority: AnnouncementPriority,
      updatedAt: ZonedDateTime,
      pinned: Boolean = false,
  ) = Announcement(id, title, "u0", content, priority, updatedAt, pinned = pinned)
  val uiState =
      AnnouncementFeedUIState(
          pinnedAnnouncements =
              listOf(
                  announcement(
                      "1",
                      "Gates and opening hours",
                      "Main gate opens at 10:00, north gate at 12:00.",
                      AnnouncementPriority.INFO,
                      at(16, 9, 0),
                      pinned = true,
                  )
              ),
          sections =
              listOf(
                  AnnouncementFeedSection(
                      LocalDate.of(2026, 10, 17),
                      listOf(
                          announcement(
                              "2",
                              "Stage B is closed",
                              "Strong wind on the hill. Send everyone to the north gate.",
                              AnnouncementPriority.URGENT,
                              at(17, 14, 2),
                          ),
                          announcement(
                              "3",
                              "Headliner moved to 21:30",
                              "The band's van is late.",
                              AnnouncementPriority.WARNING,
                              at(17, 11, 30),
                          ),
                      ),
                  ),
                  AnnouncementFeedSection(
                      LocalDate.of(2026, 10, 16),
                      listOf(
                          announcement(
                              "4",
                              "Lost and found",
                              "A blue backpack was left at the entrance.",
                              AnnouncementPriority.INFO,
                              at(16, 16, 5),
                          )
                      ),
                  ),
              ),
      )
  AppTheme {
    AnnouncementFeedContent(
        uiState = uiState,
        canAddAnnouncement = true,
        today = LocalDate.of(2026, 10, 17),
    )
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
internal fun AnnouncementFeedEmptyPreview() {
  AppTheme {
    AnnouncementFeedContent(uiState = AnnouncementFeedUIState(), canAddAnnouncement = true)
  }
}
