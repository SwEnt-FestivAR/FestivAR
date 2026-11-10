// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.announcement.details.AnnouncementDetailsScreenTestTags as Tags
import com.android.festivar.ui.theme.AppTheme
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnouncementDetailsScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val postedAt = ZonedDateTime.parse("2026-10-17T14:02:00+02:00")
  private val announcement =
      Announcement(
          announcementId = "announcement-1",
          title = "Stage B is closed",
          authorId = "author-1",
          content = "Strong wind on the hill.",
          priority = AnnouncementPriority.URGENT,
          updatedAt = postedAt,
          links = listOf("https://vidyfest.ch/map"),
      )
  private val events =
      EventsRepositoryLocal(
          listOf(
              Event(
                  eventId = "event-1",
                  title = "Vidyfest",
                  startDate = postedAt.minusHours(1),
                  endDate = postedAt.plusHours(2),
                  announcements = listOf(announcement),
              )
          )
      )

  @Test
  fun showsDetailsAndSwitchesPinButtonAfterSaving() {
    val viewModel =
        AnnouncementDetailsViewModel(
            eventId = "event-1",
            announcementId = announcement.announcementId,
            eventsRepository = events,
            usersRepository =
                UsersRepositoryLocal(
                    listOf(User(uid = "author-1", name = "Marie", surname = "Dupont"))
                ),
        )
    composeTestRule.setContent { AppTheme { AnnouncementDetailsScreen(viewModel = viewModel) } }

    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithTag(Tags.TITLE).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag(Tags.PRIORITY_BADGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.TITLE).assertTextContains(announcement.title)
    composeTestRule.onNodeWithTag(Tags.POSTED_AT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.END_TIME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.AUTHOR).assertTextContains("Posted by Marie Dupont")
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION).assertTextContains(announcement.content)
    composeTestRule.onNodeWithTag(Tags.link(announcement.links.single())).assertIsDisplayed()

    composeTestRule.onNodeWithTag(Tags.PIN_BUTTON).assertIsEnabled().performClick()
    composeTestRule.onNodeWithText("Unpin from top").assertIsDisplayed()
    assertTrue(runBlocking { events.getEvent("event-1").announcements.single().pinned })

    composeTestRule.onNodeWithTag(Tags.PIN_BUTTON).performClick()
    composeTestRule.onNodeWithText("Pin to top").assertIsDisplayed()
    assertFalse(runBlocking { events.getEvent("event-1").announcements.single().pinned })
  }
}
