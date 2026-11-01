// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnouncementFeedContentTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val today = LocalDate.of(2026, 10, 17)
  private val pinned =
      announcement(
          id = "pinned",
          title = "Gates and opening hours",
          date = today,
          hour = 9,
          priority = AnnouncementPriority.INFO,
          pinned = true,
      )
  private val urgent =
      announcement(
          id = "urgent",
          title = "Stage B is closed",
          date = today,
          hour = 14,
          priority = AnnouncementPriority.URGENT,
      )

  @Test
  fun displaysPinnedAndDateGroupedAnnouncements() {
    show(
        AnnouncementFeedUiState(
            pinnedAnnouncements = listOf(pinned),
            sections = listOf(AnnouncementFeedSection(today, listOf(urgent))),
        )
    )

    composeTestRule.onNodeWithText("Announcements").assertIsDisplayed()
    composeTestRule.onNodeWithText("Gates and opening hours").assertIsDisplayed()
    composeTestRule.onNodeWithText("Medium").assertIsDisplayed()
    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    composeTestRule.onNodeWithText("Stage B is closed").assertIsDisplayed()
    composeTestRule.onNodeWithText("Urgent").assertIsDisplayed()
  }

  @Test
  fun backAndAddActionsCallTheirCallbacks() {
    var backCalls = 0
    var addCalls = 0
    show(
        AnnouncementFeedUiState(),
        onBack = { backCalls++ },
        onAddAnnouncement = { addCalls++ },
    )

    composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.BACK).performClick()
    composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.ADD).performClick()

    assertEquals(1, backCalls)
    assertEquals(1, addCalls)
  }

  @Test
  fun errorNoticeRetriesAndPullDownRefreshes() {
    val refreshes = AtomicInteger()
    show(
        AnnouncementFeedUiState(errorMsg = "Network down"),
        onRefresh = { refreshes.incrementAndGet() },
    )

    composeTestRule.onNodeWithText("Could not load announcements: Network down").assertIsDisplayed()
    composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.RETRY).performClick()
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.PULL_TO_REFRESH)
        .performTouchInput { swipeDown() }

    composeTestRule.waitUntil { refreshes.get() == 2 }
    assertEquals(2, refreshes.get())
  }

  private fun show(
      state: AnnouncementFeedUiState,
      onRefresh: () -> Unit = {},
      onBack: () -> Unit = {},
      onAddAnnouncement: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        AnnouncementFeedContent(
            state = state,
            onBack = onBack,
            onRefresh = onRefresh,
            onAddAnnouncement = onAddAnnouncement,
            today = today,
        )
      }
    }
  }

  private fun announcement(
      id: String,
      title: String,
      date: LocalDate,
      hour: Int,
      priority: AnnouncementPriority,
      pinned: Boolean = false,
  ) =
      Announcement(
          announcementId = id,
          title = title,
          authorId = "organizer",
          updatedAt =
              ZonedDateTime.of(
                  date.year,
                  date.monthValue,
                  date.dayOfMonth,
                  hour,
                  0,
                  0,
                  0,
                  ZoneId.of("Europe/Zurich"),
              ),
          priority = priority,
          pinned = pinned,
      )
}
