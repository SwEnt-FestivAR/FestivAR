// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.details

import android.content.ActivityNotFoundException
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
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
import org.junit.Assert.assertEquals
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
                  ownerId = "owner-1",
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
            dataRepository =
                DataRepositoryLocal(
                    authRepository = FakeAuthRepository(),
                    eventsRepository = events,
                    usersRepository =
                        UsersRepositoryLocal(
                            listOf(User(uid = "author-1", name = "Marie", surname = "Dupont"))
                        ),
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
    composeTestRule
        .onNodeWithTag(Tags.link(announcement.links.single()))
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(Tags.PIN_BUTTON).assertIsEnabled().performClick()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.announcement_details_unpin_from_top)
        )
        .assertIsDisplayed()
    assertTrue(runBlocking { events.getEvent("event-1").announcements.single().pinned })

    composeTestRule.onNodeWithTag(Tags.PIN_BUTTON).performClick()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.announcement_details_pin_to_top)
        )
        .assertIsDisplayed()
    assertFalse(runBlocking { events.getEvent("event-1").announcements.single().pinned })
  }

  @Test
  fun showsLoadingErrorAndRetriesWhenAnnouncementIsUnavailable() {
    var retryCount = 0
    composeTestRule.setContent {
      AppTheme {
        AnnouncementDetailsContent(
            state =
                AnnouncementDetailsUiState(errorMsg = R.string.announcement_details_load_failed),
            onRetry = { retryCount++ },
        )
      }
    }

    composeTestRule.onNodeWithTag(Tags.LOADING).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(Tags.ERROR)
        .assertTextContains(
            composeTestRule.activity.getString(R.string.announcement_details_load_failed)
        )
    composeTestRule.onNodeWithTag(Tags.RETRY).performClick()
    assertEquals(1, retryCount)
    composeTestRule.onNodeWithTag(Tags.PIN_BUTTON).assertDoesNotExist()
  }

  @Test
  fun showsFallbackAuthorAndHidesEmptyOptionalDetails() {
    val announcement = announcement.copy(priority = AnnouncementPriority.INFO)
    var backCount = 0
    composeTestRule.setContent {
      AppTheme {
        AnnouncementDetailsContent(
            state =
                AnnouncementDetailsUiState(
                    announcement = announcement.copy(content = "", links = emptyList()),
                    authorNameRes = R.string.announcement_details_user_fetch_failed,
                    isLoading = false,
                ),
            onBack = { backCount++ },
        )
      }
    }

    composeTestRule
        .onNodeWithTag(Tags.AUTHOR)
        .assertTextContains(
            composeTestRule.activity.getString(
                R.string.announcement_details_posted_by,
                composeTestRule.activity.getString(R.string.announcement_details_user_fetch_failed),
            )
        )
    composeTestRule.onNodeWithTag(Tags.END_TIME).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.link("https://vidyfest.ch/map")).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
  }

  @Test
  fun showsExistingDetailsWhileRefreshingAndDisablesPinWithoutAuthor() {
    composeTestRule.setContent {
      AppTheme {
        AnnouncementDetailsContent(
            state =
                AnnouncementDetailsUiState(
                    announcement = announcement.copy(content = "", links = emptyList()),
                    isLoading = true,
                )
        )
      }
    }

    composeTestRule.onNodeWithTag(Tags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.LOADING).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.AUTHOR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.link("https://vidyfest.ch/map")).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.PIN_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun opensHttpLinkAndHidesBlankAuthorAndRetryForLoadedAnnouncement() {
    val link = "http://vidyfest.ch/map"
    var openedLink: String? = null
    composeTestRule.setContent {
      AppTheme {
        AnnouncementDetailsContent(
            state =
                AnnouncementDetailsUiState(
                    announcement =
                        announcement.copy(
                            content = " ",
                            links = listOf(link),
                            priority = AnnouncementPriority.WARNING,
                        ),
                    authorName = "",
                    errorMsg = R.string.announcement_details_save_failed,
                    isLoading = false,
                ),
            onOpenLink = { openedLink = it },
        )
      }
    }

    composeTestRule.onNodeWithTag(Tags.ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.RETRY).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.AUTHOR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.link(link)).performScrollTo().performClick()
    assertEquals(link, openedLink)
    composeTestRule.onNodeWithText("vidyfest.ch/map").assertIsDisplayed()
  }

  @Test
  fun failedUriLaunchDoesNotCrashTheScreen() {
    var openAttempts = 0
    val viewModel =
        AnnouncementDetailsViewModel(
            eventId = "event-1",
            announcementId = announcement.announcementId,
            dataRepository =
                DataRepositoryLocal(
                    authRepository = FakeAuthRepository(),
                    eventsRepository = events,
                    usersRepository = UsersRepositoryLocal(emptyList()),
                ),
        )
    composeTestRule.setContent {
      CompositionLocalProvider(
          LocalUriHandler provides
              object : UriHandler {
                override fun openUri(uri: String) {
                  when (openAttempts++) {
                    0 -> throw ActivityNotFoundException("No application can open $uri")
                    1 -> throw IllegalArgumentException("Invalid URI: $uri")
                    else -> throw SecurityException("Cannot open $uri")
                  }
                }
              }
      ) {
        AppTheme { AnnouncementDetailsScreen(viewModel = viewModel) }
      }
    }

    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithTag(Tags.TITLE).fetchSemanticsNodes().isNotEmpty()
    }
    repeat(3) {
      composeTestRule
          .onNodeWithTag(Tags.link(announcement.links.single()))
          .performScrollTo()
          .performClick()
    }
    assertEquals(3, openAttempts)
    composeTestRule.onNodeWithTag(Tags.link(announcement.links.single())).assertIsDisplayed()
  }

  @Test
  fun previewRendersRepresentativeAnnouncementDetails() {
    composeTestRule.setContent { AnnouncementDetailsContentPreview() }

    composeTestRule.onNodeWithTag(Tags.TITLE).assertTextContains("Stage B is closed")
    composeTestRule.onNodeWithTag(Tags.AUTHOR).assertTextContains("Marie Dupont", substring = true)
    composeTestRule
        .onNodeWithTag(Tags.DESCRIPTION)
        .assertTextContains("Strong wind on the hill.", substring = true)
    composeTestRule
        .onNodeWithTag(Tags.link("https://vidyfest.ch/map"))
        .performScrollTo()
        .assertIsDisplayed()
  }
}
