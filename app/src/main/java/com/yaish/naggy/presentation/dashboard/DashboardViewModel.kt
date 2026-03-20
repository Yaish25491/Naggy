package com.yaish.naggy.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaish.naggy.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TreeMap
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val now = System.currentTimeMillis()
    private val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val weekStart = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val uiState: StateFlow<DashboardUiState> = combine(
        taskRepository.getCompletedCountSince(todayStart),
        taskRepository.getCompletedCountSince(weekStart),
        taskRepository.getOverdueCount(now),
        taskRepository.getAllCompletedTimestamps()
    ) { todayCount, weekCount, overdueCount, timestamps ->
        DashboardUiState(
            completedToday = todayCount,
            completedThisWeek = weekCount,
            overdueTasks = overdueCount,
            completionHistory = calculateHistory(timestamps)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    private fun calculateHistory(timestamps: List<Long>): Map<LocalDate, Int> {
        val history = TreeMap<LocalDate, Int>()
        // Initialize last 7 days
        val today = LocalDate.now()
        for (i in 0..6) {
            history[today.minusDays(i.toLong())] = 0
        }

        timestamps.forEach { ts ->
            val date = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
            if (history.containsKey(date)) {
                history[date] = (history[date] ?: 0) + 1
            }
        }
        return history
    }
}

data class DashboardUiState(
    val completedToday: Int = 0,
    val completedThisWeek: Int = 0,
    val overdueTasks: Int = 0,
    val completionHistory: Map<LocalDate, Int> = emptyMap()
)
