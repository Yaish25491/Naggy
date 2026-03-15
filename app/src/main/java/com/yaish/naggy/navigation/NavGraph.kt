package com.yaish.naggy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yaish.naggy.presentation.addedittask.AddEditTaskScreen
import com.yaish.naggy.presentation.calendar.CalendarScreen
import com.yaish.naggy.presentation.tasklist.TaskListScreen

sealed class Screen(val route: String) {
    object TaskList : Screen("task_list")
    object Calendar : Screen("calendar")
    object AddEditTask : Screen("add_edit_task?taskId={taskId}") {
        fun createRoute(taskId: Long? = null) = if (taskId != null) "add_edit_task?taskId=$taskId" else "add_edit_task"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TaskList.route
    ) {
        composable(Screen.TaskList.route) {
            TaskListScreen(
                onAddTask = {
                    navController.navigate(Screen.AddEditTask.createRoute())
                },
                onEditTask = { taskId ->
                    navController.navigate(Screen.AddEditTask.createRoute(taskId))
                },
                onCalendarClick = {
                    navController.navigate(Screen.Calendar.route)
                },
                onBackup = onBackup,
                onRestore = onRestore
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditTask = { taskId ->
                    navController.navigate(Screen.AddEditTask.createRoute(taskId))
                }
            )
        }

        composable(
            route = Screen.AddEditTask.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            AddEditTaskScreen(
                taskId = if (taskId != -1L) taskId else null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBackup = onBackup
            )
        }
    }
}
