package com.beatflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RawRecord(
    val timestamp: Long,
    val hr: Int? = null,
    val rr: Double? = null,
    val ecgSignal: Double? = null
)
