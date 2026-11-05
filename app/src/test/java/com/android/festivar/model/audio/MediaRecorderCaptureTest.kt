// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.media.MediaRecorder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import java.io.File
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
  fun start_reportsALaterErrorOfTheRecorder() {
    var failed = false
    startCapture(onError = { failed = true })
    assertFalse(failed)

    shadow.errorListener.onError(mediaRecorder, MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN, 0)

    assertTrue(failed)
  }

  @Test
  fun stop_endsTheRecording() {
    startCapture()

    capture.stop()

    assertEquals(ShadowMediaRecorder.STATE_INITIAL, shadow.state)
  }

  @Test
  fun release_freesTheRecorder() {
    startCapture()
    capture.stop()

    capture.release()

    assertEquals(ShadowMediaRecorder.STATE_RELEASED, shadow.state)
  }

  // The fake MediaRecorder of Robolectric has no pause or resume, so this uses a mock.
  @Test
  fun pause_reachesOnlyThePauseOfTheRecorder() {
    val recorder = mockk<MediaRecorder>(relaxed = true)

    MediaRecorderCapture(recorder).pause()

    verify(exactly = 1) { recorder.pause() }
    verify(exactly = 0) { recorder.resume() }
  }

  // The fake MediaRecorder of Robolectric has no pause or resume, so this uses a mock.
  @Test
  fun resume_reachesOnlyTheResumeOfTheRecorder() {
    val recorder = mockk<MediaRecorder>(relaxed = true)

    MediaRecorderCapture(recorder).resume()

    verify(exactly = 1) { recorder.resume() }
    verify(exactly = 0) { recorder.pause() }
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
}
