// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreenTestTags as Tags
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val EVENT_ID = "event-1"
private const val UI_WAIT_TIMEOUT = 5_000L

class CreateAnnouncementScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  // FAKE: a real repository that keeps the event in memory. The tests read the stored announcements
  // back from it; nothing checks that a configured mock was called.
  private lateinit var local: EventsRepositoryLocal
  private var backCount = 0
  private var publishedCount = 0

  /** Runs before each write, so a test can make a save fail or wait. */
  private var beforeAdd: suspend () -> Unit = {}

  @Before
  fun setUp() {
    val zone = ZoneId.of("Europe/Zurich")
    local =
        EventsRepositoryLocal(
            listOf(
                Event(
                    eventId = EVENT_ID,
                    ownerId = "owner",
                    title = "Balelec",
                    startDate = ZonedDateTime.of(2026, 11, 6, 16, 0, 0, 0, zone),
                    endDate = ZonedDateTime.of(2026, 11, 7, 4, 0, 0, 0, zone),
                )
            )
        )
    // STUB over the fake: the hook lets a test fail or hold the write.
    val scripted =
        object : EventsRepository by local {
          override suspend fun addAnnouncement(eventId: String, announcement: Announcement) {
            beforeAdd()
            local.addAnnouncement(eventId, announcement)
          }
        }
    val viewModel =
        CreateAnnouncementViewModel(
            EVENT_ID,
            { "owner-1" },
            DataRepositoryLocal(FakeAuthRepository(), eventsRepository = scripted),
        )
    composeTestRule.setContent {
      AppTheme {
        CreateAnnouncementScreen(
            viewModel = viewModel,
            onBack = { backCount++ },
            onPublished = { publishedCount++ },
        )
      }
    }
  }

  private fun string(@StringRes id: Int): String =
      InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

  private fun stored(): List<Announcement> = runBlocking { local.getEvent(EVENT_ID).announcements }

  private fun typeTitle(text: String) =
      composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).performTextInput(text)

  private fun typeDescription(text: String) =
      composeTestRule.onNodeWithTag(Tags.DESCRIPTION_FIELD).performTextInput(text)

  private fun chip(priority: AnnouncementPriority) =
      composeTestRule.onNodeWithTag(Tags.priorityChip(priority))

  private fun publishButton() = composeTestRule.onNodeWithTag(Tags.PUBLISH_BUTTON)

  /** Everything the user can edit: both text fields and every priority chip. */
  private fun inputs() =
      (listOf(Tags.TITLE_FIELD, Tags.DESCRIPTION_FIELD) +
              AnnouncementPriority.entries.map { Tags.priorityChip(it) })
          .map { composeTestRule.onNodeWithTag(it) }

  private fun pressSystemBack() {
    composeTestRule.runOnUiThread {
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
  }

  private fun fillAndPublish() {
    typeTitle("Stage B is closed")
    typeDescription("Strong wind on the hill.")
    publishButton().performClick()
  }

  /**
   * Breaks if a part of the design is missing, if the empty form starts with an error, another
   * default priority, or a Publish button that can be tapped, or if back asks before leaving it.
   */
  @Test
  fun anEmptyFormShowsTheDesignWithInfoSelectedAndBackLeaves() {
    composeTestRule.onNodeWithTag(Tags.TITLE).assertTextEquals(string(R.string.create_announcement))
    composeTestRule
        .onNodeWithTag(Tags.TITLE_FIELD)
        .assertTextContains(string(R.string.announcement_form_field_title))
    composeTestRule
        .onNodeWithTag(Tags.DESCRIPTION_FIELD)
        .assertTextContains(string(R.string.announcement_form_field_description))
    composeTestRule
        .onNodeWithTag(Tags.PRIORITY_LABEL)
        .assertTextEquals(string(R.string.announcement_form_field_priority).uppercase())
    chip(AnnouncementPriority.INFO)
        .assertIsSelected()
        .assertTextEquals(string(R.string.create_announcement_priority_info))
    chip(AnnouncementPriority.WARNING)
        .assertIsNotSelected()
        .assertTextEquals(string(R.string.create_announcement_priority_warning))
    chip(AnnouncementPriority.URGENT)
        .assertIsNotSelected()
        .assertTextEquals(string(R.string.create_announcement_priority_urgent))
    publishButton().assertTextEquals(string(R.string.create_announcement_publish))
    publishButton().assertIsNotEnabled()
    composeTestRule.onNodeWithTag(Tags.TITLE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_ERROR).assertDoesNotExist()

    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
  }

  /**
   * Breaks if the fields swap their values, a chip reports the wrong priority, the button does not
   * publish what the form shows, or the inputs can still be edited once it is published.
   */
  @Test
  fun publishingSavesTheFormAndReportsItOnce() {
    typeTitle("Stage B is closed")
    typeDescription("Strong wind on the hill.")
    chip(AnnouncementPriority.URGENT).performClick()
    chip(AnnouncementPriority.URGENT).assertIsSelected()
    chip(AnnouncementPriority.INFO).assertIsNotSelected()
    publishButton().assertIsEnabled()

    publishButton().performClick()
    composeTestRule.waitForIdle()

    val saved = stored().single()
    assertEquals("Stage B is closed", saved.title)
    assertEquals("Strong wind on the hill.", saved.content)
    assertEquals(AnnouncementPriority.URGENT, saved.priority)
    assertEquals(1, publishedCount)
    assertEquals(0, backCount)
    inputs().forEach { it.assertIsNotEnabled() }
  }

  /**
   * Breaks if the screen does not pass a running save to the form (no spinner with the Publishing
   * label, Publish or the inputs still enabled), if either back leaves or asks during the save,
   * which could publish an announcement the user chose to discard, if the back handler is turned
   * off so that system back closes the activity, or if the save reports success early, stops, or
   * lets a back pressed during it fire once it ends.
   */
  @Test
  fun aRunningSaveLocksTheFormAndBackUntilItFinishes() {
    val gate = CompletableDeferred<Unit>()
    beforeAdd = { gate.await() }
    fillAndPublish()

    composeTestRule
        .onNodeWithTag(Tags.SAVING_INDICATOR, useUnmergedTree = true)
        .assertContentDescriptionEquals(string(R.string.create_announcement_publishing))
    publishButton().assertIsNotEnabled()
    inputs().forEach { it.assertIsNotEnabled() }
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    pressSystemBack()

    assertEquals(0, backCount)
    assertEquals(0, publishedCount)
    assertFalse(composeTestRule.activity.isFinishing)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()

    gate.complete(Unit)
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { publishedCount == 1 }
    assertEquals(1, stored().size)
    assertEquals(0, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  /**
   * Breaks if the screen does not pass its save error to the form, if a failed save leaves the
   * screen, or if the error survives a working retry.
   */
  @Test
  fun aFailedSaveShowsTheErrorAndARetryPublishes() {
    beforeAdd = { throw IllegalStateException("offline") }

    fillAndPublish()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(Tags.SAVE_ERROR)
        .assertTextEquals(string(R.string.create_announcement_error_save_failed))
    assertEquals(0, publishedCount)
    assertEquals(0, backCount)
    assertTrue(stored().isEmpty())

    beforeAdd = {}
    publishButton().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, stored().size)
    assertEquals(1, publishedCount)
    composeTestRule.onNodeWithTag(Tags.SAVE_ERROR).assertDoesNotExist()
  }

  /**
   * Breaks if the screen does not pass its edited state or its dialog title to the back handling,
   * or if "Discard" does not leave without publishing.
   */
  @Test
  fun systemBackFromAnEditedFormAsksWithTheCreateTitleAndDiscardLeaves() {
    typeTitle("Stage B")

    pressSystemBack()
    composeTestRule
        .onNodeWithText(string(R.string.create_announcement_discard_title))
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(Tags.DISCARD_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, backCount)
    assertTrue(stored().isEmpty())
  }
}
