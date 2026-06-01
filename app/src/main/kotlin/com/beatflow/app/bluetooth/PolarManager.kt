package com.beatflow.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connected(val deviceId: String) : ConnectionState()
    data class Connecting(val deviceId: String) : ConnectionState()
    data class ConnectionFailed(val deviceId: String, val message: String = "Error de conexión") : ConnectionState()
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
    private var connectionTimeoutDisposable: Disposable? = null

    private val _foundDevices = MutableStateFlow<List<PolarDevice>>(emptyList())
    val foundDevices: StateFlow<List<PolarDevice>> = _foundDevices.asStateFlow()

    private var lastConnectedDeviceId: String? = null
    private var btReceiver: BroadcastReceiver? = null

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isLocationEnabled = MutableStateFlow(true)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                _isBluetoothEnabled.value = powered
                if (!powered) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                connectionTimeoutDisposable?.dispose()
                connectionTimeoutDisposable = null
                lastConnectedDeviceId = polarDeviceInfo.deviceId
                _connectionState.value = ConnectionState.Connected(polarDeviceInfo.deviceId)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Connecting(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                connectionTimeoutDisposable?.dispose()
                connectionTimeoutDisposable = null
                val previous = _connectionState.value
                if (previous is ConnectionState.Connecting || previous is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.ConnectionFailed(
                        deviceId = previous.deviceId,
                        message = "Conexión perdida"
                    )
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                }
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

        btReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_OFF
                        )
                        _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                    }
                }
            }
        }
        context.registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        updateLocationState()
    }

    private fun updateLocationState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            _isLocationEnabled.value =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } else {
            _isLocationEnabled.value = true
        }
    }

    fun getBondedPolarDevices(): List<PolarDevice> {
        val bonded = bluetoothAdapter?.bondedDevices ?: return emptyList()
        return bonded
            .filter { device ->
                device.type == BluetoothDevice.DEVICE_TYPE_LE ||
                        device.type == BluetoothDevice.DEVICE_TYPE_DUAL
            }
            .mapNotNull { device ->
                val name = device.name?.trim()
                if (name != null && name.contains("Polar", ignoreCase = true)) {
                    PolarDevice(
                        deviceId = device.address,
                        name = name,
                        rssi = null
                    )
                } else null
            }
    }

    fun startScanning(): Flow<PolarDevice> = callbackFlow {
        if (bluetoothAdapter?.isEnabled != true) {
            close()
            return@callbackFlow
        }

        _foundDevices.value = emptyList()

        val bonded = getBondedPolarDevices()
        bonded.forEach { device ->
            _foundDevices.value = _foundDevices.value + device
            trySend(device)
        }

        scanDisposable = api.searchForDevice()
            .observeOn(AndroidSchedulers.mainThread())
            .timeout(12, TimeUnit.SECONDS)
            .subscribe(
                { deviceInfo ->
                    val name = deviceInfo.name?.trim()
                    if (name != null && name.contains("Polar", ignoreCase = true)) {
                        val alreadyFound = _foundDevices.value.any {
                            it.deviceId == deviceInfo.deviceId
                        }
                        if (!alreadyFound) {
                            val device = PolarDevice(
                                deviceId = deviceInfo.deviceId,
                                name = name,
                                rssi = deviceInfo.rssi
                            )
                            _foundDevices.value = _foundDevices.value + device
                            trySend(device)
                        }
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
        connectionTimeoutDisposable?.dispose()
        _connectionState.value = ConnectionState.Connecting(deviceId)
        api.connectToDevice(deviceId)
        connectionTimeoutDisposable = io.reactivex.rxjava3.core.Observable
            .timer(15, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (_connectionState.value is ConnectionState.Connecting) {
                    _connectionState.value = ConnectionState.ConnectionFailed(
                        deviceId = deviceId,
                        message = "Tiempo de conexión agotado"
                    )
                }
            }
    }

    fun disconnect() {
        hrDisposable?.dispose()
        ecgDisposable?.dispose()
        val deviceId = lastConnectedDeviceId ?: return
        api.disconnectFromDevice(deviceId)
    }

    fun startEcgStreaming(deviceId: String) {
        if (ecgDisposable != null) return
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

    fun startEcgStreaming() {
        val deviceId = lastConnectedDeviceId ?: return
        startEcgStreaming(deviceId)
    }

    fun cleanup() {
        scanDisposable?.dispose()
        hrDisposable?.dispose()
        ecgDisposable?.dispose()
        connectionTimeoutDisposable?.dispose()
        btReceiver?.let { context.unregisterReceiver(it) }
        btReceiver = null
        api.shutDown()
    }

    fun refreshLocationState() {
        updateLocationState()
    }
}
