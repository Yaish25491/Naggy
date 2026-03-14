package com.example.todoapp.data.repository

import com.example.todoapp.data.local.TaskDao
import com.example.todoapp.data.local.TaskEntity
import com.example.todoapp.domain.model.Task
import com.example.todoapp.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)?.toDomainModel()
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task.toEntity())
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }

    suspend fun deleteTaskById(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun completeTask(taskId: Long) {
        taskDao.updateTaskCompletion(
            taskId = taskId,
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
    }

    suspend fun uncompleteTask(taskId: Long) {
        taskDao.updateTaskCompletion(
            taskId = taskId,
            isCompleted = false,
            completedAt = null
        )
    }

    private fun TaskEntity.toDomainModel(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            deadlineTimestamp = deadlineTimestamp,
            reminderLeadTimeMinutes = reminderLeadTimeMinutes,
            reminderTimeOfDay = reminderTimeOfDay,
            isCompleted = isCompleted,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }

    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            deadlineTimestamp = deadlineTimestamp,
            reminderLeadTimeMinutes = reminderLeadTimeMinutes,
            reminderTimeOfDay = reminderTimeOfDay,
            isCompleted = isCompleted,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
}
