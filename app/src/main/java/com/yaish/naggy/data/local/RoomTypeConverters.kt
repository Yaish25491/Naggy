package com.yaish.naggy.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.RecurrencePattern

class RoomTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): Priority {
        return try {
            Priority.valueOf(priority)
        } catch (e: Exception) {
            Priority.NONE
        }
    }

    @TypeConverter
    fun fromRecurrencePattern(pattern: RecurrencePattern): String {
        return pattern.name
    }

    @TypeConverter
    fun toRecurrencePattern(pattern: String): RecurrencePattern {
        return try {
            RecurrencePattern.valueOf(pattern)
        } catch (e: Exception) {
            RecurrencePattern.NONE
        }
    }
}
