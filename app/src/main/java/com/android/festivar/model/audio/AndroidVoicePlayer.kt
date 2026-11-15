// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.core.net.toUri
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Android [VoicePlayer]: streams a voice note from its URL with a [MediaPlayer].
 *
 * **One [MediaPlayer] per note.** [play] creates a fresh player and frees the previous one, and a
 * player is freed as soon as its note ends or fails. Nothing is reused, so there is no state to
 * reset between two notes. [MediaPlayer.release] also clears the listeners of the freed player, so
 * a late callback of a replaced or stopped note can never change the [state] of the current one.
 *
 * **State.** [PlayerState.Playing] is reported from the moment [play] is called, while the note is
 * still loading, because the contract has no separate "loading" state. It becomes
 * [PlayerState.Idle] when the note ends or is stopped, and [PlayerState.Failed] when it cannot be
 * loaded or played; the cause is written to the log.
 *
 * **Pause and resume.** [pause] keeps the [MediaPlayer] and its position, and [resume] continues
 * from there without loading the note again. Only [stop] (or the end of the note) frees the player
 * and loses the position. The state alone tells whether the note should run, so pausing or resuming
 * while the note is still loading just changes the state: the prepared listener starts the note
 * only if the state is still [PlayerState.Playing] when it is ready.
 *
 * **Audio.** The note is declared as speech played as media (media volume). The player does not
 * request audio focus: a note is a few seconds long and the user starts it deliberately.
 *
 * **Threading.** Use it from the main thread. A [MediaPlayer] reports back on the thread that
 * created it, so every state change happens on that thread.
 *
 * **Lifecycle.** The owner calls [release] when the screen is closed. As the [VoicePlayer] contract
 * says, the instance must not be used afterwards.
 *
 * @param context Any context; only its application context is kept, so no screen is leaked.
 */
class AndroidVoicePlayer(context: Context) : VoicePlayer {
  private val appContext = context.applicationContext

  private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
  override val state: StateFlow<PlayerState> = _state.asStateFlow()

  /** The player of the note being played, with how far its loading went. */
  private var current: Current = Current.None

  override fun play(audioUrl: String) {
    freeCurrent()
    _state.value = PlayerState.Playing(audioUrl)

    val player = MediaPlayer()
    current = Current.Loading(player)
    player.setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    )
    player.setOnPreparedListener {
      current = Current.Ready(player)
      // Paused while loading: stay ready, and start when resume() is called.
      if (_state.value is PlayerState.Playing) it.start()
    }
    player.setOnCompletionListener { finish(PlayerState.Idle) }
    player.setOnErrorListener { _, what, extra ->
      Log.e(LOG_TAG, "MediaPlayer error what=$what extra=$extra")
      finish(PlayerState.Failed(audioUrl))
      true // Handled here, so the completion listener does not run as well.
    }

    // The exceptions setDataSource and prepareAsync document: a network or file problem (IO), a
    // URL that cannot be used, or a player in the wrong state (runtime).
    try {
      player.setDataSource(appContext, audioUrl.toUri())
      player.prepareAsync()
    } catch (e: IOException) {
      fail(audioUrl, "Cannot load the voice note", e)
    } catch (e: RuntimeException) {
      fail(audioUrl, "Cannot load the voice note", e)
    }
  }

  override fun pause() {
    val playing = _state.value as? PlayerState.Playing ?: return
    val refusal = "The player refused to pause the voice note"
    if (changePlayback(playing.audioUrl, refusal) { pause() }) {
      _state.value = PlayerState.Paused(playing.audioUrl)
    }
  }

  override fun resume() {
    val paused = _state.value as? PlayerState.Paused ?: return
    val refusal = "The player refused to resume the voice note"
    if (changePlayback(paused.audioUrl, refusal) { start() }) {
      _state.value = PlayerState.Playing(paused.audioUrl)
    }
  }

  override fun stop() = finish(PlayerState.Idle)

  override fun release() = stop()

  /**
   * Applies [action] to the current player once it has finished loading. Before that there is
   * nothing to apply: the prepared listener decides from the state. Returns false, and fails the
   * note, if the player is in a state that refuses the action, with [refusal] as the logged reason.
   */
  private fun changePlayback(
      audioUrl: String,
      refusal: String,
      action: MediaPlayer.() -> Unit,
  ): Boolean =
      when (val note = current) {
        Current.None -> false
        is Current.Loading -> true
        is Current.Ready ->
            try {
              note.player.action()
              true
            } catch (e: IllegalStateException) {
              fail(audioUrl, refusal, e)
              false
            }
      }

  /** Logs why the note failed, then frees its player and moves to [PlayerState.Failed]. */
  private fun fail(audioUrl: String, reason: String, cause: Exception) {
    Log.e(LOG_TAG, reason, cause)
    finish(PlayerState.Failed(audioUrl))
  }

  /** Frees the current player and moves to its final [result] state. */
  private fun finish(result: PlayerState) {
    freeCurrent()
    _state.value = result
  }

  private fun freeCurrent() {
    when (val note = current) {
      Current.None -> Unit
      is Current.Loading -> note.player.release()
      is Current.Ready -> note.player.release()
    }
    current = Current.None
  }

  /** The player of the note: none, loading (it cannot be paused or started yet) or ready. */
  private sealed interface Current {
    object None : Current

    class Loading(val player: MediaPlayer) : Current

    class Ready(val player: MediaPlayer) : Current
  }

  private companion object {
    const val LOG_TAG = "AndroidVoicePlayer"
  }
}
