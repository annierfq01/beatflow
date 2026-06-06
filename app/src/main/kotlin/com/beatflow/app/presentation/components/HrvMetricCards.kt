package com.beatflow.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatflow.app.domain.model.HrvMetrics

@Composable
fun TimeDomainCard(metrics: HrvMetrics) {
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
        MetricRow("NN50", "%.0f latidos".format(metrics.nn50))
        MetricRow("pNN20", "%.2f %%".format(metrics.pnn20))
        MetricRow("NN20", "%.0f latidos".format(metrics.nn20))
    }
}

@Composable
fun FrequencyDomainCard(metrics: HrvMetrics) {
    MetricCard(
        title = "Dominio de la Frecuencia",
        icon = Icons.Default.Equalizer
    ) {
        if (metrics.freqWarning) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Registro < 5 min. Los valores de frecuencia pueden no ser fiables.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        MetricRow("VLF (0.0033-0.04 Hz)", "%.2f ms²".format(metrics.vlf))
        MetricRow("LF (0.04-0.15 Hz)", "%.2f ms²".format(metrics.lf))
        MetricRow("HF (0.15-0.4 Hz)", "%.2f ms²".format(metrics.hf))
        MetricRow("Potencia Total", "%.2f ms²".format(metrics.totalPower))
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        MetricRow("LF nu", "%.1f %%".format(metrics.lfNu))
        MetricRow("HF nu", "%.1f %%".format(metrics.hfNu))
        MetricRow("Relación LF/HF", "%.2f".format(metrics.lfHfRatio))
    }
}

@Composable
fun NonLinearCard(metrics: HrvMetrics) {
    MetricCard(
        title = "Métricas No Lineales",
        icon = Icons.Default.ScatterPlot
    ) {
        MetricRow("SD1 (Poincaré)", "%.2f ms".format(metrics.sd1))
        MetricRow("SD2 (Poincaré)", "%.2f ms".format(metrics.sd2))
        MetricRow("SD1/SD2", "%.3f".format(metrics.sd1Sd2Ratio))
    }
}

@Composable
fun MetricCard(
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
