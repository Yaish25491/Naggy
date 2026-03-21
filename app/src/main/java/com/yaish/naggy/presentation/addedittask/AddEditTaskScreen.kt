package com.yaish.naggy.presentation.addedittask

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yaish.naggy.R
import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.RecurrencePattern
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.ui.components.*
import com.yaish.naggy.ui.theme.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: Long? = null,
    onNavigateBack: () -> Unit,
    onBackup: () -> Unit = {},
    onSyncToCalendar: (Task) -> Unit = {},
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var syncToCalendar by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.loadTask(taskId)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (taskId == null) "NEW TASK" else "EDIT TASK",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveTask { savedTask ->
                                if (syncToCalendar) {
                                    onSyncToCalendar(savedTask)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("SAVE", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Task Info
            GlassCard {
                Text(
                    text = "TASK DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = { Text("Enter title...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                TextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    placeholder = { Text("Add notes...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    minLines = 2
                )
            }

            // Priority Selection
            Column {
                Text(
                    text = "PRIORITY LEVEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (priority in Priority.values()) {
                        val isSelected = uiState.priority == priority
                        val color = when (priority) {
                            Priority.HIGH -> Color(0xFFFF5252)
                            Priority.MEDIUM -> Color(0xFFFFAB40)
                            Priority.LOW -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .border(0.5.dp, if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.updatePriority(priority) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priority.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Reminders
            GlassCard {
                Text(
                    text = "SCHEDULING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                SettingsHUDItem(
                    title = "DEADLINE",
                    value = if (uiState.deadlineTimestamp != null) {
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(uiState.deadlineTimestamp!!), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                    } else "NOT SET",
                    icon = Icons.Default.Timer,
                    onClick = {
                        showDateTimePicker(context) { timestamp ->
                            viewModel.updateDeadlineTimestamp(timestamp)
                        }
                    }
                )

                var expandedLeadTime by remember { mutableStateOf(false) }
                SettingsHUDItem(
                    title = "REMINDER",
                    value = "${uiState.reminderLeadTimeMinutes}M PRE",
                    icon = Icons.Default.NotificationsActive,
                    onClick = { expandedLeadTime = true }
                )
                
                var expandedRecurrence by remember { mutableStateOf(false) }
                SettingsHUDItem(
                    title = "RECURRENCE",
                    value = uiState.recurrencePattern.name,
                    icon = Icons.Default.Sync,
                    onClick = { expandedRecurrence = true },
                    isLast = true
                )

                DropdownMenu(
                    expanded = expandedLeadTime,
                    onDismissRequest = { expandedLeadTime = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf(15, 30, 60, 1440).forEach { mins ->
                        DropdownMenuItem(
                            text = { Text("${mins}M BEFORE", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.updateReminderLeadTime(mins)
                                expandedLeadTime = false
                            }
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedRecurrence,
                    onDismissRequest = { expandedRecurrence = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    for (pattern in RecurrencePattern.values()) {
                        DropdownMenuItem(
                            text = { Text(pattern.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.updateRecurrence(pattern)
                                expandedRecurrence = false
                            }
                        )
                    }
                }
            }

            // Sync
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, null, tint = if (syncToCalendar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "CALENDAR SYNC",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = syncToCalendar,
                        onCheckedChange = { syncToCalendar = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsHUDItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLast: Boolean = false
) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        }
        if (!isLast) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        }
    }
}

private fun showDateTimePicker(context: android.content.Context, onSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, dayOfMonth ->
        TimePickerDialog(context, { _, hourOfDay, minute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, hourOfDay, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onSelected(selectedCalendar.timeInMillis)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
}
