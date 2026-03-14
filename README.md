# Todo App - Smart Task Reminders

An Android task management app with alarm-like reminders and snooze functionality.

## Features

- ✅ Create tasks with deadlines
- ⏰ Set reminder time and lead time (how far before deadline)
- 🔔 Full-screen alarm notifications (works on lock screen)
- 😴 Snooze reminders like an alarm (5, 10, 15, 30 minutes)
- ✔️ Mark tasks as complete
- 📊 Track task status (Upcoming, Overdue, Completed)

## Screenshots

*Note: Add screenshots here after running the app*

## Tech Stack

- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Room** - Local database
- **AlarmManager** - Precise alarm scheduling
- **Hilt** - Dependency injection
- **MVVM + Clean Architecture** - App architecture

## Requirements

- Android Studio Arctic Fox or later
- Android SDK 26+ (Android 8.0 Oreo)
- Gradle 8.3+

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd to-do-app
```

### 2. Open in Android Studio

1. Open Android Studio
2. Select `File > Open`
3. Navigate to the project directory
4. Click `OK`

### 3. Sync Gradle

Android Studio will automatically sync Gradle. If not:
1. Click `File > Sync Project with Gradle Files`
2. Wait for sync to complete

### 4. Run the App

1. Connect an Android device or start an emulator
2. Click the `Run` button (green play icon)
3. Select your device
4. App will install and launch

### 5. Grant Permissions

When you first run the app, you'll need to grant permissions:

#### Notification Permission (Android 13+)
- A permission dialog will appear
- Tap `Allow` to receive reminder notifications

#### Exact Alarm Permission (Android 12+)
- A dialog will appear asking to enable exact alarms
- Tap `Open Settings`
- Enable `Alarms & reminders` permission
- Return to the app

## How to Use

### Creating a Task

1. Tap the `+` button (floating action button)
2. Enter task title (required)
3. Enter description (optional)
4. Select deadline date and time
5. Choose how far before deadline to remind you
6. Select what time of day you want the reminder
7. Tap `Save`

### Managing Tasks

- **Complete:** Tap the circle icon next to a task
- **View details:** See task card in the list
- **Status badges:**
  - Green = Upcoming
  - Red = Overdue
  - Gray = Completed

### When a Reminder Fires

1. A full-screen notification appears (even on lock screen)
2. You have three options:
   - **Mark Done:** Completes the task and dismisses
   - **Snooze:** Postpone reminder (choose 5, 10, 15, or 30 min)
   - **Dismiss:** Close notification, task remains active

## Project Structure

```
app/
├── data/           # Database, DAOs, Repository
├── domain/         # Business logic, Use cases
├── presentation/   # UI screens and ViewModels
├── alarm/          # Alarm scheduling system
├── di/             # Dependency injection
└── navigation/     # App navigation
```

## Building APK

To build a release APK:

1. In Android Studio: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
2. Or via command line:
   ```bash
   ./gradlew assembleRelease
   ```
3. APK will be in `app/build/outputs/apk/release/`

## Troubleshooting

### Reminders Not Working

- **Check permissions:** Ensure exact alarm permission is enabled
- **Battery optimization:** Some devices restrict background alarms
  - Go to `Settings > Battery > Battery Optimization`
  - Find "Todo App" and select "Don't optimize"

### Build Errors

- **Clean project:** `Build > Clean Project`
- **Rebuild:** `Build > Rebuild Project`
- **Invalidate caches:** `File > Invalidate Caches / Restart`

### Database Issues

- Uninstall and reinstall the app to reset the database

## Testing

The app can be tested using:
- Android emulator (API 26+)
- Physical Android device

To test reminders quickly:
1. Create a task with a 15-minute lead time
2. Set deadline to current time + 20 minutes
3. Wait for reminder to fire

## Architecture

The app follows Clean Architecture principles:

- **Data Layer:** Room database, repositories
- **Domain Layer:** Business logic, use cases
- **Presentation Layer:** Compose UI, ViewModels

Benefits:
- Separation of concerns
- Testability
- Maintainability
- Scalability

## Dependencies

Main dependencies (see `gradle/libs.versions.toml`):
- AndroidX Core KTX
- Jetpack Compose
- Room Database
- Hilt (Dagger)
- Navigation Compose
- Material 3

## License

This project is for educational purposes.

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review CLAUDE.md for development details
3. Check existing GitHub issues

## Acknowledgments

Built with Claude Code using modern Android development best practices.
