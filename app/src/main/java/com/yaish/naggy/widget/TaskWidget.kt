package com.yaish.naggy.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.yaish.naggy.data.local.TaskDao
import com.yaish.naggy.domain.model.Task
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskWidgetEntryPoint {
        fun taskDao(): TaskDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TaskWidgetEntryPoint::class.java
        )
        val taskDao = entryPoint.taskDao()

        provideContent {
            val tasks by taskDao.getActiveTasks().collectAsState(initial = emptyList())
            
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Naggy Tasks",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = sp(16),
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    if (tasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No active tasks", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(tasks.take(5)) { taskEntity ->
                                val task = taskEntity.toDomainModel()
                                WidgetTaskItem(task)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sp(value: Int) = androidx.compose.ui.unit.sp(value.toFloat())

    private com.yaish.naggy.data.local.TaskEntity.toDomainModel(): Task {
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
}

@Composable
private fun WidgetTaskItem(task: Task) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val deadline = Instant.ofEpochMilli(task.deadlineTimestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(GlanceTheme.colors.surface)
            .padding(8.dp)
    ) {
        Text(
            text = task.title,
            style = TextStyle(fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface),
            maxLines = 1
        )
        Text(
            text = deadline,
            style = TextStyle(fontSize = sp(12), color = GlanceTheme.colors.onSurfaceVariant)
        )
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}
