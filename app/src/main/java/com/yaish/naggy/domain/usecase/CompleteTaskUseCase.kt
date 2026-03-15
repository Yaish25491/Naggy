package com.yaish.naggy.domain.usecase

import com.yaish.naggy.alarm.AlarmScheduler
import com.yaish.naggy.data.repository.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {

    suspend operator fun invoke(taskId: Long) {
        // Mark task as completed
        taskRepository.completeTask(taskId)

        // Cancel scheduled alarm
        alarmScheduler.cancel(taskId)
    }
}
