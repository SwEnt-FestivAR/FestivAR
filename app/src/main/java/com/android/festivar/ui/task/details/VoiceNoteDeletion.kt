// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_TAG = "VoiceNoteDeletion"

/**
 * The owner deleting the voice note: it is removed from the task, then its audio file is removed
 * from the storage. If the task cannot be saved the note stays. A file that cannot be removed is
 * only logged: the note is already gone.
 *
 * @param scope where the deletion runs.
 * @param host the task shown, and the way to save it.
 * @param currentUserId the user deleting, who must own the task.
 * @param services what keeps the audio.
 * @param stopPlaying called with the URL of the note once it is removed, to stop playing it.
 */
internal class VoiceNoteDeletion(
    private val scope: CoroutineScope,
    private val host: VoiceNoteHost,
    private val currentUserId: String,
    private val services: VoiceNoteServices,
    private val stopPlaying: (url: String) -> Unit,
) {
  /** Deletes the voice note. Does nothing unless the user can delete it. */
  fun delete() {
    val task = host.startSaving { it.canDeleteVoiceNote } ?: return
    scope.launch {
      try {
        val note = checkNotNull(task.voiceNote) { "canDeleteVoiceNote requires a note." }
        val updated = task.removeVoiceNote(currentUserId)
        host.saveTask(updated)
        stopPlaying(note.audioUrl)
        host.finishSaving(updated)
        withContext(NonCancellable) { services.deleteAudioQuietly(note.audioUrl) }
      } catch (e: CancellationException) {
        host.finishSaving(null)
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error deleting the voice note of task ${task.taskId}", e)
        host.finishSaving(null, failed = true)
      }
    }
  }
}
