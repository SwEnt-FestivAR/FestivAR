// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import com.android.festivar.model.audio.Audio
import com.android.festivar.model.audio.PendingRecording
import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingStageTest {
  @Test
  fun pending_returnsWaitingAndSendingRecordings_onlyForHeldStages() {
    val duration = Duration.ofSeconds(7)
    val audio = Audio(File("recording.m4a"), duration)

    assertEquals(PendingRecording.Waiting(duration), RecordingStage.Waiting(audio).pending)
    assertEquals(PendingRecording.Sending(duration), RecordingStage.Sending(audio).pending)
    assertEquals(null, RecordingStage.Idle.pending)
    assertEquals(null, RecordingStage.Failed.pending)
    assertEquals(null, RecordingStage.SendFailed(audio).pending)
  }
}
