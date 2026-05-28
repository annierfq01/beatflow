package com.beatflow.app.bluetooth

import android.content.Context
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
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
    @ApplicationContext private val context: Context
) {
    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context,
        setOf(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
        )
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

    private var lastConnectedDeviceId: String? = null

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                if (!powered) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                lastConnectedDeviceId = polarDeviceInfo.deviceId
                _connectionState.value = ConnectionState.Connected(polarDeviceInfo.deviceId)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Connecting(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun bleSdkFeatureReady(
                identifier: String,
                feature: PolarBleApi.PolarBleSdkFeature
            ) {
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_HR) {
                    hrDisposable = api.startHrStreaming(identifier)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { hrData: PolarHrData ->
                                val sample = hrData.samples.firstOrNull()
                                if (sample != null) {
                                    _hrMeasurements.value = HrMeasurement(
                                        hr = sample.hr,
                                        rr = sample.rrsMs.map { it.toDouble() },
                                        timestamp = System.currentTimeMillis()
                                    )
                                }
                            },
                            { _ -> }
                        )
                }
            }

            override fun disInformationReceived(
                identifier: String,
                uuid: UUID,
                value: String
            ) {}

            override fun disInformationReceived(
                identifier: String,
                disInfo: com.polar.androidcommunications.api.ble.model.DisInfo
            ) {}

            override fun htsNotificationReceived(
                identifier: String,
                data: com.polar.sdk.api.model.PolarHealthThermometerData
            ) {}

            override fun batteryLevelReceived(identifier: String, level: Int) {}
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
        val deviceId = lastConnectedDeviceId ?: return
        api.disconnectFromDevice(deviceId)
    }

    fun startEcgStreaming(deviceId: String) {
        val defaultSetting = PolarSensorSetting(
            mapOf(PolarSensorSetting.SettingType.SAMPLE_RATE to 130)
        )
        ecgDisposable = api.startEcgStreaming(deviceId, defaultSetting)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { ecgData ->
                    _ecgSamples.value = ecgData.samples.map { (it as com.polar.sdk.api.model.FecgSample).ecg.toDouble() }
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
