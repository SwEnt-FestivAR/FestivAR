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
import org.junit.Assert.assertTrue
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

  /** Makes the fake report a length of -1, like a real player does for a note it cannot measure. */
  var lengthUnknown = false

  @Implementation
  protected override fun getDuration(): Int = if (lengthUnknown) -1 else super.getDuration()

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

  /** Makes the fake take far longer than the timeout to load [first], like a stalled connection. */
  private fun stallLoading() =
      ShadowMediaPlayer.addMediaInfo(
          DataSource.toDataSource(context, first.toUri()),
          ShadowMediaPlayer.MediaInfo(DURATION_MS, STALLED_MS),
      )

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

  /** Breaks if the position does not move while the note plays. */
  @Test
  fun position_movesWhilePlaying() {
    player.play(first)
    idle()

    idle(Duration.ofMillis(2_000))
    val early = player.position.value
    idle(Duration.ofMillis(1_000))

    assertTrue(early >= Duration.ofMillis(1_900))
    assertTrue(player.position.value >= early + Duration.ofMillis(900))
  }

  /** Breaks if the position moves while paused, or is lost on pause and resume. */
  @Test
  fun position_isKeptWhilePausedAndContinuesOnResume() {
    player.play(first)
    idle(Duration.ofMillis(2_000))
    player.pause()
    val pausedAt = player.position.value

    idle(Duration.ofMillis(3_000))
    assertEquals(pausedAt, player.position.value)
    assertTrue(pausedAt >= Duration.ofMillis(1_900))

    player.resume()
    idle(Duration.ofMillis(1_000))

    assertTrue(player.position.value >= pausedAt + Duration.ofMillis(900))
  }

  /** Breaks if stop keeps the old position. */
  @Test
  fun position_isResetWhenTheNoteStops() {
    player.play(first)
    idle(Duration.ofMillis(2_000))

    player.stop()

    assertEquals(Duration.ZERO, player.position.value)
  }

  /** Breaks if the position stays at the end of a note that finished. */
  @Test
  fun position_isResetWhenTheNoteEnds() {
    player.play(first)
    idle(Duration.ofMillis(DURATION_MS + 1_000L))

    assertEquals(Duration.ZERO, player.position.value)
  }

  /** Breaks if the position is not updated within about 100 ms of the note starting. */
  @Test
  fun position_isUpdatedAboutEvery100Milliseconds() {
    player.play(first)
    idle()

    idle(Duration.ofMillis(100))
    val after100 = player.position.value
    idle(Duration.ofMillis(100))

    assertTrue(after100 > Duration.ZERO)
    assertTrue(player.position.value > after100)
  }

  /** Breaks if pausing does not read the exact position when it falls between two updates. */
  @Test
  fun pause_readsTheExactPositionBetweenTwoUpdates() {
    player.play(first)
    idle(Duration.ofMillis(2_025))

    player.pause()

    assertEquals(
        Duration.ofMillis(mediaPlayers.single().currentPosition.toLong()),
        player.position.value,
    )
    assertTrue(player.position.value > Duration.ofMillis(2_000))
  }

  /** Breaks if a note resumed while it is still loading never reports its position. */
  @Test
  fun position_movesWhenResumedWhileStillLoading() {
    player.play(first)
    player.pause()
    player.resume()

    idle(Duration.ofMillis(1_000))

    assertTrue(player.position.value >= Duration.ofMillis(500))
  }

  /** Breaks if a failed note keeps its position. */
  @Test
  fun position_isResetWhenTheNoteFails() {
    player.play(first)
    idle(Duration.ofMillis(2_000))

    players.single().invokeErrorListener(1, 0)

    assertEquals(Duration.ZERO, player.position.value)
  }

  /** Breaks if the position stops following the player after a seek. */
  @Test
  fun seekTo_thenThePositionKeepsFollowingThePlayer() {
    player.play(first)
    idle()
    player.seekTo(Duration.ofSeconds(10))

    idle(Duration.ofSeconds(1))

    assertTrue(player.position.value >= Duration.ofMillis(10_900))
  }

  /** Breaks if seekTo does not move the position of a playing note. */
  @Test
  fun seekTo_movesThePositionWhilePlaying() {
    player.play(first)
    idle()

    player.seekTo(Duration.ofSeconds(10))

    assertEquals(Duration.ofSeconds(10), player.position.value)
    assertEquals(PlayerState.Playing(first), player.state.value)
    // The fake MediaPlayer completes a seek when the looper runs.
    idle(Duration.ofMillis(100))
    assertTrue(mediaPlayers.single().currentPosition >= 10_000)
  }

  /** Breaks if seekTo on a paused note moves the position back or resumes the note. */
  @Test
  fun seekTo_movesThePositionWhilePausedAndStaysPaused() {
    player.play(first)
    idle(Duration.ofMillis(2_000))
    player.pause()

    player.seekTo(Duration.ofSeconds(5))
    idle(Duration.ofSeconds(1))

    assertEquals(Duration.ofSeconds(5), player.position.value)
    assertEquals(PlayerState.Paused(first), player.state.value)
  }

  /** Breaks if seekTo goes before the start or after the end of the note. */
  @Test
  fun seekTo_isClampedToTheNote() {
    player.play(first)
    idle()

    player.seekTo(Duration.ofSeconds(100))
    assertEquals(Duration.ofMillis(DURATION_MS.toLong()), player.position.value)

    player.seekTo(Duration.ofSeconds(-5))
    assertEquals(Duration.ZERO, player.position.value)
  }

  /** Breaks if seekTo on a note of unknown length crashes or moves the position. */
  @Test
  fun seekTo_doesNothingWhenTheLengthIsUnknown() {
    player.play(first)
    (players.single() as ReleaseCountingShadowMediaPlayer).lengthUnknown = true
    idle()

    player.seekTo(Duration.ofSeconds(5))

    assertEquals(Duration.ZERO, player.position.value)
    assertEquals(PlayerState.Playing(first), player.state.value)
  }

  /** Breaks if seekTo while loading or with nothing playing changes anything or crashes. */
  @Test
  fun seekTo_doesNothingWhileLoadingOrWhenNothingPlays() {
    player.seekTo(Duration.ofSeconds(5))
    assertEquals(Duration.ZERO, player.position.value)

    player.play(first)
    player.seekTo(Duration.ofSeconds(5))

    assertEquals(Duration.ZERO, player.position.value)
    assertEquals(PlayerState.Playing(first), player.state.value)
  }

  /** Breaks if a note that does not load within 10 seconds stays Playing or keeps its player. */
  @Test
  fun play_failsAndFreesThePlayerWhenTheNoteDoesNotLoadInTime() {
    stallLoading()
    player.play(first)

    idle(Duration.ofMillis(TIMEOUT_MS - 1))
    assertEquals(PlayerState.Playing(first), player.state.value)

    idle(Duration.ofMillis(1))

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(1, players.single().releaseCount())
    assertEquals("The voice note did not load in time", lastLoggedError())
  }

  /** Breaks if a note that is ready in time is failed by the timeout afterwards. */
  @Test
  fun play_aNoteReadyInTimeIsNotFailedByTheTimeout() {
    player.play(first)
    idle()

    idle(Duration.ofMillis(AFTER_TIMEOUT_MS))

    assertEquals(PlayerState.Playing(first), player.state.value)
    assertEquals(State.STARTED, players.single().state)
  }

  /** Breaks if a stopped note is later reported as Failed by its timeout. */
  @Test
  fun stop_whileLoadingCancelsTheTimeout() {
    stallLoading()
    player.play(first)
    player.stop()

    idle(Duration.ofMillis(AFTER_TIMEOUT_MS))

    assertEquals(PlayerState.Idle, player.state.value)
  }

  /** Breaks if a note paused while it is stuck loading stays Paused for ever. */
  @Test
  fun pause_whileLoadingStillFailsWhenTheNoteDoesNotLoadInTime() {
    stallLoading()
    player.play(first)
    player.pause()

    idle(Duration.ofMillis(TIMEOUT_MS))

    assertEquals(PlayerState.Failed(first), player.state.value)
    assertEquals(1, players.single().releaseCount())
  }

  private companion object {
    const val DURATION_MS = 14_000
    const val STALLED_MS = 1_000_000
    const val TIMEOUT_MS = 10_000L
    const val AFTER_TIMEOUT_MS = 11_000L
  }
}
