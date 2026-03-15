package com.yaish.naggy.domain.usecase

import com.yaish.naggy.alarm.AlarmScheduler
import com.yaish.naggy.data.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {

    suspend operator fun invoke(taskId: Long) {
        // Delete task from database
        taskRepository.deleteTaskById(taskId)

        // Cancel scheduled alarm
        alarmScheduler.cancel(taskId)
    }
}
