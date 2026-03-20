package com.yaish.naggy.presentation.addedittask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.domain.usecase.CreateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.RecurrencePattern

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    var onTriggerBackup: (() -> Unit)? = null
    private var editingTaskId: Long? = null

    fun loadTask(taskId: Long) {
        editingTaskId = taskId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                _uiState.value = _uiState.value.copy(
                    title = task.title,
                    description = task.description,
                    deadlineTimestamp = task.deadlineTimestamp,
                    reminderLeadTimeMinutes = task.reminderLeadTimeMinutes,
                    reminderTimeOfDay = task.reminderTimeOfDay,
                    priority = task.priority,
                    tags = task.tags,
                    recurrencePattern = task.recurrencePattern,
                    recurrenceRule = task.recurrenceRule,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Task not found"
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, titleError = null)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateDeadlineTimestamp(timestamp: Long) {
        _uiState.value = _uiState.value.copy(deadlineTimestamp = timestamp)
    }

    fun updateReminderLeadTime(minutes: Int) {
        _uiState.value = _uiState.value.copy(reminderLeadTimeMinutes = minutes)
    }

    fun updateReminderTimeOfDay(time: String) {
        _uiState.value = _uiState.value.copy(reminderTimeOfDay = time)
    }

    fun updatePriority(priority: Priority) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun addTag(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        if (tag.isNotBlank() && !currentTags.contains(tag)) {
            currentTags.add(tag)
            _uiState.value = _uiState.value.copy(tags = currentTags)
        }
    }

    fun removeTag(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        currentTags.remove(tag)
        _uiState.value = _uiState.value.copy(tags = currentTags)
    }

    fun updateRecurrence(pattern: RecurrencePattern, rule: String? = null) {
        _uiState.value = _uiState.value.copy(recurrencePattern = pattern, recurrenceRule = rule)
    }

    fun saveTask(onSuccess: (Task) -> Unit) {
        val state = _uiState.value

        // Validate
        if (state.title.isBlank()) {
            _uiState.value = state.copy(titleError = "Title is required")
            return
        }

        if (state.deadlineTimestamp == null) {
            _uiState.value = state.copy(errorMessage = "Please select a deadline")
            return
        }

        if (state.reminderTimeOfDay.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please select a reminder time")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val task = Task(
                id = editingTaskId ?: 0,
                title = state.title,
                description = state.description,
                deadlineTimestamp = state.deadlineTimestamp!!,
                reminderLeadTimeMinutes = state.reminderLeadTimeMinutes,
                reminderTimeOfDay = state.reminderTimeOfDay,
                priority = state.priority,
                tags = state.tags,
                recurrencePattern = state.recurrencePattern,
                recurrenceRule = state.recurrenceRule
            )

            val result = if (editingTaskId != null) {
                // Update existing task
                taskRepository.updateTask(task)
                Result.success(Unit)
            } else {
                // Create new task
                createTaskUseCase(task)
            }

            result.fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false)
                    // onTriggerBackup?.invoke() // Removed to prevent annoying backup toasts on every save
                    onSuccess(task)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to save task"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val deadlineTimestamp: Long? = null,
    val reminderLeadTimeMinutes: Int = 1440, // Default: 1 day
    val reminderTimeOfDay: String = "09:00",
    val priority: Priority = Priority.NONE,
    val tags: List<String> = emptyList(),
    val recurrencePattern: RecurrencePattern = RecurrencePattern.NONE,
    val recurrenceRule: String? = null,
    val titleError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
