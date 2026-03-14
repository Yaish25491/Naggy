package com.example.todoapp.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.repository.SettingsRepository
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.data.repository.UserData
import com.example.todoapp.domain.model.Task
import com.example.todoapp.domain.usecase.CompleteTaskUseCase
import com.example.todoapp.domain.usecase.DeleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val lastBackupTime: StateFlow<Long> = settingsRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val userData: StateFlow<UserData?> = settingsRepository.userData
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var onTriggerBackup: (() -> Unit)? = null

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            taskRepository.getAllTasks().collect { taskList ->
                _tasks.value = taskList
                _isLoading.value = false
            }
        }
    }

    fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                taskRepository.uncompleteTask(taskId)
            } else {
                completeTaskUseCase(taskId)
            }
            onTriggerBackup?.invoke()
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            onTriggerBackup?.invoke()
        }
    }
}
