package com.beatflow.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val GRID_ROWS = 8
private const val GRID_COLS = 10

@Composable
fun RealtimeChart(
    data: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    gridColor: Color,
    title: String = "",
    unit: String = "",
    modifier: Modifier = Modifier
) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50L)
            tick++
        }
    }

    val displayData = remember(data, tick) { data.toList() }
    val range = remember(maxValue, minValue) { if (maxValue - minValue < 0.001f) 1f else maxValue - minValue }
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padding = 36f
                val gw = size.width - padding * 2
                val gh = size.height - padding * 2

                drawChartGrid(gridColor, padding, gw, gh)

                if (displayData.size >= 2) {
                    drawChartTrace(displayData, minValue, maxValue, range, lineColor, padding, gw, gh)
                } else {
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(padding, padding + gh / 2f),
                        end = Offset(padding + gw, padding + gh / 2f),
                        strokeWidth = 2f
                    )
                }

                drawChartLabels(textMeasurer, minValue, maxValue, unit, padding, gw, gh)
            }

            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = lineColor.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 2.dp)
                )
            }

            Text(
                text = "${displayData.size} pts",
                color = Color.White.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                ),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 2.dp)
            )
        }
    }
}

private fun DrawScope.drawChartGrid(gridColor: Color, pad: Float, gw: Float, gh: Float) {
    val colStep = gw / GRID_COLS
    val rowStep = gh / GRID_ROWS
    for (col in 0..GRID_COLS) {
        drawLine(
            color = gridColor.copy(alpha = 0.4f),
            start = Offset(pad + col * colStep, pad),
            end = Offset(pad + col * colStep, pad + gh),
            strokeWidth = if (col % 5 == 0) 0.8f else 0.4f
        )
    }
    for (row in 0..GRID_ROWS) {
        drawLine(
            color = gridColor.copy(alpha = 0.4f),
            start = Offset(pad, pad + row * rowStep),
            end = Offset(pad + gw, pad + row * rowStep),
            strokeWidth = if (row % 4 == 0) 0.8f else 0.4f
        )
    }
}

private fun DrawScope.drawChartTrace(
    data: List<Float>,
    minValue: Float,
    maxValue: Float,
    range: Float,
    lineColor: Color,
    pad: Float,
    gw: Float,
    gh: Float
) {
    val n = data.size
    val maxVisible = gw.toInt().coerceAtMost(n)
    val startIdx = (n - maxVisible).coerceAtLeast(0)
    val visibleCount = n - startIdx
    if (visibleCount < 2) return

    val xStep = gw / maxVisible.toFloat()
    val xOffset = pad + (if (maxVisible > n) (maxVisible - n) * xStep else 0f)

    val path = Path()
    var first = true

    for (i in startIdx until n) {
        val x = xOffset + (i - startIdx) * xStep
        val normalized = (data[i] - minValue) / range
        val y = pad + gh - (normalized * gh)
        val clamped = y.coerceIn(pad, pad + gh)
        if (first) {
            path.moveTo(x, clamped)
            first = false
        } else {
            path.lineTo(x, clamped)
        }
    }

    drawPath(path, lineColor.copy(alpha = 0.15f), style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, lineColor.copy(alpha = 0.5f), style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, lineColor, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawChartLabels(
    textMeasurer: TextMeasurer,
    minValue: Float,
    maxValue: Float,
    unit: String,
    pad: Float,
    gw: Float,
    gh: Float
) {
    val style = TextStyle(
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace
    )
    val rowStep = gh / GRID_ROWS
    for (row in 0..GRID_ROWS) {
        val y = pad + row * rowStep
        val value = maxValue - (row.toFloat() / GRID_ROWS) * (maxValue - minValue)
        val label = "%.0f %s".format(value, unit).trimEnd()
        val measured = textMeasurer.measure(label, style)
        drawText(textMeasurer, label, topLeft = Offset(3f, y - measured.size.height / 2f), style = style)
    }
}
