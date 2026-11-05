// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The recorder on a [MediaRecorder] replaced by MockK, under Robolectric, whose own recorder cannot
 * fail on demand and writes no file. The clock and the timer are the real ones, on the looper time
 * of Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class AndroidVoiceRecorderTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val recorder by lazy { AndroidVoiceRecorder(context) }

  private val files = mutableListOf<File>()
  private val errorListeners = mutableListOf<MediaRecorder.OnErrorListener>()

  private var startFailure: Exception? = null
  private var stopFailure: RuntimeException? = null
  private var releaseFailure: RuntimeException? = null
  private var pauseFailure: RuntimeException? = null
  private var resumeFailure: RuntimeException? = null
  private var writesAudio = true
  private var pauses = 0
  private var resumes = 0
  private var stops = 0
  private var releases = 0

  private val minMillis = AndroidVoiceRecorder.MIN_DURATION.toMillis()
  private val tickMillis = AndroidVoiceRecorder.TICK_INTERVAL_MILLIS

  @Before
  fun setUp() {
    mockkConstructor(MediaRecorder::class)
    every { anyConstructed<MediaRecorder>().setOnErrorListener(capture(errorListeners)) } returns
        Unit
    every { anyConstructed<MediaRecorder>().setOutputFile(any<String>()) } answers
        {
          files += File(firstArg<String>())
        }
    every { anyConstructed<MediaRecorder>().prepare() } returns Unit
    every { anyConstructed<MediaRecorder>().start() } answers
        {
          startFailure?.let { throw it }
          if (writesAudio) files.last().writeText("audio")
        }
    every { anyConstructed<MediaRecorder>().pause() } answers
        {
          pauses++
          pauseFailure?.let { throw it }
        }
    every { anyConstructed<MediaRecorder>().resume() } answers
        {
          resumes++
          resumeFailure?.let { throw it }
        }
    every { anyConstructed<MediaRecorder>().stop() } answers
        {
          stops++
          stopFailure?.let { throw it }
        }
    every { anyConstructed<MediaRecorder>().release() } answers
        {
          releases++
          releaseFailure?.let { throw it }
        }
  }

  @After
  fun tearDown() {
    context.cacheDir.setWritable(true)
    files.forEach { it.delete() }
    unmockkAll()
  }

  private fun advance(millis: Long) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
  }

  private fun recordingOf(millis: Long) = RecorderState.Recording(Duration.ofMillis(millis))

  private fun pausedAt(millis: Long) =
      RecorderState.Recording(Duration.ofMillis(millis), isPaused = true)

  private fun reportError(index: Int = errorListeners.lastIndex) {
    errorListeners[index].onError(mockk(), MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN, 0)
  }

  /** Asserts the state is [expected] and no timer is left to change it afterwards. */
  private fun assertStaysIn(expected: RecorderState) {
    advance(tickMillis * 10)
    assertEquals(expected, recorder.state.value)
  }

  /** Asserts the last recording is over in [finalState], with recorder freed and file deleted. */
  private fun assertDropped(finalState: RecorderState = RecorderState.Idle) {
    assertEquals(errorListeners.size, releases)
    assertFalse(files.last().exists())
    assertStaysIn(finalState)
  }

  /** Asserts a failure left nothing behind: the next recording works from the start. */
  private fun assertCanRecordAgain() {
    startFailure = null
    stopFailure = null
    releaseFailure = null
    context.cacheDir.setWritable(true)
    recorder.start()
    assertEquals(recordingOf(0), recorder.state.value)
    advance(minMillis)
    assertEquals(files.last(), recorder.stop().file)
  }

  private fun record(millis: Long = minMillis) {
    recorder.start()
    advance(millis)
  }

  private fun assertFails(cause: Exception? = null, call: () -> Unit): VoiceRecorderException {
    val thrown = assertThrows(VoiceRecorderException::class.java) { call() }
    if (cause != null) assertSame(cause, thrown.cause)
    return thrown
  }

  private fun assertIllegal(call: () -> Unit) {
    assertThrows(IllegalStateException::class.java) { call() }
  }

  /** Breaks if a recording does not write to a new .m4a file of the cache or not from zero. */
  @Test
  fun start_recordsIntoANewFileOfTheCacheFromZero() {
    recorder.start()

    val file = files.single()
    assertTrue(file.exists())
    assertEquals(context.cacheDir, file.parentFile)
    assertTrue(file.name.startsWith("voice_note_") && file.name.endsWith(".m4a"))
    assertEquals(recordingOf(0), recorder.state.value)
  }

  /** Breaks if the source, format, file or quality of the recording changes. */
  @Test
  fun start_recordsAacInMpeg4FromTheMicrophone() {
    recorder.start()

    verify {
      anyConstructed<MediaRecorder>().setAudioSource(MediaRecorder.AudioSource.MIC)
      anyConstructed<MediaRecorder>().setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      anyConstructed<MediaRecorder>().setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      anyConstructed<MediaRecorder>().setAudioSamplingRate(44_100)
      anyConstructed<MediaRecorder>().setAudioEncodingBitRate(64_000)
      anyConstructed<MediaRecorder>().setOutputFile(files.single().absolutePath)
    }
  }

  /** Breaks if MediaRecorder(Context) is used below Android 12, where it crashes. */
  @Test
  @Config(sdk = [28])
  fun start_worksOnTheOldestSupportedAndroid() {
    recorder.start()

    assertEquals(recordingOf(0), recorder.state.value)
  }

  /** Breaks if a second recording reuses the file or the start time of the first one. */
  @Test
  fun start_afterAStopUsesAnotherFileAndTimesItFromItsOwnStart() {
    record()
    val first = recorder.stop()
    advance(tickMillis)

    record()

    assertNotEquals(first.file, files[1])
    assertTrue(first.file.exists())
    assertEquals(AndroidVoiceRecorder.MIN_DURATION, recorder.stop().duration)
  }

  /** Breaks if a second start replaces or harms the recording in progress. */
  @Test
  fun start_whileRunningThrowsAndLeavesTheRecordingIntact() {
    record()

    assertIllegal { recorder.start() }

    assertEquals(recordingOf(minMillis), recorder.state.value)
    assertEquals(1, files.size)
    assertEquals(0, stops)
    assertEquals(files.single(), recorder.stop().file)
  }

  /** Breaks if a file that cannot be created leaks an IOException or blocks later starts. */
  @Test
  fun start_failsWhenTheFileCannotBeCreated() {
    assertTrue(context.cacheDir.setWritable(false))

    val thrown = assertFails { recorder.start() }

    assertTrue(thrown.cause is IOException)
    assertTrue(files.isEmpty())
    assertStaysIn(RecorderState.Idle)
    assertCanRecordAgain()
  }

  /** Breaks if the IllegalStateException of a busy microphone looks like a wrong call. */
  @Test
  fun start_failsWhenTheRecorderCannotStart() {
    listOf(IOException("cannot write"), IllegalStateException("busy")).forEach { cause ->
      startFailure = cause
      assertFails(cause) { recorder.start() }
      assertDropped()
    }
    assertCanRecordAgain()
  }

  /** Breaks if a recording that failed by itself blocks the next start. */
  @Test
  fun start_afterARecorderErrorRecordsAgain() {
    record()
    reportError()

    assertCanRecordAgain()
  }

  /** Breaks if stop deletes the file, gets the duration wrong or keeps the recorder open. */
  @Test
  fun stop_returnsTheFileWithItsDurationAndKeepsIt() {
    record(1_234)

    val audio = recorder.stop()

    assertEquals(files.single(), audio.file)
    assertTrue(audio.file.exists())
    assertEquals(Duration.ofMillis(1_234), audio.duration)
    assertEquals(1, stops)
    assertEquals(1, releases)
    assertStaysIn(RecorderState.Idle)
  }

  /** Breaks if a zero duration leaks the IllegalArgumentException of Audio. */
  @Test
  fun stop_dropsARecordingShorterThanTheMinimumAndKeepsOneOfExactlyIt() {
    listOf(0, minMillis - 1).forEach { millis ->
      record(millis)
      assertFails { recorder.stop() }
      assertDropped()
    }

    record()

    assertEquals(AndroidVoiceRecorder.MIN_DURATION, recorder.stop().duration)
  }

  /** Breaks if a failed stop keeps the file or is reported as too short. */
  @Test
  fun stop_failsAndDropsTheRecordingWhenTheRecorderCannotStop() {
    val cause = RuntimeException("nothing captured")
    stopFailure = cause
    record()

    val thrown = assertFails(cause) { recorder.stop() }

    assertEquals("The recording could not be finished.", thrown.message)
    assertDropped()
    assertCanRecordAgain()
  }

  /** Breaks if a quick tap, which makes MediaRecorder.stop throw, is not called too short. */
  @Test
  fun stop_reportsAShortRecordingAsTooShortWhenTheRecorderCannotStop() {
    val cause = RuntimeException("no valid audio")
    stopFailure = cause
    record(minMillis - 1)

    val thrown = assertFails(cause) { recorder.stop() }

    assertEquals("The recording is too short.", thrown.message)
    assertDropped()
  }

  /** Breaks if a file with no data is returned as a recording. */
  @Test
  fun stop_failsAndDropsAnEmptyFile() {
    writesAudio = false
    record()

    assertFails { recorder.stop() }

    assertDropped()
  }

  /** Breaks if paused time counts in the duration, or a stop while paused loses the file. */
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
    assertEquals(1, stops)
    assertEquals(1, releases)
    assertStaysIn(RecorderState.Idle)
  }

  /** Breaks if a stop with no recording is not refused as a wrong call. */
  @Test
  fun stop_whenIdleThrows() {
    assertIllegal { recorder.stop() }

    assertEquals(RecorderState.Idle, recorder.state.value)
  }

  /** Breaks if a cancel with no recording throws or touches a recorder. */
  @Test
  fun cancel_whenIdleDoesNothing() {
    recorder.cancel()

    assertEquals(RecorderState.Idle, recorder.state.value)
    assertEquals(0, stops)
  }

  /** Breaks if a cancel leaves the file or the recorder behind, or blocks the next start. */
  @Test
  fun cancel_dropsTheRecordingAndEndsInIdle() {
    record()

    recorder.cancel()

    assertEquals(1, stops)
    assertDropped()
    assertCanRecordAgain()
  }

  /** Breaks if a failing recorder makes cancel throw, which onCleared cannot handle. */
  @Test
  fun cancel_neverThrowsEvenIfTheRecorderDoes() {
    stopFailure = RuntimeException()
    releaseFailure = IllegalStateException()
    recorder.start()

    recorder.cancel()

    assertDropped()
  }

  /** Breaks if a recorder error is not reported as Failed with everything freed. */
  @Test
  fun recorderError_whileRecordingDropsTheRecordingAndEndsInFailed() {
    record()

    reportError()

    assertDropped(RecorderState.Failed)
  }

  /** Breaks if a failed recording passes through Idle, which looks like a cancel. */
  @Test
  fun recorderError_goesFromRecordingToFailedWithoutPassingThroughIdle() {
    // The collector runs inside each change of state, so it also sees the ones that last no time.
    // Recording(0), which the error replaces at once, shows that it does.
    val seen = mutableListOf<RecorderState>()
    val collector = CoroutineScope(Dispatchers.Unconfined)
    collector.launch { recorder.state.toList(seen) }

    recorder.start()
    reportError()
    collector.cancel()

    assertEquals(listOf(RecorderState.Idle, recordingOf(0), RecorderState.Failed), seen)
  }

  /** Breaks if a late error of an old recorder drops a finished or newer recording. */
  @Test
  fun recorderError_ofAnEndedRecordingIsIgnored() {
    record()
    val audio = recorder.stop()

    reportError(0)

    assertTrue(audio.file.exists())
    assertEquals(RecorderState.Idle, recorder.state.value)

    recorder.start()

    reportError(0)

    assertTrue(files[1].exists())
    assertEquals(1, releases)
    assertEquals(recordingOf(0), recorder.state.value)
  }

  /** Breaks if a pause with nothing running reaches the recorder instead of being refused. */
  @Test
  fun pause_whenIdleOrAlreadyPausedThrowsAndChangesNothing() {
    assertIllegal { recorder.pause() }

    assertEquals(RecorderState.Idle, recorder.state.value)

    record(tickMillis)
    recorder.pause()

    assertIllegal { recorder.pause() }

    assertEquals(1, pauses)
    assertEquals(pausedAt(tickMillis), recorder.state.value)
  }

  /** Breaks if the timer runs during a pause or restarts from another value on resume. */
  @Test
  fun pause_freezesTheTimerAndResumeContinuesItFromTheSameValue() {
    record(tickMillis * 2)

    recorder.pause()

    assertEquals(1, pauses)
    advance(tickMillis * 10)
    assertEquals(pausedAt(tickMillis * 2), recorder.state.value)

    recorder.resume()

    assertEquals(1, resumes)
    advance(tickMillis - 1)
    assertEquals(recordingOf(tickMillis * 2), recorder.state.value)
    advance(1)
    assertEquals(recordingOf(tickMillis * 3), recorder.state.value)
  }

  /** Breaks if a failed pause leaks IllegalStateException or keeps the recording. */
  @Test
  fun pause_failsAndDropsTheRecordingWithoutLeakingIllegalStateException() {
    val cause = IllegalStateException("wrong state")
    pauseFailure = cause
    recorder.start()

    assertFails(cause) { recorder.pause() }

    assertEquals(1, stops)
    assertDropped()
  }

  /** Breaks if a resume with nothing paused reaches the recorder instead of being refused. */
  @Test
  fun resume_whenIdleOrRunningThrowsAndChangesNothing() {
    assertIllegal { recorder.resume() }

    assertEquals(RecorderState.Idle, recorder.state.value)

    record(tickMillis)

    assertIllegal { recorder.resume() }

    assertEquals(0, resumes)
    assertEquals(recordingOf(tickMillis), recorder.state.value)
  }

  /** Breaks if a failed resume leaks IllegalStateException or keeps the recording. */
  @Test
  fun resume_failsAndDropsTheRecordingWithoutLeakingIllegalStateException() {
    val cause = IllegalStateException("wrong state")
    resumeFailure = cause
    recorder.start()
    recorder.pause()

    assertFails(cause) { recorder.resume() }

    assertEquals(1, stops)
    assertDropped()
    assertCanRecordAgain()
  }

  /** Breaks if the timer does not update the state exactly once per tick. */
  @Test
  fun timer_advancesOnEachTick() {
    record(tickMillis)
    assertEquals(recordingOf(tickMillis), recorder.state.value)

    advance(tickMillis - 1)
    assertEquals(recordingOf(tickMillis), recorder.state.value)

    advance(1)
    assertEquals(recordingOf(tickMillis * 2), recorder.state.value)
  }
}
