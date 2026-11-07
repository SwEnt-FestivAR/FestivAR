// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme
import java.time.Duration

/** Heights (in dp) of the 25 bars of the static waveform of the design. */
private val waveform =
    listOf(
        6,
        12,
        18,
        10,
        22,
        14,
        8,
        16,
        20,
        9,
        13,
        17,
        7,
        11,
        19,
        12,
        6,
        15,
        10,
        8,
        14,
        18,
        9,
        12,
        6,
    )

/**
 * A voice note as the design shows it: the play button, a waveform, the duration and, if there is
 * one, an action of the owner (the trash), all on the same rounded card.
 *
 * @param duration The length of the voice note.
 * @param isPlaying Whether it plays now, which changes the icon of the button.
 * @param onTogglePlay Plays the note, or stops it if it is playing. `null` disables the button, for
 *   a recording that is not uploaded yet and so has nothing to play.
 * @param trailing An action at the end of the card, `null` if there is none. The card is narrower
 *   on the right without it (14dp instead of 8dp), as in the design.
 */
@Composable
internal fun VoiceNotePlayer(
    duration: Duration,
    isPlaying: Boolean,
    onTogglePlay: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
  Row(
      modifier
          .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
          .padding(
              start = 10.dp,
              top = 10.dp,
              end = if (trailing != null) 8.dp else 14.dp,
              bottom = 10.dp,
          ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PlayButton(isPlaying, onTogglePlay)
    Waveform(Modifier.weight(1f))
    Text(
        formatVoiceDuration(duration),
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag(VoiceNoteTestTags.VOICE_NOTE_DURATION),
    )
    trailing?.invoke()
  }
}

/** The trash of the owner, 40dp like the play button, to put at the end of the player. */
@Composable
internal fun VoiceNoteDeleteButton(onClick: () -> Unit) {
  IconButton(
      onClick = onClick,
      modifier = Modifier.size(40.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_DELETE),
  ) {
    Icon(
        Icons.Outlined.DeleteOutline,
        contentDescription = stringResource(R.string.voice_note_delete),
    )
  }
}

/** The lime round button that plays the note, or stops it while it plays. */
@Composable
private fun PlayButton(isPlaying: Boolean, onClick: (() -> Unit)?) {
  FilledIconButton(
      onClick = onClick ?: {},
      modifier = Modifier.size(40.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_PLAY),
      enabled = onClick != null,
      // A player with nothing to play yet keeps the lime button of the design (the "send confirm"
      // frame), it is only not pressable.
      colors =
          IconButtonDefaults.filledIconButtonColors(
              disabledContainerColor = MaterialTheme.colorScheme.primary,
              disabledContentColor = MaterialTheme.colorScheme.onPrimary,
          ),
  ) {
    Icon(
        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        contentDescription =
            stringResource(if (isPlaying) R.string.voice_note_pause else R.string.voice_note_play),
    )
  }
}

/**
 * A static waveform: the bars only decorate, they do not follow the audio. They stay at the start
 * of the free space, as in the design.
 */
@Composable
private fun Waveform(modifier: Modifier = Modifier) {
  val color = MaterialTheme.colorScheme.onSurfaceVariant
  Row(
      modifier.height(22.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    waveform.forEach { height ->
      Box(Modifier.width(3.dp).height(height.dp).background(color, RoundedCornerShape(2.dp)))
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNotePlayerPreview() {
  AppTheme {
    VoiceNotePlayer(
        Duration.ofSeconds(14),
        isPlaying = false,
        onTogglePlay = {},
        Modifier.padding(20.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNotePlayerPlayingPreview() {
  AppTheme {
    VoiceNotePlayer(
        Duration.ofSeconds(14),
        isPlaying = true,
        onTogglePlay = {},
        Modifier.padding(20.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun VoiceNotePlayerWithTrashPreview() {
  AppTheme {
    VoiceNotePlayer(
        Duration.ofSeconds(14),
        isPlaying = false,
        onTogglePlay = {},
        Modifier.padding(20.dp),
        trailing = { VoiceNoteDeleteButton(onClick = {}) },
    )
  }
}
