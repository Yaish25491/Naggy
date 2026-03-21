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

            // Calculate reminder timestamp
            val reminderTimestamp = task.calculateReminderTimestamp()

            // Insert task into database
            val taskId = taskRepository.insertTask(task)

            // Schedule alarm
            // Even if in the past, AlarmManager will trigger it immediately
            alarmScheduler.schedule(taskId, reminderTimestamp)

            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
