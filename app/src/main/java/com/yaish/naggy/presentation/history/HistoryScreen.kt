package com.yaish.naggy.presentation.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yaish.naggy.domain.model.Priority
import com.yaish.naggy.ui.components.GlassCard
import com.yaish.naggy.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val completedTasks by viewModel.completedTasks.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().matchParentSize().blur(10.dp)
                ) {}
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "TASK HISTORY",
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (completedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No completed tasks found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(completedTasks, key = { it.id }) { task ->
                        HistoryItem(
                            task = task,
                            onRestore = { viewModel.uncompleteTask(task.id) },
                            onDelete = { viewModel.deleteHistory(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    task: com.yaish.naggy.domain.model.Task,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary

    val completionDate = task.completedAt?.let {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))
    } ?: "Unknown date"

    GlassCard(
        borderColor = onBg.copy(alpha = 0.05f),
        onClick = { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark Icon
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = onBg.copy(alpha = 0.3f)
                )
                Text(
                    text = "Completed on $completionDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = primary.copy(alpha = 0.8f)
                )
            }

            // Priority indicator
            if (task.priority != Priority.NONE) {
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(
                            when (task.priority) {
                                Priority.HIGH -> Color(0xFFFF5252).copy(alpha = 0.3f)
                                Priority.MEDIUM -> Color(0xFFFFAB40).copy(alpha = 0.3f)
                                else -> primary.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                if (task.description.isNotBlank()) {
                    HorizontalDivider(color = onBg.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        task.description, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = onBg.copy(alpha = 0.5f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRestore) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESTORE")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252).copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
