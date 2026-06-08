package com.beatflow.app.presentation.main

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import com.beatflow.app.bluetooth.ConnectionState
import com.beatflow.app.presentation.navigation.Routes as AppRoutes
import com.beatflow.app.presentation.theme.BeatFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToMeasurement: () -> Unit,
    onNavigateToImport: () -> Unit = {},
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val foundDevices by viewModel.foundDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val isLocationEnabled by viewModel.isLocationEnabled.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()

    val context = LocalContext.current

    var showDeviceDialog by remember { mutableStateOf(false) }
    var showBtDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showProtocolDialog by remember { mutableStateOf(false) }
    var protocolTotal by remember { mutableIntStateOf(5) }
    var protocolInsp by remember { mutableIntStateOf(5) }
    var protocolExp by remember { mutableIntStateOf(5) }
    var isConnecting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ConnectionState.Connected -> {
                isConnecting = false
                showDeviceDialog = false
                snackbarHostState.showSnackbar("✓ Conectado a ${(connectionState as ConnectionState.Connected).deviceId}")
            }
            is ConnectionState.ConnectionFailed -> {
                isConnecting = false
                val err = connectionState as ConnectionState.ConnectionFailed
                snackbarHostState.showSnackbar("✗ ${err.message}")
            }
            is ConnectionState.Connecting -> {
                isConnecting = true
            }
            else -> {}
        }
    }

    LaunchedEffect(scanMessage) {
        when (scanMessage) {
            "BLUETOOTH_OFF" -> showBtDialog = true
            "LOCATION_OFF" -> showLocationDialog = true
            "NO_DEVICES" -> {
                snackbarHostState.showSnackbar(
                    "No se encontraron dispositivos Polar. Verifica que el sensor esté encendido."
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                            viewModel.dismissMessage()
                            if (!isBluetoothEnabled) {
                                showBtDialog = true
                            } else if (!isLocationEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                showLocationDialog = true
                            } else {
                                showDeviceDialog = true
                                viewModel.startScan()
                            }
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
                        imageVector = when (connectionState) {
                            is ConnectionState.Connected -> Icons.Default.BluetoothConnected
                            is ConnectionState.Connecting -> Icons.Default.BluetoothSearching
                            else -> Icons.Default.Bluetooth
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> "CONECTADO"
                            is ConnectionState.Connecting -> "CONECTANDO…"
                            is ConnectionState.ConnectionFailed -> "RECONECTAR"
                            else -> "BUSCAR DISPOSITIVOS"
                        },
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showProtocolDialog = true },
                    enabled = connectionState is ConnectionState.Connected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "INICIAR PROTOCOLO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connectionState is ConnectionState.Connected)
                            BeatFlowColors.Primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 4.dp)
        ) {
            IconButton(
                onClick = onNavigateToImport,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "Importar datos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Acerca de",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            title = {
                Text(if (isConnecting) "Conectando…" else "Dispositivos Polar")
            },
            text = {
                if (isConnecting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Conectando con el dispositivo…")
                    }
                } else if (isScanning && foundDevices.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Buscando dispositivos…")
                    }
                } else if (foundDevices.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ningún dispositivo Polar encontrado")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            viewModel.dismissMessage()
                            viewModel.startScan()
                        }) {
                            Text("REINTENTAR")
                        }
                    }
                } else {
                    LazyColumn {
                        items(foundDevices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    viewModel.stopScan()
                                    viewModel.connectToDevice(device.deviceId)
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
                if (isConnecting) {
                    TextButton(onClick = {
                        isConnecting = false
                        viewModel.dismissConnectionError()
                        showDeviceDialog = false
                    }) {
                        Text("Cancelar")
                    }
                } else {
                    TextButton(onClick = {
                        showDeviceDialog = false
                        viewModel.stopScan()
                    }) {
                        Text("Cancelar")
                    }
                }
            },
            dismissButton = {
                if (!isConnecting && !isScanning) {
                    TextButton(onClick = {
                        viewModel.dismissMessage()
                        viewModel.startScan()
                    }) {
                        Text("Buscar de nuevo")
                    }
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("BeatFlow") },
            text = {
                Column {
                    Text("Desarrollado por:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Annier Jesús Fajardo Quesada",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "annierfq01@gmail.com",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Aplicación para medición de HRV con sensor Polar H10.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("CERRAR") }
            }
        )
    }

    if (showProtocolDialog) {
        AlertDialog(
            onDismissRequest = { showProtocolDialog = false },
            title = { Text("Protocolo de respiración controlada") },
            text = {
                Column {
                    Text("Tiempo total de medición", style = MaterialTheme.typography.labelMedium)
                    Text("$protocolTotal min", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = protocolTotal.toFloat(), onValueChange = { protocolTotal = it.roundToInt() },
                        valueRange = 1f..20f, steps = 18
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Inspiración", style = MaterialTheme.typography.labelMedium)
                    Text("$protocolInsp s", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = protocolInsp.toFloat(), onValueChange = { protocolInsp = it.roundToInt() },
                        valueRange = 2f..10f, steps = 7
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Espiración", style = MaterialTheme.typography.labelMedium)
                    Text("$protocolExp s", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = protocolExp.toFloat(), onValueChange = { protocolExp = it.roundToInt() },
                        valueRange = 2f..10f, steps = 7
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Respiración guiada: ${protocolInsp}s IN / ${protocolExp}s OUT\n" +
                                    "Total: ${protocolTotal} min (${protocolTotal * 60 / (protocolInsp + protocolExp)} ciclos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showProtocolDialog = false
                    AppRoutes.pendingProtocolSecs = protocolTotal * 60
                    AppRoutes.pendingInspirationSecs = protocolInsp
                    AppRoutes.pendingExpirationSecs = protocolExp
                    onNavigateToMeasurement()
                }) { Text("INICIAR") }
            },
            dismissButton = {
                TextButton(onClick = { showProtocolDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showBtDialog) {
        AlertDialog(
            onDismissRequest = { showBtDialog = false },
            icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
            title = { Text("Bluetooth desactivado") },
            text = { Text("Activa el Bluetooth para buscar dispositivos Polar.") },
            confirmButton = {
                Button(onClick = {
                    showBtDialog = false
                    context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }) { Text("ACTIVAR BLUETOOTH") }
            },
            dismissButton = {
                TextButton(onClick = { showBtDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title = { Text("Ubicación desactivada") },
            text = {
                Text("En Android 10+ es necesario activar la ubicación para buscar dispositivos Bluetooth.")
            },
            confirmButton = {
                Button(onClick = {
                    showLocationDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("ABRIR AJUSTES") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("CANCELAR") }
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
                is ConnectionState.ConnectionFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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
                            is ConnectionState.ConnectionFailed -> MaterialTheme.colorScheme.error
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
                        is ConnectionState.ConnectionFailed -> "Error de conexión"
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
                    is ConnectionState.ConnectionFailed -> Icons.Default.Bluetooth
                    is ConnectionState.Disconnected -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                tint = when (state) {
                    is ConnectionState.Connected -> BeatFlowColors.Success
                    is ConnectionState.Connecting -> BeatFlowColors.Warning
                    is ConnectionState.ConnectionFailed -> MaterialTheme.colorScheme.error
                    is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
