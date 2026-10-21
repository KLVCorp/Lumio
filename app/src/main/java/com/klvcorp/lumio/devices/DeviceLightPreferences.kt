package com.klvcorp.lumio.devices

import android.content.Context

/** Independent last-requested light states for each Bluetooth controller. */
class DeviceLightPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("lumio_light_per_device", Context.MODE_PRIVATE)
    private fun key(address: String, property: String) = "${address.uppercase()}_$property"

    fun color(address: String, fallback: Int) = preferences.getInt(key(address, "color"), fallback)
    fun brightness(address: String, fallback: Int) = preferences.getInt(key(address, "brightness"), fallback)
    fun powered(address: String, fallback: Boolean) = preferences.getBoolean(key(address, "power"), fallback)
    fun saveColor(address: String, value: Int) { preferences.edit().putInt(key(address, "color"), value).apply() }
    fun saveBrightness(address: String, value: Int) { preferences.edit().putInt(key(address, "brightness"), value).apply() }
    fun savePower(address: String, value: Boolean) { preferences.edit().putBoolean(key(address, "power"), value).apply() }
}
