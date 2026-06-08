package com.beatflow.app.presentation.measurement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.bluetooth.ConnectionState
import com.beatflow.app.bluetooth.HrMeasurement
import com.beatflow.app.bluetooth.PolarManager
import com.beatflow.app.data.repository.SessionRepository
import com.beatflow.app.domain.model.RawRecord
import com.beatflow.app.util.SoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val polarManager: PolarManager,
    private val sessionRepository: SessionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    init {
        try {
            SoundPlayer.init()
        } catch (_: Exception) { }
    }

    private var _sessionId: Long? = null

    private val _hrHistory = MutableStateFlow<List<HrMeasurement>>(emptyList())
    val hrHistory: StateFlow<List<HrMeasurement>> = _hrHistory.asStateFlow()

    private val _rrIntervals = MutableStateFlow<List<Double>>(emptyList())
    val rrIntervals: StateFlow<List<Double>> = _rrIntervals.asStateFlow()

    private val _ecgBuffer = MutableStateFlow<List<Double>>(emptyList())
    val ecgBuffer: StateFlow<List<Double>> = _ecgBuffer.asStateFlow()

    private val _hrBuffer = MutableStateFlow<List<Float>>(emptyList())
    val hrBuffer: StateFlow<List<Float>> = _hrBuffer.asStateFlow()

    private val _rrBuffer = MutableStateFlow<List<Float>>(emptyList())
    val rrBuffer: StateFlow<List<Float>> = _rrBuffer.asStateFlow()

    private val _sessionDuration = MutableStateFlow(0L)
    val sessionDuration: StateFlow<Long> = _sessionDuration.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    val batteryLevel: StateFlow<Int> = polarManager.batteryLevel

    private val _breathingPhase = MutableStateFlow("")
    val breathingPhase: StateFlow<String> = _breathingPhase.asStateFlow()

    private val _phaseTimeLeft = MutableStateFlow(0)
    val phaseTimeLeft: StateFlow<Int> = _phaseTimeLeft.asStateFlow()

    private val _protocolTimeLeft = MutableStateFlow(0)
    val protocolTimeLeft: StateFlow<Int> = _protocolTimeLeft.asStateFlow()

    private val _protocolCompleted = MutableStateFlow(false)
    val protocolCompleted: StateFlow<Boolean> = _protocolCompleted.asStateFlow()

    private var timerJob: Job? = null
    private var ecgSaveJob: Job? = null
    private var protocolJob: Job? = null
    private var hrCollectorJob: Job? = null
    private var ecgCollectorJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var protocolTotalSecs = 0
    private var inspirationSecs = 5
    private var expirationSecs = 5
    private val ecgPersistenceBuffer = mutableListOf<Double>()
    private val pendingRecords = mutableListOf<RawRecord>()
    private var lastHr = 60f
    private var lastRr = 800f
    private val hrRingBuffer = ArrayDeque<Float>()
    private val rrRingBuffer = ArrayDeque<Float>()
    private val ecgRingBuffer = ArrayDeque<Float>()

    fun startSession() {
        try {
            sessionStartTime = System.currentTimeMillis()
            _isRecording.value = true
            resetBuffers()
            startStreaming()
            startTimers()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    fun startSessionWithProtocol(totalSecs: Int, inspSecs: Int, expSecs: Int) {
        try {
            protocolTotalSecs = totalSecs
            inspirationSecs = inspSecs
            expirationSecs = expSecs
            sessionStartTime = System.currentTimeMillis()
            _isRecording.value = true
            resetBuffers()
            startStreaming()
            startTimers()
            startProtocolTimer()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private fun resetBuffers() {
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
        repeat(HR_BUFFER_SIZE) { hrRingBuffer.addLast(lastHr) }
        repeat(RR_BUFFER_SIZE) { rrRingBuffer.addLast(lastRr) }
        repeat(ECG_BUFFER_SIZE) { ecgRingBuffer.addLast(0f) }
        _ecgBuffer.value = ecgRingBuffer.map { it.toDouble() }
    }

    private fun startStreaming() {
        val connectedDeviceId = (polarManager.connectionState.value as? ConnectionState.Connected)?.deviceId
        if (connectedDeviceId != null) {
            polarManager.startHrStreaming(connectedDeviceId)
            polarManager.startEcgStreaming(connectedDeviceId)
        }

        viewModelScope.launch {
            _sessionId = sessionRepository.createSession(sessionStartTime)
        }
    }

    private fun startTimers() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _sessionDuration.value = System.currentTimeMillis() - sessionStartTime
            }
        }

        ecgSaveJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val toSave = ecgPersistenceBuffer.toList()
                ecgPersistenceBuffer.clear()
                val timestamp = System.currentTimeMillis()
                toSave.forEach { value ->
                    pendingRecords.add(
                        RawRecord(timestamp = timestamp, hr = null, rr = null, ecgSignal = value)
                    )
                }
                if (pendingRecords.size >= 50) flushRecords()
            }
        }

        hrCollectorJob = viewModelScope.launch {
            try {
                polarManager.hrMeasurements.collect { measurement ->
                    if (measurement != null) {
                        val prevHistory = _hrHistory.value
                        _hrHistory.value = prevHistory + measurement

                        lastHr = measurement.hr.toFloat()
                        measurement.rr.forEach { rrMs ->
                            lastRr = rrMs.toFloat()
                            _rrIntervals.value = _rrIntervals.value + rrMs
                            rrRingBuffer.addLast(lastRr)
                            if (rrRingBuffer.size > RR_BUFFER_SIZE) rrRingBuffer.removeFirst()
                            pendingRecords.add(
                                RawRecord(timestamp = measurement.timestamp, hr = measurement.hr, rr = rrMs, ecgSignal = null)
                            )
                        }
                        _rrBuffer.value = rrRingBuffer.toList()
                        hrRingBuffer.addLast(lastHr)
                        if (hrRingBuffer.size > HR_BUFFER_SIZE) hrRingBuffer.removeFirst()
                        _hrBuffer.value = hrRingBuffer.toList()

                        if (pendingRecords.size >= 10) flushRecords()
                    }
                }
            } catch (_: Exception) { }
        }

        ecgCollectorJob = viewModelScope.launch {
            try {
                polarManager.ecgSamples.collect { samples ->
                    if (samples.isNotEmpty()) {
                        ecgPersistenceBuffer.addAll(samples)
                        samples.forEach { value ->
                            ecgRingBuffer.addLast(value.toFloat())
                            if (ecgRingBuffer.size > ECG_BUFFER_SIZE) ecgRingBuffer.removeFirst()
                        }
                        _ecgBuffer.value = ecgRingBuffer.map { it.toDouble() }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun startProtocolTimer() {
        val totalSecs = protocolTotalSecs
        val inspSecs = inspirationSecs
        val expSecs = expirationSecs
        _breathingPhase.value = "INSPIRA"
        _phaseTimeLeft.value = inspSecs
        _protocolTimeLeft.value = totalSecs
        var totalLeft = totalSecs
        var isInspiration = true
        var phaseLeft = inspSecs

        protocolJob = viewModelScope.launch {
            try {
                SoundPlayer.beep(context)
                while (totalLeft > 0) {
                    delay(1000)
                    totalLeft--
                    phaseLeft--
                    _protocolTimeLeft.value = totalLeft
                    _phaseTimeLeft.value = phaseLeft

                    if (phaseLeft <= 0) {
                        isInspiration = !isInspiration
                        phaseLeft = if (isInspiration) inspSecs else expSecs
                        _breathingPhase.value = if (isInspiration) "INSPIRA" else "EXPIRA"
                        _phaseTimeLeft.value = phaseLeft
                        SoundPlayer.beep(context)
                    }

                    if (totalLeft <= 0) {
                        _isRecording.value = false
                        timerJob?.cancel()
                        ecgSaveJob?.cancel()
                        flushRecords()
                        _breathingPhase.value = "COMPLETADO"
                        _protocolCompleted.value = true
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun stopSession(): Long {
        _isRecording.value = false
        timerJob?.cancel()
        ecgSaveJob?.cancel()
        protocolJob?.cancel()
        hrCollectorJob?.cancel()
        ecgCollectorJob?.cancel()
        SoundPlayer.release()
        _hrHistory.value = emptyList()
        _rrIntervals.value = emptyList()
        _ecgBuffer.value = emptyList()
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
        protocolJob?.cancel()
        hrCollectorJob?.cancel()
        ecgCollectorJob?.cancel()
        polarManager.stopAllStreaming()
        SoundPlayer.release()
        flushRecords()
    }

    companion object {
        const val HR_BUFFER_SIZE = 300
        const val RR_BUFFER_SIZE = 300
        const val ECG_BUFFER_SIZE = 650
    }
}
