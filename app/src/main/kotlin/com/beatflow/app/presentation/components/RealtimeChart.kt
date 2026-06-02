package com.beatflow.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val GRID_ROWS = 8
private const val GRID_COLS = 10
private const val MAX_VISIBLE_PTS = 300
private const val PAD = 42f
private const val Y_LABEL_W = 36f

@Composable
fun RealtimeChart(
    data: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    gridColor: Color,
    title: String = "",
    unit: String = "",
    timeWindowSeconds: Int = 0,
    maxVisiblePoints: Int = MAX_VISIBLE_PTS,
    modifier: Modifier = Modifier
) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { delay(50L); tick++ }
    }

    val displayData = remember(data, tick) { data.toList() }
    val range = remember(maxValue, minValue) { if (maxValue - minValue < 0.001f) 1f else maxValue - minValue }
    val textMeasurer = rememberTextMeasurer()
    var windowStart by remember { mutableIntStateOf(0) }
    var isFollowing by remember { mutableStateOf(true) }
    var tooltipIdx by remember { mutableIntStateOf(-1) }

    val n = displayData.size
    val maxVisible = maxVisiblePoints

    LaunchedEffect(n, isFollowing) {
        if (isFollowing && n > maxVisible) {
            windowStart = n - maxVisible
        }
    }

    val currentWindowStart by rememberUpdatedState(windowStart)
    val currentIsFollowing by rememberUpdatedState(isFollowing)
    val currentDisplayData by rememberUpdatedState(displayData)

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maxVisible) {
                        val vis = maxVisible
                        detectTapGestures { offset ->
                            val gw = size.width - PAD * 2
                            val xStep = gw / vis.toFloat()
                            val idx = currentWindowStart + ((offset.x - PAD) / xStep).roundToInt()
                            tooltipIdx = if (idx in currentDisplayData.indices) idx else -1
                        }
                    }
                    .pointerInput(maxVisible) {
                        val vis = maxVisible
                        detectHorizontalDragGestures { _, dragAmount ->
                            val shift = -(dragAmount / 12f).roundToInt()
                            if (shift != 0) {
                                val maxStart = (currentDisplayData.size - vis).coerceAtLeast(0)
                                windowStart = (currentWindowStart + shift).coerceIn(0, maxStart)
                                isFollowing = currentWindowStart >= maxStart
                            }
                        }
                    }
            ) {
                if (n < 2) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(PAD, PAD + (size.height - PAD * 2) / 2f),
                        end = Offset(PAD + (size.width - PAD * 2), PAD + (size.height - PAD * 2) / 2f),
                        strokeWidth = 2f
                    )
                    drawChartGrid(gridColor, PAD, size.width - PAD * 2, size.height - PAD * 2)
                    drawRightLabels(textMeasurer, minValue, maxValue, unit, PAD, size.width - PAD * 2, size.height - PAD * 2)
                    return@Canvas
                }

                val gw = size.width - PAD * 2
                val gh = size.height - PAD * 2

                val startIdx = windowStart.coerceIn(0, n - 1)
                val visibleCount = (n - startIdx).coerceAtMost(maxVisible)

                drawChartGrid(gridColor, PAD, gw, gh)
                drawChartTrace(
                    displayData, minValue, maxValue, range, lineColor,
                    PAD, gw, gh, startIdx, visibleCount, maxVisible
                )
                drawRightLabels(textMeasurer, minValue, maxValue, unit, PAD, gw, gh)
                if (timeWindowSeconds > 0) {
                    drawXLabels(textMeasurer, timeWindowSeconds, PAD, gw, gh, visibleCount)
                }

                if (tooltipIdx in displayData.indices) {
                    val xStep = gw / maxVisible.toFloat()
                    val tipX = PAD + (tooltipIdx - windowStart) * xStep
                    val norm = (displayData[tooltipIdx] - minValue) / range
                    val tipY = PAD + gh - (norm * gh)

                    val tipText = "%.1f %s".format(displayData[tooltipIdx], unit).trimEnd()
                    val m = textMeasurer.measure(tipText, TextStyle(
                        color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    ))
                    val bgW = m.size.width + 10f
                    val bgH = m.size.height + 6f
                    val bgX = (tipX - bgW / 2f).coerceIn(2f, size.width - bgW - 2f)
                    val bgY = (tipY - bgH - 8f).coerceIn(2f, size.height - bgH - 2f)

                    drawRoundRect(
                        color = Color(0xCC111111),
                        topLeft = Offset(bgX, bgY),
                        size = Size(bgW, bgH),
                        cornerRadius = CornerRadius(4f)
                    )
                    drawText(
                        textMeasurer, tipText,
                        topLeft = Offset(bgX + 5f, bgY + 3f),
                        style = TextStyle(
                            color = lineColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = Offset(tipX, tipY)
                    )
                }
            }

            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = lineColor.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp
                    ),
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 2.dp)
                )
            }

            if (!isFollowing && n > maxVisible) {
                Text(
                    text = "< arrastrar",
                    color = Color.White.copy(alpha = 0.25f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace, fontSize = 8.sp
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawChartGrid(gridColor: Color, pad: Float, gw: Float, gh: Float) {
    val colStep = gw / GRID_COLS
    val rowStep = gh / GRID_ROWS
    for (col in 0..GRID_COLS) {
        val x = pad + col * colStep
        val isMajor = col % 5 == 0
        drawLine(
            color = if (isMajor) gridColor.copy(alpha = 0.5f) else gridColor.copy(alpha = 0.25f),
            start = Offset(x, pad), end = Offset(x, pad + gh),
            strokeWidth = if (isMajor) 1f else 0.5f
        )
    }
    for (row in 0..GRID_ROWS) {
        val y = pad + row * rowStep
        val isMajor = row % 4 == 0
        drawLine(
            color = if (isMajor) gridColor.copy(alpha = 0.5f) else gridColor.copy(alpha = 0.25f),
            start = Offset(pad, y), end = Offset(pad + gw, y),
            strokeWidth = if (isMajor) 1f else 0.5f
        )
    }
}

private fun DrawScope.drawChartTrace(
    data: List<Float>, minValue: Float, maxValue: Float, range: Float,
    lineColor: Color, pad: Float, gw: Float, gh: Float,
    startIdx: Int, visibleCount: Int, maxVisible: Int
) {
    val xStep = gw / maxVisible.toFloat()
    val path = Path()
    var first = true

    for (i in startIdx until (startIdx + visibleCount).coerceAtMost(data.size)) {
        val x = pad + (i - startIdx) * xStep
        val norm = (data[i] - minValue) / range
        val y = (pad + gh - (norm * gh)).coerceIn(pad, pad + gh)
        if (first) { path.moveTo(x, y); first = false }
        else path.lineTo(x, y)
    }

    drawPath(path, lineColor.copy(alpha = 0.12f), style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, lineColor.copy(alpha = 0.4f), style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, lineColor, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawRightLabels(
    measurer: TextMeasurer, minValue: Float, maxValue: Float,
    unit: String, pad: Float, gw: Float, gh: Float
) {
    val style = TextStyle(
        color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp, fontFamily = FontFamily.Monospace
    )
    val rowStep = gh / GRID_ROWS
    for (row in 0..GRID_ROWS) {
        val y = pad + row * rowStep
        val value = maxValue - (row.toFloat() / GRID_ROWS) * (maxValue - minValue)
        val label = "%.0f".format(value)
        val m = measurer.measure(label, style)
        drawText(measurer, label, topLeft = Offset(pad + gw + 4f, y - m.size.height / 2f), style = style)
    }
}

private fun DrawScope.drawXLabels(
    measurer: TextMeasurer, windowSeconds: Int,
    pad: Float, gw: Float, gh: Float, visibleCount: Int
) {
    val style = TextStyle(
        color = Color.White.copy(alpha = 0.35f), fontSize = 7.sp, fontFamily = FontFamily.Monospace
    )
    val steps = listOf(0, windowSeconds / 4, windowSeconds / 2, 3 * windowSeconds / 4, windowSeconds).distinct()
    for (s in steps) {
        val fraction = s.toFloat() / windowSeconds.toFloat()
        val x = pad + gw - (gw * fraction)
        val lbl = "-${s}s"
        val m = measurer.measure(lbl, style)
        val lx = (x - m.size.width / 2f).coerceIn(pad, pad + gw - m.size.width)
        drawText(measurer, lbl, topLeft = Offset(lx, pad + gh + 4f), style = style)
    }
}
