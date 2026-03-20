package com.yaish.naggy.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val deadlineTimestamp: Long,
    val reminderLeadTimeMinutes: Int,
    val reminderTimeOfDay: String, // "HH:mm" format
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {

    /**
     * Calculate the exact reminder timestamp based on deadline, lead time, and time of day
     */
    fun calculateReminderTimestamp(): Long {
        // Convert deadline to LocalDateTime
        val deadlineDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(deadlineTimestamp),
            ZoneId.systemDefault()
        )

        // Subtract lead time from deadline to get reminder date
        val reminderDateTime = deadlineDateTime.minusMinutes(reminderLeadTimeMinutes.toLong())

        // Parse the time of day (HH:mm)
        val timeParts = reminderTimeOfDay.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Set the time of day for the reminder
        val finalReminderDateTime = reminderDateTime
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        // Convert back to timestamp
        return finalReminderDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Calculate the timestamp for the next daily reminder (tomorrow at the requested time of day)
     */
    fun calculateNextDailyReminderTimestamp(): Long {
        val now = LocalDateTime.now()
        val timeParts = reminderTimeOfDay.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        var nextReminderDateTime = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        // If the calculated time is in the past or present, schedule for tomorrow
        if (!nextReminderDateTime.isAfter(now)) {
            nextReminderDateTime = nextReminderDateTime.plusDays(1)
        }

        return nextReminderDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Get current status based on deadline and completion
     */
    fun getStatus(): TaskStatus {
        return when {
            isCompleted -> TaskStatus.COMPLETED
            System.currentTimeMillis() > deadlineTimestamp -> TaskStatus.OVERDUE
            else -> TaskStatus.UPCOMING
        }
    }

    /**
     * Format deadline for display
     */
    fun getFormattedDeadline(): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(deadlineTimestamp),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")
        return dateTime.format(formatter)
    }

    /**
     * Get time until deadline (for display)
     */
    fun getTimeUntilDeadline(): String {
        val now = System.currentTimeMillis()
        val diff = deadlineTimestamp - now

        if (diff < 0) {
            // Overdue
            val absDiff = -diff
            return when {
                absDiff < 60 * 60 * 1000 -> "${absDiff / (60 * 1000)} min ago"
                absDiff < 24 * 60 * 60 * 1000 -> "${absDiff / (60 * 60 * 1000)} hours ago"
                else -> "${absDiff / (24 * 60 * 60 * 1000)} days ago"
            }
        } else {
            // Upcoming
            return when {
                diff < 60 * 60 * 1000 -> "in ${diff / (60 * 1000)} min"
                diff < 24 * 60 * 60 * 1000 -> "in ${diff / (60 * 60 * 1000)} hours"
                else -> "in ${diff / (24 * 60 * 60 * 1000)} days"
            }
        }
    }
}
