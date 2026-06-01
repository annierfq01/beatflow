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
    val isBluetoothEnabled: StateFlow<Boolean> = polarManager.isBluetoothEnabled
    val isLocationEnabled: StateFlow<Boolean> = polarManager.isLocationEnabled

    private val _foundDevices = MutableStateFlow<List<PolarDevice>>(emptyList())
    val foundDevices: StateFlow<List<PolarDevice>> = _foundDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    fun startScan() {
        if (_isScanning.value) return
        _scanMessage.value = null

        if (!polarManager.isBluetoothEnabled.value) {
            _scanMessage.value = "BLUETOOTH_OFF"
            return
        }

        if (!polarManager.isLocationEnabled.value) {
            _scanMessage.value = "LOCATION_OFF"
            return
        }

        _isScanning.value = true
        _foundDevices.value = emptyList()

        viewModelScope.launch {
            polarManager.startScanning().collect { device ->
                _foundDevices.value = _foundDevices.value + device
            }
            _isScanning.value = false

            if (_foundDevices.value.isEmpty() && _scanMessage.value == null) {
                _scanMessage.value = "NO_DEVICES"
            }
        }
    }

    fun stopScan() {
        polarManager.stopScanning()
        _isScanning.value = false
    }

    fun dismissMessage() {
        _scanMessage.value = null
    }

    fun connectToDevice(deviceId: String) {
        _foundDevices.value = emptyList()
        polarManager.connectToDevice(deviceId)
    }

    fun dismissConnectionError() {
        if (connectionState.value is ConnectionState.ConnectionFailed) {
            polarManager.disconnect()
        }
    }

    fun refreshLocationState() {
        polarManager.refreshLocationState()
    }

    override fun onCleared() {
        super.onCleared()
        polarManager.cleanup()
    }
}
