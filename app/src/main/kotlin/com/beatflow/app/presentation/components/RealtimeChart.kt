package com.beatflow.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Componente de gráfico en tiempo real que simula un monitor médico.
 * 
 * Características:
 * - Líneas suaves que fluyen automáticamente (scroll horizontal)
 * - Grid dinámico con líneas de referencia
 * - Rendimiento optimizado con Canvas nativo
 * - Aspecto profesional de monitor ECG
 */
@Composable
fun RealtimeChart(
    data: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    gridColor: Color,
    title: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 40f
            val graphWidth = width - (padding * 2)
            val graphHeight = height - (padding * 2)

            // Dibujar grid de referencia
            drawGrid(gridColor, padding, graphWidth, graphHeight)

            // Dibujar línea del gráfico con scroll automático
            if (data.isNotEmpty()) {
                drawRealTimeChart(
                    data,
                    minValue,
                    maxValue,
                    lineColor,
                    padding,
                    graphWidth,
                    graphHeight
                )
            }

            // Dibujar ejes
            drawAxes(padding, graphWidth, graphHeight, lineColor)
        }
    }
}

/**
 * Dibuja el grid de fondo similar a papel milimetrado de monitors médicos
 */
private fun DrawScope.drawGrid(
    gridColor: Color,
    padding: Float,
    graphWidth: Float,
    graphHeight: Float
) {
    // Líneas horizontales (5 divisiones)
    repeat(6) { i ->
        val y = padding + (graphHeight / 5) * i
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + graphWidth, y),
            strokeWidth = 0.5f,
            alpha = 0.5f
        )
    }

    // Líneas verticales (10 divisiones para efecto de scroll)
    repeat(11) { i ->
        val x = padding + (graphWidth / 10) * i
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + graphHeight),
            strokeWidth = 0.5f,
            alpha = 0.5f
        )
    }
}

/**
 * Dibuja el gráfico en tiempo real con efecto de scrolling automático
 * similar a monitores médicos reales (ECG, frecuencia cardíaca, etc)
 */
private fun DrawScope.drawRealTimeChart(
    data: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    padding: Float,
    graphWidth: Float,
    graphHeight: Float
) {
    if (data.size < 2) return

    val range = maxOf(maxValue - minValue, 1f)

    // Calcular cuántos puntos se pueden mostrar en el ancho disponible
    val visiblePoints = minOf(data.size, max(2, (graphWidth).toInt()))
    val startIdx = maxOf(0, data.size - visiblePoints)

    // Dibujar línea continua desde los últimos N puntos
    // Esto crea el efecto de "scroll hacia la izquierda" del monitor
    for (i in startIdx until data.size - 1) {
        val relativeIdx = i - startIdx
        val nextRelativeIdx = relativeIdx + 1

        // Calcular posiciones X (distribuir uniformemente en el ancho disponible)
        val x1 = padding + (relativeIdx.toFloat() / visiblePoints) * graphWidth
        val x2 = padding + (nextRelativeIdx.toFloat() / visiblePoints) * graphWidth

        // Normalizar valores Y entre 0 y 1, luego mapear a altura del gráfico
        val normalizedY1 = (data[i] - minValue) / range
        val normalizedY2 = (data[i + 1] - minValue) / range

        // Invertir Y porque Canvas tiene origen en esquina superior izquierda
        val y1 = padding + graphHeight - (normalizedY1 * graphHeight)
        val y2 = padding + graphHeight - (normalizedY2 * graphHeight)

        // Dibujar línea suave entre dos puntos consecutivos
        drawLine(
            color = lineColor,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2.5f
        )
    }
}

/**
 * Dibuja los ejes del gráfico
 */
private fun DrawScope.drawAxes(
    padding: Float,
    graphWidth: Float,
    graphHeight: Float,
    axisColor: Color
) {
    // Eje X (horizontal)
    drawLine(
        color = axisColor,
        start = Offset(padding, padding + graphHeight),
        end = Offset(padding + graphWidth, padding + graphHeight),
        strokeWidth = 1f,
        alpha = 0.7f
    )

    // Eje Y (vertical)
    drawLine(
        color = axisColor,
        start = Offset(padding, padding),
        end = Offset(padding, padding + graphHeight),
        strokeWidth = 1f,
        alpha = 0.7f
    )
}
