package com.beatflow.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.bluetooth.HrMeasurement
import com.beatflow.app.bluetooth.PolarManager
import com.beatflow.app.data.repository.SessionRepository
import com.beatflow.app.domain.model.RawRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.concurrent.synchronized
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val polarManager: PolarManager,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private var _sessionId: Long? = null

    private val _hrHistory = MutableStateFlow<List<HrMeasurement>>(emptyList())
    val hrHistory: StateFlow<List<HrMeasurement>> = _hrHistory.asStateFlow()

    private val _rrIntervals = MutableStateFlow<List<Double>>(emptyList())
    val rrIntervals: StateFlow<List<Double>> = _rrIntervals.asStateFlow()

    private val _ecgBuffer = MutableStateFlow<List<Double>>(emptyList())
    val ecgBuffer: StateFlow<List<Double>> = _ecgBuffer.asStateFlow()

    private val _sessionDuration = MutableStateFlow(0L)
    val sessionDuration: StateFlow<Long> = _sessionDuration.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var timerJob: Job? = null
    private var ecgSaveJob: Job? = null
    private var sessionStartTime: Long = 0L
    private val ecgDisplayBuffer = mutableListOf<Double>()
    private val ecgPersistenceBuffer = mutableListOf<Double>()
    private val pendingRecords = mutableListOf<RawRecord>()

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        _isRecording.value = true
        _hrHistory.value = emptyList()
        _rrIntervals.value = emptyList()
        _ecgBuffer.value = emptyList()
        ecgDisplayBuffer.clear()
        ecgPersistenceBuffer.clear()
        pendingRecords.clear()
        polarManager.startEcgStreaming()

        viewModelScope.launch {
            _sessionId = sessionRepository.createSession(sessionStartTime)
        }

        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _sessionDuration.value = System.currentTimeMillis() - sessionStartTime
            }
        }

        ecgSaveJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(500)
                val toSave = synchronized(ecgPersistenceBuffer) {
                    val batch = ecgPersistenceBuffer.toList()
                    ecgPersistenceBuffer.clear()
                    batch
                }
                val timestamp = System.currentTimeMillis()
                toSave.forEach { value ->
                    pendingRecords.add(
                        RawRecord(
                            timestamp = timestamp,
                            hr = null,
                            rr = null,
                            ecgSignal = value
                        )
                    )
                }
                if (pendingRecords.size >= 50) {
                    flushRecords()
                }
            }
        }

        viewModelScope.launch {
            polarManager.hrMeasurements.collect { measurement ->
                if (measurement != null) {
                    val prevHistory = _hrHistory.value
                    _hrHistory.value = prevHistory + measurement

                    measurement.rr.forEach { rrMs ->
                        _rrIntervals.value = _rrIntervals.value + rrMs
                        pendingRecords.add(
                            RawRecord(
                                timestamp = measurement.timestamp,
                                hr = measurement.hr,
                                rr = rrMs,
                                ecgSignal = null
                            )
                        )
                    }

                    if (pendingRecords.size >= 10) {
                        flushRecords()
                    }
                }
            }
        }

        viewModelScope.launch {
            polarManager.ecgSamples.collect { samples ->
                if (samples.isNotEmpty()) {
                    ecgDisplayBuffer.addAll(samples)
                    synchronized(ecgPersistenceBuffer) {
                        ecgPersistenceBuffer.addAll(samples)
                    }
                    while (ecgDisplayBuffer.size > ECG_WINDOW_SIZE) {
                        ecgDisplayBuffer.removeFirst()
                    }
                    _ecgBuffer.value = ecgDisplayBuffer.toList()
                }
            }
        }
    }

    fun stopSession(): Long {
        _isRecording.value = false
        timerJob?.cancel()
        ecgSaveJob?.cancel()
        flushRecords()
        return _sessionId ?: -1L
    }

    private fun flushRecords() {
        val sid = _sessionId ?: return
        val batch = pendingRecords.toList()
        pendingRecords.clear()
        viewModelScope.launch {
            sessionRepository.addRecords(sid, batch)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        ecgSaveJob?.cancel()
        flushRecords()
    }

    companion object {
        private const val ECG_WINDOW_SIZE = 800
    }
}
