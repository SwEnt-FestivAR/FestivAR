// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
          createCapture = { newCapture().also { captures += it } },
          createOutputFile = { newFile().also { files += it } },
          clock = { scope.testScheduler.currentTime },
          timerScope = scope,
      )

  private val minMillis = AndroidVoiceRecorder.MIN_DURATION.toMillis()
  private val maxMillis = AndroidVoiceRecorder.MAX_DURATION.toMillis()
  private val tickMillis = AndroidVoiceRecorder.TICK_INTERVAL_MILLIS

  private fun advance(millis: Long) {
    scope.advanceTimeBy(millis)
    scope.runCurrent()
  }

  private fun recordingOf(millis: Long) = RecorderState.Recording(Duration.ofMillis(millis))

  private fun pausedAt(millis: Long) =
      RecorderState.Recording(Duration.ofMillis(millis), isPaused = true)

  /** Asserts the state is [expected] and no timer is left to change it afterwards. */
  private fun assertStaysIn(expected: RecorderState) {
    advance(tickMillis * 10)
    assertEquals(expected, recorder.state.value)
  }

  /** Asserts the last recording is over in [finalState], with capture freed and file deleted. */
  private fun assertDropped(finalState: RecorderState = RecorderState.Idle) {
    assertTrue(captures.last().released)
    assertFalse(files.last().exists())
    assertStaysIn(finalState)
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
  fun start_recordsIntoANewFileFromZero() {
    recorder.start()

    assertTrue(files.single().exists())
    assertEquals(files.single(), captures.single().outputFile)
    assertEquals(recordingOf(0), recorder.state.value)
  }

  @Test
  fun start_afterAStopUsesAnotherFileAndKeepsTheFirst() {
    record()
    val first = recorder.stop()

    recorder.start()

    assertNotEquals(first.file, files[1])
    assertEquals(files[1], captures[1].outputFile)
    assertTrue(first.file.exists())
  }

  @Test
  fun start_afterAStopTimesTheRecordingFromItsOwnStart() {
    record()
    recorder.stop()
    advance(tickMillis)

    record()

    assertEquals(AndroidVoiceRecorder.MIN_DURATION, recorder.stop().duration)
  }

  @Test
  fun start_whileRunningThrowsAndLeavesTheRecordingIntact() {
    record()

    assertIllegal { recorder.start() }

    assertEquals(recordingOf(minMillis), recorder.state.value)
    assertEquals(1, captures.size)
    assertEquals(1, files.size)
    assertFalse(captures.single().stopped)
    assertSame(files.single(), recorder.stop().file)
  }

  @Test
  fun start_whilePausedThrowsAndLeavesTheRecordingIntact() {
    record()
    recorder.pause()

    assertIllegal { recorder.start() }

    assertEquals(pausedAt(minMillis), recorder.state.value)
    assertEquals(1, captures.size)
    assertEquals(1, files.size)
    assertFalse(captures.single().stopped)
    assertSame(files.single(), recorder.stop().file)
  }

  @Test
  fun start_failsWhenTheFileCannotBeCreated() {
    val cause = IOException("no space")
    newFile = { throw cause }

    assertFails(cause) { recorder.start() }

    assertTrue(captures.isEmpty())
    assertStaysIn(RecorderState.Idle)
    assertCanRecordAgain()
  }

  @Test
  fun start_failsAndDeletesTheFileWhenTheCaptureCannotBeCreated() {
    val cause = RuntimeException("no recorder")
    newCapture = { throw cause }

    assertFails(cause) { recorder.start() }

    assertFalse(files.single().exists())
    assertStaysIn(RecorderState.Idle)
    assertCanRecordAgain()
  }

  // Includes a busy microphone: its IllegalStateException must not look like a wrong-state call.
  @Test
  fun start_failsWhenTheCaptureCannotStart() {
    listOf(IOException("cannot write"), IllegalStateException("busy"), SecurityException("denied"))
        .forEach { cause ->
          newCapture = { FakeCapture(startFailure = cause) }
          assertFails(cause) { recorder.start() }
          assertDropped()
        }
    assertCanRecordAgain()
  }

  @Test
  fun start_reportsItsOwnFailureWhenReleaseAlsoThrows() {
    val cause = IOException("cannot write")
    newCapture = { FakeCapture(startFailure = cause, releaseFailure = IllegalStateException()) }

    assertFails(cause) { recorder.start() }

    assertDropped()
  }

  @Test
  fun start_afterACaptureErrorRecordsAgain() {
    record()
    captures.single().reportError()

    assertCanRecordAgain()
  }

  @Test
  fun stop_returnsTheFileWithTheDurationOfTheClockAndKeepsIt() {
    record(1_234)

    val audio = recorder.stop()

    assertSame(files.single(), audio.file)
    assertTrue(audio.file.exists())
    assertEquals(Duration.ofMillis(1_234), audio.duration)
    assertTrue(captures.single().stopped)
    assertTrue(captures.single().released)
    assertStaysIn(RecorderState.Idle)
  }

  @Test
  fun stop_keepsTheRecordingWhenReleaseThrows() {
    newCapture = { FakeCapture(releaseFailure = IllegalStateException()) }
    record()

    assertTrue(recorder.stop().file.exists())

    assertStaysIn(RecorderState.Idle)
  }

  // A zero duration must fail here, not leak the IllegalArgumentException of Audio.
  @Test
  fun stop_failsAndDropsARecordingShorterThanTheMinimum() {
    listOf(0, minMillis - 1).forEach { millis ->
      record(millis)
      assertFails { recorder.stop() }
      assertDropped()
    }
  }

  @Test
  fun stop_keepsARecordingOfExactlyTheMinimum() {
    record()

    assertEquals(AndroidVoiceRecorder.MIN_DURATION, recorder.stop().duration)
  }

  @Test
  fun stop_failsAndDropsTheRecordingWhenTheCaptureCannotStop() {
    val cause = RuntimeException("nothing captured")
    newCapture = { FakeCapture(stopFailure = cause) }
    record()

    assertFails(cause) { recorder.stop() }

    assertDropped()
    assertCanRecordAgain()
  }

  @Test
  fun stop_failsAndDropsAnEmptyFile() {
    newCapture = { FakeCapture(writesAudio = false) }
    record()

    assertFails { recorder.stop() }

    assertDropped()
  }

  @Test
  fun stop_leavesEveryPauseOutOfTheDurationUpToAStopWhilePaused() {
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
    assertStaysIn(RecorderState.Idle)
  }

  @Test
  fun stop_atTheLimitReturnsTheFile() {
    record(maxMillis)

    val audio = recorder.stop()

    assertSame(files.single(), audio.file)
    assertTrue(audio.file.exists())
    assertEquals(AndroidVoiceRecorder.MAX_DURATION, audio.duration)
    assertStaysIn(RecorderState.Idle)
  }

  @Test
  fun stop_whenIdleThrows() {
    assertIllegal { recorder.stop() }

    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  @Test
  fun stop_afterACaptureErrorThrowsAndStaysFailed() {
    record()
    captures.single().reportError()

    assertIllegal { recorder.stop() }

    assertEquals(RecorderState.Failed, recorder.state.value)
  }

  @Test
  fun cancel_whenIdleDoesNothing() {
    recorder.cancel()

    assertEquals(RecorderState.Idle, recorder.state.value)
    assertTrue(captures.isEmpty())
  }

  @Test
  fun cancel_dropsTheRecordingAndEndsInIdle() {
    record()

    recorder.cancel()

    assertTrue(captures.single().stopped)
    assertDropped()
    assertCanRecordAgain()
  }

  @Test
  fun cancel_whilePausedDropsTheRecordingAndEndsInIdle() {
    recorder.start()
    recorder.pause()

    recorder.cancel()

    assertTrue(captures.single().stopped)
    assertDropped()
  }

  @Test
  fun cancel_neverThrowsEvenIfTheCaptureDoes() {
    newCapture = {
      FakeCapture(stopFailure = RuntimeException(), releaseFailure = IllegalStateException())
    }
    recorder.start()

    recorder.cancel()

    assertDropped()
  }

  @Test
  fun captureError_whileRecordingDropsTheRecordingAndEndsInFailed() {
    record()

    captures.single().reportError()

    assertDropped(RecorderState.Failed)
  }

  @Test
  fun captureError_whilePausedDropsTheRecordingAndEndsInFailed() {
    recorder.start()
    recorder.pause()

    captures.single().reportError()

    assertDropped(RecorderState.Failed)
  }

  // The collector runs inside each change of state, so it also sees the ones that last no time.
  // Recording(0), which the error replaces at once, shows that it does.
  @Test
  fun captureError_goesFromRecordingToFailedWithoutPassingThroughIdle() {
    val seen = mutableListOf<RecorderState>()
    scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
      recorder.state.toList(seen)
    }

    recorder.start()
    captures.single().reportError()

    assertEquals(listOf(RecorderState.Idle, recordingOf(0), RecorderState.Failed), seen)
  }

  @Test
  fun captureError_afterAStopIsIgnored() {
    record()
    val audio = recorder.stop()

    captures.single().reportError()

    assertTrue(audio.file.exists())
    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  @Test
  fun captureError_ofThePreviousRecordingLeavesTheNewOneIntact() {
    record()
    recorder.stop()
    recorder.start()

    captures[0].reportError()

    assertTrue(files[1].exists())
    assertFalse(captures[1].released)
    assertEquals(recordingOf(0), recorder.state.value)
  }

  @Test
  fun pause_whenIdleThrows() {
    assertIllegal { recorder.pause() }

    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  @Test
  fun pause_whilePausedThrowsAndChangesNothing() {
    record(tickMillis)
    recorder.pause()

    assertIllegal { recorder.pause() }

    assertEquals(1, captures.single().pauses)
    assertEquals(pausedAt(tickMillis), recorder.state.value)
  }

  @Test
  fun pause_freezesTheTimer() {
    record(tickMillis * 2)

    recorder.pause()

    assertEquals(1, captures.single().pauses)
    advance(tickMillis * 10)
    assertEquals(pausedAt(tickMillis * 2), recorder.state.value)
  }

  @Test
  fun pause_failsAndDropsTheRecordingWithoutLeakingIllegalStateException() {
    val cause = IllegalStateException("wrong state")
    newCapture = { FakeCapture(pauseFailure = cause) }
    recorder.start()

    assertFails(cause) { recorder.pause() }

    assertTrue(captures.single().stopped)
    assertDropped()
  }

  @Test
  fun resume_whenIdleThrows() {
    assertIllegal { recorder.resume() }

    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  @Test
  fun resume_whileRunningThrowsAndChangesNothing() {
    record(tickMillis)

    assertIllegal { recorder.resume() }

    assertEquals(0, captures.single().resumes)
    assertEquals(recordingOf(tickMillis), recorder.state.value)
  }

  @Test
  fun resume_continuesTheTimerFromTheSameValue() {
    record(tickMillis * 2)
    recorder.pause()
    advance(tickMillis * 10)

    recorder.resume()

    assertEquals(1, captures.single().resumes)
    advance(tickMillis - 1)
    assertEquals(recordingOf(tickMillis * 2), recorder.state.value)
    advance(1)
    assertEquals(recordingOf(tickMillis * 3), recorder.state.value)
  }

  @Test
  fun resume_failsAndDropsTheRecordingWithoutLeakingIllegalStateException() {
    val cause = IllegalStateException("wrong state")
    newCapture = { FakeCapture(resumeFailure = cause) }
    recorder.start()
    recorder.pause()

    assertFails(cause) { recorder.resume() }

    assertTrue(captures.single().stopped)
    assertDropped()
    assertCanRecordAgain()
  }

  @Test
  fun resume_justBeforeTheLimitPassesItByLessThanATick() {
    val justBefore = maxMillis - tickMillis / 2
    record(justBefore)
    recorder.pause()
    recorder.resume()
    assertEquals(1, captures.single().resumes)

    advance(tickMillis)

    assertEquals(pausedAt(justBefore + tickMillis), recorder.state.value)
    assertEquals(Duration.ofMillis(justBefore + tickMillis), recorder.stop().duration)
  }

  @Test
  fun resume_atTheLimitThrowsAndLeavesTheRecordingIntact() {
    record(maxMillis)

    assertFails { recorder.resume() }

    assertEquals(pausedAt(maxMillis), recorder.state.value)
    assertEquals(0, captures.single().resumes)
    assertFalse(captures.single().stopped)
    assertFalse(captures.single().released)
    assertTrue(files.single().exists())
    assertEquals(AndroidVoiceRecorder.MAX_DURATION, recorder.stop().duration)
  }

  @Test
  fun timer_advancesOnEachTick() {
    record(tickMillis)
    assertEquals(recordingOf(tickMillis), recorder.state.value)

    advance(tickMillis - 1)
    assertEquals(recordingOf(tickMillis), recorder.state.value)

    advance(1)
    assertEquals(recordingOf(tickMillis * 2), recorder.state.value)
  }

  @Test
  fun timer_pausesTheCaptureAtTheLimit() {
    record(maxMillis - tickMillis)
    assertEquals(recordingOf(maxMillis - tickMillis), recorder.state.value)
    assertEquals(0, captures.single().pauses)

    advance(tickMillis)
    assertEquals(pausedAt(maxMillis), recorder.state.value)

    assertStaysIn(pausedAt(maxMillis))
    assertEquals(1, captures.single().pauses)
    assertFalse(captures.single().stopped)
  }

  @Test
  fun timer_dropsTheRecordingAndEndsInFailedWhenThePauseAtTheLimitFails() {
    newCapture = {
      FakeCapture(pauseFailure = IllegalStateException(), stopFailure = RuntimeException())
    }

    record(maxMillis)

    assertEquals(1, captures.single().pauses)
    assertTrue(captures.single().stopped)
    assertDropped(RecorderState.Failed)
  }

  @Test
  fun timer_keepsItsScopeAliveWhenThePauseAtTheLimitFails() {
    newCapture = { FakeCapture(pauseFailure = IllegalStateException()) }

    record(maxMillis)

    // An exception that escaped from the timer would have cancelled this scope.
    assertTrue(scope.isActive)
    assertCanRecordAgain()
  }
}
