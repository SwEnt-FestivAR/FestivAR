// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.theme.AppTheme
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The voice note section for every state, with no ViewModel. */
@RunWith(AndroidJUnit4::class)
class VoiceNoteContentTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val note =
      VoiceNote(
          "n1",
          "https://x/n1.m4a",
          Duration.ofSeconds(14),
          ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC),
      )

  private fun text(id: Int, vararg args: Any) =
      ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

  // JUnit creates a new instance of the class for every test, so these mocks start every test
  // without any call
  private val onTogglePlay = mockk<() -> Unit>(relaxed = true)
  private val onDelete = mockk<() -> Unit>(relaxed = true)
  private val onConfirmDelete = mockk<() -> Unit>(relaxed = true)
  private val onDismissDelete = mockk<() -> Unit>(relaxed = true)

  private val callbacks =
      VoiceNoteCallbacks.NoOp.let {
        VoiceNoteCallbacks(
            onTogglePlay = onTogglePlay,
            onDelete = onDelete,
            onConfirmDelete = onConfirmDelete,
            onDismissDelete = onDismissDelete,
            onRecordStart = it.onRecordStart,
            onRecordRelease = it.onRecordRelease,
            onRecordCancel = it.onRecordCancel,
            onRetry = it.onRetry,
            onOpenSettings = it.onOpenSettings,
        )
      }

  /** Fails if a callback was called that the test did not verify, so a stray call is caught. */
  private fun assertNoOtherCall() =
      confirmVerified(onTogglePlay, onDelete, onConfirmDelete, onDismissDelete)

  /** The fixed current day of the tests: 07/11/2026, the same year as the note. */
  private fun clockOn(date: LocalDate) =
      Clock.fixed(Instant.parse("${date}T12:00:00Z"), ZoneOffset.UTC)

  private val thisYear = clockOn(LocalDate.of(2026, 11, 7))

  private fun show(state: VoiceNoteUiState, clock: Clock = thisYear) = composeTestRule.setContent {
    AppTheme { VoiceNoteSection(state, callbacks, clock = clock) }
  }

  private fun tag(tag: String) = composeTestRule.onNodeWithTag(tag)

  /** The date and the time of the note as the author line shows them, in the year of the note. */
  private val recordedAt =
      formatRecordedAt(note.createdAt, LocalDate.of(2026, 11, 7), text(R.string.voice_note_today))

  /**
   * Breaks if the owner no longer sees the label, the author line with the date and the time, the
   * duration, the play button and the trash.
   */
  @Test
  fun ownerSeesTheNoteWithTrash() {
    show(VoiceNoteUiState(note = note, canManage = true))
    composeTestRule.onNodeWithText(text(R.string.voice_note_label)).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(text(R.string.voice_note_author_you, recordedAt))
    tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).assertTextContains("0:14")
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertIsDisplayed()
  }

  /** Breaks if a volunteer sees the trash, or the note is not credited to the default organizer. */
  @Test
  fun volunteerSeesTheNoteWithoutTrash() {
    show(VoiceNoteUiState(note = note, canManage = false))
    composeTestRule.onNodeWithText(text(R.string.voice_note_label)).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(
            text(R.string.voice_note_author_other, text(R.string.voice_note_organizer), recordedAt)
        )
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertDoesNotExist()
  }

  /** Breaks if the note is credited to the organizer even though its owner is somebody else. */
  @Test
  fun volunteerSeesTheNameOfTheOwner() {
    composeTestRule.setContent {
      AppTheme {
        VoiceNoteSection(
            VoiceNoteUiState(note = note),
            callbacks,
            ownerName = "Alice",
            clock = thisYear,
        )
      }
    }
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(text(R.string.voice_note_author_other, "Alice", recordedAt))
  }

  /** Breaks if the year of a note recorded in another year is not shown in the author line. */
  @Test
  fun authorLineShowsTheYearOfAnOlderNote() {
    show(VoiceNoteUiState(note = note, canManage = true), clockOn(LocalDate.of(2027, 1, 5)))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(
            text(
                R.string.voice_note_author_you,
                formatRecordedAt(
                    note.createdAt,
                    LocalDate.of(2027, 1, 5),
                    text(R.string.voice_note_today),
                ),
            )
        )
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR).assertTextContains("2026", substring = true)
  }

  /** Breaks if a note recorded today shows its date instead of "Today". */
  @Test
  fun authorLineSaysTodayForANoteOfToday() {
    show(VoiceNoteUiState(note = note, canManage = true), clockOn(LocalDate.of(2026, 10, 17)))
    tag(VoiceNoteTestTags.VOICE_NOTE_AUTHOR)
        .assertTextContains(
            text(R.string.voice_note_author_you, "${text(R.string.voice_note_today)}, 09:12")
        )
  }

  /**
   * Breaks if a volunteer sees the label, the empty state or a player for a task with no note: the
   * whole section must be hidden.
   */
  @Test
  fun volunteerSeesNothingWithoutNote() {
    show(VoiceNoteUiState())
    composeTestRule.onNodeWithText(text(R.string.voice_note_label)).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertDoesNotExist()
  }

  /** Breaks if the owner of a task without a note no longer sees the empty state and its texts. */
  @Test
  fun ownerSeesTheEmptyStateWithoutNote() {
    show(VoiceNoteUiState(canManage = true))
    composeTestRule.onNodeWithText(text(R.string.voice_note_label)).assertIsDisplayed()
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_empty_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_empty_body)).assertIsDisplayed()
  }

  /** Breaks if the empty state stays on screen next to an existing note. */
  @Test
  fun emptyStateIsHiddenWhenANoteExists() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertDoesNotExist()
  }

  /** Breaks if tapping the play button does not call onTogglePlay exactly once. */
  @Test
  fun playTogglesThroughTheCallback() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).performClick()
    verify(exactly = 1) { onTogglePlay() }
    assertNoOtherCall()
  }

  /** Breaks if the button does not offer to stop the note while it plays. */
  @Test
  fun playButtonChangesWhilePlaying() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Playing(note.audioUrl)))
    composeTestRule
        .onNodeWithContentDescription(text(R.string.voice_note_pause))
        .assertIsDisplayed()
  }

  /** Breaks if tapping the stop button of a playing note does not call onTogglePlay once. */
  @Test
  fun pauseTogglesThroughTheCallback() {
    show(VoiceNoteUiState(note = note, player = PlayerState.Playing(note.audioUrl)))
    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_pause)).performClick()
    verify(exactly = 1) { onTogglePlay() }
    assertNoOtherCall()
  }

  /** Breaks if the button does not offer to play the note when nothing plays. */
  @Test
  fun idlePlayButtonOffersToPlay() {
    show(VoiceNoteUiState(note = note))
    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_play)).assertIsDisplayed()
  }

  /** Breaks if the trash of the owner loses its localized content description. */
  @Test
  fun trashHasAContentDescription() {
    show(VoiceNoteUiState(note = note, canManage = true))
    composeTestRule
        .onNodeWithContentDescription(text(R.string.voice_note_delete))
        .assertIsDisplayed()
  }

  /** Breaks if the trash deletes right away instead of only asking to confirm. */
  @Test
  fun trashAsksToConfirm() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).performClick()
    verify(exactly = 1) { onDelete() }
    assertNoOtherCall()
  }

  /** Breaks if the dialog loses its texts or its two buttons. */
  @Test
  fun deleteDialogShowsItsTextsAndButtons() {
    show(VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_delete_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.voice_note_delete_body)).assertIsDisplayed()
    tag(VoiceNoteTestTags.KEEP_BUTTON).assertIsDisplayed()
    tag(VoiceNoteTestTags.CONFIRM_DELETE_BUTTON).assertIsDisplayed()
  }

  /** Breaks if "Keep it" does not call onDismissDelete once, or also confirms the deletion. */
  @Test
  fun keepButtonDismissesTheDialog() {
    show(VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.KEEP_BUTTON).performClick()
    verify(exactly = 1) { onDismissDelete() }
    assertNoOtherCall()
  }

  /** Breaks if "Delete" does not call onConfirmDelete once, or also dismisses the dialog. */
  @Test
  fun deleteButtonConfirmsTheDeletion() {
    show(VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE_BUTTON).performClick()
    verify(exactly = 1) { onConfirmDelete() }
    assertNoOtherCall()
  }

  /**
   * Breaks if leaving the dialog without choosing does not call onDismissDelete. The back button of
   * the device goes through the same `onDismissRequest` as a tap outside of the dialog.
   */
  @Test
  fun leavingTheDialogDismissesIt() {
    show(VoiceNoteUiState(note = note, canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertIsDisplayed()
    Espresso.pressBack()
    composeTestRule.waitForIdle()
    verify(exactly = 1) { onDismissDelete() }
    assertNoOtherCall()
  }

  /** Breaks if the delete dialog is shown without being asked for. */
  @Test
  fun noDialogByDefault() {
    show(VoiceNoteUiState(note = note, canManage = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertDoesNotExist()
  }

  /** Breaks if a volunteer can get the delete dialog, even if the state asks to confirm. */
  @Test
  fun volunteerNeverSeesTheDeleteDialog() {
    show(VoiceNoteUiState(note = note, canManage = false, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertDoesNotExist()
  }

  /** Breaks if the dialog shows for a volunteer of a task with no note (the section is hidden). */
  @Test
  fun volunteerWithoutNoteNeverSeesTheDeleteDialog() {
    show(VoiceNoteUiState(canManage = false, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertDoesNotExist()
  }

  /**
   * Breaks if the owner of a task with no note gets a dialog to delete a note that is not there.
   */
  @Test
  fun ownerWithoutNoteNeverSeesTheDeleteDialog() {
    show(VoiceNoteUiState(canManage = true, showDeleteConfirm = true))
    tag(VoiceNoteTestTags.VOICE_NOTE_EMPTY).assertIsDisplayed()
    tag(VoiceNoteTestTags.CONFIRM_DELETE).assertDoesNotExist()
  }
}
