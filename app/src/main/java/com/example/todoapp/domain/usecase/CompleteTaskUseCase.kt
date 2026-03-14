package com.example.todoapp.domain.usecase

import com.example.todoapp.alarm.AlarmScheduler
import com.example.todoapp.data.repository.TaskRepository
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
