// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The layout of the player against the "Voice note row" of the design in Figma. */
@RunWith(AndroidJUnit4::class)
class VoiceNotePlayerTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun show(trailing: (@Composable () -> Unit)? = null) = composeTestRule.setContent {
    AppTheme {
      Box(Modifier.width(372.dp).testTag(CARD)) {
        VoiceNotePlayer(
            Duration.ofSeconds(14),
            isPlaying = false,
            onTogglePlay = {},
            modifier = Modifier.fillMaxWidth(),
            trailing = trailing,
        )
      }
    }
  }

  private fun tag(tag: String) = composeTestRule.onNodeWithTag(tag)

  /** Breaks if the card is not the 60dp of the design (a 40dp button with 10dp around). */
  @Test
  fun cardIs60dpHighWithAPlayButtonOf40dp() {
    show()
    tag(CARD).assertHeightIsEqualTo(60.dp)
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertWidthIsEqualTo(40.dp)
    tag(VoiceNoteTestTags.VOICE_NOTE_PLAY).assertHeightIsEqualTo(40.dp)
  }

  /** Breaks if the action of the owner is outside the card, or the padding is not the 8dp. */
  @Test
  fun trailingActionIsInsideTheCardWith8dpOnTheRight() {
    show(trailing = { Box(Modifier.size(40.dp).testTag(TRAILING)) })

    val card = tag(CARD).getBoundsInRoot()
    val trailing = tag(TRAILING).getBoundsInRoot()
    assertEquals(8f, (card.right - trailing.right).value, 0.5f)
    val duration = tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).getBoundsInRoot()
    assertEquals(true, trailing.left >= duration.right)
  }

  /** Breaks if the card has the padding of the owner when there is no action. */
  @Test
  fun cardHas14dpOnTheRightWithoutAnAction() {
    show()

    val card = tag(CARD).getBoundsInRoot()
    val duration = tag(VoiceNoteTestTags.VOICE_NOTE_DURATION).getBoundsInRoot()
    assertEquals(14f, (card.right - duration.right).value, 0.5f)
  }

  /** Breaks if the trash is not the 40dp of the design, or does not carry its test tag. */
  @Test
  fun deleteButtonIs40dp() {
    composeTestRule.setContent { AppTheme { VoiceNoteDeleteButton(onClick = {}) } }

    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertWidthIsEqualTo(40.dp)
    tag(VoiceNoteTestTags.VOICE_NOTE_DELETE).assertHeightIsEqualTo(40.dp)
  }

  private companion object {
    const val CARD = "card"
    const val TRAILING = "trailing"
  }
}
