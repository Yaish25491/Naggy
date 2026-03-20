package com.yaish.naggy.presentation.addedittask

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yaish.naggy.R
import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.RecurrencePattern
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

import com.yaish.naggy.domain.model.Task

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    LaunchedEffect(Unit) {
        viewModel.onTriggerBackup = onBackup
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (taskId == null) stringResource(R.string.add_task_title) 
                        else stringResource(R.string.edit_task_title)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(stringResource(R.string.task_title)) },
                placeholder = { Text(stringResource(R.string.task_title_hint)) },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text(stringResource(R.string.task_description)) },
                placeholder = { Text(stringResource(R.string.task_description_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Priority
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (priority in Priority.values()) {
                        FilterChip(
                            selected = uiState.priority == priority,
                            onClick = { viewModel.updatePriority(priority) },
                            label = { Text(priority.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (priority) {
                                    Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                                    Priority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                                    Priority.LOW -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        )
                    }
                }
            }

            // Tags
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                var tagInput by remember { mutableStateOf("") }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("Add tag...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank()) {
                                viewModel.addTag(tagInput.trim())
                                tagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Deadline Date
            val deadlineText = if (uiState.deadlineTimestamp != null) {
                val dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(uiState.deadlineTimestamp!!),
                    ZoneId.systemDefault()
                )
                dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
            } else {
                "Select deadline"
            }

            OutlinedTextField(
                value = deadlineText,
                onValueChange = {},
                label = { Text(stringResource(R.string.deadline_date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDateTimePicker(context) { timestamp ->
                            viewModel.updateDeadlineTimestamp(timestamp)
                        }
                    },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Reminder Lead Time
            var expandedLeadTime by remember { mutableStateOf(false) }
            var showCustomLeadTimeDialog by remember { mutableStateOf(false) }
            val leadTimeOptions = mapOf(
                15 to "15 minutes before",
                30 to "30 minutes before",
                60 to "1 hour before",
                120 to "2 hours before",
                1440 to "1 day before",
                2880 to "2 days before",
                10080 to "1 week before"
            )

            val currentLeadTimeText = leadTimeOptions[uiState.reminderLeadTimeMinutes] 
                ?: "${uiState.reminderLeadTimeMinutes} minutes before"

            ExposedDropdownMenuBox(
                expanded = expandedLeadTime,
                onExpandedChange = { expandedLeadTime = it }
            ) {
                OutlinedTextField(
                    value = currentLeadTimeText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.reminder_lead_time)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLeadTime) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedLeadTime,
                    onDismissRequest = { expandedLeadTime = false }
                ) {
                    leadTimeOptions.forEach { (minutes, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.updateReminderLeadTime(minutes)
                                expandedLeadTime = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Custom...") },
                        onClick = {
                            expandedLeadTime = false
                            showCustomLeadTimeDialog = true
                        }
                    )
                }
            }

            if (showCustomLeadTimeDialog) {
                var customValue by remember { mutableStateOf("") }
                var customUnit by remember { mutableStateOf(1) } // 1=min, 60=hour, 1440=day
                
                AlertDialog(
                    onDismissRequest = { showCustomLeadTimeDialog = false },
                    title = { Text("Custom Lead Time") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = customValue,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) customValue = it },
                                label = { Text("Amount") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FilterChip(
                                    selected = customUnit == 1,
                                    onClick = { customUnit = 1 },
                                    label = { Text("Mins") }
                                )
                                FilterChip(
                                    selected = customUnit == 60,
                                    onClick = { customUnit = 60 },
                                    label = { Text("Hours") }
                                )
                                FilterChip(
                                    selected = customUnit == 1440,
                                    onClick = { customUnit = 1440 },
                                    label = { Text("Days") }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val amount = customValue.toIntOrNull() ?: 0
                            if (amount > 0) {
                                viewModel.updateReminderLeadTime(amount * customUnit)
                            }
                            showCustomLeadTimeDialog = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomLeadTimeDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Reminder Time of Day
            OutlinedTextField(
                value = uiState.reminderTimeOfDay,
                onValueChange = {},
                label = { Text(stringResource(R.string.reminder_time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showTimePicker(context, uiState.reminderTimeOfDay) { time ->
                            viewModel.updateReminderTimeOfDay(time)
                        }
                    },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Google Calendar Sync Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { syncToCalendar = !syncToCalendar }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = syncToCalendar,
                    onCheckedChange = { syncToCalendar = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Google Calendar")
            }

            // Recurrence
            var expandedRecurrence by remember { mutableStateOf(false) }
            val recurrenceOptions = RecurrencePattern.values()

            ExposedDropdownMenuBox(
                expanded = expandedRecurrence,
                onExpandedChange = { expandedRecurrence = it }
            ) {
                OutlinedTextField(
                    value = uiState.recurrencePattern.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecurrence) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedRecurrence,
                    onDismissRequest = { expandedRecurrence = false }
                ) {
                    for (pattern in recurrenceOptions) {
                        DropdownMenuItem(
                            text = { Text(pattern.name) },
                            onClick = {
                                viewModel.updateRecurrence(pattern)
                                expandedRecurrence = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.saveTask { savedTask ->
                        if (syncToCalendar) {
                            onSyncToCalendar(savedTask)
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save_task))
                }
            }
        }
    }
}

private fun showDateTimePicker(context: android.content.Context, onSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selectedCalendar.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(context: android.content.Context, currentTime: String, onSelected: (String) -> Unit) {
    val timeParts = currentTime.split(":")
    val hour = timeParts[0].toInt()
    val minute = timeParts[1].toInt()

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onSelected(formattedTime)
        },
        hour,
        minute,
        true
    ).show()
}
