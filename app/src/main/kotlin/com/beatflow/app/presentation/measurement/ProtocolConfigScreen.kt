package com.beatflow.app.presentation.measurement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatflow.app.presentation.theme.BeatFlowColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolConfigScreen(
    protocolType: String,
    onNavigateToMeasurement: (totalSecs: Int, inspSecs: Int, expSecs: Int, standUpSecs: Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    var totalMinutes by remember { mutableIntStateOf(5) }
    var inspSecs by remember { mutableIntStateOf(5) }
    var expSecs by remember { mutableIntStateOf(5) }
    var standUpSecs by remember { mutableIntStateOf(120) }

    val title = when (protocolType) {
        "basal" -> "Configurar Reposo/Basal"
        "respiracion" -> "Configurar Test Respiración"
        "ortostatico" -> "Configurar Test Ortostático"
        else -> "Configurar"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Atrás", color = BeatFlowColors.HeartRed)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Parámetros de la medición",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    when (protocolType) {
                        "basal" -> {
                            Text(
                                text = "Tiempo total de medición",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$totalMinutes min",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = BeatFlowColors.HeartRed
                            )
                            Slider(
                                value = totalMinutes.toFloat(),
                                onValueChange = { totalMinutes = it.roundToInt() },
                                valueRange = 1f..30f,
                                steps = 28
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1 min", style = MaterialTheme.typography.bodySmall)
                                Text("30 min", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        "respiracion" -> {
                            Text(
                                text = "Tiempo total de medición",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalMinutes min",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BeatFlowColors.HeartRed
                            )
                            Slider(
                                value = totalMinutes.toFloat(),
                                onValueChange = { totalMinutes = it.roundToInt() },
                                valueRange = 1f..20f,
                                steps = 18
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Inspiración",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$inspSecs s",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BeatFlowColors.Primary
                            )
                            Slider(
                                value = inspSecs.toFloat(),
                                onValueChange = { inspSecs = it.roundToInt() },
                                valueRange = 2f..10f,
                                steps = 7
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Espiración",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$expSecs s",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BeatFlowColors.HeartRed
                            )
                            Slider(
                                value = expSecs.toFloat(),
                                onValueChange = { expSecs = it.roundToInt() },
                                valueRange = 2f..10f,
                                steps = 7
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Respiración guiada: ${inspSecs}s IN / ${expSecs}s OUT\n" +
                                            "Total: ${totalMinutes} min (${totalMinutes * 60 / (inspSecs + expSecs)} ciclos)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        "ortostatico" -> {
                            Text(
                                text = "Segundo en que el sujeto se pone de pie",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$standUpSecs s",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = BeatFlowColors.HeartRed
                            )
                            Slider(
                                value = standUpSecs.toFloat(),
                                onValueChange = { standUpSecs = it.roundToInt() },
                                valueRange = 10f..300f,
                                steps = 28
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("10 s", style = MaterialTheme.typography.bodySmall)
                                Text("300 s", style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tiempo total de medición",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalMinutes min",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BeatFlowColors.Primary
                            )
                            Slider(
                                value = totalMinutes.toFloat(),
                                onValueChange = { totalMinutes = it.roundToInt() },
                                valueRange = 2f..30f,
                                steps = 27
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("2 min", style = MaterialTheme.typography.bodySmall)
                                Text("30 min", style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "El sujeto se pondrá de pie en el segundo $standUpSecs.\n" +
                                            "Registro total: ${totalMinutes} min (${standUpSecs}s pre + ${totalMinutes * 60 - standUpSecs}s post)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ATRÁS", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val totalSecs = totalMinutes * 60
                        when (protocolType) {
                            "basal" -> onNavigateToMeasurement(totalSecs, 0, 0, 0)
                            "respiracion" -> onNavigateToMeasurement(totalSecs, inspSecs, expSecs, 0)
                            "ortostatico" -> onNavigateToMeasurement(totalSecs, 0, 0, standUpSecs)
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeatFlowColors.HeartRed
                    )
                ) {
                    Text("INICIAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
