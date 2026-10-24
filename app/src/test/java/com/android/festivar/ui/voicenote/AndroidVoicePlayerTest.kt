// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import com.android.festivar.model.voicenote.PlayerState
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The player driven by fake playbacks, so no real [android.media.MediaPlayer] is needed. */
class AndroidVoicePlayerTest {
  private class FakePlayback(
      private val failToPrepare: Boolean = false,
      private val crashOnPrepare: Boolean = false,
      private val failToRelease: Boolean = false,
  ) : AudioPlayback {
    var url: String? = null
    var started = false
    var released = false
    private var onPrepared: () -> Unit = {}
    private var onCompleted: () -> Unit = {}
    private var onError: () -> Unit = {}

    override fun prepareAsync(
        url: String,
        onPrepared: () -> Unit,
        onCompleted: () -> Unit,
        onError: () -> Unit,
    ) {
      if (failToPrepare) throw IOException("no network")
      if (crashOnPrepare) throw IllegalStateException("bad state")
      this.url = url
      this.onPrepared = onPrepared
      this.onCompleted = onCompleted
      this.onError = onError
    }

    override fun start() {
      started = true
    }

    override fun release() {
      released = true
      if (failToRelease) throw IllegalStateException("already released")
    }

    fun prepared() = onPrepared()

    fun completed() = onCompleted()

    fun error() = onError()
  }

  private val playbacks = mutableListOf<FakePlayback>()
  private var failNext = false
  private var crashNext = false
  private var releaseFails = false
  private var createFails = false
  private lateinit var player: AndroidVoicePlayer

  @Before
  fun setUp() {
    player =
        AndroidVoicePlayer(
            AudioPlaybackFactory {
              if (createFails) throw IllegalStateException("no player")
              FakePlayback(failNext, crashNext, releaseFails).also { playbacks += it }
            }
        )
  }

  @Test
  fun startsIdle() {
    assertEquals(PlayerState.Idle, player.state.value)
  }

  @Test
  fun playLoadsTheUrlThenStartsWhenPrepared() {
    player.play("u1")
    assertEquals(PlayerState.Playing("u1"), player.state.value)
    assertEquals("u1", playbacks.single().url)
    assertFalse(playbacks.single().started)
    playbacks.single().prepared()
    assertTrue(playbacks.single().started)
  }

  @Test
  fun finishingGoesBackToIdleAndReleases() {
    player.play("u1")
    playbacks.single().prepared()
    playbacks.single().completed()
    assertEquals(PlayerState.Idle, player.state.value)
    assertTrue(playbacks.single().released)
  }

  @Test
  fun anErrorFails() {
    player.play("u1")
    playbacks.single().error()
    assertEquals(PlayerState.Failed("u1"), player.state.value)
    assertTrue(playbacks.single().released)
  }

  @Test
  fun aLoadThatThrowsFails() {
    failNext = true
    player.play("u1")
    assertEquals(PlayerState.Failed("u1"), player.state.value)
    assertTrue(playbacks.single().released)
  }

  @Test
  fun aLoadThatCrashesFails() {
    crashNext = true
    player.play("u1")
    assertEquals(PlayerState.Failed("u1"), player.state.value)
    assertTrue(playbacks.single().released)
  }

  @Test
  fun aPlayerThatCannotBeCreatedFails() {
    createFails = true
    player.play("u1")
    assertEquals(PlayerState.Failed("u1"), player.state.value)
    assertTrue(playbacks.isEmpty())
  }

  @Test
  fun aReleaseThatThrowsIsIgnored() {
    releaseFails = true
    player.play("u1")
    player.stop()
    assertEquals(PlayerState.Idle, player.state.value)
  }

  @Test
  fun stopReleasesAndGoesIdle() {
    player.play("u1")
    player.stop()
    assertEquals(PlayerState.Idle, player.state.value)
    assertTrue(playbacks.single().released)
  }

  @Test
  fun stopWhenIdleDoesNothing() {
    player.stop()
    assertEquals(PlayerState.Idle, player.state.value)
    assertTrue(playbacks.isEmpty())
  }

  @Test
  fun playingAnotherNoteReplacesTheFirst() {
    player.play("u1")
    player.play("u2")
    assertEquals(PlayerState.Playing("u2"), player.state.value)
    assertTrue(playbacks[0].released)
    assertFalse(playbacks[1].released)
  }

  @Test
  fun callbacksOfAReplacedPlaybackAreIgnored() {
    player.play("u1")
    player.play("u2")
    playbacks[0].error()
    playbacks[0].prepared()
    assertEquals(PlayerState.Playing("u2"), player.state.value)
    assertFalse(playbacks[0].started)
  }

  @Test
  fun callbacksAfterStopAreIgnored() {
    player.play("u1")
    player.stop()
    playbacks.single().error()
    assertEquals(PlayerState.Idle, player.state.value)
  }

  @Test
  fun releaseFreesAndIgnoresLaterPlays() {
    player.play("u1")
    player.release()
    assertTrue(playbacks.single().released)
    assertEquals(PlayerState.Idle, player.state.value)
    player.play("u2")
    assertEquals(1, playbacks.size)
    assertEquals(PlayerState.Idle, player.state.value)
  }
}
