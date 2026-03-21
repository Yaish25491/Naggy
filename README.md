# Naggy - Smart Task Management

<div align="center">

**A powerful Android task management app with intelligent reminders, full-screen alarms, and cloud backup**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.02-blue.svg)](https://developer.android.com/jetpack/compose)

[Features](#features) • [Screenshots](#screenshots) • [Installation](#installation) • [Tech Stack](#tech-stack) • [Architecture](#architecture)

</div>

---

## Overview

Naggy is a feature-rich Android task management application designed to help you never miss a deadline. With smart reminder calculations, full-screen alarm notifications, and seamless Google Drive backup, Naggy ensures your tasks are always organized and accessible.

## Features

### Core Task Management
- **Smart Task Creation** - Create tasks with titles, descriptions, and flexible deadline scheduling.
- **Intelligent Reminders** - Set custom reminder lead times (15 minutes to any custom duration) with specific time-of-day preferences.
- **Persistent Nagging** - Once a reminder fires, the app nags you daily at the requested time until the task is marked as resolved.
- **Advanced Filtering & Sorting** - Organize your directives by priority (High/Medium/Low), custom tags, or deadline proximity.
- **Task History** - A dedicated hub for all resolved tasks, allowing for restoration or permanent archival.

### Alarm System
- **Full-Screen Notifications** - High-priority, intrusive alerts that work even over the lock screen.
- **Haptic Vibration** - Optional persistent vibration alarm to ensure you never miss a critical task (without the need for sound).
- **Flexible Snooze** - Postpone reminders with a single tap (5, 10, 15, or 30 minutes).
- **Immediate Nagging** - Alarms trigger immediately even if the scheduled time has already passed, keeping you synchronized.

### Analytics & Cloud
- **Productivity Dashboard** - Visualize your efficiency with a multi-line wave chart showing task completion trends and critical alerts.
- **Temporal Grid** - A high-tech calendar view to map your directives across the timeline.
- **Google Calendar Sync** - Seamlessly mirror your tasks to your primary Google Calendar with custom reminder offsets.
- **Cloud Backup** - Securely back up and restore your entire system data using Google Drive integration.

## Visual Identity: Glassmorphic Minimalist

Naggy features a sophisticated, futuristic design language inspired by modern premium interfaces (Tesla, Apple).

- **Dark Mode (Glassmorphic Minimalist)**: Deep Space (#080A10) background with Twilight Grey (#14171F) surfaces and Ice Blue accents.
- **Light Mode (Ethereal Glass)**: Pristine White (#F8F9FF) background with semi-transparent opalescent glass containers and Sky Blue accents.
- **Motion & Depth**: Smooth entrance animations, background blurs, and subtle glowing indicators for a professional, "live" feel.

## Screenshots

<div align="center">

| New Task | Task List (Dark Mode) | Dashboard |
|:---------:|:---------:|:---------:|
| <img src="docs/screenshots/dashboard.png" width="200" /> | <img src="docs/screenshots/task_list.png" width="200" /> | <img src="docs/screenshots/calendar.png" width="200" /> |

| Calender | Backup | Settings | Task List (White Mode) |
|:---------:|:---------:|:---------:|:---------:|
| <img src="docs/screenshots/add_task.png" width="200" /> | <img src="docs/screenshots/settings.png" width="200" /> | <img src="docs/screenshots/history.png" width="200" /> | <img src="docs/screenshots/alarm.png" width="200" /> |

</div>

## Installation

### Prerequisites
- **Android Studio** Arctic Fox or later
- **Android SDK** 26+ (Android 8.0 Oreo or higher)
- **Gradle** 8.3+
- **JDK** 17

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd to-do-app
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select `File > Open`
   - Navigate to the cloned directory
   - Click `OK`

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle dependencies
   - If not, manually sync: `File > Sync Project with Gradle Files`
   - Wait for the sync to complete

4. **Configure Google Drive (Optional)**
   - For backup functionality, you'll need a Google Cloud Console project
   - Add your `google-services.json` to the `app/` directory
   - Enable Google Drive API in your Google Cloud Console

5. **Run the application**
   - Connect an Android device (API 26+) or start an emulator
   - Click the green **Run** button (▶️)
   - Select your target device
   - The app will build, install, and launch

### Required Permissions

On first launch, Naggy will request the following permissions:

#### Runtime Permissions
- **Notifications** (Android 13+) - Required for showing task reminders
  - Prompted automatically on first launch
  - Tap **Allow** to enable notifications

#### Special Permissions
- **Exact Alarms** (Android 12+) - Required for precise alarm scheduling
  - A dialog will guide you to system settings
  - Navigate to `Settings > Apps > Naggy > Alarms & reminders`
  - Toggle **ON** to enable

#### Optional Permissions
- **Internet & Network State** - For Google Drive backup synchronization
- **Google Account Access** - For Drive backup authentication

## Usage

### Creating a Task

1. Tap the **+** button in the center of the navigation dock
2. Enter task details:
   - **Title** (required) - Brief description of the directive
   - **Description** (optional) - Additional context or data logs
3. Set the **Terminal Deadline**:
   - Tap the deadline field to select date and time
4. Configure **Nag Trigger**:
   - **Reminder Offset** - How far before the deadline to start nagging you
   - **Recurrence** - Set the task to repeat daily, weekly, monthly, or yearly
5. Tap **Execute (Save)**

### Managing Tasks

- **Resolve Task** - Tap the circle icon next to the task to mark as completed
- **Temporal Status**:
  - Green badge = Upcoming
  - Red badge = Overdue
- **Restore Task** - Access the **History Hub** from the dock to restore previously resolved tasks

### When a Reminder Fires

1. A full-screen "Critical System Alert" appears
2. Choose an action:
   - **Resolve Task** - Completes the task and cancels all future nags
   - **Snooze** - Postpone the reminder (5, 10, 15, or 30 minutes)
   - **Dismiss** - Close the alert (persistent nagging will trigger again tomorrow)

### System Management

- **Backup/Restore**: Tap the **Cloud icon** in the dock to manage your system data via Google Drive
- **Task History**: Tap the **Checkmark icon** in the dock to view and manage all resolved directives
- **Settings**: Tap the **Gear icon** in the top bar to toggle Dark/Light mode and Haptic Vibration feedback
- **Temporal Grid**: Tap the **Calendar icon** in the dock to visualize your directive timeline
- **User Account**: Tap your **Profile Circle** in the top bar to view account synchronization details

## Tech Stack

### Core Technologies

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Kotlin | 1.9.22 |
| **UI Framework** | Jetpack Compose | BOM 2024.02 |
| **Database** | Room | 2.6.1 |
| **DI** | Hilt (Dagger) | 2.50 |
| **Async** | Coroutines + Flow | - |
| **Navigation** | Navigation Compose | 2.7.7 |

### Android Jetpack Components

- **Lifecycle** - Lifecycle-aware components
- **ViewModel** - UI-related data holder
- **DataStore** - Preferences and settings storage
- **Room** - SQLite abstraction layer
- **WorkManager** - Background task scheduling
- **Glance** - App Widget development

### External Libraries

- **Google Play Services Auth** - Google Sign-In
- **Google Drive API** - Cloud backup storage
- **Material 3** - Modern Material Design components

## Architecture

Naggy follows **Clean Architecture** principles with **MVVM** (Model-View-ViewModel) pattern for maximum maintainability and testability.

### Project Structure

```
app/src/main/java/com/yaish/naggy/
│
├── 📁 data/                        # Data Layer
│   ├── local/                      # Local data sources
│   │   ├── TaskDao.kt             # Room DAO interface
│   │   ├── TaskDatabase.kt        # Room database
│   │   └── TaskEntity.kt          # Database entity
│   └── repository/                 # Repository implementations
│       ├── TaskRepository.kt      # Task data repository
│       ├── SettingsRepository.kt  # Settings & preferences
│       └── DriveServiceHelper.kt  # Google Drive operations
│
├── 📁 domain/                      # Domain Layer (Business Logic)
│   ├── model/                      # Domain models
│   │   ├── Task.kt                # Core task model
│   │   └── TaskStatus.kt          # Task status enum
│   └── usecase/                    # Use cases
│       ├── CreateTaskUseCase.kt   # Create task logic
│       ├── CompleteTaskUseCase.kt # Complete task logic
│       ├── DeleteTaskUseCase.kt   # Delete task logic
│       └── SnoozeReminderUseCase.kt # Snooze logic
│
├── 📁 presentation/                # Presentation Layer (UI)
│   ├── tasklist/                   # Task list screen
│   │   ├── TaskListScreen.kt      # Compose UI
│   │   └── TaskListViewModel.kt   # ViewModel
│   ├── addedittask/                # Add/Edit screen
│   │   ├── AddEditTaskScreen.kt
│   │   └── AddEditTaskViewModel.kt
│   ├── calendar/                   # Calendar view
│   │   ├── CalendarScreen.kt
│   │   └── CalendarViewModel.kt
│   └── alarm/                      # Alarm notification
│       ├── AlarmActivity.kt       # Full-screen alarm UI
│       └── AlarmViewModel.kt
│
├── 📁 alarm/                       # Alarm Scheduling System
│   ├── AlarmScheduler.kt          # Schedules exact alarms
│   └── AlarmReceiver.kt           # Receives alarm broadcasts
│
├── 📁 widget/                      # Home Screen Widget
│   └── TaskWidget.kt              # Glance widget implementation
│
├── 📁 di/                          # Dependency Injection
│   └── DatabaseModule.kt          # Hilt modules
│
├── 📁 navigation/                  # Navigation Graph
│   └── NavGraph.kt                # Compose navigation routes
│
├── 📁 ui/theme/                    # UI Theming
│   ├── Theme.kt                   # Theme configuration
│   └── Type.kt                    # Typography styles
│
├── MainActivity.kt                 # Main entry point
└── TodoApplication.kt              # Application class
```

### Architecture Layers

#### Data Layer
Handles all data operations including:
- Room database interactions via DAOs
- Google Drive backup/restore operations
- Settings and preferences management
- Data source abstraction

#### Domain Layer
Contains pure business logic:
- Domain models (Task, TaskStatus)
- Use cases for each business operation
- Independent of Android framework
- Easily testable

#### Presentation Layer
UI and user interactions:
- Jetpack Compose screens
- ViewModels for state management
- Reactive UI with Kotlin Flow
- Material 3 design implementation

#### Reminder System Architecture
- **AlarmScheduler**: Uses Android's `AlarmManager` to schedule precise wake-up alarms (`setExactAndAllowWhileIdle`) based on task deadlines and reminder lead times.
- **AlarmReceiver**: A `BroadcastReceiver` that handles the alarm intent in the background, fetches the task from the database using Coroutines, and posts a high-priority notification.
- **AlarmActivity**: A full-screen intent activity launched from the notification that allows users to mark tasks as done, snooze them, or dismiss the alarm—even over the lock screen.
- **SnoozeReminderUseCase**: Schedules a new alarm by calculating a new trigger timestamp and passing it to the `AlarmScheduler`.

### Data Flow

```
┌─────────────┐
│    User     │
│  Interaction│
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Compose    │
│    Screen   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  ViewModel  │◄─────── Observes Flow
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Use Case   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │
└──────┬──────┘
       │
       ├──────────────┐
       ▼              ▼
┌──────────┐   ┌──────────┐
│   Room   │   │  Drive   │
│   DAO    │   │ Service  │
└──────────┘   └──────────┘
```

### Key Design Patterns

- **Repository Pattern** - Abstracts data sources
- **Observer Pattern** - Reactive UI with Flow
- **Dependency Injection** - Hilt for loose coupling
- **Single Source of Truth** - Database is the source
- **Unidirectional Data Flow** - Data flows down, events flow up

## Building the App

### Debug Build

```bash
# Using Gradle wrapper
./gradlew assembleDebug

# Output location
app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

```bash
# Build release APK
./gradlew assembleRelease

# Output location
app/build/outputs/apk/release/app-release-unsigned.apk
```

For a signed release:
1. Create a keystore: `keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias`
2. Add signing configuration to `app/build.gradle.kts`
3. Build: `./gradlew assembleRelease`

## Testing

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

### Testing Reminders Quickly

1. Create a task with a 15-minute lead time
2. Set the deadline to current time + 20 minutes
3. The reminder will fire in 5 minutes
4. Test snooze, mark done, and dismiss actions

### Debugging Alarms

Check if alarms are scheduled:
```bash
adb shell dumpsys alarm | grep -A 5 com.yaish.naggy
```

Inspect the database:
```bash
# Android Studio > View > Tool Windows > App Inspection > Database Inspector
```

## Troubleshooting

### Reminders Not Firing

**Problem:** Alarms not appearing at scheduled time

**Solutions:**
1. **Check Exact Alarm Permission**
   - Go to `Settings > Apps > Naggy > Alarms & reminders`
   - Ensure the permission is enabled

2. **Disable Battery Optimization**
   - Navigate to `Settings > Battery > Battery Optimization`
   - Find "Naggy" and select **Don't optimize**

3. **Check Do Not Disturb**
   - Alarms may be suppressed by DND mode
   - Add Naggy to DND exceptions

4. **Verify Alarm Scheduling**
   - Use `adb shell dumpsys alarm` to check if alarm is registered
   - Look for package `com.yaish.naggy`

### Google Drive Backup Issues

**Problem:** Backup or restore failing

**Solutions:**
1. **Check Internet Connection** - Ensure device has stable internet
2. **Re-authenticate** - Sign out and sign back in to Google
3. **Check Google Drive Space** - Ensure you have available storage
4. **Verify API Credentials** - Check `google-services.json` is properly configured

### Build Errors

**Problem:** Gradle sync or build failures

**Solutions:**
1. **Clean Project**
   ```bash
   ./gradlew clean
   ```
   Or: `Build > Clean Project` in Android Studio

2. **Invalidate Caches**
   - `File > Invalidate Caches / Restart`
   - Select **Invalidate and Restart**

3. **Update Dependencies**
   - Check `gradle/libs.versions.toml` for outdated versions
   - Update Gradle wrapper: `./gradlew wrapper --gradle-version=8.3`

4. **Delete Build Folders**
   ```bash
   rm -rf .gradle build app/build
   ```

### Widget Not Updating

**Problem:** Home screen widget not showing latest tasks

**Solutions:**
1. **Remove and Re-add Widget** - Long-press widget > Remove, then add again
2. **Force App Refresh** - Open app to trigger widget update
3. **Check Widget Permissions** - Ensure app has necessary permissions

## Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Follow the existing code style** (Clean Architecture, MVVM)
4. **Write meaningful commit messages**
5. **Test your changes thoroughly**
6. **Update documentation** if needed
7. **Submit a pull request**

### Code Style

- Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex business logic
- Keep functions small and focused (Single Responsibility)

## License

This project is for **educational purposes** and personal use.

## Acknowledgments

- Built with [Claude Code](https://www.anthropic.com/claude) using modern Android development best practices
- Designed with [Material 3](https://m3.material.io/) design system
- Powered by [Jetpack Compose](https://developer.android.com/jetpack/compose)

## Support

For issues, questions, or feature requests:

1. **Check Troubleshooting** - Review the troubleshooting section above
2. **Read Documentation** - Check `CLAUDE.md` for development details
3. **Search Issues** - Look through existing GitHub issues
4. **Create an Issue** - Open a new issue with detailed information

---

<div align="center">

**Built with ❤️ using Kotlin and Jetpack Compose**

Made by [yaish](https://github.com/yaish)

</div>
