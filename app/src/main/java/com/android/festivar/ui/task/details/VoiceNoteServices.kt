// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.util.Log
import com.android.festivar.model.audio.AudioRepository
import com.android.festivar.model.audio.VoicePlayer
import com.android.festivar.model.audio.VoiceRecorder
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

private const val LOG_TAG = "VoiceNoteServices"

/**
 * Everything the voice note of a task needs to record, keep and play it. The Task details screen
 * gets all of them or none: without them nobody can record or delete the note, and it cannot be
 * played.
 *
 * @property recorder records the voice note.
 * @property storage keeps the audio file of the voice note.
 * @property player plays the voice note.
 * @property newId creates the id of a new voice note.
 * @property now gives the time a voice note is recorded at.
 */
class VoiceNoteServices(
    val recorder: VoiceRecorder,
    val storage: AudioRepository,
    val player: VoicePlayer,
    val newId: () -> String = { UUID.randomUUID().toString() },
    val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
  /** Removes the audio file at [url] from the storage. A file that cannot be removed is logged. */
  internal suspend fun deleteAudioQuietly(url: String) {
    try {
      storage.delete(url)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(LOG_TAG, "Could not delete the audio file $url", e)
    }
  }
}

