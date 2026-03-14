package com.example.todoapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
