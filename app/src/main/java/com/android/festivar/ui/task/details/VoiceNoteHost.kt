// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.task.Task
import kotlinx.coroutines.flow.StateFlow

/**
 * What the voice note logic needs from the Task details screen: the task it shows and the way to
 * save a change to it. Saving goes through the screen so that the voice note and the other actions
 * (take, give back, mark done) never save at the same time.
 */
internal interface VoiceNoteHost {
  /** The state of the screen: the task shown, and whether it is loading or saving. */
  val state: StateFlow<TaskDetailsUiState>

  /**
   * Reserves the screen for a save if [allowed] and a task is shown, and returns that task. Returns
   * `null`, reserving nothing, if the action must not run.
   */
  fun startSaving(allowed: (TaskDetailsUiState) -> Boolean): Task?

  /** Stores [updated] as the task of the screen. */
  suspend fun saveTask(updated: Task)

  /**
   * Ends the save started by [startSaving]. [updated] is the task to show now, or `null` if it did
   * not change. [failed] tells the screen to show its save error.
   */
  fun finishSaving(updated: Task?, failed: Boolean = false)
}
