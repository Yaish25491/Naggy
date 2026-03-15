package com.yaish.naggy.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _tasksByDate = MutableStateFlow<Map<LocalDate, List<Task>>>(emptyMap())
    val tasksByDate: StateFlow<Map<LocalDate, List<Task>>> = _tasksByDate.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getAllTasks().collect { tasks ->
                val grouped = tasks.groupBy { task ->
                    Date(task.deadlineTimestamp)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                _tasksByDate.value = grouped
            }
        }
    }
}
