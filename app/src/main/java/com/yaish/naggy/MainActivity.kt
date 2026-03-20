package com.yaish.naggy

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.sqlite.db.SimpleSQLiteQuery
import com.yaish.naggy.data.local.TaskDatabase
import com.yaish.naggy.data.repository.DriveServiceHelper
import com.yaish.naggy.data.repository.SettingsRepository
import com.yaish.naggy.navigation.NavGraph
import com.yaish.naggy.ui.theme.TodoAppTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.yaish.naggy.data.repository.CalendarServiceHelper
import com.yaish.naggy.domain.model.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private var driveServiceHelper: DriveServiceHelper? by mutableStateOf(null)
    private var calendarServiceHelper: CalendarServiceHelper? by mutableStateOf(null)

    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var database: TaskDatabase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Handle permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val isDarkThemePref by settingsRepository.isDarkTheme.collectAsState(initial = null)
            val isDarkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

            TodoAppTheme(darkTheme = isDarkTheme) {
                var showAlarmPermissionDialog by remember { mutableStateOf(false) }
                var showLoginPrompt by remember { mutableStateOf(false) }
                var showRestorePrompt by remember { mutableStateOf(false) }
                val context = LocalContext.current

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        handleSignInResult(result.data)
                    } else {
                        Log.e(TAG, "Sign in launcher failed with result code: ${result.resultCode}")
                        val message = when(result.resultCode) {
                            Activity.RESULT_CANCELED -> "Sign in canceled. Check internet or console config."
                            else -> "Sign in failed. Result code: ${result.resultCode}"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                // Initial checks
                LaunchedEffect(Unit) {
                    // Check exact alarm permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            showAlarmPermissionDialog = true
                        }
                    }
                    
                    // Check login status
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account != null) {
                        if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA), Scope(CalendarScopes.CALENDAR_EVENTS))) {
                            showLoginPrompt = true
                        } else {
                            settingsRepository.saveUserData(
                                name = account.displayName ?: "User",
                                email = account.email ?: ""
                            )
                            initializeGoogleServices(account)
                        }
                    } else {
                        val isFirstRun = settingsRepository.isFirstRun.first()
                        if (isFirstRun) {
                            showLoginPrompt = true
                        }
                    }
                }

                LaunchedEffect(driveServiceHelper) {
                    driveServiceHelper?.let { helper ->
                        helper.getBackupModifiedTime("todo_backup.db")
                            .addOnSuccessListener { remoteTime ->
                                if (remoteTime != null) {
                                    val dbFile = java.io.File(getDatabasePath(TaskDatabase.DATABASE_NAME).absolutePath)
                                    val localTime = if (dbFile.exists()) dbFile.lastModified() else 0L
                                    
                                    // If remote is newer by at least 1 minute (avoids timezone/skew issues)
                                    if (remoteTime > localTime + 60_000L) {
                                        showRestorePrompt = true
                                    }
                                }
                            }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        onBackup = { backupDatabase() },
                        onRestore = { restoreDatabase() },
                        onSyncToCalendar = { task -> syncTaskToCalendar(task) }
                    )
                }

                if (showRestorePrompt) {
                    AlertDialog(
                        onDismissRequest = { showRestorePrompt = false },
                        title = { Text("Backup Found") },
                        text = { Text("A newer backup was found on your Google Drive. Would you like to restore it now?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showRestorePrompt = false
                                restoreDatabase()
                            }) {
                                Text("Restore")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRestorePrompt = false }) {
                                Text("Skip")
                            }
                        }
                    )
                }

                if (showLoginPrompt) {
                    val webClientId = stringResource(id = R.string.default_web_client_id)
                    AlertDialog(
                        onDismissRequest = { showLoginPrompt = false },
                        title = { Text("Welcome!") },
                        text = { Text("To enable backup, restore, and Google Calendar sync, please sign in with your Google account and grant the required permissions.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLoginPrompt = false
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestProfile()
                                    .requestIdToken(webClientId)
                                    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA), Scope(CalendarScopes.CALENDAR_EVENTS))
                                    .build()
                                val client = GoogleSignIn.getClient(this@MainActivity, gso)
                                googleSignInLauncher.launch(client.signInIntent)
                            }) {
                                Text("Sign In")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLoginPrompt = false }) {
                                Text("Later")
                            }
                        }
                    )
                }

                // Alarm permission dialog
                if (showAlarmPermissionDialog) {
                    AlarmPermissionDialog(
                        onDismiss = { showAlarmPermissionDialog = false },
                        onOpenSettings = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                                startActivity(intent)
                            }
                            showAlarmPermissionDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            lifecycleScope.launch {
                settingsRepository.setFirstRun(false)
                settingsRepository.saveUserData(
                    name = account.displayName ?: "User",
                    email = account.email ?: ""
                )
            }
            initializeGoogleServices(account)
            Toast.makeText(this, "Welcome, ${account.displayName}!", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            Log.e(TAG, "Sign in failed: ${e.statusCode}")
            Toast.makeText(this, "Login Failed (Code ${e.statusCode})", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeGoogleServices(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(DriveScopes.DRIVE_APPDATA, CalendarScopes.CALENDAR_EVENTS)
        )
        credential.selectedAccount = account.account

        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory()

        val googleDriveService = Drive.Builder(
            httpTransport,
            jsonFactory,
            credential
        )
            .setApplicationName("Naggy")
            .build()
        driveServiceHelper = DriveServiceHelper(googleDriveService)

        val googleCalendarService = Calendar.Builder(
            httpTransport,
            jsonFactory,
            credential
        )
            .setApplicationName("Naggy")
            .build()
        calendarServiceHelper = CalendarServiceHelper(googleCalendarService)
    }

    private fun syncTaskToCalendar(task: Task) {
        if (calendarServiceHelper == null) {
            Toast.makeText(this, "Please sign in to sync with Calendar", Toast.LENGTH_SHORT).show()
            return
        }

        calendarServiceHelper?.insertTaskEvent(
            title = task.title,
            description = task.description,
            deadlineTimestamp = task.deadlineTimestamp,
            reminderLeadTimeMinutes = task.reminderLeadTimeMinutes
        )?.addOnSuccessListener {
            Toast.makeText(this, "Synced to Google Calendar", Toast.LENGTH_SHORT).show()
        }?.addOnFailureListener {
            Log.e(TAG, "Calendar sync failed", it)
            val errorMessage = it.message ?: it.cause?.message ?: "Unknown error"
            Toast.makeText(this, "Failed to sync to Calendar: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }

    private fun backupDatabase() {
        if (driveServiceHelper == null) {
            Toast.makeText(this, "Please sign in to backup", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Preparing backup...", Toast.LENGTH_SHORT).show()
                
                // Force Room to flush WAL to DB file
                withContext(Dispatchers.IO) {
                    val cursor = database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                    cursor.moveToFirst()
                    cursor.close()
                }

                val dbPath = getDatabasePath(TaskDatabase.DATABASE_NAME).absolutePath
                driveServiceHelper?.uploadFile(dbPath, "todo_backup.db")
                    ?.addOnSuccessListener { 
                        Toast.makeText(this@MainActivity, "Backup uploaded successfully!", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            settingsRepository.setLastBackupTime(System.currentTimeMillis())
                        }
                    }
                    ?.addOnFailureListener { 
                        Toast.makeText(this@MainActivity, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                Toast.makeText(this@MainActivity, "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreDatabase() {
        if (driveServiceHelper == null) {
            Toast.makeText(this, "Please sign in to restore", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Downloading backup...", Toast.LENGTH_SHORT).show()
        val dbPath = getDatabasePath(TaskDatabase.DATABASE_NAME).absolutePath
        
        // Close the database to release file locks before overwriting it
        database.close()
        
        driveServiceHelper?.downloadFile("todo_backup.db", dbPath)
            ?.addOnSuccessListener { 
                try {
                    // Delete WAL and SHM files to prevent Room from corrupting the restored DB
                    java.io.File("$dbPath-wal").delete()
                    java.io.File("$dbPath-shm").delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting WAL files", e)
                }

                Toast.makeText(this, "Data restored! Restarting Naggy...", Toast.LENGTH_LONG).show()
                
                // Hard restart
                val restartIntent = Intent(this, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, restartIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
                exitProcess(0)
            }
            ?.addOnFailureListener { 
                Toast.makeText(this, "Restore failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@Composable
fun AlarmPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exact Alarms Permission") },
        text = { Text("Naggy needs permission to schedule exact alarms for reminders.") },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
