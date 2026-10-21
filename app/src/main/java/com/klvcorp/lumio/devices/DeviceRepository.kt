package com.klvcorp.lumio.devices

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

data class SavedDevice(val address: String, val name: String, val nickname: String = "") {
    val displayName: String get() = nickname.ifBlank { name }
}

class DeviceRepository(context: Context) {
    private val prefs = context.getSharedPreferences("lumio_devices", Context.MODE_PRIVATE)
    val saved = mutableStateListOf<SavedDevice>()
    var lastAddress: String?
        get() = prefs.getString("last_address", null)
        set(value) { prefs.edit().putString("last_address", value).apply() }
    private var autoConnectState by mutableStateOf(prefs.getBoolean("auto_connect", false))
    val autoConnect: Boolean get() = autoConnectState

    fun setAutoConnect(enabled: Boolean) {
        autoConnectState = enabled
        prefs.edit().putBoolean("auto_connect", enabled).apply()
    }

    init {
        runCatching {
            val arr = JSONArray(prefs.getString("saved", "[]"))
            for (i in 0 until arr.length()) {
                val v = arr.getJSONObject(i)
                saved.add(SavedDevice(v.getString("address"), v.optString("name"), v.optString("nickname")))
            }
        }
    }

    fun add(address: String, name: String) {
        if (saved.none { it.address == address }) saved.add(SavedDevice(address, name.ifBlank { "ELK-BLEDOM" }))
        lastAddress = address; persist()
    }
    fun rename(address: String, nickname: String) {
        val index = saved.indexOfFirst { it.address == address }
        if (index != -1) { saved[index] = saved[index].copy(nickname = nickname.trim()); persist() }
    }
    fun remove(address: String) {
        saved.removeAll { it.address == address }
        if (lastAddress == address) lastAddress = null
        persist()
    }
    private fun persist() {
        val data = JSONArray()
        saved.forEach { data.put(JSONObject().put("address", it.address).put("name", it.name).put("nickname", it.nickname)) }
        prefs.edit().putString("saved", data.toString()).apply()
    }
}
