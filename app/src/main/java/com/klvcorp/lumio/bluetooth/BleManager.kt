package com.klvcorp.lumio.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/** One active GATT connection; scans all nearby BLE devices. UI filters the visible list. */
data class FoundBleDevice(val address: String, val name: String, val rssi: Int)

class BleManager(private val context: Context) {
    var devices by mutableStateOf<List<FoundBleDevice>>(emptyList()); private set
    var status by mutableStateOf("idle"); private set
    var isScanning by mutableStateOf(false); private set
    var connectedAddress by mutableStateOf<String?>(null); private set
    var serviceDetails by mutableStateOf(""); private set
    var isReady by mutableStateOf(false); private set
    var commandStatus by mutableStateOf(""); private set
    var lastError by mutableStateOf(""); private set

    var onReady: ((String) -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private fun ui(fn: () -> Unit) = handler.post(fn)
    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var gatt: BluetoothGatt? = null
    private var writable: BluetoothGattCharacteristic? = null
    private val commands = ArrayDeque<Pair<String, ByteArray>>()
    private var sending = false
    private var sendTimeout: Runnable? = null
    private var scanTimeout: Runnable? = null
    // Aggregate radio advertisements; a noisy BLE environment should not trigger a
    // complete Compose list recomposition on each received advertisement.
    private val scanResults = linkedMapOf<String, FoundBleDevice>()
    private var publishScanTask: Runnable? = null
    private var lastPublishedAt = 0L
    private fun publishScanResults() {
        publishScanTask?.let(handler::removeCallbacks)
        publishScanTask = null
        devices = scanResults.values.sortedWith(
            compareByDescending<FoundBleDevice> { ElkBledomProtocol.compatible(it.name) }
                .thenByDescending { it.rssi }
        )
        lastPublishedAt = android.os.SystemClock.elapsedRealtime()
    }
    private fun queueScanPublish() {
        if (publishScanTask != null) return
        val elapsed = android.os.SystemClock.elapsedRealtime() - lastPublishedAt
        val task = Runnable { publishScanResults() }
        publishScanTask = task
        handler.postDelayed(task, (350L - elapsed).coerceAtLeast(0L))
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address
            val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull() ?: ""
            ui {
                if (!isScanning) return@ui
                val old = scanResults[address]
                val displayName = name.ifBlank { old?.name ?: "" }
                val item = FoundBleDevice(address, displayName, result.rssi)
                // Ignore repeated advertisements with immaterial signal changes.
                if (old == null || old.name != item.name || old.rssi / 5 != item.rssi / 5) {
                    scanResults[address] = item
                    queueScanPublish()
                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            ui { stopScan(); status = "scan_error:$errorCode"; lastError = status }
        }
    }

    fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31) arrayOf(
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasPermissions() = permissions().all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions()) { status = "permission_required"; return }
        val bt = adapter ?: run { status = "bluetooth_unavailable"; return }
        if (!bt.isEnabled) { status = "bluetooth_off"; return }
        stopScan()
        scanResults.clear(); devices = emptyList(); lastPublishedAt = 0L; lastError = ""
        try {
            val scanner = bt.bluetoothLeScanner ?: run { status = "scanner_unavailable"; return }
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
            isScanning = true; status = "scanning"
            val timer = Runnable { stopScan() }
            scanTimeout = timer; handler.postDelayed(timer, 15_000)
        } catch (e: Exception) { status = "scan_error:${e.javaClass.simpleName}"; lastError = status }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeout?.let { handler.removeCallbacks(it) }; scanTimeout = null
        if (isScanning) runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        isScanning = false
        publishScanTask?.let(handler::removeCallbacks); publishScanTask = null
        if (scanResults.isNotEmpty()) publishScanResults()
        if (status == "scanning") status = "idle"
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (!hasPermissions()) { status = "permission_required"; return }
        stopScan(); disconnect()
        val bt = adapter ?: run { status = "bluetooth_unavailable"; return }
        if (!bt.isEnabled) { status = "bluetooth_off"; return }
        try {
            status = "connecting"; commandStatus = ""; lastError = ""
            gatt = bt.getRemoteDevice(address).connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(client: BluetoothGatt, code: Int, state: Int) {
                    if (code == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                        ui { if (client === gatt) { connectedAddress = address; status = "discovering" } }
                        try { client.discoverServices() } catch (e: SecurityException) {
                            ui { status = "permission_required" }
                        }
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED || code != BluetoothGatt.GATT_SUCCESS) {
                        ui {
                            if (client === gatt) {
                                gatt = null; writable = null; connectedAddress = null; isReady = false
                                commands.clear(); sending = false; cancelSendTimer()
                                serviceDetails = ""; status = if (code == 0) "disconnected" else "gatt_error:$code"
                                onDisconnect?.invoke()
                            }
                            runCatching { client.close() }
                        }
                    }
                }

                override fun onServicesDiscovered(client: BluetoothGatt, code: Int) {
                    if (code != BluetoothGatt.GATT_SUCCESS) {
                        ui { if (client === gatt) { status = "service_error:$code"; lastError = status } }
                        return
                    }
                    val match = ElkBledomProtocol.knownPairs.firstNotNullOfOrNull { (serviceId, charId) ->
                        val characteristic = client.getService(serviceId)?.getCharacteristic(charId)
                        if (characteristic != null && characteristic.properties and (
                            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                        ) != 0) characteristic else null
                    }
                    val details = client.services.joinToString("\n") { service ->
                        "${shortId(service.uuid)}: " + service.characteristics.joinToString(", ") { ch ->
                            val write = (ch.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                            "${shortId(ch.uuid)}${if (write) " [WRITE]" else ""}"
                        }
                    }
                    ui {
                        if (client === gatt) {
                            writable = match; serviceDetails = details; isReady = match != null
                            status = if (match != null) "connected" else "unsupported_characteristic"
                            if (match != null) onReady?.invoke(address)
                        }
                    }
                }

                override fun onCharacteristicWrite(client: BluetoothGatt, ch: BluetoothGattCharacteristic, code: Int) {
                    ui {
                        if (client !== gatt || !sending) return@ui
                        if (code != BluetoothGatt.GATT_SUCCESS) {
                            lastError = "write_error:$code"; commandStatus = lastError
                        }
                        finishWrite()
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) { status = "connect_error:${e.javaClass.simpleName}"; lastError = status }
    }

    fun power(on: Boolean) = enqueue("power", ElkBledomProtocol.power(on))
    fun color(argb: Int) = enqueue("color", ElkBledomProtocol.rgb(argb))
    fun brightness(percent: Int) = enqueue("brightness", ElkBledomProtocol.brightness(percent))
    fun nativeRainbow(speed: Int) {
        enqueue("effect", ElkBledomProtocol.nativeRainbow())
        enqueue("effect_speed", ElkBledomProtocol.nativeEffectSpeed(speed))
    }

    /** Deduplicates pending slider/color frames to prevent flooding slower controllers. */
    private fun enqueue(kind: String, data: ByteArray) {
        if (!isReady || writable == null) { commandStatus = "not_ready"; return }
        val copy = commands.filterNot { it.first == kind }
        commands.clear(); commands.addAll(copy)
        commands.addLast(kind to data)
        if (!sending) dispatch()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun dispatch() {
        if (sending || commands.isEmpty()) return
        val client = gatt ?: run { commands.clear(); return }
        val ch = writable ?: run { commands.clear(); return }
        val (kind, data) = commands.removeFirst()
        val writeType = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        sending = true
        val accepted = try {
            if (Build.VERSION.SDK_INT >= 33) {
                client.writeCharacteristic(ch, data, writeType) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                ch.writeType = writeType
                ch.value = data
                client.writeCharacteristic(ch)
            }
        } catch (e: Exception) { lastError = "write_exception:${e.javaClass.simpleName}"; false }
        if (!accepted) { sending = false; commandStatus = "write_rejected:$kind"; handler.postDelayed({ dispatch() }, 180); return }
        // Avoid triggering a Compose recomposition for every animation frame.
        if (kind != "color") commandStatus = "sent:$kind"
        // Some controllers do not fire callbacks for write-without-response.
        val timeout = Runnable { if (sending) finishWrite() }
        sendTimeout = timeout
        handler.postDelayed(timeout, if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) 20L else 1_200L)
    }

    private fun cancelSendTimer() { sendTimeout?.let { handler.removeCallbacks(it) }; sendTimeout = null }
    private fun finishWrite() {
        cancelSendTimer(); sending = false
        handler.postDelayed({ dispatch() }, 2)
    }

    private fun shortId(id: UUID) = id.toString().let { if (it.endsWith("-0000-1000-8000-00805f9b34fb")) it.substring(4, 8).uppercase() else it }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        cancelSendTimer(); commands.clear(); sending = false
        val old = gatt; gatt = null; writable = null; connectedAddress = null; isReady = false
        serviceDetails = ""; status = "disconnected"
        runCatching { old?.disconnect(); old?.close() }
    }
    fun close() { stopScan(); disconnect(); handler.removeCallbacksAndMessages(null) }
}
