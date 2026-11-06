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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import java.time.Duration

/** Relative heights of the bars of the static waveform. */
private val waveform =
    listOf(0.3f, 0.5f, 0.8f, 0.6f, 0.9f, 0.4f, 0.7f, 1f, 0.55f, 0.35f, 0.65f, 0.85f, 0.45f, 0.3f)

/**
 * A voice note as the design shows it: the play button, a waveform and the duration, on a rounded
 * card.
 *
 * @param duration The length of the voice note.
 * @param isPlaying Whether it plays now, which changes the icon of the button and the waveform.
 * @param onTogglePlay Plays the note, or stops it if it is playing.
 */
@Composable
internal fun VoiceNotePlayer(
    duration: Duration,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier
          .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
          .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PlayButton(isPlaying, onTogglePlay)
    Waveform(isPlaying, Modifier.weight(1f))
    Text(
        formatVoiceDuration(duration),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.testTag(VoiceNoteTestTags.VOICE_NOTE_DURATION),
    )
  }
}

/** The lime round button that plays the note, or stops it while it plays. */
@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
  FilledIconButton(
      onClick = onClick,
      modifier = Modifier.size(40.dp).testTag(VoiceNoteTestTags.VOICE_NOTE_PLAY),
  ) {
    Icon(
        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        contentDescription =
            stringResource(if (isPlaying) R.string.voice_note_pause else R.string.voice_note_play),
    )
  }
}

/** A static waveform: the bars only decorate, they do not follow the audio. */
@Composable
private fun Waveform(isPlaying: Boolean, modifier: Modifier = Modifier) {
  val color =
      if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
      else MaterialTheme.colorScheme.outline
  Row(
      modifier.height(28.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    waveform.forEach { level ->
      Box(Modifier.width(3.dp).height(28.dp * level).background(color, RoundedCornerShape(2.dp)))
    }
  }
}
