package com.example.todoapp.presentation.addedittask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.domain.model.Task
import com.example.todoapp.domain.usecase.CreateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    var onTriggerBackup: (() -> Unit)? = null

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

    fun saveTask(onSuccess: () -> Unit) {
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
                title = state.title,
                description = state.description,
                deadlineTimestamp = state.deadlineTimestamp!!,
                reminderLeadTimeMinutes = state.reminderLeadTimeMinutes,
                reminderTimeOfDay = state.reminderTimeOfDay
            )

            val result = createTaskUseCase(task)

            result.fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false)
                    onTriggerBackup?.invoke()
                    onSuccess()
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
    val titleError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
