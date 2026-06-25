package com.beatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.beatflow.app.domain.model.PatientData

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val patientNombre: String? = null,
    val patientApellidos: String? = null,
    val patientEdad: Int? = null,
    val patientSexo: String? = null,
    val exported: Boolean = false,
    val protocolConfigJson: String? = null
) {
    fun toPatientData(): PatientData? {
        if (patientNombre == null || patientApellidos == null || patientEdad == null || patientSexo == null) {
            return null
        }
        return PatientData(
            nombre = patientNombre,
            apellidos = patientApellidos,
            edad = patientEdad,
            sexo = patientSexo
        )
    }
}
