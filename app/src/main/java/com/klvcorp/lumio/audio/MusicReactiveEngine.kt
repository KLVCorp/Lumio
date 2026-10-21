package com.klvcorp.lumio.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.sqrt

/** Microphone-based local sound response. Does not capture internal/DRM audio. */
class MusicReactiveEngine(private val context: Context, private val sendColor: (Int) -> Unit) {
    var active by mutableStateOf(false); private set
    var level by mutableStateOf(0f); private set
    var error by mutableStateOf(""); private set
    private var record: AudioRecord? = null
    private var worker: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false

    @SuppressLint("MissingPermission")
    fun start(baseColor: Int) {
        stop()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            error = "mic_permission"; return
        }
        val rate = 8000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) { error = "audio_unavailable"; return }
        try {
            val audio = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 4096))
            if (audio.state != AudioRecord.STATE_INITIALIZED) { audio.release(); error = "audio_unavailable"; return }
            audio.startRecording(); record = audio; running = true; active = true; error = ""
            worker = Thread {
                val buffer = ShortArray(1024)
                var lastUpdate = 0L
                while (running) {
                    val n = try { audio.read(buffer, 0, buffer.size) } catch (_: Exception) { -1 }
                    if (n <= 0) break
                    var sum = 0.0
                    for (i in 0 until n) sum += buffer[i].toDouble() * buffer[i].toDouble()
                    val rms = sqrt(sum / n) / 32768.0
                    val volume = (rms * 15).coerceIn(0.0, 1.0).toFloat()
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastUpdate > 210L) {
                        lastUpdate = now
                        val r = (android.graphics.Color.red(baseColor) * volume).toInt().coerceIn(0, 255)
                        val g = (android.graphics.Color.green(baseColor) * volume).toInt().coerceIn(0, 255)
                        val b = (android.graphics.Color.blue(baseColor) * volume).toInt().coerceIn(0, 255)
                        handler.post { if (running) { level = volume; sendColor(android.graphics.Color.rgb(r, g, b)) } }
                    }
                }
                handler.post { if (running) stop() }
            }.also { it.name = "LumioMusic"; it.start() }
        } catch (e: Exception) { error = "audio_error:${e.javaClass.simpleName}"; stop() }
    }

    fun stop() {
        running = false; active = false; level = 0f
        val audio = record; record = null
        runCatching { audio?.stop(); audio?.release() }
        worker = null
    }
    fun close() { stop(); handler.removeCallbacksAndMessages(null) }
}
