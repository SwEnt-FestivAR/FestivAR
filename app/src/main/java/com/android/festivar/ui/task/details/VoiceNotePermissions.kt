// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

/** Whether the current user owns the task and can manage its voice note. */
internal val TaskDetailsUiState.canManageVoiceNote: Boolean
  get() = task?.canManageVoiceNote(currentUserId) == true

/** Whether the owner can add a voice note while the screen is idle. */
internal val TaskDetailsUiState.canAddVoiceNote: Boolean
  get() = canManageVoiceNote && task?.voiceNote == null && !isUpdating && !isLoading

/** Whether the owner can delete a voice note while the screen is idle. */
internal val TaskDetailsUiState.canDeleteVoiceNote: Boolean
  get() = canManageVoiceNote && task?.voiceNote != null && !isUpdating && !isLoading
