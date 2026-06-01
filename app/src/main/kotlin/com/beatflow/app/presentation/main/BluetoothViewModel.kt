package com.beatflow.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatflow.app.bluetooth.ConnectionState
import com.beatflow.app.bluetooth.PolarDevice
import com.beatflow.app.bluetooth.PolarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val polarManager: PolarManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = polarManager.connectionState

    private val _foundDevices = MutableStateFlow<List<PolarDevice>>(emptyList())
    val foundDevices: StateFlow<List<PolarDevice>> = _foundDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _foundDevices.value = emptyList()

        viewModelScope.launch {
            polarManager.startScanning().collect { device ->
                _foundDevices.value = _foundDevices.value + device
            }
            _isScanning.value = false
        }
    }

    fun stopScan() {
        polarManager.stopScanning()
        _isScanning.value = false
    }

    fun connectToDevice(deviceId: String) {
        polarManager.connectToDevice(deviceId)
    }

    override fun onCleared() {
        super.onCleared()
        polarManager.cleanup()
    }
}
