// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.festivar.model.task.TasksRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the state of the Create task screen and saves the new task in [tasksRepository].
 *
 * @param eventId The event the created task belongs to.
 * @param tasksRepository Where the task is saved. Only the interface is known here, never Firebase.
 */
class CreateTaskViewModel(
    private val eventId: String,
    private val tasksRepository: TasksRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(CreateTaskUiState())
  val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

  fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }

  fun updateDescription(description: String) = _uiState.update {
    it.copy(description = description)
  }

  fun updateLocation(location: String) = _uiState.update { it.copy(location = location) }

  fun updateStartDate(date: LocalDate) = _uiState.update { it.copy(startDate = date) }

  fun updateStartTime(time: LocalTime) = _uiState.update { it.copy(startTime = time) }

  fun updateEndDate(date: LocalDate) = _uiState.update { it.copy(endDate = date) }

  fun updateEndTime(time: LocalTime) = _uiState.update { it.copy(endTime = time) }

  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  /** Saves the task if the form is valid. Does nothing while a save is running. */
  fun createTask() {
    val form = _uiState.value
    if (!form.canCreate) return
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      runCatching { tasksRepository.addTask(form.toTask(tasksRepository.getNewUid(), eventId)) }
          .onSuccess { _uiState.update { it.copy(isSaving = false, isCreated = true) } }
          .onFailure { error ->
            _uiState.update {
              it.copy(isSaving = false, errorMsg = error.message ?: "Unable to create the task.")
            }
          }
    }
  }

  companion object {
    fun factory(eventId: String, tasksRepository: TasksRepository): ViewModelProvider.Factory =
        viewModelFactory {
          initializer { CreateTaskViewModel(eventId, tasksRepository) }
        }
  }
}
