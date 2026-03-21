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
        // Mark current task instance as completed
        taskRepository.completeTask(taskId)

        // Cancel scheduled alarm for the current instance (including any future nagging)
        alarmScheduler.cancel(taskId)
    }
}
