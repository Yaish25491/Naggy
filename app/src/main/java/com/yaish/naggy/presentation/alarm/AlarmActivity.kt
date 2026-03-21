package com.yaish.naggy.presentation.alarm

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yaish.naggy.R
import com.yaish.naggy.alarm.AlarmScheduler
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.ui.theme.TodoAppTheme
import com.yaish.naggy.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val viewModel: AlarmViewModel by viewModels()
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) {
            finish()
            return
        }

        viewModel.loadTask(taskId)
        
        lifecycleScope.launch {
            settingsRepository.isVibrationEnabled.collect { enabled ->
                if (enabled) {
                    startVibration()
                } else {
                    stopVibration()
                }
            }
        }

        setContent {
            val isDarkThemePref by settingsRepository.isDarkTheme.collectAsState(initial = null)
            val isDarkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

            TodoAppTheme(darkTheme = isDarkTheme) {
                AlarmScreen(
                    viewModel = viewModel,
                    taskId = taskId,
                    onDismiss = { 
                        stopVibration()
                        finish() 
                    }
                )
            }
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 500, 500)
            val amplitudes = intArrayOf(0, 255, 0)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVibration()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    taskId: Long,
    onDismiss: () -> Unit
) {
    val task by viewModel.task.collectAsState()
    var showSnoozeDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        task?.let { currentTask ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = stringResource(R.string.reminder_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Task Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTask.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (currentTask.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentTask.description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Deadline info
                        val deadlineText = if (System.currentTimeMillis() > currentTask.deadlineTimestamp) {
                            stringResource(R.string.overdue_by, currentTask.getTimeUntilDeadline())
                        } else {
                            stringResource(R.string.due_at, currentTask.getFormattedDeadline())
                        }

                        Text(
                            text = deadlineText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mark Done Button
                    Button(
                        onClick = {
                            viewModel.markDone(taskId, onDismiss)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.mark_done),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Snooze Button
                    OutlinedButton(
                        onClick = { showSnoozeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.snooze),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Dismiss Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dismiss),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Snooze Dialog
            if (showSnoozeDialog) {
                SnoozeDialog(
                    onDismiss = { showSnoozeDialog = false },
                    onSnooze = { minutes ->
                        viewModel.snooze(taskId, minutes, onDismiss)
                        showSnoozeDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun SnoozeDialog(
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val snoozeOptions = listOf(
        5 to stringResource(R.string.snooze_5_min),
        10 to stringResource(R.string.snooze_10_min),
        15 to stringResource(R.string.snooze_15_min),
        30 to stringResource(R.string.snooze_30_min)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.snooze)) },
        text = {
            Column {
                for (option in snoozeOptions) {
                    TextButton(
                        onClick = { onSnooze(option.first) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = option.second,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
