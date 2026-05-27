package com.beatflow.app.domain

import com.beatflow.app.domain.model.HrvMetrics
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

object HrvCalculator {

    fun calculate(rrIntervals: List<Double>): HrvMetrics? {
        if (rrIntervals.size < 30) return null

        val rrMs = rrIntervals
        val n = rrMs.size

        val meanRr = rrMs.average()
        val meanHr = 60000.0 / meanRr

        val sdnn = sqrt(rrMs.map { (it - meanRr).pow(2) }.average())

        val rrDiffs = (1 until n).map { i -> abs(rrMs[i] - rrMs[i - 1]) }
        val rmssd = sqrt(rrDiffs.map { it.pow(2) }.average())

        val pnn50 = (rrDiffs.count { it > 50.0 }.toDouble() / rrDiffs.size) * 100.0

        val minHr = 60000.0 / rrMs.max()
        val maxHr = 60000.0 / rrMs.min()

        val (lf, hf) = calculateFrequencyDomain(rrMs)
        val lfHfRatio = if (hf > 0.0) lf / hf else 0.0

        val sd1 = sqrt(rrDiffs.map { it.pow(2) }.average() / 2.0)
        val sd2 = sqrt(2.0 * sdnn.pow(2) - sd1.pow(2))

        return HrvMetrics(
            meanHr = meanHr,
            sdnn = sdnn,
            rmssd = rmssd,
            pnn50 = pnn50,
            maxHr = maxHr,
            minHr = minHr,
            lf = lf,
            hf = hf,
            lfHfRatio = lfHfRatio,
            sd1 = sd1,
            sd2 = sd2
        )
    }

    private fun calculateFrequencyDomain(rrMs: List<Double>): Pair<Double, Double> {
        val n = rrMs.size
        if (n < 4) return 0.0 to 0.0

        val rrSeconds = rrMs.map { it / 1000.0 }
        val mean = rrSeconds.average()
        val detrended = rrSeconds.map { it - mean }

        val fftSize = nextPowerOfTwo(n)
        val real = DoubleArray(fftSize) { if (it < n) detrended[it] else 0.0 }
        val imag = DoubleArray(fftSize)

        fft(real, imag)

        val samplingPeriod = mean / 1000.0
        val freqResolution = 1.0 / (fftSize * samplingPeriod)

        var lf = 0.0
        var hf = 0.0

        for (i in 0 until fftSize / 2) {
            val freq = i * freqResolution
            val power = real[i].pow(2) + imag[i].pow(2)

            when {
                freq in 0.04..0.15 -> lf += power
                freq in 0.15..0.4 -> hf += power
            }
        }

        return lf to hf
    }

    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tmpR = real[i]; val tmpI = imag[i]
                real[i] = real[j]; imag[i] = imag[j]
                real[j] = tmpR; imag[j] = tmpI
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wLenR = cos(angle)
            val wLenI = sin(angle)
            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val tR = wR * real[i + k + len / 2] - wI * imag[i + k + len / 2]
                    val tI = wR * imag[i + k + len / 2] + wI * real[i + k + len / 2]
                    real[i + k] = uR + tR
                    imag[i + k] = uI + tI
                    real[i + k + len / 2] = uR - tR
                    imag[i + k + len / 2] = uI - tI
                    val newWr = wR * wLenR - wI * wLenI
                    wI = wR * wLenI + wI * wLenR
                    wR = newWr
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var x = 1
        while (x < n) x = x shl 1
        return x
    }
}
