package com.beatflow.app.bluetooth

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connected(val deviceId: String) : ConnectionState()
    data class Connecting(val deviceId: String) : ConnectionState()
}

data class HrMeasurement(
    val hr: Int,
    val rr: List<Double>,
    val timestamp: Long = System.currentTimeMillis()
)

data class PolarDevice(
    val deviceId: String,
    val name: String,
    val rssi: Int? = null
)

@Singleton
class PolarManager @Inject constructor(
    private val context: Context
) {
    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context,
        PolarBleApi.ALL_FEATURES
    )

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _hrMeasurements = MutableStateFlow<HrMeasurement?>(null)
    val hrMeasurements: StateFlow<HrMeasurement?> = _hrMeasurements.asStateFlow()

    private val _ecgSamples = MutableStateFlow<List<Double>>(emptyList())
    val ecgSamples: StateFlow<List<Double>> = _ecgSamples.asStateFlow()

    private var scanDisposable: Disposable? = null
    private var ecgDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null

    private val _foundDevices = MutableStateFlow<List<PolarDevice>>(emptyList())
    val foundDevices: StateFlow<List<PolarDevice>> = _foundDevices.asStateFlow()

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                if (!powered) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun deviceConnected(device: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Connected(device.deviceId)
            }

            override fun deviceConnecting(device: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Connecting(device.deviceId)
            }

            override fun deviceDisconnected(device: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun hrNotificationReady(identifier: String) {
                hrDisposable = api.startHrStreaming(identifier)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { hrData ->
                            _hrMeasurements.value = HrMeasurement(
                                hr = hrData.hr,
                                rr = hrData.rrs.toList(),
                                timestamp = System.currentTimeMillis()
                            )
                        },
                        { _ -> }
                    )
            }

            override fun polarFirmwareVersionReceived(identifier: String, firmware: String) {}

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {}
        })
    }

    fun startScanning(): Flow<PolarDevice> = callbackFlow {
        _foundDevices.value = emptyList()

        scanDisposable = api.searchForDevice()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { deviceInfo ->
                    if (deviceInfo.name.startsWith("Polar")) {
                        val device = PolarDevice(
                            deviceId = deviceInfo.deviceId,
                            name = deviceInfo.name,
                            rssi = null
                        )
                        _foundDevices.value = _foundDevices.value + device
                        trySend(device)
                    }
                },
                { close(it) },
                { close() }
            )

        awaitClose { stopScanning() }
    }

    fun stopScanning() {
        scanDisposable?.dispose()
        scanDisposable = null
    }

    fun connectToDevice(deviceId: String) {
        api.connectToDevice(deviceId)
    }

    fun disconnect() {
        hrDisposable?.dispose()
        ecgDisposable?.dispose()
        api.disconnectFromDevice(
            when (val state = _connectionState.value) {
                is ConnectionState.Connected -> state.deviceId
                is ConnectionState.Connecting -> state.deviceId
                is ConnectionState.Disconnected -> return
            }
        )
    }

    fun startEcgStreaming(deviceId: String) {
        ecgDisposable = api.startEcgStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { ecgData ->
                    _ecgSamples.value = ecgData.samples.toList()
                },
                { _ -> }
            )
    }

    fun cleanup() {
        scanDisposable?.dispose()
        hrDisposable?.dispose()
        ecgDisposable?.dispose()
        api.shutDown()
    }
}
