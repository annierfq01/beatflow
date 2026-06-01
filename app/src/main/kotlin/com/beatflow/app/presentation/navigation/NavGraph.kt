package com.beatflow.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beatflow.app.presentation.importdata.ImportScreen
import com.beatflow.app.presentation.main.MainScreen
import com.beatflow.app.presentation.measurement.MeasurementScreen
import com.beatflow.app.presentation.patient.PatientFormScreen
import com.beatflow.app.presentation.report.ReportScreen

object Routes {
    const val MAIN = "main"
    const val MEASUREMENT = "measurement"
    const val PATIENT_FORM = "patient_form/{sessionId}"
    const val REPORT = "report/{sessionId}"
    const val IMPORT = "import"

    fun patientForm(sessionId: Long) = "patient_form/$sessionId"
    fun report(sessionId: Long) = "report/$sessionId"
}

@Composable
fun BeatFlowNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToMeasurement = {
                    navController.navigate(Routes.MEASUREMENT)
                },
                onNavigateToImport = {
                    navController.navigate(Routes.IMPORT)
                }
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(
                onNavigateHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MEASUREMENT) {
            MeasurementScreen(
                onNavigateToPatientForm = { sessionId ->
                    navController.navigate(Routes.patientForm(sessionId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PATIENT_FORM,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            PatientFormScreen(
                sessionId = sessionId,
                onNavigateToReport = { sid ->
                    navController.navigate(Routes.report(sid)) {
                        popUpTo(Routes.MAIN)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            ReportScreen(
                sessionId = sessionId,
                onNavigateHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }
    }
}
