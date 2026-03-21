package com.yaish.naggy.presentation.calendar

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yaish.naggy.R
import com.yaish.naggy.domain.model.Task
import com.yaish.naggy.presentation.tasklist.TaskItem
import com.yaish.naggy.ui.components.*
import com.yaish.naggy.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onEditTask: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val tasksByDate by viewModel.tasksByDate.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "CALENDAR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onBg)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = onBg
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            GlassCard {
                CalendarHeader(
                    currentMonth = currentMonth,
                    onMonthChange = { currentMonth = it }
                )
                
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    tasksByDate = tasksByDate
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "TASKS FOR ${selectedDate.dayOfMonth} ${selectedDate.month.name}",
                style = MaterialTheme.typography.labelLarge,
                color = primary,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val tasksForSelectedDate = tasksByDate[selectedDate] ?: emptyList()

            if (tasksForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks scheduled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tasksForSelectedDate) { task ->
                        TaskItem(
                            task = task,
                            onToggleComplete = { /* Handled in dashboard/list */ },
                            onDelete = { /* Handled in dashboard/list */ },
                            onEdit = { onEditTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = onBg)
        }
        
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onBg,
            letterSpacing = 1.sp
        )
        
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = onBg)
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    tasksByDate: Map<LocalDate, List<Task>>
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 
    val days = (1..daysInMonth).toList()
    val weekDays = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
    val onBg = MaterialTheme.colorScheme.onBackground

    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = onBg.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        var dayIndex = 0
        for (i in 0..5) { 
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0..6) {
                    val currentDayIndex = i * 7 + j
                    if (currentDayIndex < firstDayOfMonth || dayIndex >= daysInMonth) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val day = days[dayIndex]
                        val date = currentMonth.atDay(day)
                        val isSelected = date == selectedDate
                        val hasTasks = tasksByDate.containsKey(date)
                        
                        MinimalistDayCell(
                            day = day,
                            isSelected = isSelected,
                            hasTasks = hasTasks,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                        dayIndex++
                    }
                }
            }
            if (dayIndex >= daysInMonth) break
        }
    }
}

@Composable
fun MinimalistDayCell(
    day: Int,
    isSelected: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) primary.copy(alpha = 0.15f) 
                else Color.Transparent
            )
            .border(
                0.5.dp, 
                if (isSelected) primary else onBg.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) primary else onBg,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            if (hasTasks) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(if (isSelected) primary else onBg.copy(alpha = 0.3f), CircleShape)
                )
            }
        }
    }
}
