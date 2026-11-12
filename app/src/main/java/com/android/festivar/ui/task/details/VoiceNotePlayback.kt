// Co-authored-by: Copilot App
// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.PlayerState
import com.android.festivar.model.audio.VoicePlayer
import kotlinx.coroutines.flow.StateFlow

/**
 * Listening to the voice note of the task: one button plays it and stops it.
 *
 * @param player plays the audio.
 * @param noteUrl where the voice note of the task is, or `null` if the task has none.
 */
internal class VoiceNotePlayback(
    private val player: VoicePlayer,
    private val noteUrl: () -> String?,
) {
  /** What the player is doing. */
  val state: StateFlow<PlayerState>
    get() = player.state

  /** Plays the voice note, or stops it if it is playing. Does nothing if the task has none. */
  fun toggle() {
    val url = noteUrl() ?: return
    if (isOn(url)) player.stop() else player.play(url)
  }

  /** Stops the note at [url] if the player is playing it or holding it paused. */
  fun stop(url: String) {
    if (isOn(url)) player.stop()
  }

  /** Frees the player. */
  fun release() = player.release()

  private fun isOn(url: String): Boolean =
      when (val current = player.state.value) {
        is PlayerState.Playing -> current.audioUrl == url
        is PlayerState.Paused -> current.audioUrl == url
        else -> false
      }
}
