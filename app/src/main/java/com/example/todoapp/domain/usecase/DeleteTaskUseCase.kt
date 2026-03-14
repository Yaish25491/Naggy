package com.example.todoapp.domain.usecase

import com.example.todoapp.alarm.AlarmScheduler
import com.example.todoapp.data.repository.TaskRepository
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
