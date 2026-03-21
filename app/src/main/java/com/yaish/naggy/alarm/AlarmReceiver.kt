package com.yaish.naggy.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yaish.naggy.R
import com.yaish.naggy.data.repository.SettingsRepository
import com.yaish.naggy.data.repository.TaskRepository
import com.yaish.naggy.presentation.alarm.AlarmActivity
import com.yaish.naggy.domain.usecase.CompleteTaskUseCase
import com.yaish.naggy.domain.usecase.SnoozeReminderUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var completeTaskUseCase: CompleteTaskUseCase

    @Inject
    lateinit var snoozeReminderUseCase: SnoozeReminderUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        when (intent.action) {
            ACTION_DONE -> {
                scope.launch {
                    completeTaskUseCase(taskId)
                    cancelNotification(context, taskId)
                }
            }
            ACTION_SNOOZE -> {
                scope.launch {
                    snoozeReminderUseCase(taskId, 10) // Default 10 min snooze from notification
                    cancelNotification(context, taskId)
                }
            }
            else -> {
                // Initial alarm trigger
                scope.launch {
                    val task = taskRepository.getTaskById(taskId)
                    if (task != null && !task.isCompleted) {
                        val vibrationEnabled = settingsRepository.isVibrationEnabled.first()
                        showNotification(context, taskId, task.title, task.getFormattedDeadline(), vibrationEnabled)
                        
                        // Schedule the next daily reminder to keep "nagging" until completed
                        val nextReminderTimestamp = task.calculateNextDailyReminderTimestamp()
                        alarmScheduler.schedule(taskId, nextReminderTimestamp)
                    }
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        taskId: Long,
        taskTitle: String,
        deadline: String,
        vibrationEnabled: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel(context, notificationManager, vibrationEnabled)

        // Intent to launch full-screen alarm activity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done Action
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 1000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 2000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title, taskTitle))
            .setContentText(context.getString(R.string.notification_text, deadline))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // Cannot be swiped away
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_notification, "Mark Done", donePendingIntent)
            .addAction(R.drawable.ic_notification, "Snooze 10m", snoozePendingIntent)
            .apply {
                if (!vibrationEnabled) {
                    setVibrate(longArrayOf(0L))
                }
            }
            .build()

        // Show notification
        notificationManager.notify(taskId.toInt(), notification)
    }

    private fun cancelNotification(context: Context, taskId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())
    }

    private fun createNotificationChannel(context: Context, notificationManager: NotificationManager, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(vibrationEnabled)
                setSound(null, null) // No sound as requested
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "task_reminders"
        private const val ACTION_DONE = "com.yaish.naggy.ACTION_DONE"
        private const val ACTION_SNOOZE = "com.yaish.naggy.ACTION_SNOOZE"
    }
}
