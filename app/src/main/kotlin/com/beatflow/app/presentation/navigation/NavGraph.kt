package com.beatflow.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beatflow.app.presentation.importdata.ImportScreen
import com.beatflow.app.presentation.main.MainScreen
import com.beatflow.app.presentation.measurement.MeasurementScreen
import com.beatflow.app.presentation.measurement.ProtocolConfigScreen
import com.beatflow.app.presentation.measurement.ProtocolMeasurementScreen
import com.beatflow.app.presentation.measurement.ProtocolSelectScreen
import com.beatflow.app.presentation.patient.PatientFormScreen
import com.beatflow.app.presentation.report.ReportScreen

object Routes {
    const val MAIN = "main"
    const val MEASUREMENT = "measurement"
    const val PROTOCOL_SELECT = "protocol_select"
    const val PROTOCOL_CONFIG = "protocol_config/{protocolType}"
    const val PROTOCOL_MEASUREMENT = "protocol_measurement"
    const val PATIENT_FORM = "patient_form/{sessionId}"
    const val REPORT = "report/{sessionId}"
    const val IMPORT = "import"

    var pendingProtocolType = "basal"
    var pendingTotalSecs = 300
    var pendingInspirationSecs = 5
    var pendingExpirationSecs = 5
    var pendingStandUpSecs = 120

    fun protocolConfig(protocolType: String) = "protocol_config/$protocolType"
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
                onNavigateToProtocolSelect = {
                    navController.navigate(Routes.PROTOCOL_SELECT)
                },
                onNavigateToMeasurement = {
                    navController.navigate(Routes.MEASUREMENT)
                },
                onNavigateToImport = {
                    navController.navigate(Routes.IMPORT)
                }
            )
        }
        composable(Routes.PROTOCOL_SELECT) {
            ProtocolSelectScreen(
                onNavigateToConfig = { protocolType ->
                    navController.navigate(Routes.protocolConfig(protocolType))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PROTOCOL_CONFIG,
            arguments = listOf(navArgument("protocolType") { type = NavType.StringType })
        ) { backStackEntry ->
            val protocolType = backStackEntry.arguments?.getString("protocolType") ?: "basal"
            ProtocolConfigScreen(
                protocolType = protocolType,
                onNavigateToMeasurement = { totalSecs, inspSecs, expSecs, standUpSecs ->
                    Routes.pendingProtocolType = protocolType
                    Routes.pendingTotalSecs = totalSecs
                    Routes.pendingInspirationSecs = inspSecs
                    Routes.pendingExpirationSecs = expSecs
                    Routes.pendingStandUpSecs = standUpSecs
                    navController.navigate(Routes.PROTOCOL_MEASUREMENT)
                },
                onNavigateBack = { navController.popBackStack() }
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
        composable(Routes.PROTOCOL_MEASUREMENT) {
            val protocolType = remember { Routes.pendingProtocolType.also { Routes.pendingProtocolType = "basal" } }
            val totalSecs = remember { Routes.pendingTotalSecs.also { Routes.pendingTotalSecs = 300 } }
            val inspSecs = remember { Routes.pendingInspirationSecs.also { Routes.pendingInspirationSecs = 5 } }
            val expSecs = remember { Routes.pendingExpirationSecs.also { Routes.pendingExpirationSecs = 5 } }
            val standUpSecs = remember { Routes.pendingStandUpSecs.also { Routes.pendingStandUpSecs = 120 } }
            ProtocolMeasurementScreen(
                protocolType = protocolType,
                protocolTotalSecs = totalSecs,
                inspirationSecs = inspSecs,
                expirationSecs = expSecs,
                standUpSecs = standUpSecs,
                onNavigateToPatientForm = { sessionId ->
                    navController.navigate(Routes.patientForm(sessionId))
                },
                onNavigateBack = { navController.popBackStack() }
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
