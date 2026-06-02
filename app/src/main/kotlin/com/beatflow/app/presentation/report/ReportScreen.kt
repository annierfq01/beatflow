package com.beatflow.app.presentation.report

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatflow.app.domain.model.HrvMetrics
import com.beatflow.app.domain.model.HrvSession
import com.beatflow.app.presentation.components.FrequencyDomainCard
import com.beatflow.app.presentation.components.NonLinearCard
import com.beatflow.app.presentation.components.RealtimeChart
import com.beatflow.app.presentation.components.TimeDomainCard
import com.beatflow.app.presentation.theme.BeatFlowColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    sessionId: Long,
    onNavigateHome: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportReportToUri(uri)
        }
    }

    LaunchedEffect(exportedFile) {
        exportedFile?.let {
            snackbarHostState.showSnackbar("Reporte guardado: ${it.name}")
        }
    }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Reporte HRV") },
                actions = {
                    IconButton(onClick = {
                        val filename = viewModel.getDefaultFilename()
                        saveLauncher.launch(filename)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateHome,
                containerColor = BeatFlowColors.HeartRed
            ) {
                Icon(Icons.Default.Home, contentDescription = "Inicio", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                session?.let { s ->
                    SessionSummaryCard(s)
                    Spacer(modifier = Modifier.height(12.dp))
                    PatientInfoCard(s)
                    Spacer(modifier = Modifier.height(12.dp))
                    metrics?.let { m ->
                        TimeDomainCard(m)
                        Spacer(modifier = Modifier.height(12.dp))
                        FrequencyDomainCard(m)
                        Spacer(modifier = Modifier.height(12.dp))
                        NonLinearCard(m)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    EcgReportCard(s)
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(session: HrvSession) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    MetricCard(
        title = "Resumen de la Sesión",
        icon = Icons.Default.Schedule
    ) {
        MetricRow("Fecha/Hora inicio", dateFormat.format(Date(session.startTime)))
        MetricRow("Fecha/Hora fin", dateFormat.format(Date(session.endTime)))
        MetricRow("Duración", formatDuration(session.durationMs))
        MetricRow("Latidos registrados", "${session.records.size}")
    }
}

@Composable
private fun PatientInfoCard(session: HrvSession) {
    MetricCard(
        title = "Datos del Paciente",
        icon = Icons.Default.Person
    ) {
        MetricRow("Nombre", "${session.patientData.nombre} ${session.patientData.apellidos}")
        MetricRow("Edad", "${session.patientData.edad} años")
        MetricRow("Sexo", session.patientData.sexo)
    }
}

@Composable
private fun MetricCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BeatFlowColors.HeartRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BeatFlowColors.HeartRed
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EcgReportCard(session: HrvSession) {
    val ecgValues = session.records.mapNotNull { it.ecgSignal?.toFloat() }

    MetricCard(
        title = "Señal ECG",
        icon = Icons.Default.ShowChart
    ) {
        Text(
            text = "${ecgValues.size} muestras registradas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (ecgValues.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                RealtimeChart(
                    data = ecgValues,
                    minValue = ecgValues.minOrNull() ?: -2f,
                    maxValue = ecgValues.maxOrNull() ?: 2f,
                    lineColor = BeatFlowColors.Primary,
                    gridColor = BeatFlowColors.ChartGrid,
                    title = "ECG (μV)"
                )
            }
        } else {
            Text(
                text = "No se registraron datos de ECG",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
