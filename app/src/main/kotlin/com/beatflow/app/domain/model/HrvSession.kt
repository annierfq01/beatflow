package com.beatflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HrvSession(
    val patientData: PatientData,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val records: List<RawRecord>,
    val metrics: HrvMetrics? = null,
    val protocolConfig: ProtocolConfig? = null
)
