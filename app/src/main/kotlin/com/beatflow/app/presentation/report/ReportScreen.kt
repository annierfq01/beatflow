package com.beatflow.app.presentation.report

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
                    IconButton(onClick = viewModel::exportReport) {
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
private fun TimeDomainCard(metrics: HrvMetrics) {
    MetricCard(
        title = "Dominio del Tiempo",
        icon = Icons.Default.Timer
    ) {
        MetricRow("Frecuencia cardíaca media", "%.1f BPM".format(metrics.meanHr))
        MetricRow("FC Máxima", "%.1f BPM".format(metrics.maxHr))
        MetricRow("FC Mínima", "%.1f BPM".format(metrics.minHr))
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        MetricRow("SDNN", "%.2f ms".format(metrics.sdnn))
        MetricRow("RMSSD", "%.2f ms".format(metrics.rmssd))
        MetricRow("pNN50", "%.2f %%".format(metrics.pnn50))
    }
}

@Composable
private fun FrequencyDomainCard(metrics: HrvMetrics) {
    MetricCard(
        title = "Dominio de la Frecuencia",
        icon = Icons.Default.Equalizer
    ) {
        MetricRow("LF (0.04-0.15 Hz)", "%.2f ms²".format(metrics.lf))
        MetricRow("HF (0.15-0.4 Hz)", "%.2f ms²".format(metrics.hf))
        MetricRow("Relación LF/HF", "%.2f".format(metrics.lfHfRatio))
    }
}

@Composable
private fun NonLinearCard(metrics: HrvMetrics) {
    MetricCard(
        title = "Métricas No Lineales",
        icon = Icons.Default.ScatterPlot
    ) {
        MetricRow("SD1 (Poincaré)", "%.2f ms".format(metrics.sd1))
        MetricRow("SD2 (Poincaré)", "%.2f ms".format(metrics.sd2))
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

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
