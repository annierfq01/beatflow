package com.beatflow.app.presentation.measurement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatflow.app.presentation.theme.BeatFlowColors

data class ProtocolOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val protocolOptions = listOf(
    ProtocolOption(
        id = "basal",
        title = "Reposo / Basal",
        description = "Medición en reposo sin estímulos controlados. Selecciona la duración de la grabación.",
        icon = Icons.Default.Accessibility
    ),
    ProtocolOption(
        id = "respiracion",
        title = "Test Respiración",
        description = "Respiración guiada con tiempos de inspiración y espiración controlados.",
        icon = Icons.Default.Spa
    ),
    ProtocolOption(
        id = "ortostatico",
        title = "Test Ortostático",
        description = "Registro continuo con cambio postural. El sujeto se pone de pie en un momento definido.",
        icon = Icons.Default.DirectionsWalk
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSelectScreen(
    onNavigateToConfig: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedProtocol by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar protocolo") },
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
                text = "Elige el tipo de medición",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            protocolOptions.forEach { option ->
                val isSelected = selectedProtocol == option.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { selectedProtocol = option.id },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            BeatFlowColors.HeartRed.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp
                    ) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedProtocol = option.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BeatFlowColors.HeartRed
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) BeatFlowColors.HeartRed
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isSelected) BeatFlowColors.HeartRed
                                       else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
                        selectedProtocol?.let { onNavigateToConfig(it) }
                    },
                    enabled = selectedProtocol != null,
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeatFlowColors.HeartRed
                    )
                ) {
                    Text(
                        "SIGUIENTE",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
