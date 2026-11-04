// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The recorder driven by fake captures and virtual time, so no real [android.media.MediaRecorder]
 * is needed. The clock of the recorder is the virtual time of the timer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidVoiceRecorderTest {
  private class FakeCapture(
      private val startFailure: Exception? = null,
      private val stopFailure: RuntimeException? = null,
      private val releaseFailure: RuntimeException? = null,
      private val writesAudio: Boolean = true,
      private val pauseFailure: RuntimeException? = null,
      private val resumeFailure: RuntimeException? = null,
  ) : AudioCapture {
    var outputFile: File? = null
    var pauses = 0
    var resumes = 0
    var stopped = false
    var released = false
    private var onError: () -> Unit = {}

    override fun start(outputFile: File, onError: () -> Unit) {
      startFailure?.let { throw it }
      this.outputFile = outputFile
      this.onError = onError
      if (writesAudio) outputFile.writeText("audio")
    }

    override fun pause() {
      pauses++
      pauseFailure?.let { throw it }
    }

    override fun resume() {
      resumes++
      resumeFailure?.let { throw it }
    }

    override fun stop() {
      stopped = true
      stopFailure?.let { throw it }
    }

    override fun release() {
      released = true
      releaseFailure?.let { throw it }
    }

    fun reportError() = onError()
  }

  @get:Rule val folder = TemporaryFolder()

  private val scope = TestScope()
  private val captures = mutableListOf<FakeCapture>()
  private val files = mutableListOf<File>()
  private var newCapture: () -> FakeCapture = { FakeCapture() }
  private var newFile: () -> File = { folder.newFile() }

  private val recorder =
      AndroidVoiceRecorder(
          factory = AudioCaptureFactory { newCapture().also { captures += it } },
          createOutputFile = { newFile().also { files += it } },
          clock = { scope.testScheduler.currentTime },
          timerScope = scope,
      )

  private val minMillis = AndroidVoiceRecorder.MIN_DURATION.toMillis()
  private val tickMillis = AndroidVoiceRecorder.TICK_INTERVAL_MILLIS

  private fun advance(millis: Long) {
    scope.advanceTimeBy(millis)
    scope.runCurrent()
  }

  private fun recordingOf(millis: Long) = RecorderState.Recording(Duration.ofMillis(millis))

  private fun pausedAt(millis: Long) =
      RecorderState.Recording(Duration.ofMillis(millis), isPaused = true)

  /** Asserts the state is [RecorderState.Idle] and no timer is left to change it afterwards. */
  private fun assertIdleForGood() {
    advance(tickMillis * 10)
    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  /** Asserts the last recording is over, with its capture freed and its file deleted. */
  private fun assertDropped() {
    assertTrue(captures.last().released)
    assertFalse(files.last().exists())
    assertIdleForGood()
  }

  /** Asserts a failure left nothing behind: the next recording works from the start. */
  private fun assertCanRecordAgain() {
    newCapture = { FakeCapture() }
    newFile = { folder.newFile() }
    recorder.start()
    assertEquals(recordingOf(0), recorder.state.value)
    advance(minMillis)
    assertSame(files.last(), recorder.stop().file)
  }

  private fun record(millis: Long = minMillis) {
    recorder.start()
    advance(millis)
  }

  private fun assertFails(cause: Exception? = null, call: () -> Unit) {
    val thrown = assertThrows(VoiceRecorderException::class.java) { call() }
    if (cause != null) assertSame(cause, thrown.cause)
  }

  private fun assertIllegal(call: () -> Unit) {
    assertThrows(IllegalStateException::class.java) { call() }
  }

  @Test
  fun eachRecordingGoesIntoItsOwnNewFileAndIsTimedFromItsOwnStart() {
    recorder.start()
    assertTrue(files.single().exists())
    assertEquals(files.single(), captures.single().outputFile)
    assertEquals(recordingOf(0), recorder.state.value)
    advance(minMillis)
    val first = recorder.stop()

    advance(tickMillis)
    record()
    val second = recorder.stop()

    assertNotEquals(first.file, second.file)
    assertTrue(first.file.exists())
    assertEquals(second.file, captures[1].outputFile)
    assertEquals(AndroidVoiceRecorder.MIN_DURATION, second.duration)
  }

  @Test
  fun startWhileRunningOrPausedThrowsAndLeavesTheRecordingIntact() {
    record()
    assertIllegal { recorder.start() }
    assertEquals(recordingOf(minMillis), recorder.state.value)

    recorder.pause()
    assertIllegal { recorder.start() }
    assertEquals(pausedAt(minMillis), recorder.state.value)

    assertEquals(1, captures.size)
    assertEquals(1, files.size)
    assertFalse(captures.single().stopped)
    assertSame(files.single(), recorder.stop().file)
  }

  @Test
  fun aFileThatCannotBeCreatedFailsTheStart() {
    val cause = IOException("no space")
    newFile = { throw cause }

    assertFails(cause) { recorder.start() }

    assertTrue(captures.isEmpty())
    assertIdleForGood()
    assertCanRecordAgain()
  }

  @Test
  fun aRecorderThatCannotBeCreatedFailsTheStartAndDeletesTheFile() {
    val cause = RuntimeException("no recorder")
    newCapture = { throw cause }

    assertFails(cause) { recorder.start() }

    assertFalse(files.single().exists())
    assertIdleForGood()
    assertCanRecordAgain()
  }

  // Includes a busy microphone: its IllegalStateException must not look like a wrong-state call.
  @Test
  fun aCaptureThatCannotStartFailsTheStart() {
    listOf(IOException("cannot write"), IllegalStateException("busy"), SecurityException("denied"))
        .forEach { cause ->
          newCapture = { FakeCapture(startFailure = cause) }
          assertFails(cause) { recorder.start() }
          assertDropped()
        }
    assertCanRecordAgain()
  }

  @Test
  fun aReleaseThatThrowsLosesNeitherTheRecordingNorTheFailureOfTheStart() {
    val cause = IOException("cannot write")
    newCapture = { FakeCapture(startFailure = cause, releaseFailure = IllegalStateException()) }
    assertFails(cause) { recorder.start() }
    assertDropped()

    newCapture = { FakeCapture(releaseFailure = IllegalStateException()) }
    record()
    assertTrue(recorder.stop().file.exists())
    assertIdleForGood()
  }

  @Test
  fun stopReturnsTheFileWithTheDurationOfTheClockAndKeepsIt() {
    record(1_234)

    val audio = recorder.stop()

    assertSame(files.single(), audio.file)
    assertTrue(audio.file.exists())
    assertEquals(Duration.ofMillis(1_234), audio.duration)
    assertTrue(captures.single().stopped)
    assertTrue(captures.single().released)
    assertIdleForGood()
  }

  // A zero duration must fail here, not leak the IllegalArgumentException of Audio.
  @Test
  fun onlyARecordingOfAtLeastTheMinimumDurationIsKept() {
    listOf(0, minMillis - 1).forEach { millis ->
      record(millis)
      assertFails { recorder.stop() }
      assertDropped()
    }
    record()
    assertEquals(AndroidVoiceRecorder.MIN_DURATION, recorder.stop().duration)
  }

  @Test
  fun aCaptureStopThatThrowsFailsTheStop() {
    val cause = RuntimeException("nothing captured")
    newCapture = { FakeCapture(stopFailure = cause) }
    record()

    assertFails(cause) { recorder.stop() }

    assertDropped()
    assertCanRecordAgain()
  }

  @Test
  fun anEmptyOrMissingFileFailsTheStop() {
    newCapture = { FakeCapture(writesAudio = false) }
    record()
    assertFails { recorder.stop() }
    assertDropped()

    newCapture = { FakeCapture() }
    record()
    files.last().delete()
    assertFails { recorder.stop() }
    assertDropped()
  }

  @Test
  fun cancelDropsTheRecordingAndDoesNothingWhenIdle() {
    recorder.cancel()
    assertEquals(RecorderState.Idle, recorder.state.value)
    assertTrue(captures.isEmpty())

    record()
    recorder.cancel()
    assertTrue(captures.single().stopped)
    assertDropped()
    assertCanRecordAgain()
  }

  @Test
  fun cancelNeverThrowsEvenIfTheCaptureDoes() {
    newCapture = {
      FakeCapture(stopFailure = RuntimeException(), releaseFailure = IllegalStateException())
    }
    recorder.start()

    recorder.cancel()

    assertDropped()
  }

  @Test
  fun anErrorWhileRecordingDropsTheRecording() {
    record()

    captures.single().reportError()

    assertDropped()
    assertIllegal { recorder.stop() }
    assertCanRecordAgain()
  }

  @Test
  fun anErrorOfAnEndedCaptureIsIgnored() {
    record()
    val audio = recorder.stop()
    captures[0].reportError()
    assertTrue(audio.file.exists())
    assertEquals(RecorderState.Idle, recorder.state.value)

    recorder.start()
    captures[0].reportError()
    assertTrue(files[1].exists())
    assertFalse(captures[1].released)
    assertEquals(recordingOf(0), recorder.state.value)
  }

  @Test
  fun cancelOrAnErrorWhilePausedDropsTheRecording() {
    recorder.start()
    recorder.pause()
    recorder.cancel()
    assertTrue(captures.last().stopped)
    assertDropped()

    recorder.start()
    recorder.pause()
    captures.last().reportError()
    assertDropped()
    assertIllegal { recorder.resume() }
  }

  @Test
  fun callsThatDoNotMatchTheStateThrowAndChangeNothing() {
    assertIllegal { recorder.stop() }
    assertIllegal { recorder.pause() }
    assertIllegal { recorder.resume() }
    assertEquals(RecorderState.Idle, recorder.state.value)

    record(tickMillis)
    assertIllegal { recorder.resume() }
    assertEquals(0, captures.single().resumes)
    assertEquals(recordingOf(tickMillis), recorder.state.value)

    recorder.pause()
    assertIllegal { recorder.pause() }
    assertEquals(1, captures.single().pauses)
    assertEquals(pausedAt(tickMillis), recorder.state.value)
  }

  @Test
  fun theTimerAdvancesOnEachTick() {
    record(tickMillis)
    assertEquals(recordingOf(tickMillis), recorder.state.value)
    advance(tickMillis - 1)
    assertEquals(recordingOf(tickMillis), recorder.state.value)
    advance(1)
    assertEquals(recordingOf(tickMillis * 2), recorder.state.value)
  }

  @Test
  fun pauseStopsTheTimerAndResumeContinuesItFromTheSameValue() {
    record(tickMillis * 2)

    recorder.pause()
    assertEquals(1, captures.single().pauses)
    advance(tickMillis * 10)
    assertEquals(pausedAt(tickMillis * 2), recorder.state.value)

    recorder.resume()
    assertEquals(1, captures.single().resumes)
    advance(tickMillis - 1)
    assertEquals(recordingOf(tickMillis * 2), recorder.state.value)
    advance(1)
    assertEquals(recordingOf(tickMillis * 3), recorder.state.value)
  }

  @Test
  fun aPauseOrResumeThatFailsDropsTheRecordingWithoutLeakingIllegalStateException() {
    val cause = IllegalStateException("wrong state")
    newCapture = { FakeCapture(pauseFailure = cause) }
    recorder.start()
    assertFails(cause) { recorder.pause() }
    assertTrue(captures.last().stopped)
    assertDropped()

    newCapture = { FakeCapture(resumeFailure = cause) }
    recorder.start()
    recorder.pause()
    assertFails(cause) { recorder.resume() }
    assertTrue(captures.last().stopped)
    assertDropped()
    assertCanRecordAgain()
  }

  @Test
  fun theDurationLeavesOutEveryPauseUpToAStopWhilePaused() {
    record(3_000)
    recorder.pause()
    advance(10_000)
    recorder.resume()
    advance(2_000)
    recorder.pause()
    advance(5_000)

    val audio = recorder.stop()

    assertEquals(Duration.ofSeconds(5), audio.duration)
    assertTrue(audio.file.exists())
    assertTrue(captures.single().stopped)
    assertTrue(captures.single().released)
    assertIdleForGood()
  }

  @Test
  fun aPauseDoesNotCountTowardsTheMinimumDuration() {
    record(500)
    recorder.pause()
    advance(10_000)
    recorder.resume()

    assertFails { recorder.stop() }

    assertDropped()
  }
}
