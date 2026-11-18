// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Looper
import android.util.Log
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowMediaPlayer.State
import org.robolectric.shadows.util.DataSource

/**
 * Robolectric's fake [MediaPlayer] that also counts its [release] calls. The state of the fake
 * cannot tell whether the player freed it: a test that puts the fake in its released state itself
 * would pass either way.
 */
@Implements(MediaPlayer::class)
class ReleaseCountingShadowMediaPlayer : ShadowMediaPlayer() {
  var releaseCount = 0
    private set

  @Implementation
  protected override fun _release() {
    releaseCount++
    super._release()
  }
}

/**
 * [AndroidVoicePlayer] on Robolectric's fake [android.media.MediaPlayer], which reports prepared,
 * completed and error like a real one. The tests look at the fake of each note through [players].
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ReleaseCountingShadowMediaPlayer::class])
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

  private fun ShadowMediaPlayer.releaseCount() =
      (this as ReleaseCountingShadowMediaPlayer).releaseCount

  private fun lastLoggedError() =
      ShadowLog.getLogs().last { it.tag == "AndroidVoicePlayer" && it.type == Log.ERROR }.msg

  private fun reportError() =
      players
          .single()
          .invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO)

  private fun idle(duration: Duration = Duration.ZERO) =
      shadowOf(Looper.getMainLooper()).idleFor(duration)

  /** Breaks if a new player reports a state other than Idle. */
  @Test
  fun startsIdle() {
    assertEquals(PlayerState.Idle, player.state.value)
  }

  /** Breaks if the note is not Playing while loading, or never starts when prepared. */
  @Test
  fun play_reportsPlayingWhileLoadingThenStartsWhenPrepared() {
    player.play(first)

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.PREPARING, players.single().state)

    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  /** Breaks if the state stays Playing or the player is kept after the note ends. */
  @Test
  fun play_goesIdleAndFreesThePlayerWhenTheNoteEnds() {
    player.play(first)
    idle(Duration.ofMillis(DURATION_MS + 1_000L))

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if replaying a completed note keeps its old player or position. */
  @Test
  fun play_afterTheNoteEndsStartsAgainFromTheBeginning() {
    player.play(first)
    idle(Duration.ofMillis(DURATION_MS + 1_000L))

    player.play(first)
    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(2, players.size)
    assertEquals(State.STARTED, players[1].state)
    assertEquals(0, mediaPlayers[1].currentPosition)
  }

  /** Breaks if a player error does not give Failed and free the player. */
  @Test
  fun play_failsAndFreesThePlayerWhenThePlayerReportsAnError() {
    player.play(first)
    idle()

    reportError()

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if an IOException while loading is not turned into Failed, with a loading reason. */
  @Test
  fun play_failsWhenTheNoteCannotBeLoaded() {
    ShadowMediaPlayer.addException(DataSource.toDataSource(context, first.toUri()), IOException())

    player.play(first)

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(State.END, players.single().state)
    assertEquals("Cannot load the voice note", lastLoggedError())
  }

  /** Breaks if a URL the player cannot use crashes instead of giving Failed. */
  @Test
  fun play_failsWhenThePlayerCannotUseTheUrl() {
    // The fake throws an IllegalArgumentException for a source it knows nothing about.
    val unknown = "https://example.com/unknown.m4a"

    player.play(unknown)

    assertEquals(PlayerState.Failed(unknown), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if a second play leaves the player of the first note running. */
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

  /** Breaks if a note cannot be played again after a failure. */
  @Test
  fun play_afterAFailureStartsAgain() {
    player.play(first)
    idle()
    reportError()

    player.play(first)
    idle()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players[1].state)
  }

  /** Breaks if stop leaves the state not Idle or the player not freed. */
  @Test
  fun stop_goesIdleAndFreesThePlayer() {
    player.play(first)
    idle()

    player.stop()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if a note stopped while loading still starts when it is ready. */
  @Test
  fun stop_whileLoadingNeverStartsTheNote() {
    player.play(first)

    player.stop()
    idle()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if stop with nothing playing changes the state or creates a player. */
  @Test
  fun stop_whenNothingPlaysDoesNothing() {
    player.stop()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(emptyList<ShadowMediaPlayer>(), players)
  }

  /** Breaks if release leaves the note playing or the player not freed. */
  @Test
  fun release_stopsTheNote() {
    player.play(first)
    idle()

    player.release()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if pause frees the player or does not report Paused. */
  @Test
  fun pause_keepsThePlayerAndReportsPaused() {
    player.play(first)
    idle()

    player.pause()

    assertEquals(PlayerState.Paused(first), player.state.value)
    assertEquals(State.PAUSED, players.single().state)
  }

  /** Breaks if resume loads the note again or restarts it from the beginning. */
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

  /** Breaks if a resumed note does not end with Idle and a freed player. */
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

  /** Breaks if a note paused while loading starts as soon as it is ready. */
  @Test
  fun pause_whileLoadingNeverStartsTheNote() {
    player.play(first)

    player.pause()
    idle()

    assertEquals(PlayerState.Paused(first), player.state.value)
    assertEquals(State.PREPARED, players.single().state)
  }

  /** Breaks if resume after a pause during loading does not start the note. */
  @Test
  fun resume_afterPausingWhileLoadingStartsTheNote() {
    player.play(first)
    player.pause()
    idle()

    player.resume()

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  /** Breaks if resume during loading starts the note too early, or never. */
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

  /** Breaks if pause with nothing playing changes the state or creates a player. */
  @Test
  fun pause_whenNothingPlaysDoesNothing() {
    player.pause()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(emptyList<ShadowMediaPlayer>(), players)
  }

  /** Breaks if a second pause changes the state or the player. */
  @Test
  fun pause_whenAlreadyPausedDoesNothing() {
    player.play(first)
    idle()
    player.pause()

    player.pause()

    assertEquals(PlayerState.Paused(first), player.state.value)
    assertEquals(State.PAUSED, players.single().state)
  }

  /** Breaks if resume changes the state when nothing is paused. */
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

  /** Breaks if stop on a paused note leaves it Paused or keeps its player. */
  @Test
  fun stop_whilePausedGoesIdleAndFreesThePlayer() {
    player.play(first)
    idle()
    player.pause()

    player.stop()

    assertEquals(PlayerState.Idle, player.state.value)
    assertEquals(State.END, players.single().state)
  }

  /** Breaks if a note stopped while paused restarts from its old position. */
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

  /** Breaks if playing another note keeps the player of the paused note. */
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

  /** Breaks if a refused pause leaves the note Playing, or is logged as another failure. */
  @Test
  fun pause_failsAndFreesThePlayerWhenThePlayerRefusesIt() {
    player.play(first)
    idle()
    // A MediaPlayer that was already released throws IllegalStateException on pause().
    players.single().setInvalidStateBehavior(ShadowMediaPlayer.InvalidStateBehavior.EMULATE)
    players.single().setState(State.END)

    player.pause()

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(1, players.single().releaseCount())
    assertEquals("The player refused to pause the voice note", lastLoggedError())
  }

  /** Breaks if a refused start leaves the note Paused, or is logged as another failure. */
  @Test
  fun resume_failsAndFreesThePlayerWhenThePlayerRefusesIt() {
    player.play(first)
    idle()
    player.pause()
    // A MediaPlayer that was already released throws IllegalStateException on start().
    players.single().setInvalidStateBehavior(ShadowMediaPlayer.InvalidStateBehavior.EMULATE)
    players.single().setState(State.END)

    player.resume()

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(1, players.single().releaseCount())
    assertEquals("The player refused to resume the voice note", lastLoggedError())
  }

  /** Breaks if a player error while paused leaves the state Paused. */
  @Test
  fun pause_thenAPlayerErrorFailsTheNote() {
    player.play(first)
    idle()
    player.pause()

    reportError()

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(State.END, players.single().state)
  }

  private companion object {
    const val DURATION_MS = 14_000
  }
}
