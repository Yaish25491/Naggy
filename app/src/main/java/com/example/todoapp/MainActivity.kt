package com.example.todoapp

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
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
import com.example.todoapp.data.local.TaskDatabase
import com.example.todoapp.data.repository.DriveServiceHelper
import com.example.todoapp.data.repository.SettingsRepository
import com.example.todoapp.navigation.NavGraph
import com.example.todoapp.ui.theme.TodoAppTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private var driveServiceHelper: DriveServiceHelper? by mutableStateOf(null)

    @Inject
    lateinit var settingsRepository: SettingsRepository

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
            TodoAppTheme {
                var showAlarmPermissionDialog by remember { mutableStateOf(false) }
                var showLoginPrompt by remember { mutableStateOf(false) }
                val context = LocalContext.current

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        handleSignInResult(result.data)
                    } else {
                        Log.e(TAG, "Sign in launcher failed with result code: ${result.resultCode}")
                        Toast.makeText(context, "Sign in failed. Check your internet or Google Play Services.", Toast.LENGTH_LONG).show()
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
                    
                    // Check first run and login
                    val isFirstRun = settingsRepository.isFirstRun.first()
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    
                    if (account != null) {
                        Log.d(TAG, "Account found on start: ${account.email}")
                        // Ensure settings repository is synced with the account found
                        settingsRepository.saveUserData(
                            name = account.displayName ?: "User",
                            email = account.email ?: ""
                        )
                        initializeDriveService(account)
                    } else if (isFirstRun) {
                        showLoginPrompt = true
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
                        onRestore = { restoreDatabase() }
                    )
                }

                if (showLoginPrompt) {
                    val webClientId = stringResource(id = R.string.default_web_client_id)
                    AlertDialog(
                        onDismissRequest = { /* Don't allow dismiss */ },
                        title = { Text("Welcome!") },
                        text = { Text("To enable backup and restore features, please sign in with your Google account.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLoginPrompt = false
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestProfile()
                                    .requestIdToken(webClientId)
                                    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                                    .build()
                                val client = GoogleSignIn.getClient(this@MainActivity, gso)
                                googleSignInLauncher.launch(client.signInIntent)
                            }) {
                                Text("Sign In")
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
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener { account ->
                Log.d(TAG, "Sign in successful: ${account.email}")
                lifecycleScope.launch {
                    settingsRepository.setFirstRun(false)
                    settingsRepository.saveUserData(
                        name = account.displayName ?: "User",
                        email = account.email ?: ""
                    )
                }
                initializeDriveService(account)
                Toast.makeText(this, "Welcome, ${account.displayName}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Sign in failed", e)
                Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun initializeDriveService(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        val googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("Naggy")
            .build()

        driveServiceHelper = DriveServiceHelper(googleDriveService)
        Log.d(TAG, "Drive service initialized")
    }

    private fun backupDatabase() {
        if (driveServiceHelper == null) {
            Log.e(TAG, "Drive service not initialized")
            Toast.makeText(this, "Please sign in to enable backup", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Starting backup...", Toast.LENGTH_SHORT).show()
        val dbPath = getDatabasePath(TaskDatabase.DATABASE_NAME).absolutePath
        driveServiceHelper?.uploadFile(dbPath, "todo_backup.db")
            ?.addOnSuccessListener { 
                Log.d(TAG, "Backup successful")
                Toast.makeText(this, "Backup successful!", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    settingsRepository.setLastBackupTime(System.currentTimeMillis())
                }
            }
            ?.addOnFailureListener { 
                Log.e(TAG, "Backup failed", it)
                Toast.makeText(this, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun restoreDatabase() {
        if (driveServiceHelper == null) {
            Log.e(TAG, "Drive service not initialized")
            Toast.makeText(this, "Please sign in to enable restore", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Restoring data...", Toast.LENGTH_SHORT).show()
        val dbPath = getDatabasePath(TaskDatabase.DATABASE_NAME).absolutePath
        driveServiceHelper?.downloadFile("todo_backup.db", dbPath)
            ?.addOnSuccessListener { 
                Log.d(TAG, "Restore successful")
                Toast.makeText(this, "Restore successful! Restarting...", Toast.LENGTH_LONG).show()
                // Restarting activity to reload DB
                finish()
                startActivity(intent)
            }
            ?.addOnFailureListener { 
                Log.e(TAG, "Restore failed", it)
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
        text = { Text("This app needs permission to schedule exact alarms for reminders. Please enable it in settings.") },
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
