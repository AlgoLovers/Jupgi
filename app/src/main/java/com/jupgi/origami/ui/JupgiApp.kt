package com.jupgi.origami.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jupgi.origami.presentation.library.LibraryScreen
import com.jupgi.origami.presentation.settings.SettingsScreen
import com.jupgi.origami.presentation.viewer.FoldViewerScreen
import com.jupgi.origami.presentation.viewer.FoldViewerViewModel

/** 내비게이션 루트: 라이브러리(홈) → 뷰어(작품별) · 설정. */
@Composable
fun JupgiApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
        composable(ROUTE_LIBRARY) {
            LibraryScreen(
                onOpenModel = { id -> navController.navigate("viewer/$id") },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(
            route = ROUTE_VIEWER,
            arguments = listOf(navArgument(FoldViewerViewModel.ARG_MODEL_ID) { type = NavType.StringType }),
        ) {
            FoldViewerScreen(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_VIEWER = "viewer/{modelId}"
