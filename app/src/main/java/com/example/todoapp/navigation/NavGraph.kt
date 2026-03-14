package com.example.todoapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.todoapp.presentation.addedittask.AddEditTaskScreen
import com.example.todoapp.presentation.tasklist.TaskListScreen

sealed class Screen(val route: String) {
    object TaskList : Screen("task_list")
    object AddEditTask : Screen("add_edit_task")
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
                    navController.navigate(Screen.AddEditTask.route)
                },
                onBackup = onBackup,
                onRestore = onRestore
            )
        }

        composable(Screen.AddEditTask.route) {
            AddEditTaskScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBackup = onBackup
            )
        }
    }
}
