// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.map.MapCardTestTags
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.voicenote.VoiceNoteUiState
import com.android.festivar.ui.voicenote.previewNote
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The map card of the task details: where it is, that it is inert, and who sees it. */
@RunWith(AndroidJUnit4::class)
class TaskDetailsMapTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val sami = User("u1", "Sami", "Kabbaj")
  private val yasser = User("u2", "Yasser", "Ameur")

  private val task =
      Task(
          "t1",
          "e1",
          "Run power to stage",
          description = "Use the blue crate.",
          startTime = ZonedDateTime.of(2026, 10, 17, 9, 30, 0, 0, ZoneId.of("Europe/Zurich")),
          location = "Stage, north corner",
          maxAssign = 2,
          ownerId = "owner-1",
      )

  private fun show(
      state: TaskDetailsUiState,
      darkTheme: Boolean = false,
      voiceNote: VoiceNoteUiState = VoiceNoteUiState(),
  ) = composeTestRule.setContent {
    AppTheme(darkTheme = darkTheme) { TaskDetailsContent(state, voiceNote = voiceNote) }
  }

  private fun stateFor(user: User, task: Task = this.task) =
      TaskDetailsUiState(user.uid, task, isLoading = false)

  /**
   * Breaks if the card can no longer be reached by scrolling when the page also shows the people
   * and a voice note.
   */
  @Test
  fun card_isReachableByScrollingWithEverySectionShown() {
    show(
        stateFor(sami, task.addAssignee(yasser, task.startTime!!)),
        voiceNote = VoiceNoteUiState(note = previewNote),
    )

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }

  /** Breaks if the line under the map shows something else than the location of the task. */
  @Test
  fun addressLine_showsTheLocationOfTheTask() {
    show(stateFor(sami))

    composeTestRule
        .onNodeWithTag(TaskMapCardTestTags.ADDRESS)
        .performScrollTo()
        .assertTextEquals(task.location)
  }

  /** Breaks if a task without a location leaves an empty line under the map. */
  @Test
  fun addressLine_isAbsentWithoutALocation() {
    show(stateFor(sami, task.copy(location = "")))

    composeTestRule.onNodeWithTag(TaskMapCardTestTags.ADDRESS).assertDoesNotExist()
    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }

  /** Breaks if the preview of the card fails to compose, or loses its map or its address. */
  @Test
  fun preview_showsTheMapAndTheAddress() {
    composeTestRule.setContent { TaskMapCardPreview() }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskMapCardTestTags.ADDRESS).assertIsDisplayed()
  }

  /** Breaks if the card gets a click action, so a tap on it does something. */
  @Test
  fun card_hasNoClickAction() {
    show(stateFor(sami))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertHasNoClickAction()
  }

  /**
   * Breaks if the card is hidden from a viewer who is not the owner and has not taken the task, or
   * does not display in the light theme, which is the default one.
   */
  @Test
  fun card_isShownToAVolunteerWhoDidNotTakeTheTask() {
    show(stateFor(sami))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }

  /** Breaks if the card is hidden from the owner of the task. */
  @Test
  fun card_isShownToTheOwner() {
    show(stateFor(sami, task.copy(ownerId = sami.uid)))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }

  /** Breaks if the card is hidden from someone who took the task. */
  @Test
  fun card_isShownToAssignees() {
    show(stateFor(sami, task.addAssignee(sami, task.startTime!!)))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }

  /** Breaks if the card is hidden when another person holds the task. */
  @Test
  fun card_isShownWhenSomeoneElseTookTheTask() {
    show(stateFor(sami, task.addAssignee(yasser, task.startTime!!)))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }

  /** Breaks if the card does not display in the dark theme. */
  @Test
  fun card_isDisplayedInDarkTheme() {
    show(stateFor(sami), darkTheme = true)

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).performScrollTo().assertIsDisplayed()
  }
}
