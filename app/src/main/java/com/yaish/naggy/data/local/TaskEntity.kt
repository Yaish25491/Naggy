package com.yaish.naggy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.RecurrencePattern

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String = "",

    // Deadline timestamp in milliseconds
    val deadlineTimestamp: Long,

    // How many minutes before deadline to remind (e.g., 1440 = 1 day)
    val reminderLeadTimeMinutes: Int,

    // Time of day for reminder in "HH:mm" format (24-hour)
    val reminderTimeOfDay: String,

    val isCompleted: Boolean = false,
    val priority: Priority = Priority.NONE,
    val tags: List<String> = emptyList(),
    val recurrencePattern: RecurrencePattern = RecurrencePattern.NONE,
    val recurrenceRule: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
