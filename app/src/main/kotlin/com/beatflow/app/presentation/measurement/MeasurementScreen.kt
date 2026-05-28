package com.beatflow.app.presentation.measurement

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onNavigateToPatientForm: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val hrHistory by viewModel.hrHistory.collectAsState()
    val rrIntervals by viewModel.rrIntervals.collectAsState()
    val sessionDuration by viewModel.sessionDuration.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    var showStopConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startSession()
    }

    LaunchedEffect(isRecording) {
        if (!isRecording && hrHistory.isNotEmpty()) {
            onNavigateToPatientForm(viewModel.stopSession().also {
                Log.d("Measurement", "Session stopped: $it")
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medición en curso") },
                navigationIcon = {
                    if (isRecording) {
                        TextButton(onClick = { showStopConfirm = true }) {
                            Text("Detener", color = BeatFlowColors.HeartRed)
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
                .padding(16.dp)
        ) {
            TimerCard(sessionDuration)

            Spacer(modifier = Modifier.height(16.dp))

            HrChart(hrHistory)

            Spacer(modifier = Modifier.height(12.dp))

            if (hrHistory.isNotEmpty()) {
                val last = hrHistory.last()
                HrValueCard(hr = last.hr, rr = last.rr.lastOrNull())
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showStopConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeatFlowColors.HeartRed
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DETENER MEDICIÓN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("¿Detener medición?") },
            text = { Text("La sesión de medición se detendrá y podrá ingresar los datos del paciente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirm = false
                        val sessionId = viewModel.stopSession()
                        onNavigateToPatientForm(sessionId)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%02d:%02d:%02d".format(hours, mins, secs),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BeatFlowColors.HeartRed
            )
            Text(
                text = "Tiempo de grabación",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HrChart(hrHistory: List<HrMeasurement>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setScaleEnabled(false)
                    setPinchZoom(false)
                    setDrawGridBackground(false)
                    xAxis.isEnabled = false
                    axisLeft.apply {
                        setDrawGridLines(true)
                        gridColor = BeatFlowColors.ChartGrid.toArgb()
                        textColor = android.graphics.Color.GRAY
                        setDrawLabels(true)
                    }
                    axisRight.isEnabled = false
                    setTouchEnabled(false)

                    val entries = hrHistory.mapIndexed { index, hr ->
                        Entry(index.toFloat(), hr.hr.toFloat())
                    }
                    if (entries.isNotEmpty()) {
                        val dataSet = LineDataSet(entries, "HR").apply {
                            color = BeatFlowColors.ChartLine.toArgb()
                            setCircleColor(BeatFlowColors.ChartLine.toArgb())
                            circleRadius = 2f
                            setDrawValues(false)
                            lineWidth = 2f
                            mode = LineDataSet.Mode.LINEAR
                            setDrawFilled(true)
                            fillColor = BeatFlowColors.ChartLine.toArgb()
                            fillAlpha = 30
                        }
                        data = LineData(dataSet)
                        notifyDataSetChanged()
                        invalidate()
                    }
                }
            },
            update = { chart ->
                val entries = hrHistory.mapIndexed { index, hr ->
                    Entry(index.toFloat(), hr.hr.toFloat())
                }
                if (entries.isNotEmpty()) {
                    val dataSet = LineDataSet(entries, "HR").apply {
                        color = BeatFlowColors.ChartLine.toArgb()
                        setCircleColor(BeatFlowColors.ChartLine.toArgb())
                        circleRadius = 2f
                        setDrawValues(false)
                        lineWidth = 2f
                        mode = LineDataSet.Mode.LINEAR
                        setDrawFilled(true)
                        fillColor = BeatFlowColors.ChartLine.toArgb()
                        fillAlpha = 30
                    }
                    chart.data = LineData(dataSet)
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                }
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = BeatFlowColors.HeartRed,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "$hr",
                    fontSize = 28.sp,
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
                        fontSize = 28.sp,
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
