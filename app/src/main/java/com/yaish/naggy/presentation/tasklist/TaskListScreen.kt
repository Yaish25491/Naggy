package com.yaish.naggy.presentation.tasklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yaish.naggy.R
import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.domain.model.RecurrencePattern
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.domain.model.TaskStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onCalendarClick: () -> Unit,
    onDashboardClick: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val categorizedTasks by viewModel.categorizedTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onTriggerBackup = onBackup
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    // App Logo in Drawer
                    Image(
                        painter = painterResource(id = R.mipmap.naggy_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "User Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    userData?.let { user ->
                        Text(text = user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } ?: Text(text = "Not signed in", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Backup Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val formattedTime = if (lastBackupTime > 0) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(lastBackupTime))
                    } else {
                        "Never"
                    }
                    Text(text = "Last auto backup: $formattedTime", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Productivity Dashboard") },
                        selected = false,
                        onClick = {
                            onDashboardClick()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.calendar_view)) },
                        selected = false,
                        onClick = {
                            onCalendarClick()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        label = { Text(if (isVibrationEnabled) "Vibration: ON" else "Vibration: OFF") },
                        selected = false,
                        onClick = {
                            viewModel.setVibrationEnabled(!isVibrationEnabled)
                        },
                        icon = { 
                            Icon(
                                imageVector = if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.NotificationsOff, 
                                contentDescription = null,
                                tint = if (isVibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val isDark = isDarkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()
                    NavigationDrawerItem(
                        label = { Text(if (isDark) "Light Mode" else "Dark Mode") },
                        selected = false,
                        onClick = {
                            viewModel.setDarkTheme(!isDark)
                            scope.launch { drawerState.close() }
                        },
                        icon = { 
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, 
                                contentDescription = null
                            ) 
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            onBackup()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manual Backup")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            onRestore()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore")
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.task_list_title)) },
                    actions = {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                for (option in TaskSortOption.values()) {
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            viewModel.updateSortOption(option)
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (sortOption == option) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            val isFiltered = filterState.priority != null || filterState.tags.isNotEmpty()
                            BadgedBox(
                                badge = {
                                    if (isFiltered) {
                                        Badge { Text("!") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                        }
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_task)
                    )
                }
            }
        ) { paddingValues ->
            if (isLoading && categorizedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (categorizedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_tasks),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val categoriesToShow = listOf(
                        TaskCategory.OVERDUE,
                        TaskCategory.TODAY,
                        TaskCategory.THIS_WEEK,
                        TaskCategory.THIS_MONTH,
                        TaskCategory.LATER,
                        TaskCategory.COMPLETED
                    )

                    categoriesToShow.forEach { category ->
                        val tasksInCategory = categorizedTasks[category] ?: emptyList()
                        if (tasksInCategory.isNotEmpty()) {
                            item(key = category.name) {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (category == TaskCategory.OVERDUE) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
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
                }
            }
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                filterState = filterState,
                availableTags = availableTags,
                onUpdatePriority = { viewModel.updateFilterPriority(it) },
                onToggleTag = { viewModel.toggleFilterTag(it) },
                onClearAll = { viewModel.clearFilters() },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filterState: TaskFilterState,
    availableTags: List<String>,
    onUpdatePriority: (Priority?) -> Unit,
    onToggleTag: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }

            // Priority Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (priority in Priority.values()) {
                        FilterChip(
                            selected = filterState.priority == priority,
                            onClick = { 
                                if (filterState.priority == priority) onUpdatePriority(null)
                                else onUpdatePriority(priority)
                            },
                            label = { Text(priority.name) }
                        )
                    }
                }
            }

            // Tags Filter
            if (availableTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            FilterChip(
                                selected = filterState.tags.contains(tag),
                                onClick = { onToggleTag(tag) },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Results")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val status = task.getStatus()
    val statusColor = when (status) {
        TaskStatus.UPCOMING -> Color(0xFF4CAF50)
        TaskStatus.OVERDUE -> Color(0xFFF44336)
        TaskStatus.COMPLETED -> Color(0xFF9E9E9E)
    }

    val statusText = when (status) {
        TaskStatus.UPCOMING -> stringResource(R.string.status_upcoming)
        TaskStatus.OVERDUE -> stringResource(R.string.status_overdue)
        TaskStatus.COMPLETED -> stringResource(R.string.status_completed)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completion checkbox
                IconButton(
                    onClick = { 
                        onToggleComplete()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.Circle
                        },
                        contentDescription = stringResource(R.string.mark_complete),
                        tint = if (task.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Task summary details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.priority != Priority.NONE) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (task.priority) {
                                            Priority.HIGH -> Color.Red
                                            Priority.MEDIUM -> Color(0xFFFFA500) // Orange
                                            Priority.LOW -> Color.Blue
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            }
                        )
                        
                        if (task.recurrencePattern != RecurrencePattern.NONE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Recurring",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = task.getFormattedDeadline(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (task.tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            task.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Countdown display at the bottom of the summary
            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.getTimeUntilDeadline(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == TaskStatus.OVERDUE) MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 44.dp) // Align with text after checkbox
                )
            }

            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    if (task.description.isNotBlank()) {
                        Text(
                            text = "Description:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "Reminder details:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Notifies ${task.reminderLeadTimeMinutes / 60}h ${task.reminderLeadTimeMinutes % 60}m before, at ${task.reminderTimeOfDay}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Edit button
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Delete button
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
