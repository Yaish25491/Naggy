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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

import com.yaish.naggy.domain.model.Priority

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOption = MutableStateFlow(TaskSortOption.DEADLINE_ASC)
    val sortOption: StateFlow<TaskSortOption> = _sortOption.asStateFlow()

    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState: StateFlow<TaskFilterState> = _filterState.asStateFlow()

    val categorizedTasks: StateFlow<Map<TaskCategory, List<Task>>> = combine(
        taskRepository.getAllTasks(),
        _sortOption,
        _filterState,
        _isLoading
    ) { tasks, sort, filter, loading ->
        if (loading && tasks.isEmpty()) emptyMap()
        else {
            val filteredTasks = filterTasks(tasks, filter)
            val sortedTasks = sortTasks(filteredTasks, sort)
            categorizeTasks(sortedTasks)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val availableTags: StateFlow<List<String>> = taskRepository.getAllTasks()
        .map { tasks: List<Task> -> tasks.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Keep the original tasks flow if needed, but categorizedTasks is better for the UI
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    val lastBackupTime: StateFlow<Long> = settingsRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val isDarkTheme: StateFlow<Boolean?> = settingsRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isVibrationEnabled: StateFlow<Boolean> = settingsRepository.isVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

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

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
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

        val activeTasks = tasks.filter { !it.isCompleted }
        val sortedTasks = activeTasks.sortedBy { it.deadlineTimestamp }
        
        return sortedTasks.groupBy { task ->
            val taskDate = Instant.ofEpochMilli(task.deadlineTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            when {
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

    fun updateSortOption(option: TaskSortOption) {
        _sortOption.value = option
    }

    fun updateFilterPriority(priority: Priority?) {
        _filterState.value = _filterState.value.copy(priority = priority)
    }

    fun toggleFilterTag(tag: String) {
        val currentTags = _filterState.value.tags.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _filterState.value = _filterState.value.copy(tags = currentTags)
    }

    fun clearFilters() {
        _filterState.value = TaskFilterState()
    }

    private fun filterTasks(tasks: List<Task>, filter: TaskFilterState): List<Task> {
        return tasks.filter { task ->
            val matchesPriority = filter.priority == null || task.priority == filter.priority
            val matchesTags = filter.tags.isEmpty() || task.tags.any { filter.tags.contains(it) }
            matchesPriority && matchesTags
        }
    }

    private fun sortTasks(tasks: List<Task>, sort: TaskSortOption): List<Task> {
        return when (sort) {
            TaskSortOption.DEADLINE_ASC -> tasks.sortedBy { it.deadlineTimestamp }
            TaskSortOption.DEADLINE_DESC -> tasks.sortedByDescending { it.deadlineTimestamp }
            TaskSortOption.PRIORITY_DESC -> tasks.sortedWith(
                compareByDescending<Task> { it.priority.ordinal }.thenBy { it.deadlineTimestamp }
            )
            TaskSortOption.CREATED_DESC -> tasks.sortedByDescending { it.createdAt }
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

enum class TaskSortOption(val displayName: String) {
    DEADLINE_ASC("Deadline (Soonest)"),
    DEADLINE_DESC("Deadline (Latest)"),
    PRIORITY_DESC("Priority (High-Low)"),
    CREATED_DESC("Newest Created")
}

data class TaskFilterState(
    val priority: Priority? = null,
    val tags: Set<String> = emptySet()
)
