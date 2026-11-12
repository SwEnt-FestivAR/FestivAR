// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceNoteServicesTest {
  private val recorder = FakeVoiceRecorder()
  private val storage = FakeAudioRepository()
  private val player = FakeVoicePlayer()
  private val services = VoiceNoteServices(recorder, storage, player)

  @Test
  fun deleteAudioQuietly_ignoresStorageExceptions() = runTest {
    storage.deleteFailure = IllegalStateException("storage unavailable")

    services.deleteAudioQuietly("https://example.com/note.m4a")

    assertEquals(listOf("https://example.com/note.m4a"), storage.deletions)
  }

  @Test(expected = CancellationException::class)
  fun deleteAudioQuietly_rethrowsCancellationExceptions() = runTest {
    storage.deleteFailure = CancellationException("cancelled")

    services.deleteAudioQuietly("https://example.com/note.m4a")
  }
}
