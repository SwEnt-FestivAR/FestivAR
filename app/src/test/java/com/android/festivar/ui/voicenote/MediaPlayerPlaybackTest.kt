// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.voicenote.PlayerState
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/**
 * The real [MediaPlayer] glue of the voice note player, on the fake [MediaPlayer] of Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class MediaPlayerPlaybackTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val url = "https://example.com/note.m4a"

  // The fake MediaPlayer created by the playback, to make it report what a real one reports.
  private var shadow: ShadowMediaPlayer? = null

  @Before
  fun setUp() {
    ShadowMediaPlayer.addMediaInfo(
        DataSource.toDataSource(context, Uri.parse(url)),
        ShadowMediaPlayer.MediaInfo(14_000, 0),
    )
    ShadowMediaPlayer.setCreateListener { _, created -> shadow = created }
  }

  @After
  fun tearDown() {
    ShadowMediaPlayer.resetStaticState()
  }

  private fun idle(duration: Duration = Duration.ZERO) =
      shadowOf(Looper.getMainLooper()).idleFor(duration)

  @Test
  fun playback_isPreparedThenStartsAndCompletesAfterTheDuration() {
    val events = mutableListOf<String>()
    val playback = MediaPlayerPlayback(context)

    playback.prepareAsync(
        url,
        onPrepared = { events += "prepared" },
        onCompleted = { events += "completed" },
        onError = { events += "error" },
    )
    idle()
    playback.start()

    assertEquals(listOf("prepared"), events)
    assertEquals(ShadowMediaPlayer.State.STARTED, shadow?.state)

    idle(Duration.ofSeconds(15))

    assertEquals(listOf("prepared", "completed"), events)
  }

  @Test
  fun playback_reportsAnErrorOfThePlayer() {
    var failed = false
    val playback = MediaPlayerPlayback(context)
    playback.prepareAsync(url, onPrepared = {}, onCompleted = {}, onError = { failed = true })
    idle()

    shadow?.invokeErrorListener(1, 0)

    assertTrue(failed)
  }

  @Test
  fun playback_releaseFreesThePlayer() {
    val playback = MediaPlayerPlayback(context)
    playback.prepareAsync(url, onPrepared = {}, onCompleted = {}, onError = {})
    idle()

    playback.release()

    assertEquals(ShadowMediaPlayer.State.END, shadow?.state)
  }

  @Test
  fun androidVoicePlayer_playsANoteAndGoesIdleWhenItEnds() {
    val player = AndroidVoicePlayer(context)

    player.play(url)
    idle()

    assertEquals(PlayerState.Playing(url), player.state.value)
    assertEquals(ShadowMediaPlayer.State.STARTED, shadow?.state)

    idle(Duration.ofSeconds(15))

    assertEquals(PlayerState.Idle, player.state.value)
  }

  @Test
  fun androidVoicePlayer_failsWhenThePlayerReportsAnError() {
    val player = AndroidVoicePlayer(context)
    player.play(url)
    idle()

    shadow?.invokeErrorListener(1, 0)

    assertEquals(PlayerState.Failed(url), player.state.value)
    assertFalse(player.state.value is PlayerState.Playing)
  }
}
