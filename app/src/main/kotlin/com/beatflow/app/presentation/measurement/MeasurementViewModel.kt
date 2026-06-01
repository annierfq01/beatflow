package com.beatflow.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.bluetooth.HrMeasurement
import com.beatflow.app.bluetooth.PolarManager
import com.beatflow.app.data.repository.SessionRepository
import com.beatflow.app.domain.model.RawRecord
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private var lastEcgTimestamp: Long = 0L
    private var accumulatedEcgCount = 0

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
    private var lastEcgSnapshot: List<Double> = emptyList()
    private var ecgIndex: Int = 0
    private val pendingRecords = mutableListOf<RawRecord>()

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        _isRecording.value = true
        _hrHistory.value = emptyList()
        _rrIntervals.value = emptyList()
        _ecgBuffer.value = emptyList()
        lastEcgSnapshot = emptyList()
        ecgIndex = 0
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
                val ecgData = lastEcgSnapshot
                val timestamp = System.currentTimeMillis()
                if (ecgData.isNotEmpty()) {
                    val newSamples = if (ecgIndex < ecgData.size) {
                        ecgData.drop(ecgIndex)
                    } else emptyList()
                    ecgIndex = ecgData.size
                    newSamples.forEach { value ->
                        pendingRecords.add(
                            RawRecord(
                                timestamp = timestamp,
                                hr = null,
                                rr = null,
                                ecgSignal = value
                            )
                        )
                    }
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
                    _ecgBuffer.value = samples
                    lastEcgSnapshot = samples
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
}
