// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.EmptyStateTestTags
import com.android.festivar.ui.components.RefreshableListTestTags
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnnouncementFeedScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val zone = ZoneId.of("Europe/Zurich")

  /** The day of the Figma, a Saturday. */
  private val today = LocalDate.of(2026, 10, 17)
  private val yesterday = today.minusDays(1)

  private fun at(date: LocalDate, hour: Int, minute: Int = 0): ZonedDateTime =
      date.atTime(hour, minute).atZone(zone)

  // The six announcements of the Figma frame 3329:8730. The Figma cuts off the text of the last
  // one, an info of yesterday morning, so its texts are made up.
  private val pinned =
      Announcement(
          announcementId = "pinned",
          title = "Gates and opening hours",
          authorId = "u0",
          content =
              "Main gate opens at 10:00, north gate at 12:00. Last entry is 23:00 on both days.",
          updatedAt = at(yesterday, 9),
          pinned = true,
      )
  private val urgent =
      Announcement(
          announcementId = "urgent",
          title = "Stage B is closed",
          authorId = "u0",
          content = "Strong wind on the hill. Send everyone to the north gate until 16:00.",
          priority = AnnouncementPriority.URGENT,
          updatedAt = at(today, 14, 2),
      )
  private val warning =
      Announcement(
          announcementId = "warning",
          title = "Headliner moved to 21:30",
          authorId = "u0",
          content = "The band's van is late. Doors stay at 19:00, the main set starts later.",
          priority = AnnouncementPriority.WARNING,
          updatedAt = at(today, 11, 30),
      )
  private val water =
      Announcement(
          announcementId = "water",
          title = "Free water at the info tent",
          authorId = "u0",
          content = "Refill points are open by the info tent and next to the food trucks all day.",
          updatedAt = at(today, 9, 15),
      )
  private val lostAndFound =
      Announcement(
          announcementId = "lost",
          title = "Lost and found",
          authorId = "u0",
          content =
              "A blue backpack was left at the entrance. Come to the info tent if it's yours.",
          updatedAt = at(yesterday, 16, 5),
      )
  private val shuttle =
      Announcement(
          announcementId = "shuttle",
          title = "Night shuttle to the station",
          authorId = "u0",
          content = "Shuttles leave the north gate every 20 minutes after midnight.",
          updatedAt = at(yesterday, 10),
      )

  /** The feed of the Figma, as the ViewModel sorts it. */
  private val figmaState =
      AnnouncementFeedUIState(
          pinnedAnnouncements = listOf(pinned),
          sections =
              listOf(
                  AnnouncementFeedSection(today, listOf(urgent, warning, water)),
                  AnnouncementFeedSection(yesterday, listOf(lostAndFound, shuttle)),
              ),
      )

  /** What the content reports back through its callbacks. */
  private class Callbacks {
    val selected = mutableListOf<Announcement>()
    var backs = 0
  }

  /**
   * Shows [AnnouncementFeedContent] with [uiState], relative to [today]. A [tall] content lays out
   * the whole Figma feed at once, to compare the positions of all its rows.
   */
  private fun setContent(
      uiState: AnnouncementFeedUIState,
      tall: Boolean = false,
  ): Callbacks {
    val callbacks = Callbacks()
    composeTestRule.setContent {
      AppTheme {
        Box(if (tall) Modifier.requiredHeight(1400.dp) else Modifier) {
          AnnouncementFeedContent(
              uiState = uiState,
              today = today,
              onBack = { callbacks.backs++ },
              onSelectAnnouncement = { callbacks.selected += it },
          )
        }
      }
    }
    return callbacks
  }

  /** The texts of the row of [announcement], in reading order. */
  private fun rowTexts(announcement: Announcement): List<String> =
      composeTestRule
          .onNodeWithTag(AnnouncementFeedScreenTestTags.announcementRow(announcement))
          .fetchSemanticsNode()
          .config[SemanticsProperties.Text]
          .map { it.text }

  private fun top(tag: String): Float =
      composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value

  // Breaks if the pinned announcements are not on top under their header, if the days are not each
  // under their own header, newest first, or if a row loses its priority, time, title or content. A
  // pinned row shows its weekday (no date header is above it), a day's row only its time.
  @Test
  fun pinnedComeFirstThenEachDayUnderItsHeaderWithItsRows() {
    setContent(figmaState, tall = true)

    val tags = AnnouncementFeedScreenTestTags
    val order =
        listOf(
            tags.PINNED_HEADER,
            tags.announcementRow(pinned),
            tags.dateHeader(today),
            tags.announcementRow(urgent),
            tags.announcementRow(warning),
            tags.announcementRow(water),
            tags.dateHeader(yesterday),
            tags.announcementRow(lostAndFound),
            tags.announcementRow(shuttle),
        )
    val tops = order.map { top(it) }
    assertEquals(tops.sorted(), tops)
    composeTestRule.onNodeWithTag(tags.PINNED_HEADER).assertTextEquals("PINNED")
    composeTestRule.onNodeWithTag(tags.dateHeader(today)).assertTextEquals("TODAY · SAT 17 OCT")
    composeTestRule
        .onNodeWithTag(tags.dateHeader(yesterday))
        .assertTextEquals("YESTERDAY · FRI 16 OCT")
    assertEquals(listOf("Info", "Fri 09:00", pinned.title, pinned.content), rowTexts(pinned))
    assertEquals(listOf("Urgent", "14:02", urgent.title, urgent.content), rowTexts(urgent))
    assertEquals(listOf("Warning", "11:30", warning.title, warning.content), rowTexts(warning))
  }

  // Breaks if a day before yesterday gets a "Today" or "Yesterday" prefix instead of its date only,
  // or if an announcement without content still shows an empty content line.
  @Test
  fun olderDayShowsOnlyItsDateAndABlankContentIsHidden() {
    val older = today.minusDays(2)
    val withoutContent = water.copy(content = "  ", updatedAt = at(older, 9))
    setContent(
        AnnouncementFeedUIState(
            sections = listOf(AnnouncementFeedSection(older, listOf(withoutContent)))
        )
    )

    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.dateHeader(older))
        .assertTextEquals("THU 15 OCT")
    assertEquals(listOf("Info", "09:00", water.title), rowTexts(withoutContent))
  }

  // Breaks if a tap on a row reports another announcement, or if the back arrow is not connected
  // to its callback.
  @Test
  fun rowAndBackCallTheirCallbacks() {
    val callbacks = setContent(figmaState)

    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.announcementRow(urgent))
        .performClick()
    composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.BACK_BUTTON).performClick()

    assertEquals(listOf(urgent), callbacks.selected)
    assertEquals(1, callbacks.backs)
  }

  // Breaks if the feed shows another empty state than its own or offers buttons in it, if a filter
  // without announcements does not show its message, or if the error is not worded from its string
  // resource or its Retry does not refresh the feed.
  @Test
  fun emptyFilterEmptyAndErrorStatesShowTheFeedsTexts() {
    var uiState by mutableStateOf(AnnouncementFeedUIState())
    var refreshes = 0
    composeTestRule.setContent {
      AppTheme {
        AnnouncementFeedContent(
            uiState = uiState,
            onRefresh = { refreshes++ },
        )
      }
    }

    composeTestRule
        .onNodeWithTag(EmptyStateTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("No announcements yet")
    composeTestRule
        .onNodeWithText("Organizers post updates about the event here.")
        .assertIsDisplayed()
    // Only the back arrow
    composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(1)

    uiState = figmaState.copy(sections = emptyList(), selectedFilter = AnnouncementFilter.URGENT)
    composeTestRule
        .onNodeWithTag(RefreshableListTestTags.FILTER_EMPTY)
        .assertIsDisplayed()
        .assertTextEquals("Nothing here. Try another filter.")

    uiState = AnnouncementFeedUIState(errorMsg = R.string.announcement_feed_load_failed)
    composeTestRule
        .onNodeWithTag(RefreshableListTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals("Unable to load the announcements.")
    composeTestRule.onNodeWithTag(RefreshableListTestTags.RETRY).performClick()
    assertEquals(1, refreshes)
  }
}
