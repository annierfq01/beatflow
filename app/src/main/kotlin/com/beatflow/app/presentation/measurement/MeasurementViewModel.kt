package com.beatflow.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.bluetooth.ConnectionState
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

    val batteryLevel: StateFlow<Int> = polarManager.batteryLevel

    private var timerJob: Job? = null
    private var ecgSaveJob: Job? = null
    private var sessionStartTime: Long = 0L
    private val ecgPersistenceBuffer = mutableListOf<Double>()
    private val pendingRecords = mutableListOf<RawRecord>()
    private var lastHr = 60f
    private var lastRr = 800f
    private val hrRingBuffer = ArrayDeque<Float>()
    private val rrRingBuffer = ArrayDeque<Float>()
    private val ecgRingBuffer = ArrayDeque<Float>()

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        _isRecording.value = true
        _hrHistory.value = emptyList()
        _rrIntervals.value = emptyList()
        _ecgBuffer.value = emptyList()
        ecgPersistenceBuffer.clear()
        pendingRecords.clear()
        lastHr = 60f
        lastRr = 800f
        hrRingBuffer.clear()
        rrRingBuffer.clear()
        ecgRingBuffer.clear()
        repeat(HR_CHART_SIZE) { hrRingBuffer.addLast(lastHr) }
        repeat(RR_CHART_SIZE) { rrRingBuffer.addLast(lastRr) }
        repeat(ECG_CHART_SIZE) { ecgRingBuffer.addLast(0f) }

        val connectedDeviceId = (polarManager.connectionState.value as? ConnectionState.Connected)?.deviceId
        if (connectedDeviceId != null) {
            polarManager.startEcgStreaming(connectedDeviceId)
        }

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
                val toSave = ecgPersistenceBuffer.toList()
                ecgPersistenceBuffer.clear()
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

                    lastHr = measurement.hr.toFloat()
                    measurement.rr.forEach { rrMs ->
                        lastRr = rrMs.toFloat()
                        _rrIntervals.value = _rrIntervals.value + rrMs
                        rrRingBuffer.addLast(lastRr)
                        if (rrRingBuffer.size > RR_CHART_SIZE) rrRingBuffer.removeFirst()
                        pendingRecords.add(
                            RawRecord(
                                timestamp = measurement.timestamp,
                                hr = measurement.hr,
                                rr = rrMs,
                                ecgSignal = null
                            )
                        )
                    }
                    hrRingBuffer.addLast(lastHr)
                    if (hrRingBuffer.size > HR_CHART_SIZE) hrRingBuffer.removeFirst()

                    if (pendingRecords.size >= 10) {
                        flushRecords()
                    }
                }
            }
        }

        viewModelScope.launch {
            polarManager.ecgSamples.collect { samples ->
                if (samples.isNotEmpty()) {
                    ecgPersistenceBuffer.addAll(samples)
                    samples.forEach { value ->
                        ecgRingBuffer.addLast(value.toFloat())
                        if (ecgRingBuffer.size > ECG_CHART_SIZE) ecgRingBuffer.removeFirst()
                    }
                    _ecgBuffer.value = ecgRingBuffer.toList()
                }
            }
        }
    }

    fun stopSession(): Long {
        _isRecording.value = false
        timerJob?.cancel()
        ecgSaveJob?.cancel()
        polarManager.stopEcgStreaming()
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
        polarManager.stopEcgStreaming()
        flushRecords()
    }

    companion object {
        const val HR_CHART_SIZE = 5
        const val RR_CHART_SIZE = 5
        const val ECG_CHART_SIZE = 650
        private const val ECG_WINDOW_SIZE = 1200
    }
}
