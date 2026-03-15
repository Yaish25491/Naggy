package com.yaish.naggy.domain.usecase

import com.yaish.naggy.alarm.AlarmScheduler
import javax.inject.Inject

class SnoozeReminderUseCase @Inject constructor(
    private val alarmScheduler: AlarmScheduler
) {

    /**
     * Snooze a reminder for the specified number of minutes
     */
    operator fun invoke(taskId: Long, snoozeMinutes: Int) {
        val snoozeTimestamp = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
        alarmScheduler.schedule(taskId, snoozeTimestamp)
    }
}
