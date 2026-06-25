package com.beatflow.app.domain

import com.beatflow.app.domain.model.HrvMetrics
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

object HrvCalculator {

    private const val MIN_SAMPLES_TIME = 30
    private const val RECOMMENDED_SAMPLES_FREQ = 300
    private const val RESAMPLE_HZ = 4.0
    private const val ARTIFACT_THRESHOLD = 0.20

    fun calculate(rrIntervals: List<Double>): HrvMetrics? {
        val rrMs = filterArtifacts(rrIntervals)
        if (rrMs.size < MIN_SAMPLES_TIME) return null

        val n = rrMs.size
        val meanRr = rrMs.average()
        val meanHr = 60000.0 / meanRr

        val sdnn = sqrt(rrMs.map { (it - meanRr).pow(2) }.average())

        val rrDiffs = (1 until n).map { i -> abs(rrMs[i] - rrMs[i - 1]) }
        val rmssd = sqrt(rrDiffs.map { it.pow(2) }.average())

        val nn50 = rrDiffs.count { it > 50.0 }
        val nn20 = rrDiffs.count { it > 20.0 }
        val pnn50 = (nn50.toDouble() / rrDiffs.size) * 100.0
        val pnn20 = (nn20.toDouble() / rrDiffs.size) * 100.0

        val minHr = 60000.0 / rrMs.max()
        val maxHr = 60000.0 / rrMs.min()

        val sd1 = rmssd / sqrt(2.0)
        val sd2 = sqrt(2.0 * sdnn.pow(2) - 0.5 * rmssd.pow(2))
        val sd1Sd2Ratio = if (sd2 > 0.0) sd1 / sd2 else 0.0

        val freqWarning = rrMs.size < RECOMMENDED_SAMPLES_FREQ
        val (vlf, lf, hf) = calculateFrequencyDomain(rrMs)

        val totalPower = vlf + lf + hf
        val lfHfRatio = if (hf > 0.0) lf / hf else 0.0

        val lfHfSum = lf + hf
        val lfNu = if (lfHfSum > 0.0) (lf / lfHfSum) * 100.0 else 0.0
        val hfNu = if (lfHfSum > 0.0) (hf / lfHfSum) * 100.0 else 0.0

        return HrvMetrics(
            meanHr = meanHr,
            sdnn = sdnn,
            rmssd = rmssd,
            pnn50 = pnn50,
            pnn20 = pnn20,
            nn50 = nn50.toDouble(),
            nn20 = nn20.toDouble(),
            maxHr = maxHr,
            minHr = minHr,
            vlf = vlf,
            lf = lf,
            hf = hf,
            totalPower = totalPower,
            lfHfRatio = lfHfRatio,
            lfNu = lfNu,
            hfNu = hfNu,
            sd1 = sd1,
            sd2 = sd2,
            sd1Sd2Ratio = sd1Sd2Ratio,
            freqWarning = freqWarning
        )
    }

    fun filterArtifacts(rr: List<Double>): List<Double> {
        val physiological = rr.filter { it in 300.0..2000.0 }
        if (physiological.isEmpty()) return emptyList()

        val filtered = mutableListOf<Double>()
        filtered.add(physiological[0])

        for (i in 1 until physiological.size) {
            val prev = filtered.last()
            val current = physiological[i]
            val relativeChange = abs(current - prev) / prev
            if (relativeChange < ARTIFACT_THRESHOLD) {
                filtered.add(current)
            }
        }
        return filtered
    }

    private fun calculateFrequencyDomain(rrMs: List<Double>): Triple<Double, Double, Double> {
        val n = rrMs.size
        if (n < 4) return Triple(0.0, 0.0, 0.0)

        val rrSeconds = rrMs.map { it / 1000.0 }

        val mean = rrSeconds.average()
        val detrended = rrSeconds.map { it - mean }

        val tCum = DoubleArray(n)
        tCum[0] = 0.0
        for (i in 1 until n) {
            tCum[i] = tCum[i - 1] + rrSeconds[i - 1]
        }

        val step = 1.0 / RESAMPLE_HZ
        val tStart = tCum.first()
        val tEnd = tCum.last()
        val tUniform = mutableListOf<Double>()
        var t = tStart
        while (t <= tEnd) {
            tUniform.add(t)
            t += step
        }
        val mUniform = tUniform.size

        val rrUniform = DoubleArray(mUniform) { k ->
            val tk = tUniform[k]
            var idx = 1
            while (idx < n && tCum[idx] < tk) idx++
            if (idx >= n) idx = n - 1
            val t0 = tCum[idx - 1]
            val t1 = tCum[idx]
            val y0 = detrended[idx - 1]
            val y1 = detrended[idx]
            if (t1 - t0 == 0.0) y0
            else y0 + (y1 - y0) * (tk - t0) / (t1 - t0)
        }

        val fftSize = nextPowerOfTwo(mUniform)
        val real = DoubleArray(fftSize) { if (it < mUniform) rrUniform[it] else 0.0 }
        val imag = DoubleArray(fftSize)

        if (mUniform > 1) {
            for (i in 0 until mUniform) {
                val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (mUniform - 1)))
                real[i] *= hann
            }
        }

        fft(real, imag)

        val samplingPeriod = 1.0 / RESAMPLE_HZ
        val freqResolution = 1.0 / (fftSize * samplingPeriod)

        val windowCorrection = if (mUniform > 1) {
            (0 until mUniform).sumOf {
                val h = 0.5 * (1.0 - cos(2.0 * PI * it / (mUniform - 1)))
                h * h
            }
        } else {
            1.0
        }

        var vlf = 0.0
        var lf = 0.0
        var hf = 0.0

        for (i in 1 until fftSize / 2) {
            val freq = i * freqResolution
            val power = 2.0 * (real[i].pow(2) + imag[i].pow(2)) / windowCorrection

            when {
                freq in 0.0033..0.04 -> vlf += power
                freq in 0.04..0.15 -> lf += power
                freq in 0.15..0.4 -> hf += power
            }
        }

        return Triple(vlf, lf, hf)
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
                val tmpR = real[i]
                val tmpI = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tmpR
                imag[j] = tmpI
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
