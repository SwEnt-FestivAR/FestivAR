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

  /** The player of the note being played or loaded, or null when there is none. */
  private var current: MediaPlayer? = null

  override fun play(audioUrl: String) {
    freeCurrent()
    _state.value = PlayerState.Playing(audioUrl)

    val player = MediaPlayer()
    current = player
    player.setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    )
    player.setOnPreparedListener { it.start() }
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
      fail(audioUrl, e)
    } catch (e: RuntimeException) {
      fail(audioUrl, e)
    }
  }

  override fun stop() {
    freeCurrent()
    _state.value = PlayerState.Idle
  }

  override fun release() = stop()

  private fun fail(audioUrl: String, cause: Exception) {
    Log.e(LOG_TAG, "Error loading the voice note", cause)
    finish(PlayerState.Failed(audioUrl))
  }

  /** Frees the current player and moves to its final [result] state. */
  private fun finish(result: PlayerState) {
    freeCurrent()
    _state.value = result
  }

  private fun freeCurrent() {
    current?.release()
    current = null
  }

  private companion object {
    const val LOG_TAG = "AndroidVoicePlayer"
  }
}
