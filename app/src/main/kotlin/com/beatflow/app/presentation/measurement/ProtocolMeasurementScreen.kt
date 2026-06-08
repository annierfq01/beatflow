package com.beatflow.app.presentation.measurement

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatflow.app.presentation.theme.BeatFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolMeasurementScreen(
    protocolTotalSecs: Int,
    inspirationSecs: Int = 5,
    expirationSecs: Int = 5,
    onNavigateToPatientForm: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val hrHistory by viewModel.hrHistory.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val breathingPhase by viewModel.breathingPhase.collectAsState()
    val phaseTimeLeft by viewModel.phaseTimeLeft.collectAsState()
    val protocolTimeLeft by viewModel.protocolTimeLeft.collectAsState()
    val protocolCompleted by viewModel.protocolCompleted.collectAsState()

    var showStopConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startSessionWithProtocol(protocolTotalSecs, inspirationSecs, expirationSecs)
    }

    LaunchedEffect(protocolCompleted) {
        if (protocolCompleted) {
            val sessionId = viewModel.stopSession()
            onNavigateToPatientForm(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protocolo de respiración") },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hrHistory.isNotEmpty()) {
                    val last = hrHistory.last()
                    HrValueCard(hr = last.hr, rr = last.rr.lastOrNull())
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (breathingPhase.isNotEmpty()) {
                    BreathingGuideCard(
                        phase = breathingPhase,
                        phaseTimeLeft = phaseTimeLeft,
                        protocolTimeLeft = protocolTimeLeft,
                        maxPhaseTime = if (breathingPhase.contains("INSPIRA", ignoreCase = true))
                            inspirationSecs else expirationSecs
                    )
                }
            }

            Button(
                onClick = { showStopConfirm = true },
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
            title = { Text("¿Detener medición?") },
            text = { Text("La sesión se guardará y podrás ingresar los datos del paciente.") },
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

@Composable
private fun BreathingGuideCard(
    phase: String,
    phaseTimeLeft: Int,
    protocolTimeLeft: Int,
    maxPhaseTime: Int
) {
    val isInhale = phase.contains("INSPIRA", ignoreCase = true)
    val circleColor = if (isInhale) BeatFlowColors.Primary else BeatFlowColors.HeartRed
    val circleSize by animateDpAsState(
        targetValue = if (isInhale) 140.dp else 100.dp,
        animationSpec = tween(500)
    )
    val phaseFraction = if (maxPhaseTime > 0) phaseTimeLeft.toFloat() / maxPhaseTime else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phase,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = circleColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$phaseTimeLeft s",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = circleColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { phaseFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = circleColor,
                trackColor = circleColor.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Protocolo: ${protocolTimeLeft / 60}:%02d".format(protocolTimeLeft % 60),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
