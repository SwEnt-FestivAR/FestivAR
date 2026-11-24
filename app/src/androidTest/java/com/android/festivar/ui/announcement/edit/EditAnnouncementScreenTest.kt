// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.edit

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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.edit.EditAnnouncementScreenTestTags as Tags
import com.android.festivar.ui.components.RetryButtonTestTags
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

private const val EVENT_ID = "event-1"
private const val OWNER_ID = "owner-1"
private const val UI_WAIT_TIMEOUT = 5_000L

/**
 * The Edit announcement screen on its ViewModel and an in-memory repository: what it shows while
 * loading, loaded, failed and for a user who is not the organizer, what a save stores, and that it
 * passes its state, texts and actions to the shared form. The rules themselves are unit tested in
 * `EditAnnouncementViewModelTest`, and the shared form in `AnnouncementFormUiTest`.
 */
class EditAnnouncementScreenTest {
  // The same rule as CreateAnnouncementScreenTest, not the v2 one: the screen collects the state at
  // once, as on a device, so a system back pressed right after an input must already see it.
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val zone = ZoneId.of("Europe/Zurich")

  // Its author is not the organizer, and its priority, link and pin are not the defaults, so a save
  // that loses or rewrites one of them is seen.
  private val announcement =
      Announcement(
          announcementId = "ann-1",
          title = "Gates open",
          authorId = "author-1",
          content = "The north gate opens at 16:00.",
          priority = AnnouncementPriority.WARNING,
          updatedAt = ZonedDateTime.of(2026, 10, 30, 10, 0, 0, 0, zone),
          links = listOf("https://festivar.example/gates"),
          pinned = true,
      )

  // FAKE: a real repository that keeps the event in memory. The tests read the stored announcement
  // back from it; nothing checks that a configured mock was called.
  private val local =
      EventsRepositoryLocal(
          listOf(
              Event(
                  eventId = EVENT_ID,
                  ownerId = OWNER_ID,
                  title = "Balelec",
                  startDate = ZonedDateTime.of(2026, 11, 6, 16, 0, 0, 0, zone),
                  endDate = ZonedDateTime.of(2026, 11, 7, 4, 0, 0, 0, zone),
                  announcements = listOf(announcement),
              )
          )
      )

  /** Runs before each read of the event, so a test can make a load fail or wait. */
  private var beforeRead: suspend () -> Unit = {}

  /** Runs before each write, so a test can make a save fail or wait. */
  private var beforeEdit: suspend () -> Unit = {}

  // STUB over the fake: the hooks let a test fail or hold a read or a write.
  private val scripted =
      object : EventsRepository by local {
        override suspend fun getEvent(eventId: String): Event {
          beforeRead()
          return local.getEvent(eventId)
        }

        override suspend fun editAnnouncement(eventId: String, announcement: Announcement) {
          beforeEdit()
          local.editAnnouncement(eventId, announcement)
        }
      }
  private val dataRepository =
      DataRepositoryLocal(FakeAuthRepository(), eventsRepository = scripted)

  private var backCount = 0
  private var savedCount = 0

  private fun viewModel(userId: String = OWNER_ID) =
      EditAnnouncementViewModel(EVENT_ID, announcement.announcementId, { userId }, dataRepository)

  private fun showScreen(userId: String = OWNER_ID) {
    val viewModel = viewModel(userId)
    composeTestRule.setContent {
      AppTheme {
        EditAnnouncementScreen(
            viewModel = viewModel,
            onBack = { backCount++ },
            onSaved = { savedCount++ },
        )
      }
    }
  }

  private fun showLoadedScreen() {
    showScreen()
    waitFor(Tags.TITLE_FIELD)
  }

  private fun string(@StringRes id: Int): String =
      InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

  private fun stored(): Announcement = runBlocking {
    local.getEvent(EVENT_ID).announcements.single()
  }

  private fun waitFor(tag: String) =
      composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
        composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
      }

  private fun waitUntilSaved() {
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { savedCount > 0 }
    composeTestRule.waitForIdle()
  }

  private fun editTitle(text: String) =
      composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).performScrollTo().performTextReplacement(text)

  private fun editDescription(text: String) =
      composeTestRule
          .onNodeWithTag(Tags.DESCRIPTION_FIELD)
          .performScrollTo()
          .performTextReplacement(text)

  private fun chip(priority: AnnouncementPriority) =
      composeTestRule.onNodeWithTag(Tags.priorityChip(priority))

  private fun saveButton() = composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON)

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

  /**
   * Breaks if a waiting load shows no spinner or no top bar to leave by, if the form is not filled
   * from the announcement or keeps the Create texts, if an untouched form can be saved, or if back
   * asks before leaving it.
   */
  @Test
  fun theScreenShowsTheSpinnerThenTheFormFilledFromTheAnnouncement() {
    val gate = CompletableDeferred<Unit>()
    beforeRead = { gate.await() }
    showScreen()

    composeTestRule.onNodeWithTag(Tags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.TITLE).assertTextEquals(string(R.string.edit_announcement))
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertDoesNotExist()

    gate.complete(Unit)
    waitFor(Tags.TITLE_FIELD)

    composeTestRule.onNodeWithTag(Tags.LOADING).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.TITLE).assertTextEquals(string(R.string.edit_announcement))
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertTextContains(announcement.title)
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_FIELD).assertTextContains(announcement.content)
    chip(AnnouncementPriority.WARNING).assertIsSelected()
    chip(AnnouncementPriority.INFO).assertIsNotSelected()
    chip(AnnouncementPriority.URGENT).assertIsNotSelected()
    saveButton().assertTextEquals(string(R.string.edit_announcement_save))
    saveButton().assertIsNotEnabled()
    composeTestRule.onNodeWithTag(Tags.TITLE_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_ERROR).assertDoesNotExist()

    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  /**
   * Breaks if a failed load shows no error or an empty form, or if the retry does not load again.
   */
  @Test
  fun aFailedLoadShowsTheLoadErrorAndRetryLoadsTheForm() {
    beforeRead = { throw IllegalStateException("offline") }
    showScreen()
    waitFor(Tags.LOAD_ERROR)

    composeTestRule
        .onNodeWithTag(Tags.LOAD_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.edit_announcement_error_load_failed))
    composeTestRule.onNodeWithTag(RetryButtonTestTags.BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertDoesNotExist()

    beforeRead = {}
    composeTestRule.onNodeWithTag(RetryButtonTestTags.BUTTON).performClick()
    waitFor(Tags.TITLE_FIELD)

    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertTextContains(announcement.title)
    composeTestRule.onNodeWithTag(Tags.LOAD_ERROR).assertDoesNotExist()
  }

  /**
   * Breaks if a user who does not own the event gets the form or no notice saying why not, or if
   * that user cannot leave the screen.
   */
  @Test
  fun aUserWhoIsNotTheOrganizerSeesWhyInsteadOfTheForm() {
    showScreen(userId = "volunteer-1")
    waitFor(Tags.NOT_ORGANIZER)

    composeTestRule
        .onNodeWithTag(Tags.NOT_ORGANIZER)
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.edit_announcement_not_organizer))
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertDoesNotExist()

    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  /**
   * Breaks if a field or the priority does not reach the ViewModel, if the save rewrites the id,
   * author, links or pin, or if onSaved is not called exactly once. The trimming and the change
   * rules are in EditAnnouncementViewModelTest.
   */
  @Test
  fun savingStoresTheEditAndReportsItOnce() {
    showLoadedScreen()

    editTitle("Gates open at 17:00")
    editDescription("The north gate opens at 17:00.")
    chip(AnnouncementPriority.INFO).performScrollTo().performClick()
    saveButton().performClick()
    waitUntilSaved()

    val saved = stored()
    assertEquals(
        announcement.copy(
            title = "Gates open at 17:00",
            content = "The north gate opens at 17:00.",
            priority = AnnouncementPriority.INFO,
            updatedAt = saved.updatedAt,
        ),
        saved,
    )
    assertEquals(1, savedCount)
    assertEquals(0, backCount)
  }

  /**
   * Breaks if the screen does not pass its save error to the form, if it replaces the form with the
   * load error, or if a failed save locks Save or loses the edit. The retry is in
   * EditAnnouncementViewModelTest.
   */
  @Test
  fun aFailedSaveShowsTheErrorAndKeepsTheEdit() {
    beforeEdit = { throw IllegalStateException("offline") }
    showLoadedScreen()
    editTitle("Gates open at 17:00")

    saveButton().performClick()
    waitFor(Tags.SAVE_ERROR)

    Espresso.closeSoftKeyboard()
    composeTestRule
        .onNodeWithTag(Tags.SAVE_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.edit_announcement_error_save_failed))
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertTextContains("Gates open at 17:00")
    saveButton().assertIsEnabled()
    assertEquals(0, savedCount)
  }

  /**
   * Breaks if the screen does not pass a running save to the form (spinner, Save and inputs), if
   * either back acts during the save, if the back handler is turned off so that system back closes
   * the activity, or if the save reports success early, stops, or lets a back pressed during it
   * fire once it ends.
   */
  @Test
  fun aRunningSaveLocksTheFormAndBackUntilItFinishes() {
    val gate = CompletableDeferred<Unit>()
    beforeEdit = { gate.await() }
    showLoadedScreen()
    editTitle("Gates open at 17:00")
    saveButton().performClick()

    composeTestRule
        .onNodeWithTag(Tags.SAVING_INDICATOR, useUnmergedTree = true)
        .assertContentDescriptionEquals(string(R.string.edit_announcement_saving))
    saveButton().assertIsNotEnabled()
    inputs().forEach { it.assertIsNotEnabled() }
    pressSystemBack()
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()

    assertEquals(0, backCount)
    assertEquals(0, savedCount)
    assertEquals(announcement, stored())
    assertFalse(composeTestRule.activity.isFinishing)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()

    gate.complete(Unit)
    waitUntilSaved()
    assertEquals(1, savedCount)
    assertEquals("Gates open at 17:00", stored().title)
    assertEquals(0, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  /**
   * Breaks if the screen does not pass its edited state or its dialog texts to the back handling,
   * or if "Discard" does not leave or stores a part of the edit.
   */
  @Test
  fun systemBackFromAnEditedFormAsksWithTheEditTextsAndDiscardLeaves() {
    showLoadedScreen()
    editTitle("Gates open at 17:00")

    pressSystemBack()
    composeTestRule
        .onNodeWithText(string(R.string.edit_announcement_discard_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(string(R.string.edit_announcement_discard_message))
        .assertIsDisplayed()
    assertEquals(0, backCount)

    composeTestRule.onNodeWithTag(Tags.DISCARD_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, backCount)
    assertEquals(0, savedCount)
    assertEquals(announcement, stored())
  }
}
