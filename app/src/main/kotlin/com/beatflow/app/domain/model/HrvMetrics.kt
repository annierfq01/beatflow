package com.beatflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HrvMetrics(
    val meanHr: Double,
    val sdnn: Double,
    val rmssd: Double,
    val pnn50: Double,
    val maxHr: Double,
    val minHr: Double,
    val lf: Double,
    val hf: Double,
    val lfHfRatio: Double,
    val sd1: Double,
    val sd2: Double
)
