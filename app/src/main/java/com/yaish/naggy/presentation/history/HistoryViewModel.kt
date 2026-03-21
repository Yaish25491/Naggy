package com.yaish.naggy.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val completedTasks: StateFlow<List<Task>> = taskRepository.getCompletedTasks()
        .map { tasks -> tasks.sortedByDescending { it.completedAt ?: 0L } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteHistory(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTaskById(taskId)
        }
    }

    fun uncompleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.uncompleteTask(taskId)
        }
    }
}
