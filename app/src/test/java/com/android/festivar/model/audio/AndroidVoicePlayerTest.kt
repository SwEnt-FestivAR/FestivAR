// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Looper
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowMediaPlayer.State
import org.robolectric.shadows.util.DataSource

/**
 * [AndroidVoicePlayer] on Robolectric's fake [android.media.MediaPlayer], which reports prepared,
 * completed and error like a real one. The tests look at the fake of each note through [players].
 */
@RunWith(AndroidJUnit4::class)
class AndroidVoicePlayerTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val first = "https://example.com/first.m4a"
  private val second = "https://example.com/second.m4a"

  /** The fake players created so far, oldest first: one per note played. */
  private val players = mutableListOf<ShadowMediaPlayer>()

  /** The real players behind [players], in the same order, to read their position. */
  private val mediaPlayers = mutableListOf<MediaPlayer>()

  private lateinit var player: AndroidVoicePlayer

  @Before
  fun setUp() {
    listOf(first, second).forEach {
      ShadowMediaPlayer.addMediaInfo(
          DataSource.toDataSource(context, it.toUri()),
          ShadowMediaPlayer.MediaInfo(DURATION_MS, 0),
      )
    }
    ShadowMediaPlayer.setCreateListener { real, created ->
      mediaPlayers += real
      players += created
    }
    player = AndroidVoicePlayer(context)
  }

  @After
  fun tearDown() {
    ShadowMediaPlayer.resetStaticState()
  }

  private fun idle(duration: Duration = Duration.ZERO) =
      shadowOf(Looper.getMainLooper()).idleFor(duration)

  @Test
  fun startsIdle() {
    assertEquals(PlayerState.Idle, player.state.value)
  }

  @Test
  fun play_reportsPlayingWhileLoadingThenStartsWhenPrepared() {
    player.play(first)

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.PREPARING, players.single().state)

    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  @Test
  fun play_goesIdleAndFreesThePlayerWhenTheNoteEnds() {
    player.play(first)
    idle(Duration.ofMillis(DURATION_MS + 1_000L))

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun play_failsAndFreesThePlayerWhenThePlayerReportsAnError() {
    player.play(first)
    idle()

    players.single().invokeErrorListener(1, 0)

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun play_failsWhenTheNoteCannotBeLoaded() {
    ShadowMediaPlayer.addException(DataSource.toDataSource(context, first.toUri()), IOException())

    player.play(first)

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun play_failsWhenThePlayerCannotUseTheUrl() {
    // The fake throws an IllegalArgumentException for a source it knows nothing about.
    val unknown = "https://example.com/unknown.m4a"

    player.play(unknown)

    assertEquals(PlayerState.Failed(unknown), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun play_anotherNoteReplacesTheFirstOne() {
    player.play(first)
    idle()

    player.play(second)
    idle()

    assertEquals(PlayerState.Playing(second), player.state.value)
    assertEquals(State.END, players[0].state)
    assertEquals(State.STARTED, players[1].state)
  }

  @Test
  fun play_afterAFailureStartsAgain() {
    player.play(first)
    idle()
    players.single().invokeErrorListener(1, 0)

    player.play(first)
    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players[1].state)
  }

  @Test
  fun stop_goesIdleAndFreesThePlayer() {
    player.play(first)
    idle()

    player.stop()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun stop_whileLoadingNeverStartsTheNote() {
    player.play(first)

    player.stop()
    idle()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun stop_whenNothingPlaysDoesNothing() {
    player.stop()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(emptyList<ShadowMediaPlayer>(), players)
  }

  @Test
  fun release_stopsTheNote() {
    player.play(first)
    idle()

    player.release()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun pause_keepsThePlayerAndReportsPaused() {
    player.play(first)
    idle()

    player.pause()

    assertEquals(PlayerState.Paused(first), player.state.value)
    assertEquals(State.PAUSED, players.single().state)
  }

  @Test
  fun resume_continuesTheSamePlayerFromWhereItStopped() {
    player.play(first)
    idle(Duration.ofMillis(2_000))
    player.pause()
    val pausedAt = mediaPlayers.single().currentPosition

    idle(Duration.ofMillis(3_000))
    assertEquals(pausedAt, mediaPlayers.single().currentPosition)

    player.resume()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
    assertEquals(1, players.size)
    assertEquals(pausedAt, mediaPlayers.single().currentPosition)
  }

  @Test
  fun resume_thenTheNoteEndsAsUsual() {
    player.play(first)
    idle(Duration.ofMillis(2_000))
    player.pause()
    player.resume()

    idle(Duration.ofMillis(DURATION_MS + 1_000L))

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun pause_whileLoadingNeverStartsTheNote() {
    player.play(first)

    player.pause()
    idle()

    assertEquals(PlayerState.Paused(first), player.state.value)
    assertEquals(State.PREPARED, players.single().state)
  }

  @Test
  fun resume_afterPausingWhileLoadingStartsTheNote() {
    player.play(first)
    player.pause()
    idle()

    player.resume()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  @Test
  fun resume_whileStillLoadingStartsTheNoteWhenReady() {
    player.play(first)
    player.pause()
    player.resume()

    assertEquals(State.PREPARING, players.single().state)

    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  @Test
  fun pause_whenNothingPlaysDoesNothing() {
    player.pause()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(emptyList<ShadowMediaPlayer>(), players)
  }

  @Test
  fun pause_whenAlreadyPausedDoesNothing() {
    player.play(first)
    idle()
    player.pause()

    player.pause()

    assertEquals(PlayerState.Paused(first), player.state.value)
    assertEquals(State.PAUSED, players.single().state)
  }

  @Test
  fun resume_whenNothingIsPausedDoesNothing() {
    player.resume()
    assertEquals(PlayerState.Idle, player.state.value)

    player.play(first)
    idle()
    player.resume()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  @Test
  fun stop_whilePausedGoesIdleAndFreesThePlayer() {
    player.play(first)
    idle()
    player.pause()

    player.stop()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun play_afterStoppingAPausedNoteStartsAgainFromTheBeginning() {
    player.play(first)
    idle(Duration.ofMillis(2_000))
    player.pause()
    player.stop()

    player.play(first)
    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(2, players.size)
    assertEquals(0, mediaPlayers[1].currentPosition)
  }

  @Test
  fun play_anotherNoteReplacesAPausedOne() {
    player.play(first)
    idle()
    player.pause()

    player.play(second)
    idle()

    assertEquals(PlayerState.Playing(second), player.state.value)
    assertEquals(State.END, players[0].state)
    assertEquals(State.STARTED, players[1].state)
  }

  @Test
  fun pause_thenAPlayerErrorFailsTheNote() {
    player.play(first)
    idle()
    player.pause()

    players.single().invokeErrorListener(1, 0)

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  @Test
  fun release_whilePausedStopsTheNote() {
    player.play(first)
    idle()
    player.pause()

    player.release()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  private companion object {
    const val DURATION_MS = 14_000
  }
}
