package com.yaish.naggy.domain.usecase

import com.yaish.naggy.alarm.AlarmScheduler
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.domain.model.Task
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {

    suspend operator fun invoke(task: Task): Result<Long> {
        return try {
            // Validate task
            if (task.title.isBlank()) {
                return Result.failure(Exception("Title cannot be empty"))
            }

            if (task.deadlineTimestamp <= System.currentTimeMillis()) {
                return Result.failure(Exception("Deadline must be in the future"))
            }

            // Calculate reminder timestamp
            val reminderTimestamp = task.calculateReminderTimestamp()

            if (reminderTimestamp <= System.currentTimeMillis()) {
                return Result.failure(Exception("Reminder time must be in the future"))
            }

            // Insert task into database
            val taskId = taskRepository.insertTask(task)

            // Schedule alarm
            alarmScheduler.schedule(taskId, reminderTimestamp)

            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
