package com.klvcorp.lumio.effects

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.sin

/** Software effects; requires active BLE connection and running app. */
enum class LightEffect { STATIC, RAINBOW, BREATHING, PULSE, STROBE, CYCLE }

class EffectEngine(private val send: (Int) -> Unit) {
    var mode by mutableStateOf(LightEffect.STATIC); private set
    private var speedState by mutableStateOf(55)
    val speed: Int get() = speedState
    private var base = Color.rgb(163, 113, 255)
    private val handler = Handler(Looper.getMainLooper())
    private var task: Runnable? = null
    private var startTime = 0L
    private var lastFrame: Int? = null
    private var frameTargetMs = 0L

    fun setSpeed(value: Int) { speedState = value.coerceIn(1, 100) }
    fun updateBase(color: Int) { base = color }

    fun select(value: LightEffect) {
        stop()
        mode = value
        if (value == LightEffect.STATIC) { send(base); return }
        startTime = SystemClock.elapsedRealtime()
        frameTargetMs = startTime
        lastFrame = null
        schedule(0L)
    }

    private fun schedule(delay: Long) {
        val next = Runnable {
            val elapsed = (SystemClock.elapsedRealtime() - startTime).toDouble() / 1000.0
            val rate = 0.12 + speed / 100.0 * 0.9
            val phase = elapsed * rate
            val color = when (mode) {
                LightEffect.STATIC -> base
                LightEffect.RAINBOW -> Color.HSVToColor(floatArrayOf(((phase * 65.0) % 360).toFloat(), 1f, 1f))
                LightEffect.CYCLE -> {
                    val palette = intArrayOf(0xFFFF5276.toInt(), 0xFFFFD166.toInt(), 0xFF41EAD4.toInt(), 0xFF719BFF.toInt(), 0xFFB174FF.toInt())
                    val position = ((phase / 2.0) % palette.size).toInt()
                    val nextIdx = (position + 1) % palette.size
                    blend(palette[position], palette[nextIdx], ((phase / 2.0) % 1).toFloat())
                }
                LightEffect.BREATHING -> scale(base, (0.12 + 0.88 * ((1.0 + sin(phase * Math.PI * 2)) / 2)).toFloat())
                LightEffect.PULSE -> scale(base, if ((phase % 1.0) < 0.18) 1f else 0.18f)
                // Limit flashing to approximately <= 2 Hz on the fastest setting.
                LightEffect.STROBE -> if (((elapsed / (0.85 - speed / 100.0 * 0.6)).toInt() % 2) == 0) base else Color.BLACK
            }
            // Avoid redundant GATT traffic. Wall-clock phase keeps motion smooth even
            // if Android skips a frame while busy rendering.
            if (lastFrame != color) {
                lastFrame = color
                send(color)
            }
            if (mode != LightEffect.STATIC) {
                val period = if (mode == LightEffect.STROBE) 200L else 45L
                frameTargetMs += period
                val now = SystemClock.elapsedRealtime()
                if (frameTargetMs < now - period) frameTargetMs = now
                schedule((frameTargetMs - now).coerceAtLeast(0L))
            }
        }
        task = next; handler.postDelayed(next, delay)
    }

    private fun scale(c: Int, k: Float) = Color.rgb((Color.red(c) * k).toInt().coerceIn(0, 255),
        (Color.green(c) * k).toInt().coerceIn(0, 255), (Color.blue(c) * k).toInt().coerceIn(0, 255))
    private fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) * (1-t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1-t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1-t) + Color.blue(b) * t).toInt()
    )
    fun stop() { task?.let { handler.removeCallbacks(it) }; task = null; lastFrame = null; mode = LightEffect.STATIC }
    fun close() { stop(); handler.removeCallbacksAndMessages(null) }
}
