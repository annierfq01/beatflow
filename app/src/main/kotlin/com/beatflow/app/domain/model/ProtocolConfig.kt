package com.beatflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProtocolConfig(
    val type: String,
    val totalTimeSecs: Int,
    val inspirationSecs: Int? = null,
    val expirationSecs: Int? = null,
    val standUpSecs: Int? = null
) {
    companion object {
        const val TYPE_BASAL = "basal"
        const val TYPE_RESPIRACION = "respiracion"
        const val TYPE_ORTOSTATICO = "ortostatico"

        fun basal(totalTimeSecs: Int) = ProtocolConfig(
            type = TYPE_BASAL,
            totalTimeSecs = totalTimeSecs
        )

        fun respiracion(totalTimeSecs: Int, inspirationSecs: Int, expirationSecs: Int) = ProtocolConfig(
            type = TYPE_RESPIRACION,
            totalTimeSecs = totalTimeSecs,
            inspirationSecs = inspirationSecs,
            expirationSecs = expirationSecs
        )

        fun ortostatico(totalTimeSecs: Int, standUpSecs: Int) = ProtocolConfig(
            type = TYPE_ORTOSTATICO,
            totalTimeSecs = totalTimeSecs,
            standUpSecs = standUpSecs
        )
    }
}
