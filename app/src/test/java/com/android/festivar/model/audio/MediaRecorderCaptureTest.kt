// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaRecorder

/**
 * The real [MediaRecorder] glue of the voice recorder, on the fake [MediaRecorder] of Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class MediaRecorderCaptureTest {
  @get:Rule val folder = TemporaryFolder()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  // Lazy because MediaRecorder(Context) does not exist on the old Android of one of the tests.
  private val mediaRecorder by lazy { MediaRecorder(context) }
  private val shadow by lazy { shadowOf(mediaRecorder) }
  private val capture by lazy { MediaRecorderCapture(mediaRecorder) }

  private fun startCapture(onError: () -> Unit = {}): File =
      folder.newFile("note.m4a").also { capture.start(it, onError) }

  private fun voiceNoteFiles(): List<File> =
      context.cacheDir.listFiles().orEmpty().filter {
        it.name.startsWith("voice_note_") && it.name.endsWith(".m4a")
      }

  private fun idleFor(duration: Duration) = shadowOf(Looper.getMainLooper()).idleFor(duration)

  @Test
  fun start_recordsAacInMpeg4FromTheMicrophoneIntoTheFile() {
    val file = startCapture()

    assertEquals(MediaRecorder.AudioSource.MIC, shadow.audioSource)
    assertEquals(MediaRecorder.OutputFormat.MPEG_4, shadow.outputFormat)
    assertEquals(MediaRecorder.AudioEncoder.AAC, shadow.audioEncoder)
    assertEquals(file.absolutePath, shadow.outputPath)
    assertEquals(ShadowMediaRecorder.STATE_RECORDING, shadow.state)
    // Without these, the default AAC settings of many devices sound muffled.
    assertEquals(44_100, shadow.audioSamplingRate)
    assertEquals(64_000, shadow.audioEncodingBitRate)
  }

  @Test
  fun stopEndsTheRecordingAndReleaseFreesTheRecorder() {
    startCapture()

    capture.stop()
    assertEquals(ShadowMediaRecorder.STATE_INITIAL, shadow.state)

    capture.release()
    assertEquals(ShadowMediaRecorder.STATE_RELEASED, shadow.state)
  }

  // The fake MediaRecorder of Robolectric has no pause or resume, so this uses a mock.
  @Test
  fun pauseAndResumeReachTheirOwnMethodOfTheRecorder() {
    val recorder = mockk<MediaRecorder>(relaxed = true)
    val mockedCapture = MediaRecorderCapture(recorder)

    mockedCapture.pause()
    verify(exactly = 1) { recorder.pause() }
    verify(exactly = 0) { recorder.resume() }

    mockedCapture.resume()
    verify(exactly = 1) { recorder.pause() }
    verify(exactly = 1) { recorder.resume() }
  }

  @Test
  fun anErrorOfTheRecorderIsReported() {
    var failed = false
    startCapture(onError = { failed = true })
    assertFalse(failed)

    shadow.errorListener.onError(mediaRecorder, MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN, 0)

    assertTrue(failed)
  }

  @Test
  @Config(sdk = [28])
  fun capture_canBeCreatedOnTheOldestSupportedAndroid() {
    // MediaRecorder(Context) only exists from Android 12, so using it here would crash.
    val oldCapture = MediaRecorderCapture(context)

    oldCapture.start(folder.newFile("old.m4a")) {}
    oldCapture.stop()
    oldCapture.release()
  }

  @Test
  fun androidVoiceRecorder_recordsIntoAnM4aFileOfTheCacheAndDeletesItOnCancel() {
    val recorder = AndroidVoiceRecorder(context)

    recorder.start()

    assertEquals(1, voiceNoteFiles().size)

    recorder.cancel()

    assertTrue(voiceNoteFiles().isEmpty())
  }

  @Test
  fun androidVoiceRecorder_timesTheRecordingOnTheMainThread() {
    val recorder = AndroidVoiceRecorder(context)
    recorder.start()
    idleFor(Duration.ZERO)

    idleFor(Duration.ofMillis(AndroidVoiceRecorder.TICK_INTERVAL_MILLIS))

    assertEquals(
        RecorderState.Recording(Duration.ofMillis(AndroidVoiceRecorder.TICK_INTERVAL_MILLIS)),
        recorder.state.value,
    )
    recorder.cancel()
  }
}
