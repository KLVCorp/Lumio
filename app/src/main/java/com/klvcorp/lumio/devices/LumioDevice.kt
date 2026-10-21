package com.klvcorp.lumio.devices

data class LumioDevice(
    val name: String,
    val bluetoothName: String,
    val isConnected: Boolean = false
)
