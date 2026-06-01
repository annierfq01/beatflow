package com.beatflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HrvMetrics(
    val meanHr: Double,
    val sdnn: Double,
    val rmssd: Double,
    val pnn50: Double,
    val pnn20: Double,
    val nn50: Double,
    val nn20: Double,
    val maxHr: Double,
    val minHr: Double,
    val vlf: Double,
    val lf: Double,
    val hf: Double,
    val totalPower: Double,
    val lfHfRatio: Double,
    val lfNu: Double,
    val hfNu: Double,
    val sd1: Double,
    val sd2: Double,
    val sd1Sd2Ratio: Double
)
