package com.yaish.naggy.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CalendarServiceHelper(private val calendarService: Calendar) {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun insertTaskEvent(
        title: String,
        description: String,
        deadlineTimestamp: Long,
        reminderLeadTimeMinutes: Int
    ): Task<String> {
        return Tasks.call(executor) {
            val event = Event()
                .setSummary(title)
                .setDescription(description)

            // Let's assume the task takes 30 minutes and ends at the deadline
            val endDateTime = DateTime(deadlineTimestamp)
            val startDateTime = DateTime(deadlineTimestamp - (30 * 60 * 1000))

            val start = EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(java.util.TimeZone.getDefault().id)
            event.start = start

            val end = EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(java.util.TimeZone.getDefault().id)
            event.end = end

            val reminderOverrides = listOf(
                EventReminder().setMethod("popup").setMinutes(reminderLeadTimeMinutes)
            )
            val reminders = Event.Reminders()
                .setUseDefault(false)
                .setOverrides(reminderOverrides)
            event.reminders = reminders

            val createdEvent = calendarService.events().insert("primary", event).execute()
            createdEvent.id
        }
    }
}
