// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
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
import com.android.festivar.ui.announcement.create.CreateAnnouncementActions
import com.android.festivar.ui.announcement.edit.EditAnnouncementScreenTestTags as Tags
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
 * loading, loaded, failed and for a user who is not the organizer, what a save stores, and when
 * going back asks first. The rules themselves are unit tested in `EditAnnouncementViewModelTest`.
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

  // Breaks if a waiting load shows no spinner or no top bar to leave by, if the form is not filled
  // from the announcement or keeps the Create texts, or if an untouched form can be saved.
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
  }

  // Breaks if a state that has not been loaded yet shows the load error instead of the spinner.
  @Test
  fun beforeTheLoadRunsTheScreenShowsTheSpinnerNotTheLoadError() {
    val notLoaded = viewModel().uiState.value
    composeTestRule.setContent {
      AppTheme {
        EditAnnouncementContent(
            state = notLoaded,
            actions = CreateAnnouncementActions(),
            onRetry = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(Tags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.LOAD_ERROR).assertDoesNotExist()
  }

  // Breaks if a failed load shows no error or an empty form, or if the retry does not load again.
  @Test
  fun aFailedLoadShowsTheLoadErrorAndRetryLoadsTheForm() {
    beforeRead = { throw IllegalStateException("offline") }
    showScreen()
    waitFor(Tags.LOAD_ERROR)

    composeTestRule
        .onNodeWithTag(Tags.LOAD_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.edit_announcement_error_load_failed))
    composeTestRule.onNodeWithTag(Tags.LOAD_RETRY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertDoesNotExist()

    beforeRead = {}
    composeTestRule.onNodeWithTag(Tags.LOAD_RETRY).performClick()
    waitFor(Tags.TITLE_FIELD)

    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertTextContains(announcement.title)
    composeTestRule.onNodeWithTag(Tags.LOAD_ERROR).assertDoesNotExist()
  }

  // Breaks if a user who does not own the event gets the form or no notice saying why not, or if
  // that user cannot leave the screen.
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

  // Breaks if a title edit does not enable Save, if the fields swap or are stored untrimmed, if the
  // save rewrites the id, author, links or pin, or if onSaved is not called exactly once.
  @Test
  fun savingStoresTheTrimmedEditKeepsTheOtherFieldsAndReportsItOnce() {
    showLoadedScreen()

    editTitle("  Gates open at 17:00  ")
    saveButton().assertIsEnabled()
    editDescription("  The north gate opens at 17:00.  ")
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

  // Breaks if a chip reports the wrong priority, if the old chip stays selected, or if a priority
  // change alone does not count as a change to save.
  @Test
  fun aPriorityChangeAloneCanBeSaved() {
    showLoadedScreen()

    chip(AnnouncementPriority.URGENT).performScrollTo().performClick()
    chip(AnnouncementPriority.URGENT).assertIsSelected()
    chip(AnnouncementPriority.WARNING).assertIsNotSelected()
    saveButton().assertIsEnabled()

    saveButton().performClick()
    waitUntilSaved()

    val saved = stored()
    assertEquals(AnnouncementPriority.URGENT, saved.priority)
    assertEquals(announcement.title, saved.title)
    assertEquals(announcement.content, saved.content)
  }

  // Breaks if a message is mapped to the wrong field or stays once the field is filled again, or if
  // an edit with an empty field can be saved.
  @Test
  fun emptyingTheFieldsShowsTheirErrorsAndDisablesSaveUntilTheyAreFilledAgain() {
    showLoadedScreen()

    editTitle("")
    composeTestRule
        .onNodeWithTag(Tags.TITLE_ERROR)
        .assertTextEquals(string(R.string.field_error_empty_announcement_title))
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_ERROR).assertDoesNotExist()
    saveButton().assertIsNotEnabled()

    editDescription("")
    composeTestRule
        .onNodeWithTag(Tags.DESCRIPTION_ERROR)
        .assertTextEquals(string(R.string.field_error_empty_announcement_content))
    composeTestRule.onNodeWithTag(Tags.TITLE_ERROR).assertExists()

    editTitle("Gates closed")
    composeTestRule.onNodeWithTag(Tags.TITLE_ERROR).assertDoesNotExist()
    saveButton().assertIsNotEnabled()

    editDescription("The north gate is closed.")
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_ERROR).assertDoesNotExist()
    saveButton().assertIsEnabled()
  }

  // Breaks if a text typed back to the stored one still counts as a change: Save stays enabled, or
  // back asks before leaving a form with nothing to lose.
  @Test
  fun restoringTheOriginalTextDisablesSaveAndBackLeavesWithoutAsking() {
    showLoadedScreen()

    editDescription("The north gate opens at 17:00.")
    saveButton().assertIsEnabled()

    editDescription(announcement.content)
    saveButton().assertIsNotEnabled()

    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  // Breaks if a failed save is not shown, leaves the screen or loses what was typed, if it leaves
  // Save locked, or if the error survives a working retry.
  @Test
  fun aFailedSaveShowsTheErrorKeepsTheEditAndARetrySaves() {
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
    assertEquals(announcement, stored())

    beforeEdit = {}
    saveButton().performClick()
    waitUntilSaved()

    assertEquals("Gates open at 17:00", stored().title)
    assertEquals(1, savedCount)
    composeTestRule.onNodeWithTag(Tags.SAVE_ERROR).assertDoesNotExist()
  }

  // Breaks if the spinner is not shown or has no screen reader label, if Save can be tapped again
  // or an input edited during the save, or if the save reports success early or never finishes.
  @Test
  fun aRunningSaveShowsTheIndicatorAndLocksTheForm() {
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
    assertEquals(0, savedCount)
    assertEquals(announcement, stored())

    gate.complete(Unit)
    waitUntilSaved()
    assertEquals(1, savedCount)
    assertEquals("Gates open at 17:00", stored().title)
  }

  // Breaks if either back leaves or asks during the save, also right after the tap, before the
  // screen is redrawn, if the back handler is turned off so that system back closes the activity,
  // or if a back pressed during the save fires once it ends.
  @Test
  fun backDoesNothingDuringARunningSaveAndTheSaveFinishes() {
    val gate = CompletableDeferred<Unit>()
    beforeEdit = { gate.await() }
    showLoadedScreen()
    editTitle("Gates open at 17:00")
    saveButton().performClick()

    pressSystemBack()
    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()

    assertEquals(0, backCount)
    assertFalse(composeTestRule.activity.isFinishing)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()

    gate.complete(Unit)
    waitUntilSaved()
    assertEquals("Gates open at 17:00", stored().title)
    assertEquals(0, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  // Breaks if a form filled from the announcement counts as changed, so that the back arrow or
  // system back asks before leaving it.
  @Test
  fun backFromAnUnchangedFormLeavesWithoutAsking() {
    showLoadedScreen()

    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    assertEquals(1, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()

    pressSystemBack()
    assertEquals(2, backCount)
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
  }

  // Breaks if the back arrow leaves an edited form, if the dialog asks with other texts than this
  // screen's, or if "Keep editing" leaves or loses the text.
  @Test
  fun backFromAnEditedFormAsksAndKeepEditingStays() {
    showLoadedScreen()
    editTitle("Gates open at 17:00")

    composeTestRule.onNodeWithTag(Tags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(string(R.string.edit_announcement_discard_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(string(R.string.edit_announcement_discard_message))
        .assertIsDisplayed()
    assertEquals(0, backCount)

    composeTestRule.onNodeWithTag(Tags.KEEP_EDITING_BUTTON).performClick()
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertTextContains("Gates open at 17:00")
    assertEquals(0, backCount)
  }

  // Breaks if the system back button skips the question, if "Discard" does not leave or leaves
  // twice, or if discarding stores a part of the edit.
  @Test
  fun systemBackFromAnEditedFormAsksAndDiscardLeavesTheAnnouncementAsStored() {
    showLoadedScreen()
    editTitle("Gates open at 17:00")

    pressSystemBack()
    composeTestRule.onNodeWithTag(Tags.DISCARD_DIALOG).assertIsDisplayed()
    assertEquals(0, backCount)

    composeTestRule.onNodeWithTag(Tags.DISCARD_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, backCount)
    assertEquals(0, savedCount)
    assertEquals(announcement, stored())
  }
}
