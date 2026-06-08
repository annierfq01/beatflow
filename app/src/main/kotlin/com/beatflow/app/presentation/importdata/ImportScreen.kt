package com.beatflow.app.presentation.importdata

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beatflow.app.export.FileImporter
import com.beatflow.app.export.ImportedSession
import com.beatflow.app.presentation.components.FrequencyDomainCard
import com.beatflow.app.presentation.components.NonLinearCard
import com.beatflow.app.presentation.components.TimeDomainCard
import com.beatflow.app.presentation.theme.BeatFlowColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer = remember { FileImporter() }
    var importedData by remember { mutableStateOf<ImportedSession?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            error = null
            scope.launch {
                val result = importer.importSession(context, uri)
                result.onSuccess { data ->
                    importedData = data
                }.onFailure { e ->
                    error = e.message ?: "Error al leer el archivo"
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar datos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Leyendo archivo…")
                    }
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error ?: "",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        error = null
                        filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    }) {
                        Text("INTENTAR DE NUEVO")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onNavigateHome) {
                        Text("VOLVER")
                    }
                }
            }
            importedData != null -> {
                ImportedDataContent(
                    data = importedData!!,
                    onPickAnother = {
                        importedData = null
                        filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    onNavigateHome = onNavigateHome,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Seleccionando archivo…")
                }
            }
        }
    }
}

@Composable
private fun ImportedDataContent(
    data: ImportedSession,
    onPickAnother: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rrSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && data.rrIntervals.isNotEmpty()) {
            scope.launch {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val content = data.rrIntervals.joinToString("\n") { "%.0f".format(it) }
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BeatFlowColors.Success.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BeatFlowColors.Success
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Archivo importado correctamente", fontWeight = FontWeight.Bold)
                    Text(
                        "${data.patientData.nombre} ${data.patientData.apellidos}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (data.rrIntervals.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val name = "${data.patientData.nombre}_${data.patientData.apellidos}_RR.txt"
                            rrSaveLauncher.launch(name)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar RR")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Paciente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                MetricDetailRow("Nombre", "${data.patientData.nombre} ${data.patientData.apellidos}")
                MetricDetailRow("Edad", "${data.patientData.edad} años")
                MetricDetailRow("Sexo", data.patientData.sexo)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MetricDetailRow("Inicio", dateFormat.format(Date(data.startTime)))
                MetricDetailRow("Fin", dateFormat.format(Date(data.endTime)))
                MetricDetailRow("Duración", formatDuration(data.durationMs))
            }
        }

        data.metrics?.let { metrics ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Análisis de Variabilidad de la Frecuencia Cardíaca",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimeDomainCard(metrics)
            Spacer(modifier = Modifier.height(12.dp))
            FrequencyDomainCard(metrics)
            Spacer(modifier = Modifier.height(12.dp))
            NonLinearCard(metrics)
        } ?: run {
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sin datos suficientes para el análisis HRV",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPickAnother,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("OTRO ARCHIVO")
            }
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.weight(1f)
            ) {
                Text("INICIO")
            }
        }
    }
}

@Composable
private fun MetricDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d min".format(minutes, seconds)
}
