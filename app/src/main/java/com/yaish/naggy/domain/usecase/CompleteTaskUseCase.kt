package com.yaish.naggy.domain.usecase

import com.yaish.naggy.alarm.AlarmScheduler
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.domain.model.RecurrencePattern
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {

    suspend operator fun invoke(taskId: Long) {
        val task = taskRepository.getTaskById(taskId) ?: return

        // Mark current task instance as completed
        taskRepository.completeTask(taskId)

        // Cancel scheduled alarm for the current instance
        alarmScheduler.cancel(taskId)

        // Handle recurrence
        if (task.recurrencePattern != RecurrencePattern.NONE) {
            val nextDeadline = task.calculateNextRecurrenceTimestamp()
            if (nextDeadline != null) {
                // Create a new task instance for the next occurrence
                val newTask = task.copy(
                    id = 0, // Generate new ID
                    deadlineTimestamp = nextDeadline,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = System.currentTimeMillis()
                )

                // Insert into repository
                val newId = taskRepository.insertTask(newTask)

                // Calculate and schedule reminder for the new task
                val reminderTimestamp = newTask.calculateReminderTimestamp()
                if (reminderTimestamp > System.currentTimeMillis()) {
                    alarmScheduler.schedule(newId, reminderTimestamp)
                }
            }
        }
    }
}
