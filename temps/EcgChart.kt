package com.beatflow.app.presentation.measurement

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
//  CONSTANTS
// ─────────────────────────────────────────────────────────────

private const val SAMPLE_RATE_HZ   = 130        // Polar H10 ECG sample rate
private const val WINDOW_SECONDS   = 5           // seconds visible in chart
private const val MAX_SAMPLES      = SAMPLE_RATE_HZ * WINDOW_SECONDS  // 650 samples
private const val ECG_SCALE_UV     = 2000f       // μV full-scale (±2000 μV)
private const val GRID_ROWS        = 8           // horizontal grid lines
private const val GRID_COLS        = 10          // vertical grid lines

// ─────────────────────────────────────────────────────────────
//  DATA MODEL
// ─────────────────────────────────────────────────────────────

/**
 * Holds a circular buffer of ECG samples (in μV) and exposes
 * a snapshot list for rendering.
 *
 * Usage:
 *   val state = remember { EcgChartState() }
 *   // Feed samples from PolarManager:
 *   ecgFlow.collect { samples -> state.addSamples(samples) }
 */
class EcgChartState(
    private val maxSamples: Int = MAX_SAMPLES
) {
    private val buffer = ArrayDeque<Float>(maxSamples)

    /** Thread-safe: call from any coroutine/dispatcher.
     *  Pass raw μV values from the Polar ECG data. */
    @Synchronized
    fun addSamples(samples: IntArray) {
        for (s in samples) {
            if (buffer.size >= maxSamples) buffer.removeFirst()
            buffer.addLast(s.toFloat())
        }
    }

    @Synchronized
    fun addSample(sampleUv: Float) {
        if (buffer.size >= maxSamples) buffer.removeFirst()
        buffer.addLast(sampleUv)
    }

    /** Returns an immutable snapshot for rendering. */
    @Synchronized
    fun snapshot(): List<Float> = buffer.toList()

    /** Number of samples currently buffered. */
    @Synchronized
    fun size(): Int = buffer.size

    /** Clear all buffered samples. */
    @Synchronized
    fun clear() = buffer.clear()
}

// ─────────────────────────────────────────────────────────────
//  MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────

/**
 * Real-time ECG chart drawn with Compose Canvas.
 *
 * @param state         [EcgChartState] fed externally with Polar ECG samples.
 * @param modifier      Standard Compose modifier.
 * @param lineColor     Color of the ECG trace. Default: bright green (clinical style).
 * @param backgroundColor Chart background. Default: near-black.
 * @param refreshRateMs How often the chart redraws. Default: 50 ms (20 fps).
 * @param showGrid      Whether to draw the ECG paper grid. Default: true.
 * @param showLabels    Whether to draw axis labels (time / μV). Default: true.
 * @param title         Optional title shown in the top-left corner.
 * @param scaleUv       Half-range of the Y axis in μV. Default: 2000 μV (±2 mV).
 */
@Composable
fun EcgChart(
    state: EcgChartState,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00FF7F),          // spring green
    backgroundColor: Color = Color(0xFF0D1117),    // near-black
    gridColor: Color = Color(0xFF1E3A2F),          // dark green grid
    gridMajorColor: Color = Color(0xFF2E5040),     // slightly brighter major lines
    refreshRateMs: Long = 50L,
    showGrid: Boolean = true,
    showLabels: Boolean = true,
    title: String = "ECG",
    scaleUv: Float = ECG_SCALE_UV
) {
    // Trigger recomposition at refreshRateMs
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(refreshRateMs)
            tick++
        }
    }

    // Capture a snapshot each tick (tick is read so recomposition fires)
    val samples = remember(tick) { state.snapshot() }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            val w = size.width
            val h = size.height
            val midY = h / 2f

            // ── Grid ──────────────────────────────────────────
            if (showGrid) {
                drawEcgGrid(
                    w, h,
                    gridColor = gridColor,
                    majorColor = gridMajorColor
                )
            }

            // ── Baseline ──────────────────────────────────────
            drawLine(
                color = gridMajorColor.copy(alpha = 0.6f),
                start = Offset(0f, midY),
                end   = Offset(w, midY),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // ── ECG trace ─────────────────────────────────────
            if (samples.size >= 2) {
                drawEcgTrace(
                    samples  = samples,
                    width    = w,
                    height   = h,
                    midY     = midY,
                    scaleUv  = scaleUv,
                    color    = lineColor,
                    maxPoints = MAX_SAMPLES
                )
            } else {
                // Flat line while waiting for data
                drawLine(
                    color       = lineColor.copy(alpha = 0.3f),
                    start       = Offset(0f, midY),
                    end         = Offset(w, midY),
                    strokeWidth = 2f
                )
            }

            // ── Axis labels ───────────────────────────────────
            if (showLabels) {
                drawAxisLabels(
                    textMeasurer = textMeasurer,
                    w = w, h = h,
                    scaleUv = scaleUv,
                    windowSeconds = WINDOW_SECONDS
                )
            }
        }

        // ── Title badge ───────────────────────────────────────
        if (title.isNotBlank()) {
            Text(
                text  = title,
                color = lineColor.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 11.sp
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 4.dp)
            )
        }

        // ── Sample count badge (debug) ─────────────────────────
        Text(
            text  = "${samples.size} / $MAX_SAMPLES pts",
            color = Color.White.copy(alpha = 0.35f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize   = 9.sp
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  DRAWING HELPERS
// ─────────────────────────────────────────────────────────────

private fun DrawScope.drawEcgGrid(
    w: Float,
    h: Float,
    gridColor: Color,
    majorColor: Color
) {
    val colStep = w / GRID_COLS
    val rowStep = h / GRID_ROWS

    // Minor lines every cell
    for (col in 0..GRID_COLS) {
        val x = col * colStep
        val isMajor = col % 5 == 0
        drawLine(
            color       = if (isMajor) majorColor else gridColor,
            start       = Offset(x, 0f),
            end         = Offset(x, h),
            strokeWidth = if (isMajor) 1.2f else 0.6f
        )
    }
    for (row in 0..GRID_ROWS) {
        val y = row * rowStep
        val isMajor = row % 4 == 0
        drawLine(
            color       = if (isMajor) majorColor else gridColor,
            start       = Offset(0f, y),
            end         = Offset(w, y),
            strokeWidth = if (isMajor) 1.2f else 0.6f
        )
    }
}

private fun DrawScope.drawEcgTrace(
    samples: List<Float>,
    width: Float,
    height: Float,
    midY: Float,
    scaleUv: Float,
    color: Color,
    maxPoints: Int
) {
    val path = Path()
    val n    = samples.size
    // X is spread across the full window (even if buffer not full)
    val xStep = width / maxPoints.toFloat()
    // X offset: draw from the right edge backward so new samples scroll in
    val xOffset = (maxPoints - n) * xStep

    var firstPoint = true
    samples.forEachIndexed { i, uv ->
        val x = xOffset + i * xStep
        val y = midY - (uv / scaleUv) * (height / 2f)
        val yClamped = y.coerceIn(0f, height)
        if (firstPoint) {
            path.moveTo(x, yClamped)
            firstPoint = false
        } else {
            path.lineTo(x, yClamped)
        }
    }

    // Glow effect: wide semi-transparent stroke beneath sharp line
    drawPath(
        path        = path,
        color       = color.copy(alpha = 0.15f),
        style       = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path        = path,
        color       = color.copy(alpha = 0.5f),
        style       = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path        = path,
        color       = color,
        style       = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawAxisLabels(
    textMeasurer: TextMeasurer,
    w: Float,
    h: Float,
    scaleUv: Float,
    windowSeconds: Int
) {
    val labelStyle = TextStyle(
        color      = Color.White.copy(alpha = 0.45f),
        fontSize   = 8.sp,
        fontFamily = FontFamily.Monospace
    )

    // Y axis: ±scaleUv labels
    listOf(
        0f      to "+${(scaleUv / 1000).roundToInt()} mV",
        h / 2f  to "0",
        h       to "-${(scaleUv / 1000).roundToInt()} mV"
    ).forEach { (y, label) ->
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(
            textMeasurer = textMeasurer,
            text         = label,
            style        = labelStyle,
            topLeft      = Offset(4f, y - measured.size.height / 2f)
        )
    }

    // X axis: 0 s … window seconds
    for (s in 0..windowSeconds) {
        val x   = s * w / windowSeconds
        val lbl = "${s}s"
        val measured = textMeasurer.measure(lbl, labelStyle)
        drawText(
            textMeasurer = textMeasurer,
            text         = lbl,
            style        = labelStyle,
            topLeft      = Offset(
                (x - measured.size.width / 2f).coerceIn(0f, w - measured.size.width),
                h - measured.size.height - 2f
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  COMPACT VARIANT (sin labels, para cards pequeñas)
// ─────────────────────────────────────────────────────────────

/**
 * Minimal ECG sparkline, useful inside a summary card.
 * No grid, no labels — just the trace on a transparent background.
 */
@Composable
fun EcgSparkline(
    state: EcgChartState,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00FF7F),
    refreshRateMs: Long = 50L
) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(refreshRateMs)
            tick++
        }
    }
    val samples = remember(tick) { state.snapshot() }

    Canvas(modifier = modifier) {
        if (samples.size >= 2) {
            drawEcgTrace(
                samples   = samples,
                width     = size.width,
                height    = size.height,
                midY      = size.height / 2f,
                scaleUv   = ECG_SCALE_UV,
                color     = lineColor,
                maxPoints = MAX_SAMPLES
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  PREVIEW  (simulates a real ECG waveform)
// ─────────────────────────────────────────────────────────────

/** Generates a synthetic ECG-like signal for preview/testing purposes. */
private fun syntheticEcg(numSamples: Int = MAX_SAMPLES): IntArray {
    val out = IntArray(numSamples)
    val beatsPerSample = 72.0 / 60.0 / SAMPLE_RATE_HZ  // 72 BPM

    for (i in out.indices) {
        val phase = (i * beatsPerSample * Math.PI * 2) % (Math.PI * 2)
        val p  = 80  * gauss(phase, 0.3,  0.05)   // P wave
        val q  = -80 * gauss(phase, 0.44, 0.015)  // Q dip
        val r  = 1500 * gauss(phase, 0.5,  0.012) // R spike
        val s  = -200 * gauss(phase, 0.56, 0.015) // S dip
        val t  = 200 * gauss(phase, 0.75, 0.07)   // T wave
        val noise = (Math.random() - 0.5) * 30
        out[i] = (p + q + r + s + t + noise).toInt()
    }
    return out
}

private fun gauss(x: Double, mu: Double, sigma: Double): Double {
    val d = x / (Math.PI * 2) - mu
    return Math.exp(-0.5 * (d / sigma) * (d / sigma))
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, widthDp = 400, heightDp = 220)
@Composable
fun EcgChartPreview() {
    val state = remember {
        EcgChartState().also { s ->
            s.addSamples(syntheticEcg())
        }
    }
    EcgChart(
        state    = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp),
        title    = "ECG · Polar H10"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, widthDp = 200, heightDp = 80)
@Composable
fun EcgSparklinePreview() {
    val state = remember {
        EcgChartState().also { s ->
            s.addSamples(syntheticEcg())
        }
    }
    EcgSparkline(
        state    = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(4.dp)
    )
}
