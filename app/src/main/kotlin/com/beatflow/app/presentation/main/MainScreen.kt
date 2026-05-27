package com.beatflow.app.presentation.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatflow.app.bluetooth.ConnectionState
import com.beatflow.app.presentation.theme.BeatFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToMeasurement: () -> Unit,
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val foundDevices by viewModel.foundDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var showDeviceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            snackbarHostState.showSnackbar("Conexión exitosa")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BeatFlowColors.HeartRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "BeatFlow",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = BeatFlowColors.HeartRed
            )

            Text(
                text = "Medidor de HRV",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            ConnectionStatusCard(connectionState)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (connectionState !is ConnectionState.Connected) {
                        showDeviceDialog = true
                        viewModel.startScan()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionState is ConnectionState.Connected)
                        BeatFlowColors.Success else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (connectionState is ConnectionState.Connected)
                        Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (connectionState is ConnectionState.Connected)
                        "CONECTADO" else "BUSCAR DISPOSITIVOS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToMeasurement,
                enabled = connectionState is ConnectionState.Connected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeatFlowColors.HeartRed,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "INICIAR MEDICIÓN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connectionState is ConnectionState.Connected)
                        MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeviceDialog = false
                viewModel.stopScan()
            },
            title = { Text("Dispositivos Polar") },
            text = {
                if (isScanning && foundDevices.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Buscando dispositivos…")
                    }
                } else if (foundDevices.isEmpty()) {
                    Text("Ningún dispositivo Polar encontrado")
                } else {
                    LazyColumn {
                        items(foundDevices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    viewModel.connectToDevice(device.deviceId)
                                    showDeviceDialog = false
                                    viewModel.stopScan()
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = BeatFlowColors.Primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = device.deviceId,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeviceDialog = false
                    viewModel.stopScan()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ConnectionStatusCard(state: ConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is ConnectionState.Connected -> BeatFlowColors.Success.copy(alpha = 0.1f)
                is ConnectionState.Connecting -> BeatFlowColors.Warning.copy(alpha = 0.1f)
                is ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            is ConnectionState.Connected -> BeatFlowColors.Success
                            is ConnectionState.Connecting -> BeatFlowColors.Warning
                            is ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when (state) {
                        is ConnectionState.Connected -> "Conectado"
                        is ConnectionState.Connecting -> "Conectando…"
                        is ConnectionState.Disconnected -> "Desconectado"
                    },
                    fontWeight = FontWeight.Medium
                )
                if (state is ConnectionState.Connected) {
                    Text(
                        text = state.deviceId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = when (state) {
                    is ConnectionState.Connected -> Icons.Default.BluetoothConnected
                    is ConnectionState.Connecting -> Icons.Default.BluetoothSearching
                    is ConnectionState.Disconnected -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                tint = when (state) {
                    is ConnectionState.Connected -> BeatFlowColors.Success
                    is ConnectionState.Connecting -> BeatFlowColors.Warning
                    is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
