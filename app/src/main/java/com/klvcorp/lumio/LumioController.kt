package com.klvcorp.lumio

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klvcorp.lumio.automation.AutomationManager
import com.klvcorp.lumio.automation.ScheduleAction
import com.klvcorp.lumio.automation.TimerMode
import com.klvcorp.lumio.audio.MusicReactiveEngine
import com.klvcorp.lumio.bluetooth.BleManager
import com.klvcorp.lumio.devices.DeviceLightPreferences
import com.klvcorp.lumio.devices.DeviceRepository
import com.klvcorp.lumio.devices.FavoriteColorRepository
import com.klvcorp.lumio.devices.SceneRepository
import com.klvcorp.lumio.effects.EffectEngine
import com.klvcorp.lumio.effects.LightEffect

class LumioController(private val context: Context) {
    private val prefs = context.getSharedPreferences("lumio_preferences", Context.MODE_PRIVATE)
    private val perDevice = DeviceLightPreferences(context)
    val ble = BleManager(context.applicationContext)
    val devices = DeviceRepository(context.applicationContext)
    val scenes = SceneRepository(context.applicationContext)
    val favorites = FavoriteColorRepository(context.applicationContext)
    val effects = EffectEngine { frame -> if (powered && ble.isReady) ble.color(frame) }
    val music = MusicReactiveEngine(context.applicationContext) { frame -> if (powered && ble.isReady) ble.color(frame) }

    var color by mutableStateOf(prefs.getInt("last_color", Color.rgb(170, 122, 255))); private set
    private var brightnessState by mutableStateOf(prefs.getInt("last_brightness", 80).coerceIn(1, 100))
    val brightness get() = brightnessState
    var powered by mutableStateOf(prefs.getBoolean("last_power", true)); private set
    private var languageState by mutableStateOf(prefs.getString("language", "system") ?: "system")
    val language get() = languageState
    private var themeState by mutableStateOf(prefs.getString("theme", "dark") ?: "dark")
    val theme get() = themeState
    private var backgroundState by mutableStateOf(prefs.getBoolean("keep_running", false))
    val backgroundEnabled get() = backgroundState

    val automation = AutomationManager(
        setBrightness = { value ->
            // Timer-driven updates affect hardware but should not overwrite the user's chosen baseline.
            brightnessState = value
            if (ble.isReady) ble.brightness(value)
        },
        setPower = { value -> power(value) },
        finished = { clearStoredTimer() },
        deviceReady = { ble.isReady }
    )

    init {
        ble.onReady = { address ->
            val discoveredName = ble.devices.firstOrNull { it.address == address }?.name.orEmpty()
            devices.add(address, discoveredName)
            // Only refresh UI values. Never assume the physical LED is in this state.
            color = perDevice.color(address, color)
            brightnessState = perDevice.brightness(address, brightness).coerceIn(1, 100)
            powered = perDevice.powered(address, powered)
            effects.stop(); music.stop()
            restorePendingTimer()
        }
        ble.onDisconnect = {
            effects.stop(); music.stop()
            // Keep the wall-clock deadline in memory and storage while disconnected.
            // The timer will apply the final command after reconnection.
        }
        // Populate timer UI immediately after app relaunch, not only after BLE reconnect.
        restorePendingTimer()
        restoreSchedule()
        automation.startScheduleClock { ble.isReady }
    }

    fun reconnectIfEnabled() {
        if (devices.autoConnect && ble.hasPermissions() &&
            ble.connectedAddress == null && ble.status != "connecting" && ble.status != "discovering") {
            devices.lastAddress?.let { address -> runCatching { ble.connect(address) } }
        }
    }

    private fun rememberActivePower(on: Boolean) {
        prefs.edit().putBoolean("last_power", on).apply()
        ble.connectedAddress?.let { perDevice.savePower(it, on) }
    }
    fun power(on: Boolean) {
        powered = on
        rememberActivePower(on)
        if (!on) { effects.stop(); music.stop() }
        if (ble.isReady) {
            ble.power(on)
            if (on) { ble.color(color); ble.brightness(brightness) }
        }
    }

    fun chooseColor(c: Int) {
        effects.stop(); music.stop()
        color = c or 0xFF000000.toInt()
        prefs.edit().putInt("last_color", color).apply()
        ble.connectedAddress?.let { perDevice.saveColor(it, color) }
        if (ble.isReady) ble.color(color)
    }

    fun setBrightness(level: Int) {
        brightnessState = level.coerceIn(1, 100)
        prefs.edit().putInt("last_brightness", brightness).apply()
        ble.connectedAddress?.let { perDevice.saveBrightness(it, brightness) }
        if (ble.isReady) ble.brightness(brightness)
    }

    fun chooseEffect(effect: LightEffect) {
        music.stop(); cancelSavedTimer()
        if (!powered) power(true)
        effects.updateBase(color)
        effects.select(effect)
    }

    fun startMicrophone() {
        effects.stop(); cancelSavedTimer()
        if (!powered) power(true)
        music.start(color)
    }
    fun stopMicrophone() { music.stop(); if (ble.isReady) ble.color(color) }

    /** Experimental native effect; works only on some ELK firmware revisions. */
    fun nativeRainbow() {
        effects.stop(); music.stop(); cancelSavedTimer()
        if (!powered) power(true)
        ble.nativeRainbow(effects.speed)
    }

    fun startTimer(mode: TimerMode, minutes: Int) {
        if (!ble.isReady) return
        music.stop(); effects.stop()
        val now = System.currentTimeMillis()
        val end = now + minutes.coerceIn(1, 180) * 60_000L
        val initial = brightness
        prefs.edit()
            .putString("pending_timer_mode", mode.name)
            .putLong("pending_timer_start", now)
            .putLong("pending_timer_end", end)
            .putInt("pending_timer_initial_brightness", initial)
            .apply()
        automation.restore(mode, now, end, initial)
    }

    private fun restorePendingTimer() {
        val endAt = prefs.getLong("pending_timer_end", 0L)
        if (endAt <= 0L) return
        val mode = runCatching {
            TimerMode.valueOf(prefs.getString("pending_timer_mode", "OFF") ?: "OFF")
        }.getOrDefault(TimerMode.OFF)
        val initial = prefs.getInt("pending_timer_initial_brightness", brightness).coerceIn(1, 100)
        val startAt = prefs.getLong("pending_timer_start", endAt - 60_000L)
        // A deadline may have passed while Bluetooth was disconnected. Do not
        // discard it: the automation engine will deliver the final command
        // once the controller is connected again.
        automation.restore(mode, startAt.coerceAtMost(endAt - 1L), endAt, initial)
    }

    private fun clearStoredTimer() {
        prefs.edit().remove("pending_timer_start").remove("pending_timer_end")
            .remove("pending_timer_mode").remove("pending_timer_initial_brightness").apply()
    }
    fun cancelSavedTimer() { automation.cancelTimer(); clearStoredTimer() }

    fun setSchedule(enabled: Boolean, hour: Int, minute: Int, action: ScheduleAction, days: Set<Int>) {
        automation.configureSchedule(enabled, hour, minute, action, days)
        prefs.edit().putBoolean("schedule_enabled", enabled)
            .putInt("schedule_hour", hour).putInt("schedule_minute", minute)
            .putString("schedule_action", action.name)
            .putString("schedule_days", days.sorted().joinToString(",")).apply()
    }
    private fun restoreSchedule() {
        val days = (prefs.getString("schedule_days", "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7")
            .split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val action = runCatching {
            ScheduleAction.valueOf(prefs.getString("schedule_action", "TURN_OFF") ?: "TURN_OFF")
        }.getOrDefault(ScheduleAction.TURN_OFF)
        automation.configureSchedule(prefs.getBoolean("schedule_enabled", false), prefs.getInt("schedule_hour", 22),
            prefs.getInt("schedule_minute", 0), action, days)
    }

    fun setBackgroundEnabled(value: Boolean) {
        backgroundState = value
        prefs.edit().putBoolean("keep_running", value).apply()
        val service = Intent(context, LumioBackgroundService::class.java)
        if (value && ble.hasPermissions()) {
            try { context.startForegroundService(service) } catch (_: Exception) {
                backgroundState = false
                prefs.edit().putBoolean("keep_running", false).apply()
            }
        } else if (!value) context.stopService(service)
    }

    fun setLanguage(value: String) { languageState = value; prefs.edit().putString("language", value).apply() }
    fun setTheme(value: String) { themeState = value; prefs.edit().putString("theme", value).apply() }
    fun close() { automation.stop(); music.close(); effects.close(); ble.close() }
}
