// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The layout of the player against the "Voice note row" of the design in Figma. */
@RunWith(AndroidJUnit4::class)
class VoiceNotePlayerTest {
  @get:Rule val composeTestRule = createComposeRule()

  // JUnit creates a new instance of the class for every test, so these mocks start every test
  // without any call
  private val onTogglePlay = mockk<() -> Unit>(relaxed = true)
  private val onDelete = mockk<() -> Unit>(relaxed = true)

  private fun text(id: Int) =
      ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

  @Composable
  private fun PlayerCard(
      cardTag: String,
      isPlaying: Boolean = false,
      trailing: (@Composable () -> Unit)? = null,
  ) {
    Box(Modifier.width(372.dp).testTag(cardTag)) {
      VoiceNotePlayer(
          Duration.ofSeconds(14),
          isPlaying = isPlaying,
          onTogglePlay = onTogglePlay,
          modifier = Modifier.fillMaxWidth(),
          trailing = trailing,
      )
    }
  }

  private fun show(isPlaying: Boolean = false, trailing: (@Composable () -> Unit)? = null) =
      composeTestRule.setContent {
        AppTheme { PlayerCard(CARD, isPlaying = isPlaying, trailing = trailing) }
      }

  private fun tag(tag: String) = composeTestRule.onNodeWithTag(tag)

  /** Breaks if the play button or the duration is drawn outside the card. */
  @Test
  fun playButtonAndDurationAreInsideTheCard() {
    show()

    val card = tag(CARD).getBoundsInRoot()
    val play = tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).getBoundsInRoot()
    val duration = tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).getBoundsInRoot()
    assertTrue(play.left >= card.left && play.right <= duration.left)
    assertTrue(duration.right <= card.right)
  }

  /** Breaks if the action of the owner is outside the card or not after the duration. */
  @Test
  fun trailingActionIsInsideTheCardAfterTheDuration() {
    show(trailing = { Box(Modifier.size(40.dp).testTag(TRAILING)) })

    val card = tag(CARD).getBoundsInRoot()
    val trailing = tag(TRAILING).getBoundsInRoot()
    val duration = tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).getBoundsInRoot()
    assertTrue(trailing.left >= duration.right)
    assertTrue(trailing.right <= card.right)
  }

  /** Breaks if the card keeps the wide right padding when it has an action of the owner. */
  @Test
  fun cardIsTighterOnTheRightWhenThereIsAnAction() {
    composeTestRule.setContent {
      AppTheme {
        Column {
          PlayerCard(CARD)
          PlayerCard(CARD_WITH_ACTION) { Box(Modifier.size(40.dp).testTag(TRAILING)) }
        }
      }
    }

    val plain = tag(CARD).getBoundsInRoot()
    val duration =
        composeTestRule
            .onAllNodesWithTag(VoiceNoteTestTags.VOICE_NOTE_DURATION)[0]
            .getBoundsInRoot()
    val withAction = tag(CARD_WITH_ACTION).getBoundsInRoot()
    val trailing = tag(TRAILING).getBoundsInRoot()
    assertTrue(withAction.right - trailing.right < plain.right - duration.right)
  }

  /** Breaks if a player with nothing to play still lets the button be pressed. */
  @Test
  fun playButtonIsDisabledWhenThePlayerIsNotEnabled() {
    composeTestRule.setContent {
      AppTheme { VoiceNotePlayer(Duration.ofSeconds(7), false, {}, enabled = false) }
    }
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertIsNotEnabled()
  }

  /** Breaks if tapping the play button does not call onTogglePlay exactly once. */
  @Test
  fun playButtonCallsOnTogglePlay() {
    show()
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).performClick()
    verify(exactly = 1) { onTogglePlay() }
    confirmVerified(onTogglePlay)
  }

  /** Breaks if tapping the stop button of a playing note does not call onTogglePlay once. */
  @Test
  fun stopButtonCallsOnTogglePlay() {
    show(isPlaying = true)
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).performClick()
    verify(exactly = 1) { onTogglePlay() }
    confirmVerified(onTogglePlay)
  }

  /** Breaks if the button does not offer to play the note when nothing plays. */
  @Test
  fun idleButtonOffersToPlay() {
    show()
    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_play)).assertIsDisplayed()
  }

  /** Breaks if the button does not offer to stop the note while it plays. */
  @Test
  fun playingButtonOffersToStop() {
    show(isPlaying = true)
    composeTestRule.onNodeWithContentDescription(text(R.string.voice_note_stop)).assertIsDisplayed()
  }

  /** Breaks if tapping the trash does not call its onClick exactly once. */
  @Test
  fun deleteButtonCallsOnClick() {
    composeTestRule.setContent { AppTheme { VoiceNoteDeleteButton(onClick = onDelete) } }
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).performClick()
    verify(exactly = 1) { onDelete() }
    confirmVerified(onDelete)
  }

  /** Breaks if the trash loses its localized content description. */
  @Test
  fun deleteButtonHasAContentDescription() {
    composeTestRule.setContent { AppTheme { VoiceNoteDeleteButton(onClick = onDelete) } }
    composeTestRule
        .onNodeWithContentDescription(text(R.string.voice_note_delete))
        .assertIsDisplayed()
  }

  private companion object {
    const val CARD = "card"
    const val CARD_WITH_ACTION = "cardWithAction"
    const val TRAILING = "trailing"
  }
}
