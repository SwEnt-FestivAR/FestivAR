// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.android.festivar.model.voicenote.PlayerState
import com.android.festivar.model.voicenote.VoicePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A single playback of one audio file: the small part of [MediaPlayer] that is used. */
internal interface AudioPlayback {
  /**
   * Starts loading the audio at [url] without blocking, then calls [onPrepared] when it can start,
   * [onCompleted] when it ends and [onError] if it cannot be loaded or played.
   */
  fun prepareAsync(
      url: String,
      onPrepared: () -> Unit,
      onCompleted: () -> Unit,
      onError: () -> Unit,
  )

  /** Starts playing once prepared. */
  fun start()

  /** Frees the playback. It must not be used afterwards. */
  fun release()
}

/** Creates the [AudioPlayback] of each note, so the tests can replace [MediaPlayer]. */
internal fun interface AudioPlaybackFactory {
  fun create(): AudioPlayback
}

/** The [AudioPlayback] backed by a [MediaPlayer] that streams from a URL. */
internal class MediaPlayerPlayback(private val context: Context) : AudioPlayback {
  private val player =
      MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
      }

  override fun prepareAsync(
      url: String,
      onPrepared: () -> Unit,
      onCompleted: () -> Unit,
      onError: () -> Unit,
  ) {
    player.setOnPreparedListener { onPrepared() }
    player.setOnCompletionListener { onCompleted() }
    player.setOnErrorListener { _, _, _ ->
      onError()
      true
    }
    player.setDataSource(context, Uri.parse(url))
    player.prepareAsync()
  }

  override fun start() = player.start()

  override fun release() = player.release()
}

/**
 * The Android [VoicePlayer]: streams the note from its URL with a [MediaPlayer]. It plays one note
 * at a time and must be used from the main thread, where the [MediaPlayer] reports back.
 */
class AndroidVoicePlayer internal constructor(private val factory: AudioPlaybackFactory) :
    VoicePlayer {

  /** Creates a player that uses the application context of [context]. */
  constructor(
      context: Context
  ) : this(AudioPlaybackFactory { MediaPlayerPlayback(context.applicationContext) })

  private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
  override val state: StateFlow<PlayerState> = _state.asStateFlow()

  private var current: AudioPlayback? = null
  private var released = false

  override fun play(audioUrl: String) {
    if (released) return
    freeCurrent()
    _state.value = PlayerState.Playing(audioUrl)
    val playback =
        try {
          factory.create()
        } catch (e: RuntimeException) {
          _state.value = PlayerState.Failed(audioUrl)
          return
        }
    current = playback
    // A callback of a playback that was replaced or stopped in the meantime is ignored.
    fun ifCurrent(action: () -> Unit) = { if (current === playback) action() }
    try {
      playback.prepareAsync(
          audioUrl,
          onPrepared = ifCurrent { playback.start() },
          onCompleted = ifCurrent { finish(PlayerState.Idle) },
          onError = ifCurrent { finish(PlayerState.Failed(audioUrl)) },
      )
    } catch (e: java.io.IOException) {
      finish(PlayerState.Failed(audioUrl))
    } catch (e: RuntimeException) {
      finish(PlayerState.Failed(audioUrl))
    }
  }

  override fun stop() {
    freeCurrent()
    _state.value = PlayerState.Idle
  }

  override fun release() {
    freeCurrent()
    released = true
    _state.value = PlayerState.Idle
  }

  /** Ends the current playback with the final [result] state. */
  private fun finish(result: PlayerState) {
    freeCurrent()
    _state.value = result
  }

  private fun freeCurrent() {
    val playback = current ?: return
    current = null
    try {
      playback.release()
    } catch (e: RuntimeException) {
      // Nothing more to free: the playback is already unusable.
    }
  }
}
