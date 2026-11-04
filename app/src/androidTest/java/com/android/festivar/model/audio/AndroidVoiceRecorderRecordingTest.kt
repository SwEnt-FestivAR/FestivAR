// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.Manifest
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The recorder on a real [android.media.MediaRecorder]. Robolectric's one writes nothing, so this
 * is the only check that a real capture produces a playable file.
 */
@RunWith(AndroidJUnit4::class)
class AndroidVoiceRecorderRecordingTest {
  @get:Rule
  val microphone: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val recorder = AndroidVoiceRecorder(instrumentation.targetContext)
  private var audio: Audio? = null

  @After
  fun tearDown() {
    instrumentation.runOnMainSync { recorder.cancel() }
    audio?.file?.delete()
  }

  private fun playableDurationMillis(audio: Audio): Long {
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(audio.file.absolutePath)
      return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
    } finally {
      retriever.release()
    }
  }

  @Test
  fun aRealRecordingIsAPlayableFile() {
    instrumentation.runOnMainSync { recorder.start() }
    Thread.sleep(RECORDING_MILLIS)
    instrumentation.runOnMainSync { audio = recorder.stop() }

    val recorded = checkNotNull(audio)
    assertTrue(recorded.file.length() > 0)
    assertTrue(playableDurationMillis(recorded) > 0)
  }

  @Test
  fun aPauseLeavesItsGapOutOfTheRealFile() {
    instrumentation.runOnMainSync { recorder.start() }
    Thread.sleep(RECORDING_MILLIS)
    instrumentation.runOnMainSync { recorder.pause() }
    Thread.sleep(RECORDING_MILLIS)
    instrumentation.runOnMainSync { recorder.resume() }
    Thread.sleep(RECORDING_MILLIS)
    instrumentation.runOnMainSync { audio = recorder.stop() }

    val recorded = checkNotNull(audio)
    val fileMillis = playableDurationMillis(recorded)
    // About 4 s recorded in the 6 s it took: longer than one run, and without the pause.
    assertTrue("The file lasts $fileMillis ms", fileMillis in 2_500L..5_000L)
  }

  private companion object {
    const val RECORDING_MILLIS = 2_000L
  }
}
