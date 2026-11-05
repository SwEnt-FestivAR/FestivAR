// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The public constructor of the recorder, with its real parts, on the fakes of Robolectric. */
@RunWith(AndroidJUnit4::class)
class AndroidVoiceRecorderWiringTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val recorder = AndroidVoiceRecorder(context)

  @After
  fun tearDown() {
    recorder.cancel()
  }

  private fun voiceNoteFiles(): List<File> =
      context.cacheDir.listFiles().orEmpty().filter {
        it.name.startsWith("voice_note_") && it.name.endsWith(".m4a")
      }

  private fun idleFor(duration: Duration) = shadowOf(Looper.getMainLooper()).idleFor(duration)

  @Test
  fun start_recordsIntoAnM4aFileOfTheCacheThatCancelDeletes() {
    recorder.start()

    assertEquals(1, voiceNoteFiles().size)

    recorder.cancel()

    assertTrue(voiceNoteFiles().isEmpty())
  }

  @Test
  fun start_timesTheRecordingOnTheMainThread() {
    recorder.start()
    idleFor(Duration.ZERO)

    idleFor(Duration.ofMillis(AndroidVoiceRecorder.TICK_INTERVAL_MILLIS))

    assertEquals(
        RecorderState.Recording(Duration.ofMillis(AndroidVoiceRecorder.TICK_INTERVAL_MILLIS)),
        recorder.state.value,
    )
  }
}
