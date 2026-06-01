package com.beatflow.app.presentation.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatflow.app.bluetooth.HrMeasurement
import com.beatflow.app.presentation.theme.BeatFlowColors
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

enum class ChartType(val label: String) {
    ECG("ECG"),
    HR("Frecuencia Cardíaca"),
    RR("Intervalos RR")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onNavigateToPatientForm: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val hrHistory by viewModel.hrHistory.collectAsState()
    val rrIntervals by viewModel.rrIntervals.collectAsState()
    val ecgBuffer by viewModel.ecgBuffer.collectAsState()
    val sessionDuration by viewModel.sessionDuration.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    var showStopConfirm by remember { mutableStateOf(false) }
    var stopAction by remember { mutableStateOf<String?>(null) }
    var selectedChart by remember { mutableStateOf(ChartType.ECG) }

    LaunchedEffect(Unit) {
        viewModel.startSession()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medición en curso") },
                navigationIcon = {
                    if (isRecording) {
                        TextButton(onClick = {
                            stopAction = "CANCEL"
                            showStopConfirm = true
                        }) {
                            Text("Cancelar", color = BeatFlowColors.HeartRed)
                        }
                    }
                },
                actions = {
                    if (batteryLevel >= 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.BatteryFull,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (batteryLevel > 20) Color(0xFF4CAF50) else Color(0xFFFF5722)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$batteryLevel%",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TimerCard(sessionDuration)

            Spacer(modifier = Modifier.height(8.dp))

            ChartSelector(selectedChart) { selectedChart = it }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedChart) {
                    ChartType.ECG -> {
                        if (ecgBuffer.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Esperando datos de ECG…\nConecta el sensor Polar H10",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            EcgChart(ecgBuffer)
                        }
                    }
                    ChartType.HR -> {
                        if (hrHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Esperando datos de frecuencia cardíaca…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            HrChart(hrHistory)
                        }
                    }
                    ChartType.RR -> {
                        if (rrIntervals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Esperando intervalos RR…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            RrChart(rrIntervals)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (hrHistory.isNotEmpty()) {
                val last = hrHistory.last()
                HrValueCard(hr = last.hr, rr = last.rr.lastOrNull())
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    stopAction = "STOP"
                    showStopConfirm = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeatFlowColors.HeartRed
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DETENER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = {
                Text(if (stopAction == "CANCEL") "¿Cancelar medición?" else "¿Detener medición?")
            },
            text = {
                Text(
                    if (stopAction == "CANCEL") "Volverás al inicio."
                    else "La sesión se guardará y podrás ingresar los datos del paciente."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirm = false
                        val sessionId = viewModel.stopSession()
                        when (stopAction) {
                            "CANCEL" -> onNavigateBack()
                            else -> onNavigateToPatientForm(sessionId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BeatFlowColors.HeartRed)
                ) { Text("DETENER") }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) {
                    Text("CONTINUAR")
                }
            }
        )
    }
}

@Composable
private fun ChartSelector(selected: ChartType, onSelect: (ChartType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChartType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.label, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = when (type) {
                            ChartType.ECG -> Icons.Default.ShowChart
                            ChartType.HR -> Icons.Default.Favorite
                            ChartType.RR -> Icons.Default.Accessibility
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun TimerCard(durationMs: Long) {
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    val hours = minutes / 60
    val mins = minutes % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "%02d:%02d:%02d".format(hours, mins, secs),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BeatFlowColors.HeartRed
            )
        }
    }
}

@Composable
private fun HrChart(hrHistory: List<HrMeasurement>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LineChart(ctx).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setScaleEnabled(false)
                    setPinchZoom(false)
                    setDrawGridBackground(false)
                    xAxis.isEnabled = false
                    axisLeft.apply {
                        axisMinimum = 30f
                        axisMaximum = 220f
                        setDrawGridLines(true)
                        gridColor = BeatFlowColors.ChartGrid.toArgb()
                        textColor = android.graphics.Color.GRAY
                        setDrawLabels(true)
                    }
                    axisRight.isEnabled = false
                    setTouchEnabled(false)
                }
            },
            update = { chart ->
                val windowSize = MeasurementViewModel.HR_CHART_SIZE
                val data = hrHistory.takeLast(windowSize)
                if (data.isEmpty()) return@update
                val values = data.map { it.hr.toFloat() }
                val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = (windowSize - 1).toFloat()
                val dataSet = LineDataSet(entries, "HR").apply {
                    color = BeatFlowColors.ChartLine.toArgb()
                    setCircleColor(BeatFlowColors.ChartLine.toArgb())
                    circleRadius = 3f
                    setDrawValues(false)
                    lineWidth = 3f
                    mode = LineDataSet.Mode.LINEAR
                    setDrawFilled(true)
                    fillColor = BeatFlowColors.ChartLine.toArgb()
                    fillAlpha = 25
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        )
    }
}

@Composable
private fun RrChart(rrIntervals: List<Double>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LineChart(ctx).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setScaleEnabled(false)
                    setPinchZoom(false)
                    setDrawGridBackground(false)
                    xAxis.isEnabled = false
                    axisLeft.apply {
                        axisMinimum = 200f
                        axisMaximum = 1500f
                        setDrawGridLines(true)
                        gridColor = BeatFlowColors.ChartGrid.toArgb()
                        textColor = android.graphics.Color.GRAY
                        setDrawLabels(true)
                    }
                    axisRight.isEnabled = false
                    setTouchEnabled(false)
                }
            },
            update = { chart ->
                val windowSize = MeasurementViewModel.RR_CHART_SIZE
                val data = rrIntervals.takeLast(windowSize)
                if (data.isEmpty()) return@update
                val values = data.map { it.toFloat() }
                val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = (windowSize - 1).toFloat()
                val dataSet = LineDataSet(entries, "RR").apply {
                    color = BeatFlowColors.ChartLine.toArgb()
                    setCircleColor(BeatFlowColors.ChartLine.toArgb())
                    circleRadius = 3f
                    setDrawValues(false)
                    lineWidth = 3f
                    mode = LineDataSet.Mode.LINEAR
                    setDrawFilled(true)
                    fillColor = BeatFlowColors.ChartLine.toArgb()
                    fillAlpha = 25
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        )
    }
}

@Composable
private fun EcgChart(ecgSamples: List<Double>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LineChart(ctx).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setScaleEnabled(false)
                    setPinchZoom(false)
                    setDrawGridBackground(false)
                    xAxis.apply {
                        setDrawGridLines(true)
                        gridColor = BeatFlowColors.ChartGrid.toArgb()
                        textColor = android.graphics.Color.GRAY
                        setDrawLabels(true)
                        setLabelCount(5, true)
                    }
                    axisLeft.apply {
                        axisMinimum = -2f
                        axisMaximum = 2f
                        setDrawGridLines(true)
                        gridColor = BeatFlowColors.ChartGrid.toArgb()
                        textColor = android.graphics.Color.GRAY
                    }
                    axisRight.isEnabled = false
                }
            },
            update = { chart ->
                val windowSize = MeasurementViewModel.ECG_CHART_SIZE
                val data = ecgSamples.takeLast(windowSize)
                if (data.isEmpty()) return@update
                val values = data.map { it.toFloat() }
                val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = (windowSize - 1).toFloat()
                val dataSet = LineDataSet(entries, "ECG").apply {
                    color = BeatFlowColors.Primary.toArgb()
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 1.5f
                    mode = LineDataSet.Mode.LINEAR
                }
                chart.data = LineData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        )
    }
}

@Composable
private fun HrValueCard(hr: Int, rr: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = BeatFlowColors.HeartRed,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "$hr",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeatFlowColors.HeartRed
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (rr != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.0f".format(rr),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RR (ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
