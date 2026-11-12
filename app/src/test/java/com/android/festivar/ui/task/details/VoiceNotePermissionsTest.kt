// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import com.android.festivar.model.task.Task
import com.android.festivar.model.voicenote.VoiceNote
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNotePermissionsTest {
  private val ownerId = "owner"
  private val ownerTask =
      Task(taskId = "task", eventId = "event", title = "Task", ownerId = ownerId)
  private val taskWithNote =
      ownerTask.copy(
          voiceNote =
              VoiceNote(
                  id = "note",
                  audioUrl = "https://example.com/note.m4a",
                  duration = Duration.ofSeconds(5),
                  createdAt = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC),
              ),
      )

  @Test
  fun canManageVoiceNote_isTrueOnlyForTheOwner() {
    assertTrue(state(task = ownerTask).canManageVoiceNote)
    assertFalse(state(task = ownerTask, currentUserId = "someone").canManageVoiceNote)
    assertFalse(state().canManageVoiceNote)
  }

  @Test
  fun canAddVoiceNote_requiresOwnerIdleTaskWithoutANote() {
    assertTrue(state(task = ownerTask, isLoading = false).canAddVoiceNote)
    assertFalse(state(task = taskWithNote, isLoading = false).canAddVoiceNote)
    assertFalse(state(task = ownerTask, isLoading = false, isUpdating = true).canAddVoiceNote)
    assertFalse(
        state(currentUserId = "someone", task = ownerTask, isLoading = false).canAddVoiceNote,
    )
    assertFalse(state(task = ownerTask).canAddVoiceNote)
  }

  @Test
  fun canDeleteVoiceNote_requiresOwnerIdleTaskWithANote() {
    assertTrue(state(task = taskWithNote, isLoading = false).canDeleteVoiceNote)
    assertFalse(state(task = ownerTask, isLoading = false).canDeleteVoiceNote)
    assertFalse(
        state(task = taskWithNote, isLoading = false, isUpdating = true).canDeleteVoiceNote,
    )
    assertFalse(
        state(currentUserId = "someone", task = taskWithNote, isLoading = false).canDeleteVoiceNote,
    )
    assertFalse(state(task = taskWithNote).canDeleteVoiceNote)
  }

  private fun state(
      currentUserId: String = ownerId,
      task: Task? = null,
      isLoading: Boolean = true,
      isUpdating: Boolean = false,
  ) =
      TaskDetailsUiState(
          currentUserId = currentUserId,
          task = task,
          isLoading = isLoading,
          isUpdating = isUpdating,
      )
}
