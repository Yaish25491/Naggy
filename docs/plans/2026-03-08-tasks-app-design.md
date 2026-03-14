# Tasks App with Smart Reminders - Design Document

**Date:** 2026-03-08
**Status:** Approved

## Overview

An Android mobile application that manages to-dos with advanced reminder capabilities including snooze/postpone functionality, similar to alarm behavior.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Database:** Room (SQLite)
- **Scheduling:** AlarmManager + BroadcastReceiver
- **Architecture:** MVVM + Repository pattern
- **Permissions:** SCHEDULE_EXACT_ALARM, POST_NOTIFICATIONS

## Features

### Core Functionality

1. **Task Management**
   - Create tasks with title and description
   - Set deadline dates
   - Mark tasks as complete
   - View all tasks in a list

2. **Smart Reminders**
   - Configure reminder lead time (hours/days before deadline)
   - Set specific time of day for reminder
   - Receive alarm-style notifications
   - Snooze/postpone reminders

3. **Status Tracking**
   - Upcoming (not yet due)
   - Overdue (past deadline)
   - Done (completed)

## App Structure

### Screen 1: Task List Screen

**Purpose:** Main screen showing all tasks

**UI Components:**
- RecyclerView/LazyColumn of task cards
- Each card shows:
  - Task title
  - Deadline date/time
  - Status badge with color coding
  - Completion checkbox
- Floating Action Button (FAB) for quick add
- Filter/sort options (optional v2 feature)

**Behavior:**
- Tap card to view details
- Swipe actions for delete (optional)
- Real-time status updates

### Screen 2: Add/Edit Task Screen

**Purpose:** Step-by-step task creation

**Form Fields:**
1. Task title (required)
2. Task description (optional)
3. Deadline date picker
4. Deadline time picker
5. Reminder lead time selector (e.g., "2 days before", "3 hours before")
6. Reminder time of day (e.g., 9:00 AM)

**Validation:**
- Title cannot be empty
- Reminder time must be before deadline
- Lead time must be positive

**Behavior:**
- Save creates task in database
- Schedules alarm using AlarmManager
- Returns to task list

### Screen 3: Alarm/Notification Screen

**Purpose:** Full-screen reminder when alarm fires

**UI Components:**
- Task name (large, bold)
- Deadline information
- Time remaining/overdue status
- Action buttons:
  - **Snooze** (postpone 5/10/15 minutes - configurable)
  - **Done** (mark complete, dismiss)
  - **Dismiss** (dismiss notification, keep task)

**Behavior:**
- Launches as full-screen intent (works on lock screen)
- Plays alarm sound/vibration
- Snooze re-schedules using AlarmManager
- Done marks task complete in database
- Dismiss just closes notification

## Data Model

### Task Entity

```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String = "",

    val deadlineTimestamp: Long, // Unix timestamp

    val reminderLeadTimeMinutes: Int, // How far before deadline
    val reminderTimeOfDay: String, // HH:mm format

    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
```

### Derived Properties

- **Reminder timestamp:** Calculated from deadline - lead time, adjusted to specific time of day
- **Status:** Computed based on current time vs deadline and completion status

## Technical Architecture

### Layer 1: Data Layer

**Room Database:**
- `TaskDao` - CRUD operations
- `TaskDatabase` - Database singleton
- SQL queries for filtering by status

**Repository:**
- `TaskRepository` - Mediates between ViewModel and DAO
- Handles business logic for task operations
- Exposes Flow<List<Task>> for reactive updates

### Layer 2: Domain Layer

**Use Cases:**
- `CreateTaskUseCase` - Validates and creates task, schedules alarm
- `CompleteTaskUseCase` - Marks task complete, cancels alarm
- `SnoozeReminderUseCase` - Reschedules alarm
- `DeleteTaskUseCase` - Removes task, cancels alarm

**Alarm Scheduler:**
- `AlarmScheduler` - Wraps AlarmManager
- Schedules exact alarms using PendingIntent
- Cancels alarms by task ID

### Layer 3: Presentation Layer

**ViewModels:**
- `TaskListViewModel` - Manages task list state, filtering
- `AddEditTaskViewModel` - Manages form state, validation
- `AlarmViewModel` - Handles snooze/dismiss actions

**Compose Screens:**
- `TaskListScreen` - Main list with FAB
- `AddEditTaskScreen` - Form with date/time pickers
- `AlarmNotificationActivity` - Full-screen alarm UI

### Background Components

**BroadcastReceiver:**
- `AlarmReceiver` - Receives alarm intents
- Triggers notification
- Launches full-screen intent if needed

**Notification:**
- Custom notification with action buttons
- Full-screen intent for alarm-like behavior
- Sound and vibration

## User Flow

### Adding a Task

1. User taps FAB on main screen
2. Enters task details (title, description)
3. Selects deadline date and time
4. Chooses reminder lead time (e.g., "2 days before")
5. Sets reminder time of day (e.g., "9:00 AM")
6. Taps Save
7. System calculates exact reminder timestamp
8. Schedules alarm via AlarmManager
9. Returns to task list showing new task

### Reminder Fires

1. AlarmManager triggers at scheduled time
2. `AlarmReceiver` receives broadcast
3. Displays full-screen notification (works on lock screen)
4. Plays sound/vibration
5. User sees task details and options
6. User can:
   - **Snooze:** Alarm reschedules for X minutes later
   - **Done:** Task marked complete, alarm canceled
   - **Dismiss:** Notification closes, task remains active

### Completing a Task

1. User taps checkbox on task card OR taps "Done" in alarm
2. Task marked as completed in database
3. Scheduled alarm canceled
4. UI updates to show completed status
5. (Optional) Move to completed section

## Permission Handling

### SCHEDULE_EXACT_ALARM (Android 12+)

**When needed:** To schedule precise alarms

**Request flow:**
1. Check if permission granted
2. If not, show explanation dialog
3. Direct user to system settings
4. Graceful degradation if denied (use inexact alarms)

### POST_NOTIFICATIONS (Android 13+)

**When needed:** To show notifications

**Request flow:**
1. Runtime permission request
2. Show rationale if denied
3. Essential for app functionality

## Error Handling

### Alarm Scheduling Failures

- Catch exceptions from AlarmManager
- Show user-friendly error message
- Offer retry option
- Log for debugging

### Database Errors

- Wrap operations in try-catch
- Show toast/snackbar for failures
- Don't crash, degrade gracefully

### Permission Denials

- Explain why permissions are needed
- Provide settings shortcut
- Disable features gracefully if denied

## Testing Strategy

### Unit Tests

- ViewModel logic
- Use case business logic
- Timestamp calculations
- Repository operations (with fake DAO)

### Integration Tests

- Room database operations
- AlarmScheduler integration
- Repository + DAO

### UI Tests

- Compose UI testing
- Navigation flows
- Form validation

## Future Enhancements (Out of Scope v1)

- Categories/tags for tasks
- Recurring tasks
- Task priority levels
- Search and filters
- Cloud sync
- Widgets
- Multiple snooze presets
- Custom alarm sounds
- Dark mode

## Implementation Notes

### Gradle Dependencies

- Jetpack Compose BOM
- Room KTX
- ViewModel Compose
- Navigation Compose
- Material3
- Kotlin Coroutines

### Minimum SDK

- **minSdk:** 26 (Android 8.0) - for reliable AlarmManager
- **targetSdk:** 34 (Android 14)
- **compileSdk:** 34

### Build Configuration

- Kotlin
- Gradle Kotlin DSL
- Version catalogs for dependency management

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/todoapp/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── TaskDao.kt
│   │   │   │   ├── TaskDatabase.kt
│   │   │   │   └── TaskEntity.kt
│   │   │   └── repository/
│   │   │       └── TaskRepository.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── Task.kt
│   │   │   └── usecase/
│   │   │       ├── CreateTaskUseCase.kt
│   │   │       ├── CompleteTaskUseCase.kt
│   │   │       └── SnoozeReminderUseCase.kt
│   │   ├── presentation/
│   │   │   ├── tasklist/
│   │   │   │   ├── TaskListScreen.kt
│   │   │   │   └── TaskListViewModel.kt
│   │   │   ├── addedittask/
│   │   │   │   ├── AddEditTaskScreen.kt
│   │   │   │   └── AddEditTaskViewModel.kt
│   │   │   └── alarm/
│   │   │       ├── AlarmActivity.kt
│   │   │       └── AlarmViewModel.kt
│   │   ├── alarm/
│   │   │   ├── AlarmScheduler.kt
│   │   │   └── AlarmReceiver.kt
│   │   ├── navigation/
│   │   │   └── NavGraph.kt
│   │   └── MainActivity.kt
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── drawable/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── gradle/
    └── libs.versions.toml
```

## Success Criteria

1. ✅ Users can create tasks with deadlines
2. ✅ Users can set reminder time and lead time
3. ✅ Reminders fire at exact scheduled time
4. ✅ Full-screen alarm works on lock screen
5. ✅ Snooze functionality works correctly
6. ✅ Tasks can be marked complete
7. ✅ UI shows correct status badges
8. ✅ App works offline (local database)
9. ✅ No crashes on common operations
10. ✅ Permissions handled gracefully
