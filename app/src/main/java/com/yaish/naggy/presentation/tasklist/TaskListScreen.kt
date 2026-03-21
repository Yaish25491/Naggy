package com.yaish.naggy.presentation.tasklist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.yaish.naggy.R
import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.ui.components.*
import com.yaish.naggy.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onCalendarClick: () -> Unit,
    onDashboardClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val categorizedTasks by viewModel.categorizedTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    var showUserDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Dock items in requested order: (dashboard, calender, + , backup, history)
    // Note: User said "instead of the settings button in the menu, add a history button"
    val dockItems = listOf(
        DockItem("Dashboard", Icons.Default.GridView),
        DockItem("Calendar", Icons.Default.CalendarToday),
        DockItem("Add", Icons.Default.Add), // At index 2 (middle of 5)
        DockItem("Backup", Icons.Default.CloudQueue),
        DockItem("History", Icons.Default.Check) // History button (nice V sign)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MinimalistTopBar(
                userName = userData?.name ?: "User",
                onProfileClick = { showUserDialog = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        },
        bottomBar = {
            MinimalistDock(
                items = dockItems,
                selectedItem = -1,
                onItemClick = { index ->
                    when (index) {
                        0 -> onDashboardClick()
                        1 -> onCalendarClick()
                        2 -> onAddTask()
                        3 -> showBackupDialog = true
                        4 -> onHistoryClick()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && categorizedTasks.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Removed COMPLETED category from categorizedTasks in ViewModel already,
                    // but ensuring it's not in categoriesToShow here for UI consistency.
                    val categoriesToShow = listOf(
                        TaskCategory.OVERDUE,
                        TaskCategory.TODAY,
                        TaskCategory.THIS_WEEK,
                        TaskCategory.THIS_MONTH,
                        TaskCategory.LATER
                    )

                    categoriesToShow.forEach { category ->
                        val tasksInCategory = categorizedTasks[category] ?: emptyList()
                        if (tasksInCategory.isNotEmpty()) {
                            item(key = category.name) {
                                Text(
                                    text = category.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (category == TaskCategory.OVERDUE) Color(0xFFFF5252) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    letterSpacing = 1.5.sp
                                )
                            }
                            
                            items(tasksInCategory, key = { it.id }) { task ->
                                TaskItem(
                                    task = task,
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task.id, task.isCompleted) },
                                    onDelete = { viewModel.deleteTask(task.id) },
                                    onEdit = { onEditTask(task.id) }
                                )
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showUserDialog) {
        MinimalistDialog(
            title = "USER ACCOUNT",
            onDismiss = { showUserDialog = false }
        ) {
            userData?.let { user ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(user.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(user.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } ?: Text("Not signed in", color = MaterialTheme.colorScheme.onBackground)
        }
    }

    if (showSettingsDialog) {
        MinimalistDialog(
            title = "SETTINGS",
            onDismiss = { showSettingsDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val isDark = isDarkTheme ?: isSystemInDarkTheme()
                SettingsToggleItem(
                    label = "Dark Mode",
                    checked = isDark,
                    onCheckedChange = { viewModel.setDarkTheme(it) },
                    icon = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode
                )
                
                SettingsToggleItem(
                    label = "Haptic Vibration",
                    checked = isVibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) },
                    icon = Icons.Default.Vibration
                )
            }
        }
    }

    if (showBackupDialog) {
        MinimalistDialog(
            title = "DATA MANAGEMENT",
            onDismiss = { showBackupDialog = false }
        ) {
            val onBg = MaterialTheme.colorScheme.onBackground
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Last Backup: ${if (lastBackupTime > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastBackupTime)) else "Never"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
                
                Button(
                    onClick = { onBackup(); showBackupDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BACKUP NOW", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onRestore(); showBackupDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, onBg.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = onBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTORE DATA", color = onBg)
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    val accentColor = when (task.priority) {
        Priority.HIGH -> Color(0xFFFF5252)
        Priority.MEDIUM -> Color(0xFFFFAB40)
        Priority.LOW -> primary
        else -> Color.Transparent
    }

    GlassCard(
        borderColor = if (task.isCompleted) onBg.copy(alpha = 0.05f) else accentColor.copy(alpha = 0.2f),
        onClick = { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onToggleComplete,
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (task.isCompleted) primary else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (task.isCompleted) primary else onBg.copy(alpha = 0.2f))
            ) {
                if (task.isCompleted) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color = if (task.isCompleted) onBg.copy(alpha = 0.3f) else onBg
                )
                Text(
                    text = task.getFormattedDeadline(),
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
            }

            if (task.priority != Priority.NONE && !task.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodyMedium, color = onBg.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = onBg.copy(alpha = 0.4f)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252).copy(alpha = 0.6f)) }
                }
            }
        }
    }
}

@Composable
fun MinimalistDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(24.dp))
                content()
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = onBg.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = if (checked) primary else onBg.copy(alpha = 0.4f), 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label, 
                    color = onBg, 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primary,
                    uncheckedThumbColor = onBg.copy(alpha = 0.4f),
                    uncheckedTrackColor = onBg.copy(alpha = 0.1f),
                    uncheckedBorderColor = onBg.copy(alpha = 0.2f)
                )
            )
        }
    }
}
