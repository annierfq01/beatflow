package com.beatflow.app.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.beatflow.app.data.repository.SessionRepository
import com.beatflow.app.domain.HrvCalculator
import com.beatflow.app.domain.model.HrvMetrics
import com.beatflow.app.domain.model.HrvSession
import com.beatflow.app.domain.model.PatientData
import com.beatflow.app.domain.model.ProtocolConfig
import com.beatflow.app.export.FileExporter
import kotlinx.serialization.json.Json
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val fileExporter: FileExporter
) : ViewModel() {

    private val _session = MutableStateFlow<HrvSession?>(null)
    val session: StateFlow<HrvSession?> = _session.asStateFlow()

    private val _metrics = MutableStateFlow<HrvMetrics?>(null)
    val metrics: StateFlow<HrvMetrics?> = _metrics.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sessionEntity = sessionRepository.getSession(sessionId) ?: run {
                    _error.value = "Sesión no encontrada"
                    _isLoading.value = false
                    return@launch
                }

                val patientData = sessionEntity.toPatientData() ?: run {
                    _error.value = "Datos del paciente no encontrados"
                    _isLoading.value = false
                    return@launch
                }

                val records = sessionRepository.getRecords(sessionId)
                val rrList = records.mapNotNull { it.rr }

                val hrvMetrics = if (rrList.size >= 30) {
                    HrvCalculator.calculate(rrList)
                } else {
                    _error.value = "Datos de RR insuficientes (< 30 latidos). No se pueden calcular métricas complejas."
                    null
                }

                val duration = (sessionEntity.endTime ?: sessionEntity.startTime) - sessionEntity.startTime

                val protocolConfig = sessionEntity.protocolConfigJson?.let {
                    try {
                        Json { ignoreUnknownKeys = true }
                            .decodeFromString<ProtocolConfig>(it)
                    } catch (_: Exception) { null }
                }

                _session.value = HrvSession(
                    patientData = patientData,
                    startTime = sessionEntity.startTime,
                    endTime = sessionEntity.endTime ?: sessionEntity.startTime,
                    durationMs = duration,
                    records = records,
                    metrics = hrvMetrics,
                    protocolConfig = protocolConfig
                )
                _metrics.value = hrvMetrics
            } catch (e: Exception) {
                _error.value = "Error al cargar la sesión: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun getDefaultFilename(): String {
        val session = _session.value ?: return "export.hrv"
        return fileExporter.buildFilename(session)
    }

    fun exportReportToUri(uri: Uri) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            val result = fileExporter.exportSessionToUri(currentSession, uri)
            result.onSuccess { savedUri ->
                _exportedFile.value = java.io.File(savedUri.lastPathSegment ?: "exportado")
            }.onFailure { e ->
                _error.value = "Error al guardar: ${e.message}"
            }
        }
    }
}
