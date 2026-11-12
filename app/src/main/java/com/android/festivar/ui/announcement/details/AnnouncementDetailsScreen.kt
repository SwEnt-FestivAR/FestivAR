// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co authored by: Gemini
package com.android.festivar.ui.announcement.details

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.announcementPriorityLabelRes
import com.android.festivar.ui.announcement.announcementPriorityPillStyle
import com.android.festivar.ui.components.SectionLabel
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.ZonedDateTime

private const val LOG_TAG = "AnnouncementDetailsScreen"

object AnnouncementDetailsScreenTestTags {
  const val BACK_BUTTON = "announcementDetailsBackButton"
  const val PRIORITY_BADGE = "announcementDetailsPriorityBadge"
  const val TITLE = "announcementDetailsTitle"
  const val POSTED_AT = "announcementDetailsPostedAt"
  const val END_TIME = "announcementDetailsEndTime"
  const val AUTHOR = "announcementDetailsAuthor"
  const val DESCRIPTION = "announcementDetailsDescription"
  const val PIN_BUTTON = "announcementDetailsPinButton"
  const val LOADING = "announcementDetailsLoading"
  const val ERROR = "announcementDetailsError"
  const val RETRY = "announcementDetailsRetry"

  fun link(url: String) = "announcementDetailsLink:$url"
}

/**
 * Displays announcement details from [viewModel]. Navigation remains owned by the caller through
 * [onBack]; this screen does not add or select a navigation route.
 */
@Composable
fun AnnouncementDetailsScreen(
    viewModel: AnnouncementDetailsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val uriHandler = LocalUriHandler.current
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

  AnnouncementDetailsContent(
      state = state,
      modifier = modifier,
      onBack = onBack,
      onTogglePin = viewModel::togglePin,
      onRetry = viewModel::load,
      onOpenLink = { url -> openAnnouncementLink(uriHandler, url) },
  )
}

private fun openAnnouncementLink(uriHandler: UriHandler, url: String) {
  try {
    uriHandler.openUri(url)
  } catch (e: ActivityNotFoundException) {
    Log.w(LOG_TAG, "No app can open announcement link: $url", e)
  } catch (e: IllegalArgumentException) {
    Log.w(LOG_TAG, "Invalid announcement link: $url", e)
  } catch (e: SecurityException) {
    Log.w(LOG_TAG, "Not allowed to open announcement link: $url", e)
  } catch (e: Exception) {
    Log.w(LOG_TAG, "Failed to open announcement link: $url", e)
  }
}

/** Stateless announcement details UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailsContent(
    state: AnnouncementDetailsUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenLink: (String) -> Unit = {},
) {
  Scaffold(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.BACK_BUTTON),
              ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.announcement_details_back),
                )
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
        )
      },
      bottomBar = {
        state.announcement?.let { announcement ->
          OutlinedButton(
              onClick = onTogglePin,
              enabled = state.canTogglePin,
              shape = MaterialTheme.shapes.medium,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 8.dp)
                      .height(52.dp)
                      .testTag(AnnouncementDetailsScreenTestTags.PIN_BUTTON),
          ) {
            Text(
                stringResource(
                    if (announcement.pinned) R.string.announcement_details_unpin_from_top
                    else R.string.announcement_details_pin_to_top
                ),
                style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      state.errorMsg?.let { error ->
        Text(
            text = stringResource(error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.ERROR),
        )
        if (state.announcement == null) {
          TextButton(
              onClick = onRetry,
              modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.RETRY),
          ) {
            Text(stringResource(R.string.announcement_details_retry))
          }
        }
      }
      if (state.isLoading && state.announcement == null) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
              modifier = Modifier.size(32.dp).testTag(AnnouncementDetailsScreenTestTags.LOADING),
          )
        }
      }
      state.announcement?.let { announcement ->
        AnnouncementBody(
            announcement = announcement,
            authorName = state.authorName,
            authorNameRes = state.authorNameRes,
            eventEndTime = state.eventEndTime,
            onOpenLink = onOpenLink,
        )
      }
    }
  }
}

@Composable
private fun AnnouncementBody(
    announcement: Announcement,
    authorName: String?,
    @StringRes authorNameRes: Int?,
    eventEndTime: ZonedDateTime?,
    onOpenLink: (String) -> Unit,
) {
  val displayedAuthorName =
      if (authorName == null && authorNameRes != null) stringResource(authorNameRes) else authorName

  StatusPill(
      label = stringResource(announcementPriorityLabelRes(announcement.priority)),
      style = announcementPriorityPillStyle(announcement.priority),
      modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.PRIORITY_BADGE),
  )
  Text(
      text = announcement.title,
      style = MaterialTheme.typography.displaySmall,
      modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.TITLE),
  )
  Text(
      text =
          stringResource(
              R.string.announcement_details_posted_at,
              formatDate(announcement.updatedAt.toLocalDate()),
              formatTime(announcement.updatedAt.toLocalTime()),
          ),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.POSTED_AT),
  )
  eventEndTime?.let { endTime ->
    Text(
        text =
            stringResource(
                R.string.announcement_details_until,
                formatDate(endTime.toLocalDate()),
                formatTime(endTime.toLocalTime()),
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.END_TIME),
    )
  }
  displayedAuthorName?.takeIf(String::isNotBlank)?.let { author ->
    Text(
        text = stringResource(R.string.announcement_details_posted_by, author),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.AUTHOR),
    )
  }
  if (announcement.content.isNotBlank()) {
    SectionLabel(stringResource(R.string.announcement_details_description))
    Text(
        text = announcement.content,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag(AnnouncementDetailsScreenTestTags.DESCRIPTION),
    )
  }
  if (announcement.links.isNotEmpty()) {
    SectionLabel(stringResource(R.string.announcement_details_links))
    announcement.links.forEach { url -> AnnouncementLink(url, onOpenLink) }
  }
}

@Composable
private fun AnnouncementLink(url: String, onOpenLink: (String) -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .clickable { onOpenLink(url) }
              .padding(vertical = 12.dp)
              .testTag(AnnouncementDetailsScreenTestTags.link(url)),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = stringResource(R.string.announcement_details_open_link),
        modifier = Modifier.size(20.dp),
    )
    Text(
        text = url.removePrefix("https://").removePrefix("http://"),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun AnnouncementDetailsContentPreview() {
  val date = ZonedDateTime.parse("2026-10-17T14:02:00+02:00")
  AppTheme {
    AnnouncementDetailsContent(
        state =
            AnnouncementDetailsUiState(
                announcement =
                    Announcement(
                        announcementId = "announcement-1",
                        title = "Stage B is closed",
                        authorId = "author-1",
                        content =
                            "Strong wind on the hill. Send everyone to the north gate until 16:00.\n\n" +
                                "We will post here as soon as Stage B opens again.",
                        priority = AnnouncementPriority.URGENT,
                        updatedAt = date,
                        links =
                            listOf(
                                "https://vidyfest.ch/map",
                                "https://vidyfest.ch/programme",
                            ),
                    ),
                authorName = "Marie Dupont",
                eventEndTime = date.plusHours(2),
                isLoading = false,
            )
    )
  }
}
