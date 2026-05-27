package com.beatflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PatientData(
    val nombre: String,
    val apellidos: String,
    val edad: Int,
    val sexo: String
)
