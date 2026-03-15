package com.yaish.naggy.presentation.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.domain.usecase.CompleteTaskUseCase
import com.yaish.naggy.domain.usecase.SnoozeReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val snoozeReminderUseCase: SnoozeReminderUseCase
) : ViewModel() {

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            val loadedTask = taskRepository.getTaskById(taskId)
            _task.value = loadedTask
        }
    }

    fun markDone(taskId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
            onComplete()
        }
    }

    fun snooze(taskId: Long, minutes: Int, onComplete: () -> Unit) {
        snoozeReminderUseCase(taskId, minutes)
        onComplete()
    }
}
