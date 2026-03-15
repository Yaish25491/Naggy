package com.yaish.naggy.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaish.naggy.data.repository.SettingsRepository
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.data.repository.UserData
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.domain.usecase.CompleteTaskUseCase
import com.yaish.naggy.domain.usecase.DeleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val categorizedTasks: StateFlow<Map<TaskCategory, List<Task>>> = taskRepository.getAllTasks()
        .combine(_isLoading) { tasks, loading ->
            if (loading && tasks.isEmpty()) emptyMap()
            else categorizeTasks(tasks)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Keep the original tasks flow if needed, but categorizedTasks is better for the UI
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    val lastBackupTime: StateFlow<Long> = settingsRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val isDarkTheme: StateFlow<Boolean?> = settingsRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userData: StateFlow<UserData?> = settingsRepository.userData
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var onTriggerBackup: (() -> Unit)? = null

    init {
        loadTasks()
    }

    fun setDarkTheme(isDark: Boolean?) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDark)
        }
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

    private fun categorizeTasks(tasks: List<Task>): Map<TaskCategory, List<Task>> {
        val today = LocalDate.now()
        val startOfNextWeek = today.plusDays(7)
        val startOfNextMonth = today.plusMonths(1)

        val sortedTasks = tasks.sortedBy { it.deadlineTimestamp }
        
        return sortedTasks.groupBy { task ->
            val taskDate = Instant.ofEpochMilli(task.deadlineTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            when {
                task.isCompleted -> TaskCategory.COMPLETED
                taskDate.isBefore(today) -> TaskCategory.OVERDUE
                taskDate == today -> TaskCategory.TODAY
                taskDate.isBefore(startOfNextWeek) -> TaskCategory.THIS_WEEK
                taskDate.isBefore(startOfNextMonth) -> TaskCategory.THIS_MONTH
                else -> TaskCategory.LATER
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

enum class TaskCategory(val displayName: String) {
    OVERDUE("Overdue"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LATER("Later"),
    COMPLETED("Completed")
}
