package com.beatflow.app.presentation.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.data.repository.SessionRepository
import com.beatflow.app.domain.model.PatientData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientFormViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _apellidos = MutableStateFlow("")
    val apellidos: StateFlow<String> = _apellidos.asStateFlow()

    private val _edad = MutableStateFlow("")
    val edad: StateFlow<String> = _edad.asStateFlow()

    private val _sexo = MutableStateFlow("")
    val sexo: StateFlow<String> = _sexo.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun updateNombre(value: String) { _nombre.value = value }
    fun updateApellidos(value: String) { _apellidos.value = value }
    fun updateEdad(value: String) { _edad.value = value.filter { it.isDigit() } }
    fun updateSexo(value: String) { _sexo.value = value }
    fun clearError() { _error.value = null }

    fun savePatient(sessionId: Long) {
        val nombreVal = _nombre.value.trim()
        val apellidosVal = _apellidos.value.trim()
        val edadText = _edad.value.trim()
        val sexoVal = _sexo.value.trim()

        if (nombreVal.isEmpty() || apellidosVal.isEmpty() || edadText.isEmpty() || sexoVal.isEmpty()) {
            _error.value = "Todos los campos son obligatorios"
            return
        }

        val edadNum = edadText.toIntOrNull()
        if (edadNum == null || edadNum <= 0 || edadNum > 150) {
            _error.value = "Ingrese una edad válida"
            return
        }

        val patient = PatientData(
            nombre = nombreVal,
            apellidos = apellidosVal,
            edad = edadNum,
            sexo = sexoVal
        )

        viewModelScope.launch {
            sessionRepository.finalizeSession(
                sessionId = sessionId,
                endTime = System.currentTimeMillis(),
                patient = patient
            )
            _isSaved.value = true
        }
    }
}
