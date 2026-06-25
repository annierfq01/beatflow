package com.beatflow.app.presentation.measurement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.bluetooth.ConnectionState
import com.beatflow.app.bluetooth.HrMeasurement
import com.beatflow.app.bluetooth.PolarManager
import com.beatflow.app.data.repository.SessionRepository
import com.beatflow.app.domain.model.ProtocolConfig
import com.beatflow.app.domain.model.RawRecord
import com.beatflow.app.util.SoundPlayer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val _protocolType = MutableStateFlow("")
    val protocolType: StateFlow<String> = _protocolType.asStateFlow()

    private val _orthostaticPhase = MutableStateFlow("pre")
    val orthostaticPhase: StateFlow<String> = _orthostaticPhase.asStateFlow()

    private var timerJob: Job? = null
    private var ecgSaveJob: Job? = null
    private var protocolJob: Job? = null
    private var hrCollectorJob: Job? = null
    private var ecgCollectorJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var protocolTotalSecs = 0
    private var inspirationSecs = 5
    private var expirationSecs = 5
    private var standUpSecs = 120
    private val bufferMutex = Mutex()
    private val ecgPersistenceBuffer = mutableListOf<Double>()
    private val pendingRecords = mutableListOf<RawRecord>()
    private var lastHr = 60f
    private var lastRr = 800f
    private val hrRingBuffer = ArrayDeque<Float>()
    private val rrRingBuffer = ArrayDeque<Float>()
    private val ecgRingBuffer = ArrayDeque<Float>()

    private var currentProtocolConfig: ProtocolConfig? = null

    fun getProtocolConfig(): ProtocolConfig? = currentProtocolConfig

    fun startSession() {
        try {
            sessionStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _protocolType.value = ""
            resetBuffers()
            startStreaming()
            startTimers()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    fun startBasalSession(totalSecs: Int) {
        try {
            protocolTotalSecs = totalSecs
            inspirationSecs = 0
            expirationSecs = 0
            standUpSecs = 0
            sessionStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _protocolType.value = ProtocolConfig.TYPE_BASAL
            _protocolTimeLeft.value = totalSecs
            currentProtocolConfig = ProtocolConfig.basal(totalSecs)
            resetBuffers()
            startStreaming()
            startTimers()
            startBasalTimer()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    fun startBreathingSession(totalSecs: Int, inspSecs: Int, expSecs: Int) {
        try {
            protocolTotalSecs = totalSecs
            inspirationSecs = inspSecs
            expirationSecs = expSecs
            standUpSecs = 0
            sessionStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _protocolType.value = ProtocolConfig.TYPE_RESPIRACION
            _protocolTimeLeft.value = totalSecs
            currentProtocolConfig = ProtocolConfig.respiracion(totalSecs, inspSecs, expSecs)
            resetBuffers()
            startStreaming()
            startTimers()
            startBreathingTimer()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    fun startOrthostaticSession(totalSecs: Int, standUp: Int) {
        try {
            protocolTotalSecs = totalSecs
            inspirationSecs = 0
            expirationSecs = 0
            standUpSecs = standUp
            sessionStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _protocolType.value = ProtocolConfig.TYPE_ORTOSTATICO
            _protocolTimeLeft.value = totalSecs
            _orthostaticPhase.value = "pre"
            currentProtocolConfig = ProtocolConfig.ortostatico(totalSecs, standUp)
            resetBuffers()
            startStreaming()
            startTimers()
            startOrthostaticTimer()
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
            saveProtocolConfig()
        }
    }

    private fun saveProtocolConfig() {
        val config = currentProtocolConfig ?: return
        val sid = _sessionId ?: return
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(config)
        viewModelScope.launch {
            sessionRepository.updateProtocolConfig(sid, jsonStr)
        }
    }

    private fun startTimers() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _sessionDuration.value = System.currentTimeMillis() - sessionStartTime
            }
        }

        ecgSaveJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(500)
                val toSave: List<Double>
                bufferMutex.withLock {
                    toSave = ecgPersistenceBuffer.toList()
                    ecgPersistenceBuffer.clear()
                }
                val timestamp = System.currentTimeMillis()
                bufferMutex.withLock {
                    toSave.forEach { value ->
                        pendingRecords.add(
                            RawRecord(timestamp = timestamp, hr = null, rr = null, ecgSignal = value)
                        )
                    }
                }
                val shouldFlush: Boolean
                bufferMutex.withLock { shouldFlush = pendingRecords.size >= 50 }
                if (shouldFlush) flushRecords()
            }
        }

        hrCollectorJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                polarManager.hrMeasurements.collect { measurement ->
                    if (measurement != null) {
                        val prevHistory = _hrHistory.value
                        _hrHistory.value = prevHistory + measurement

                        lastHr = measurement.hr.toFloat()
                        bufferMutex.withLock {
                            measurement.rr.forEach { rrMs ->
                                lastRr = rrMs.toFloat()
                                _rrIntervals.value = _rrIntervals.value + rrMs
                                rrRingBuffer.addLast(lastRr)
                                if (rrRingBuffer.size > RR_BUFFER_SIZE) rrRingBuffer.removeFirst()
                                pendingRecords.add(
                                    RawRecord(timestamp = measurement.timestamp, hr = measurement.hr, rr = rrMs, ecgSignal = null)
                                )
                            }
                        }
                        _rrBuffer.value = rrRingBuffer.toList()
                        hrRingBuffer.addLast(lastHr)
                        if (hrRingBuffer.size > HR_BUFFER_SIZE) hrRingBuffer.removeFirst()
                        _hrBuffer.value = hrRingBuffer.toList()

                        val shouldFlush: Boolean
                        bufferMutex.withLock { shouldFlush = pendingRecords.size >= 10 }
                        if (shouldFlush) flushRecords()
                    }
                }
            } catch (_: Exception) { }
        }

        ecgCollectorJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                polarManager.ecgSamples.collect { samples ->
                    if (samples.isNotEmpty()) {
                        bufferMutex.withLock {
                            ecgPersistenceBuffer.addAll(samples)
                        }
                        samples.forEach { value ->
                            ecgRingBuffer.addLast(value.toFloat())
                            if (ecgRingBuffer.size > ECG_BUFFER_SIZE) ecgRingBuffer.removeFirst()
                        }
                        if (protocolTotalSecs <= 0) {
                            _ecgBuffer.value = ecgRingBuffer.map { it.toDouble() }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun startBasalTimer() {
        val totalSecs = protocolTotalSecs
        _protocolTimeLeft.value = totalSecs

        protocolJob = viewModelScope.launch {
            var remaining = totalSecs
            while (remaining > 0) {
                delay(1000)
                remaining--
                _protocolTimeLeft.value = remaining

                if (remaining <= 0) {
                    _isRecording.value = false
                    timerJob?.cancel()
                    ecgSaveJob?.cancel()
                    flushRecords()
                    _protocolCompleted.value = true
                }
            }
        }
    }

    private fun startBreathingTimer() {
        val totalSecs = protocolTotalSecs
        val inspSecs = inspirationSecs
        val expSecs = expirationSecs
        _breathingPhase.value = "INSPIRA"
        _phaseTimeLeft.value = inspSecs
        _protocolTimeLeft.value = totalSecs
        var totalLeft = totalSecs
        var isInspiration = true
        var phaseLeft = inspSecs

        viewModelScope.launch(Dispatchers.Default) {
            try { SoundPlayer.beep(context) } catch (_: Exception) { }
        }

        protocolJob = viewModelScope.launch {
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
                    viewModelScope.launch(Dispatchers.Default) {
                        try { SoundPlayer.beep(context) } catch (_: Exception) { }
                    }
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
        }
    }

    private fun startOrthostaticTimer() {
        val standUp = standUpSecs
        val totalSecs = protocolTotalSecs

        _orthostaticPhase.value = "pre"
        _phaseTimeLeft.value = standUp
        _protocolTimeLeft.value = totalSecs
        var totalLeft = totalSecs
        var remainingStandUp = standUp

        viewModelScope.launch(Dispatchers.Default) {
            try { SoundPlayer.beep(context) } catch (_: Exception) { }
        }

        protocolJob = viewModelScope.launch {
            while (totalLeft > 0) {
                delay(1000)
                totalLeft--
                remainingStandUp--
                _protocolTimeLeft.value = totalLeft

                if (remainingStandUp > 0) {
                    _phaseTimeLeft.value = remainingStandUp
                } else {
                    if (_orthostaticPhase.value != "post") {
                        _orthostaticPhase.value = "post"
                        _phaseTimeLeft.value = totalLeft
                        viewModelScope.launch(Dispatchers.Default) {
                            try { SoundPlayer.beep(context) } catch (_: Exception) { }
                        }
                    }
                    if (totalLeft > 0) {
                        _phaseTimeLeft.value = totalLeft
                    }
                }

                if (totalLeft <= 0) {
                    _isRecording.value = false
                    timerJob?.cancel()
                    ecgSaveJob?.cancel()
                    flushRecords()
                    _protocolCompleted.value = true
                }
            }
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
        val batch: List<RawRecord>
        kotlinx.coroutines.runBlocking {
            bufferMutex.withLock {
                batch = pendingRecords.toList()
                pendingRecords.clear()
            }
        }
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
