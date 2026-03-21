package com.yaish.naggy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
    }

    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_VIBRATION_ENABLED] ?: true
    }

    val isDarkTheme: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_THEME]
    }

    val isFirstRun: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_FIRST_RUN] ?: true
    }

    val lastBackupTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_BACKUP_TIME] ?: 0L
    }

    val userData: Flow<UserData?> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.USER_NAME]
        val email = preferences[PreferencesKeys.USER_EMAIL]
        if (name != null && email != null) UserData(name, email) else null
    }

    suspend fun setFirstRun(isFirstRun: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_RUN] = isFirstRun
        }
    }

    suspend fun setLastBackupTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIME] = timestamp
        }
    }

    suspend fun saveUserData(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
            preferences[PreferencesKeys.USER_EMAIL] = email
        }
    }

    suspend fun setDarkTheme(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            if (isDark == null) {
                preferences.remove(PreferencesKeys.IS_DARK_THEME)
            } else {
                preferences[PreferencesKeys.IS_DARK_THEME] = isDark
            }
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VIBRATION_ENABLED] = enabled
        }
    }
}

data class UserData(val name: String, val email: String)
