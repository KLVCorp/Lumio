package com.klvcorp.lumio.automation

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar
import kotlin.math.roundToInt

enum class TimerMode { OFF, FADE_OUT, WAKE_UP }
enum class ScheduleAction { TURN_ON, TURN_OFF }

/** Clock-based timer. It can be resumed using original wall-clock deadlines. */
class AutomationManager(
    private val setBrightness: (Int) -> Unit,
    private val setPower: (Boolean) -> Unit,
    private val finished: () -> Unit = {},
    private val deviceReady: () -> Boolean = { true }
) {
    var active by mutableStateOf(false); private set
    var remainingSeconds by mutableStateOf(0L); private set
    var timerMode by mutableStateOf(TimerMode.OFF); private set
    var scheduleEnabled by mutableStateOf(false); private set
    var scheduleHour by mutableStateOf(22); private set
    var scheduleMinute by mutableStateOf(0); private set
    var scheduleAction by mutableStateOf(ScheduleAction.TURN_OFF); private set
    var scheduleWeekdays by mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)); private set

    private val handler = Handler(Looper.getMainLooper())
    private var timerTask: Runnable? = null
    private var scheduleTask: Runnable? = null
    private var startAtMs = 0L
    private var endAtMs = 0L
    private var startBrightness = 100
    private var lastBrightnessApplied = -1
    private var lastScheduleStamp = ""

    fun start(mode: TimerMode, minutes: Int, initialBrightness: Int) {
        val now = System.currentTimeMillis()
        restore(mode, now, now + minutes.coerceIn(1, 180) * 60_000L, initialBrightness)
    }

    fun restore(mode: TimerMode, startMillis: Long, endMillis: Long, initialBrightness: Int) {
        cancelTimer()
        timerMode = mode
        startAtMs = startMillis
        endAtMs = endMillis.coerceAtLeast(startMillis + 1L)
        startBrightness = initialBrightness.coerceIn(1, 100)
        lastBrightnessApplied = -1
        active = true
        if (mode == TimerMode.WAKE_UP) setPower(true)
        tick()
    }

    private fun tick() {
        if (!active) return
        val now = System.currentTimeMillis()
        val remaining = (endAtMs - now).coerceAtLeast(0L)
        remainingSeconds = (remaining + 999L) / 1000L
        val duration = (endAtMs - startAtMs).coerceAtLeast(1L)
        val progress = ((now - startAtMs).toDouble() / duration).coerceIn(0.0, 1.0)
        val newBrightness = when (timerMode) {
            TimerMode.FADE_OUT -> (startBrightness * (1.0 - progress)).roundToInt().coerceIn(1, 100)
            TimerMode.WAKE_UP -> (1 + 99.0 * progress).roundToInt().coerceIn(1, 100)
            TimerMode.OFF -> null
        }
        if (deviceReady() && newBrightness != null && lastBrightnessApplied != newBrightness) {
            lastBrightnessApplied = newBrightness
            setBrightness(newBrightness)
        }
        if (remaining <= 0L && deviceReady()) {
            if (timerMode == TimerMode.WAKE_UP) setBrightness(100) else setPower(false)
            cancelTimer()
            finished()
            return
        }
        val callback = Runnable { tick() }
        timerTask = callback
        handler.postDelayed(callback, 1000L)
    }

    fun cancelTimer() {
        timerTask?.let(handler::removeCallbacks)
        timerTask = null
        active = false
        remainingSeconds = 0L
    }

    fun configureSchedule(enabled: Boolean, hour: Int, minute: Int, action: ScheduleAction, days: Set<Int>) {
        scheduleEnabled = enabled
        scheduleHour = hour.coerceIn(0, 23)
        scheduleMinute = minute.coerceIn(0, 59)
        scheduleAction = action
        scheduleWeekdays = days.filter { it in 1..7 }.toSet()
    }

    fun startScheduleClock(isReady: () -> Boolean) {
        scheduleTask?.let(handler::removeCallbacks)
        val callback = object : Runnable {
            override fun run() {
                val cal = Calendar.getInstance()
                val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
                val stamp = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}-$scheduleHour-$scheduleMinute"
                if (scheduleEnabled && scheduleWeekdays.contains(weekday) &&
                    cal.get(Calendar.HOUR_OF_DAY) == scheduleHour && cal.get(Calendar.MINUTE) == scheduleMinute &&
                    stamp != lastScheduleStamp && isReady()) {
                    lastScheduleStamp = stamp
                    setPower(scheduleAction == ScheduleAction.TURN_ON)
                }
                handler.postDelayed(this, 10_000L)
            }
        }
        scheduleTask = callback
        handler.post(callback)
    }

    fun stop() {
        cancelTimer()
        scheduleTask?.let(handler::removeCallbacks)
        scheduleTask = null
    }
}
